/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import {
    ArrayValueModel as A,
    ArrayValueModelConstants,
    ArrayValueModelMinSize,
    BooleanModel,
    ServerToClientModel as K,
    ServerToClientModelNames,
    ServerToClientModelTypeModel,
    ValueTypeModel as V,
    ValueTypeModelConstants as T,
} from '#model';

/** {@link Reader.blockEnd} when the buffer does not hold the whole block yet. */
export const INCOMPLETE = -1;

/** The stream does not follow the protocol. */
export class ProtocolError extends Error {
    override readonly name = 'ProtocolError';
}

/** Thrown while scanning past the bytes received so far: the block is completed by a later message. */
class Incomplete extends Error {
}

const INCOMPLETE_ERROR = new Incomplete();
const UTF8 = new TextDecoder();

/**
 * ASCII strings up to this length are built with String.fromCharCode, longer ones with TextDecoder. Measured by the
 * GWT terminal (fromCharCode / TextDecoder, ns): at 16 bytes Chrome 36/49, Firefox 88/88, WebKit 110/63.
 */
const ASCII_FROM_CHAR_CODE_MAX_LENGTH = 16;

/**
 * Reads the binary stream of the server. A model is a key byte (ServerToClientModel), then a value encoded as the
 * key's ValueTypeModel says; a block is a run of models closed by END.
 *
 * {@link read} leaves the key and its value in fields, typed by the value's type, instead of allocating a model per
 * value: {@link int} for BYTE, SHORT, INTEGER and UINT31, {@link num} for LONG, DOUBLE and FLOAT, {@link bool},
 * {@link str} and {@link array}.
 */
export class Reader {
    /** ServerToClientModel of the last model read. */
    key = -1;
    int = 0;
    num = 0;
    bool = false;
    /** Null for an empty string, as the server writes null and "" alike. */
    str: string | null = null;
    array: unknown[] = [];

    private bytes: Uint8Array = new Uint8Array(0);
    private view: DataView = new DataView(this.bytes.buffer);
    private position = 0;
    private end = 0;
    /** Bytes of the last model read, for {@link rewind}. */
    private size = 0;
    private readonly dictionary: string[] = [];
    private readonly fallback: ((id: number) => string | undefined) | undefined;

    /** @param fallback where an id this reader never received is looked up: a child window asks its opener. */
    constructor(fallback?: (id: number) => string | undefined) {
        this.fallback = fallback;
    }

    /** Appends a message to what is left unread of the previous one. */
    feed(message: Uint8Array): void {
        if (this.position < this.end) {
            const merged = new Uint8Array(this.end - this.position + message.byteLength);
            merged.set(this.bytes.subarray(this.position, this.end), 0);
            merged.set(message, this.end - this.position);
            message = merged;
        }
        this.bytes = message;
        this.view = new DataView(message.buffer, message.byteOffset, message.byteLength);
        this.position = 0;
        this.end = message.byteLength;
    }

    /** The last value read as a number, whichever field its type uses. */
    get number(): number {
        const type = ServerToClientModelTypeModel[this.key];
        return type === V.LONG || type === V.DOUBLE || type === V.FLOAT ? this.num : this.int;
    }

    get offset(): number {
        return this.position;
    }

    set offset(position: number) {
        this.position = position;
    }

    hasKey(): boolean {
        return this.position < this.end;
    }

    /** Reads the next model; returns its key. */
    read(): number {
        const start = this.position;
        const key = this.uint8();
        this.key = key;
        switch (ServerToClientModelTypeModel[key]) {
            case V.NULL:
                break;
            case V.BOOLEAN:
                this.bool = this.uint8() === BooleanModel.TRUE;
                break;
            case V.BYTE:
                this.int = this.int8();
                break;
            case V.SHORT:
                this.int = this.int16();
                break;
            case V.INTEGER:
                this.int = this.int32();
                break;
            case V.UINT31:
                this.int = this.uint31();
                break;
            case V.LONG:
                this.num = this.long();
                break;
            case V.DOUBLE:
                this.num = this.need(8).getFloat64(this.advance(8));
                break;
            case V.FLOAT:
                this.num = this.need(4).getFloat32(this.advance(4));
                break;
            case V.STRING:
                this.str = this.string();
                break;
            case V.ARRAY:
                this.array = this.arrayValue();
                break;
            default:
                throw new ProtocolError(`Unknown model key ${key} at byte ${start}`);
        }
        this.size = this.position - start;
        return key;
    }

    /** Steps back before the last model read: it was the start of something else. */
    rewind(): void {
        this.position -= this.size;
        this.size = 0;
    }

    /** Where the current block ends (after its END), or INCOMPLETE; the position does not move. */
    blockEnd(): number {
        const start = this.position;
        const end = this.scanBlock(false);
        this.position = start;
        return end;
    }

    /** Moves past the current block, keeping the dictionary entries it adds. */
    skipBlock(): void {
        const start = this.position;
        if (this.scanBlock(true) === INCOMPLETE) this.position = start;
    }

    /**
     * The bytes from start to end, handed to another terminal (a window, a frame); the position moves to end. Their
     * dictionary entries are kept here too: the server shares one dictionary between all the windows of a context.
     */
    slice(start: number, end: number): Uint8Array {
        this.position = start;
        while (this.position < end) this.skipModel(true);
        return this.bytes.subarray(start, end);
    }

    /** The name of a model key, for messages. */
    static keyName(key: number): string {
        return ServerToClientModelNames[key] ?? `#${key}`;
    }

    /** The dictionary entry an id names. */
    entry(id: number): string {
        const value = this.dictionary[id] ?? this.fallback?.(id);
        if (value === undefined) throw new ProtocolError(`Unknown dictionary id ${id}`);
        return value;
    }

    // ---- values ----------------------------------------------------------------------------------------------------

    private need(bytes: number): DataView {
        if (this.position + bytes > this.end) throw INCOMPLETE_ERROR;
        return this.view;
    }

    private advance(bytes: number): number {
        const position = this.position;
        this.position += bytes;
        return position;
    }

    private uint8(): number {
        return this.need(1).getUint8(this.position++);
    }

    private int8(): number {
        return this.need(1).getInt8(this.position++);
    }

    private int16(): number {
        return this.need(2).getInt16(this.advance(2));
    }

    private uint16(): number {
        return this.need(2).getUint16(this.advance(2));
    }

    private int32(): number {
        return this.need(4).getInt32(this.advance(4));
    }

    /** Two bytes below 32768, else four with the high bit set. */
    private uint31(): number {
        const high = this.int16();
        if (high >= 0) return high;
        return ((high << 16) | this.uint16()) & 0x7fffffff;
    }

    /** Exact up to 2^53, which covers what the server sends as long: timestamps, ids, delays. */
    private long(): number {
        const view = this.need(8);
        const position = this.advance(8);
        return view.getInt32(position) * 4294967296 + view.getUint32(position + 4);
    }

    private string(): string | null {
        const tag = this.uint8();
        if (tag === T.STRING_DICTIONARY_REF) return this.entry(this.uint31());
        if (tag === T.STRING_DICTIONARY_ADD) {
            const id = this.uint31();
            const value = this.inlineString(this.uint8()) ?? '';
            this.dictionary[id] = value;
            return value;
        }
        return this.inlineString(tag);
    }

    /** A string given its length byte: the length itself up to STRING_ASCII_UINT8, else a tag. */
    private inlineString(tag: number): string | null {
        if (tag <= T.STRING_ASCII_UINT8) return this.text(true, tag);
        if (tag === T.STRING_ASCII_UINT16) return this.text(true, this.uint16());
        if (tag === T.STRING_ASCII_UINT32) return this.text(true, this.int32());
        if (tag === T.STRING_UTF8_UINT8) return this.text(false, this.uint8());
        if (tag === T.STRING_UTF8_UINT16) return this.text(false, this.uint16());
        return this.text(false, this.int32());
    }

    private text(ascii: boolean, length: number): string | null {
        return length === 0 ? null : this.chars(ascii, length);
    }

    private chars(ascii: boolean, length: number): string {
        this.need(length);
        const position = this.advance(length);
        if (ascii && length <= ASCII_FROM_CHAR_CODE_MAX_LENGTH) return fromCharCodes(this.bytes, position, length);
        return UTF8.decode(this.bytes.subarray(position, position + length));
    }

    private arrayValue(): unknown[] {
        let length = this.uint8();
        if (length === ArrayValueModelConstants.LENGTH_UINT31_ESCAPE) length = this.uint31();
        const array = new Array<unknown>(length);
        for (let i = 0; i < length; i++) array[i] = this.arrayElement();
        return array;
    }

    private arrayElement(): unknown {
        const type = this.int8();
        switch (type) {
            case A.NULL:
                return null;
            case A.BOOLEAN_FALSE:
                return false;
            case A.BOOLEAN_TRUE:
                return true;
            case A.BYTE:
                return this.int8();
            case A.SHORT:
                return this.int16();
            case A.INTEGER:
                return this.int32();
            case A.LONG:
                return this.long();
            case A.DOUBLE:
                return this.need(8).getFloat64(this.advance(8));
            case A.FLOAT:
                return this.need(4).getFloat32(this.advance(4));
            case A.STRING_DICTIONARY_REF:
                return json(this.entry(this.uint31()));
            case A.STRING_DICTIONARY_ADD: {
                const id = this.uint31();
                const value = this.arrayString(this.int8());
                this.dictionary[id] = value;
                return json(value);
            }
            default:
                return json(this.arrayString(type));
        }
    }

    /** An inline string element of an array: unlike a model value, an empty one is "". */
    private arrayString(type: number): string {
        switch (type) {
            case A.STRING_ASCII_UINT8_LENGTH:
                return this.chars(true, this.uint8());
            case A.STRING_ASCII_UINT16_LENGTH:
                return this.chars(true, this.uint16());
            case A.STRING_UTF8_UINT8_LENGTH:
                return this.chars(false, this.uint8());
            case A.STRING_UTF8_UINT16_LENGTH:
                return this.chars(false, this.uint16());
            case A.STRING_UTF8_UINT32_LENGTH:
                return this.chars(false, this.int32());
            default:
                throw new ProtocolError(`Unknown array element type ${type}`);
        }
    }

    // ---- scanning ----------------------------------------------------------------------------------------------------

    /** Moves past the models up to END included; returns where it stopped, or INCOMPLETE. */
    private scanBlock(keepEntries: boolean): number {
        try {
            while (this.position < this.end) {
                if (this.skipModel(keepEntries) === K.END) return this.position;
            }
        } catch (e) {
            if (!(e instanceof Incomplete)) throw e;
        }
        return INCOMPLETE;
    }

    /**
     * Moves past one model without building its value. Strings added to the dictionary are decoded and kept when
     * asked: a skipped block may add an entry a later block refers to.
     */
    private skipModel(keepEntries: boolean): number {
        const key = this.uint8();
        switch (ServerToClientModelTypeModel[key]) {
            case V.NULL:
                break;
            case V.BOOLEAN:
            case V.BYTE:
                this.advance(1);
                break;
            case V.SHORT:
                this.advance(2);
                break;
            case V.INTEGER:
            case V.FLOAT:
                this.advance(4);
                break;
            case V.LONG:
            case V.DOUBLE:
                this.advance(8);
                break;
            case V.UINT31:
                this.uint31();
                break;
            case V.STRING:
                this.skipString(keepEntries);
                break;
            case V.ARRAY:
                this.skipArray(keepEntries);
                break;
            default:
                throw new ProtocolError(`Unknown model key ${key} at byte ${this.position - 1}`);
        }
        this.need(0);
        return key;
    }

    private skipString(keepEntries: boolean): void {
        const tag = this.uint8();
        if (tag === T.STRING_DICTIONARY_REF) {
            this.uint31();
        } else if (tag === T.STRING_DICTIONARY_ADD) {
            const id = this.uint31();
            const start = this.position;
            const length = this.uint8();
            if (keepEntries) {
                this.dictionary[id] = this.inlineString(length) ?? '';
            } else {
                this.position = start;
                this.skipString(false);
            }
        } else if (tag > T.STRING_ASCII_UINT8) {
            const length = tag === T.STRING_UTF8_UINT8 ? this.uint8()
                : tag === T.STRING_ASCII_UINT16 || tag === T.STRING_UTF8_UINT16 ? this.uint16() : this.int32();
            this.advance(length);
        } else {
            this.advance(tag);
        }
    }

    private skipArray(keepEntries: boolean): void {
        let length = this.uint8();
        if (length === ArrayValueModelConstants.LENGTH_UINT31_ESCAPE) length = this.uint31();
        for (let i = 0; i < length; i++) {
            const type = this.int8();
            if (type === A.STRING_DICTIONARY_REF) {
                this.uint31();
            } else if (type === A.STRING_DICTIONARY_ADD) {
                const id = this.uint31();
                if (keepEntries) this.dictionary[id] = this.arrayString(this.int8());
                else this.skipArrayString(this.int8());
            } else if (type >= A.STRING_ASCII_UINT8_LENGTH) {
                this.skipArrayString(type);
            } else {
                this.advance(ArrayValueModelMinSize[type] ?? 0); // the size of a fixed-size element
            }
        }
    }

    private skipArrayString(type: number): void {
        const length = type === A.STRING_ASCII_UINT8_LENGTH || type === A.STRING_UTF8_UINT8_LENGTH ? this.uint8()
            : type === A.STRING_UTF8_UINT32_LENGTH ? this.int32() : this.uint16();
        this.advance(length);
    }
}

/** A string element that holds JSON (an object or an array) is delivered parsed, as the GWT terminal did. */
function json(value: string): unknown {
    const first = value.charCodeAt(0);
    if (first !== 0x7b && first !== 0x5b) return value; // { [
    try {
        return JSON.parse(value);
    } catch {
        return value;
    }
}

/**
 * A short ASCII string from its bytes, passed as arguments: no view and no apply(). GWT terminal measurements, 6 to
 * 16 bytes against apply(subarray): Chrome 65-83 → 11-20 ns, Firefox 122-159 → 24-37 ns, WebKit 122-183 → 24-37 ns.
 */
function fromCharCodes(b: Uint8Array, p: number, n: number): string {
    const c = String.fromCharCode;
    switch (n) {
        case 1: return c(b[p]!);
        case 2: return c(b[p]!, b[p + 1]!);
        case 3: return c(b[p]!, b[p + 1]!, b[p + 2]!);
        case 4: return c(b[p]!, b[p + 1]!, b[p + 2]!, b[p + 3]!);
        case 5: return c(b[p]!, b[p + 1]!, b[p + 2]!, b[p + 3]!, b[p + 4]!);
        case 6: return c(b[p]!, b[p + 1]!, b[p + 2]!, b[p + 3]!, b[p + 4]!, b[p + 5]!);
        case 7: return c(b[p]!, b[p + 1]!, b[p + 2]!, b[p + 3]!, b[p + 4]!, b[p + 5]!, b[p + 6]!);
        case 8: return c(b[p]!, b[p + 1]!, b[p + 2]!, b[p + 3]!, b[p + 4]!, b[p + 5]!, b[p + 6]!, b[p + 7]!);
        case 9: return c(b[p]!, b[p + 1]!, b[p + 2]!, b[p + 3]!, b[p + 4]!, b[p + 5]!, b[p + 6]!, b[p + 7]!, b[p + 8]!);
        case 10: return c(b[p]!, b[p + 1]!, b[p + 2]!, b[p + 3]!, b[p + 4]!, b[p + 5]!, b[p + 6]!, b[p + 7]!, b[p + 8]!, b[p + 9]!);
        case 11: return c(b[p]!, b[p + 1]!, b[p + 2]!, b[p + 3]!, b[p + 4]!, b[p + 5]!, b[p + 6]!, b[p + 7]!, b[p + 8]!, b[p + 9]!, b[p + 10]!);
        case 12: return c(b[p]!, b[p + 1]!, b[p + 2]!, b[p + 3]!, b[p + 4]!, b[p + 5]!, b[p + 6]!, b[p + 7]!, b[p + 8]!, b[p + 9]!, b[p + 10]!, b[p + 11]!);
        case 13: return c(b[p]!, b[p + 1]!, b[p + 2]!, b[p + 3]!, b[p + 4]!, b[p + 5]!, b[p + 6]!, b[p + 7]!, b[p + 8]!, b[p + 9]!, b[p + 10]!, b[p + 11]!, b[p + 12]!);
        case 14: return c(b[p]!, b[p + 1]!, b[p + 2]!, b[p + 3]!, b[p + 4]!, b[p + 5]!, b[p + 6]!, b[p + 7]!, b[p + 8]!, b[p + 9]!, b[p + 10]!, b[p + 11]!, b[p + 12]!, b[p + 13]!);
        case 15: return c(b[p]!, b[p + 1]!, b[p + 2]!, b[p + 3]!, b[p + 4]!, b[p + 5]!, b[p + 6]!, b[p + 7]!, b[p + 8]!, b[p + 9]!, b[p + 10]!, b[p + 11]!, b[p + 12]!, b[p + 13]!, b[p + 14]!);
        case 16: return c(b[p]!, b[p + 1]!, b[p + 2]!, b[p + 3]!, b[p + 4]!, b[p + 5]!, b[p + 6]!, b[p + 7]!, b[p + 8]!, b[p + 9]!, b[p + 10]!, b[p + 11]!, b[p + 12]!, b[p + 13]!, b[p + 14]!, b[p + 15]!);
        default: return UTF8.decode(b.subarray(p, p + n));
    }
}
