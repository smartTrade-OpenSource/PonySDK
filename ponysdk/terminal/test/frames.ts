/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import {
    ArrayValueModel as A,
    BooleanModel,
    ServerToClientModelTypeModel,
    ValueTypeModel as V,
    ValueTypeModelConstants as T,
} from '#model';

/** A model: its key, then its value; a Uint8Array value is written as is, after the key. */
export type Model = readonly [number, unknown?];

const UTF8 = new TextEncoder();

/** Writes models as the server does, for the blocks a test needs that no fixture has. */
export function encode(...models: Model[]): Uint8Array {
    const bytes: number[] = [];
    const view = new DataView(new ArrayBuffer(8));
    const put = (size: number): void => {
        for (let i = 0; i < size; i++) bytes.push(view.getUint8(i));
    };
    const uint31 = (value: number): void => {
        if (value < 0x8000) {
            view.setInt16(0, value);
            put(2);
        } else {
            view.setUint32(0, (value | 0x80000000) >>> 0);
            put(4);
        }
    };
    const string = (value: string | null): void => {
        const utf8 = UTF8.encode(value ?? '');
        if (/^[\x00-\x7f]*$/.test(value ?? '') && utf8.length <= T.STRING_ASCII_UINT8) {
            bytes.push(utf8.length);
        } else {
            bytes.push(T.STRING_UTF8_INT32);
            view.setInt32(0, utf8.length);
            put(4);
        }
        bytes.push(...utf8);
    };
    const element = (value: unknown): void => {
        if (value === null) bytes.push(A.NULL);
        else if (typeof value === 'boolean') bytes.push(value ? A.BOOLEAN_TRUE : A.BOOLEAN_FALSE);
        else if (typeof value === 'number') {
            bytes.push(A.INTEGER);
            view.setInt32(0, value);
            put(4);
        } else {
            const utf8 = UTF8.encode(String(value));
            bytes.push(A.STRING_UTF8_UINT8_LENGTH, utf8.length, ...utf8);
        }
    };
    for (const [key, value] of models) {
        bytes.push(key);
        if (value instanceof Uint8Array) {
            bytes.push(...value);
            continue;
        }
        switch (ServerToClientModelTypeModel[key]) {
            case V.NULL:
                break;
            case V.BOOLEAN:
                bytes.push(value ? BooleanModel.TRUE : BooleanModel.FALSE);
                break;
            case V.BYTE:
                view.setInt8(0, value as number);
                put(1);
                break;
            case V.SHORT:
                view.setInt16(0, value as number);
                put(2);
                break;
            case V.INTEGER:
                view.setInt32(0, value as number);
                put(4);
                break;
            case V.UINT31:
                uint31(value as number);
                break;
            case V.LONG:
                view.setBigInt64(0, BigInt(value as number));
                put(8);
                break;
            case V.DOUBLE:
                view.setFloat64(0, value as number);
                put(8);
                break;
            case V.FLOAT:
                view.setFloat32(0, value as number);
                put(4);
                break;
            case V.STRING:
                string(value as string | null);
                break;
            case V.ARRAY: {
                const array = value as unknown[];
                bytes.push(array.length);
                for (const item of array) element(item);
                break;
            }
            default:
                throw new Error(`no type for key ${key}`);
        }
    }
    return Uint8Array.from(bytes);
}
