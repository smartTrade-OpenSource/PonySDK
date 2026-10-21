/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import assert from 'node:assert/strict';
import { test } from 'node:test';

import {
    ClientToServerModel,
    DomHandlerType,
    HandlerModel,
    HandlerModelToDomHandlerType,
    MappingPath,
    ProtocolVersion,
    PUnit,
    PUnitValue,
    ServerToClientModel,
    ServerToClientModelNames,
    ServerToClientModelTypeModel,
    ValueTypeModel,
} from '#model';

// The generated model mirrors com.ponysdk.core.model: these cases pin how each kind of Java declaration is written.

test('a server-to-client key carries its wire value, its name and its value type', () => {
    assert.equal(ServerToClientModelNames[ServerToClientModel.TYPE_CREATE], 'TYPE_CREATE');
    assert.equal(ServerToClientModelTypeModel[ServerToClientModel.TYPE_CREATE], ValueTypeModel.UINT31);
    assert.equal(ServerToClientModelTypeModel[ServerToClientModel.HEARTBEAT], ValueTypeModel.NULL);
    assert.equal(ServerToClientModelTypeModel[ServerToClientModel.VALUE], ValueTypeModel.STRING);
});

test('a client-to-server key is the short string of the JSON messages', () => {
    assert.equal(ClientToServerModel.OBJECT_ID, '0');
    assert.equal(ClientToServerModel.UI_CONTEXT_ID, '1');
});

test('an enum with a byte value and a string value keeps both', () => {
    assert.equal(typeof PUnit.PX, 'number');
    assert.equal(PUnitValue[PUnit.PX], 'px');
});

test('constants and paths', () => {
    assert.equal(ProtocolVersion.CURRENT, ProtocolVersion.LISTENERS);
    assert.equal(MappingPath.WEBSOCKET, 'ws');
});

test('a DOM handler maps to its DOM event type, another handler to null', () => {
    assert.equal(HandlerModelToDomHandlerType[HandlerModel.HANDLER_DOM_CLICK], DomHandlerType.CLICK);
    assert.equal(HandlerModelToDomHandlerType[HandlerModel.HANDLER_CHANGE], null);
});
