"use strict";
var ComponentTerminal = (() => {
  var __defProp = Object.defineProperty;
  var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
  var __getOwnPropNames = Object.getOwnPropertyNames;
  var __hasOwnProp = Object.prototype.hasOwnProperty;
  var __export = (target, all) => {
    for (var name in all)
      __defProp(target, name, { get: all[name], enumerable: true });
  };
  var __copyProps = (to, from, except, desc) => {
    if (from && typeof from === "object" || typeof from === "function") {
      for (let key of __getOwnPropNames(from))
        if (!__hasOwnProp.call(to, key) && key !== except)
          __defProp(to, key, { get: () => from[key], enumerable: !(desc = __getOwnPropDesc(from, key)) || desc.enumerable });
    }
    return to;
  };
  var __toCommonJS = (mod) => __copyProps(__defProp({}, "__esModule", { value: true }), mod);

  // src/index.ts
  var src_exports = {};
  __export(src_exports, {
    ComponentRegistry: () => ComponentRegistry,
    ComponentTerminal: () => ComponentTerminal,
    EventBridge: () => EventBridge,
    ReactAdapter: () => ReactAdapter,
    SvelteAdapter: () => SvelteAdapter,
    VueAdapter: () => VueAdapter,
    WebComponentAdapter: () => WebComponentAdapter
  });

  // node_modules/fast-json-patch/module/core.mjs
  var core_exports = {};
  __export(core_exports, {
    JsonPatchError: () => JsonPatchError,
    _areEquals: () => _areEquals,
    applyOperation: () => applyOperation,
    applyPatch: () => applyPatch,
    applyReducer: () => applyReducer,
    deepClone: () => deepClone,
    getValueByPointer: () => getValueByPointer,
    validate: () => validate,
    validator: () => validator
  });

  // node_modules/fast-json-patch/module/helpers.mjs
  var __extends = /* @__PURE__ */ function() {
    var extendStatics = function(d, b) {
      extendStatics = Object.setPrototypeOf || { __proto__: [] } instanceof Array && function(d2, b2) {
        d2.__proto__ = b2;
      } || function(d2, b2) {
        for (var p in b2)
          if (b2.hasOwnProperty(p))
            d2[p] = b2[p];
      };
      return extendStatics(d, b);
    };
    return function(d, b) {
      extendStatics(d, b);
      function __() {
        this.constructor = d;
      }
      d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
  }();
  var _hasOwnProperty = Object.prototype.hasOwnProperty;
  function hasOwnProperty(obj, key) {
    return _hasOwnProperty.call(obj, key);
  }
  function _objectKeys(obj) {
    if (Array.isArray(obj)) {
      var keys_1 = new Array(obj.length);
      for (var k = 0; k < keys_1.length; k++) {
        keys_1[k] = "" + k;
      }
      return keys_1;
    }
    if (Object.keys) {
      return Object.keys(obj);
    }
    var keys = [];
    for (var i in obj) {
      if (hasOwnProperty(obj, i)) {
        keys.push(i);
      }
    }
    return keys;
  }
  function _deepClone(obj) {
    switch (typeof obj) {
      case "object":
        return JSON.parse(JSON.stringify(obj));
      case "undefined":
        return null;
      default:
        return obj;
    }
  }
  function isInteger(str) {
    var i = 0;
    var len = str.length;
    var charCode;
    while (i < len) {
      charCode = str.charCodeAt(i);
      if (charCode >= 48 && charCode <= 57) {
        i++;
        continue;
      }
      return false;
    }
    return true;
  }
  function escapePathComponent(path) {
    if (path.indexOf("/") === -1 && path.indexOf("~") === -1)
      return path;
    return path.replace(/~/g, "~0").replace(/\//g, "~1");
  }
  function unescapePathComponent(path) {
    return path.replace(/~1/g, "/").replace(/~0/g, "~");
  }
  function hasUndefined(obj) {
    if (obj === void 0) {
      return true;
    }
    if (obj) {
      if (Array.isArray(obj)) {
        for (var i_1 = 0, len = obj.length; i_1 < len; i_1++) {
          if (hasUndefined(obj[i_1])) {
            return true;
          }
        }
      } else if (typeof obj === "object") {
        var objKeys = _objectKeys(obj);
        var objKeysLength = objKeys.length;
        for (var i = 0; i < objKeysLength; i++) {
          if (hasUndefined(obj[objKeys[i]])) {
            return true;
          }
        }
      }
    }
    return false;
  }
  function patchErrorMessageFormatter(message, args) {
    var messageParts = [message];
    for (var key in args) {
      var value = typeof args[key] === "object" ? JSON.stringify(args[key], null, 2) : args[key];
      if (typeof value !== "undefined") {
        messageParts.push(key + ": " + value);
      }
    }
    return messageParts.join("\n");
  }
  var PatchError = (
    /** @class */
    function(_super) {
      __extends(PatchError2, _super);
      function PatchError2(message, name, index, operation, tree) {
        var _newTarget = this.constructor;
        var _this = _super.call(this, patchErrorMessageFormatter(message, { name, index, operation, tree })) || this;
        _this.name = name;
        _this.index = index;
        _this.operation = operation;
        _this.tree = tree;
        Object.setPrototypeOf(_this, _newTarget.prototype);
        _this.message = patchErrorMessageFormatter(message, { name, index, operation, tree });
        return _this;
      }
      return PatchError2;
    }(Error)
  );

  // node_modules/fast-json-patch/module/core.mjs
  var JsonPatchError = PatchError;
  var deepClone = _deepClone;
  var objOps = {
    add: function(obj, key, document2) {
      obj[key] = this.value;
      return { newDocument: document2 };
    },
    remove: function(obj, key, document2) {
      var removed = obj[key];
      delete obj[key];
      return { newDocument: document2, removed };
    },
    replace: function(obj, key, document2) {
      var removed = obj[key];
      obj[key] = this.value;
      return { newDocument: document2, removed };
    },
    move: function(obj, key, document2) {
      var removed = getValueByPointer(document2, this.path);
      if (removed) {
        removed = _deepClone(removed);
      }
      var originalValue = applyOperation(document2, { op: "remove", path: this.from }).removed;
      applyOperation(document2, { op: "add", path: this.path, value: originalValue });
      return { newDocument: document2, removed };
    },
    copy: function(obj, key, document2) {
      var valueToCopy = getValueByPointer(document2, this.from);
      applyOperation(document2, { op: "add", path: this.path, value: _deepClone(valueToCopy) });
      return { newDocument: document2 };
    },
    test: function(obj, key, document2) {
      return { newDocument: document2, test: _areEquals(obj[key], this.value) };
    },
    _get: function(obj, key, document2) {
      this.value = obj[key];
      return { newDocument: document2 };
    }
  };
  var arrOps = {
    add: function(arr, i, document2) {
      if (isInteger(i)) {
        arr.splice(i, 0, this.value);
      } else {
        arr[i] = this.value;
      }
      return { newDocument: document2, index: i };
    },
    remove: function(arr, i, document2) {
      var removedList = arr.splice(i, 1);
      return { newDocument: document2, removed: removedList[0] };
    },
    replace: function(arr, i, document2) {
      var removed = arr[i];
      arr[i] = this.value;
      return { newDocument: document2, removed };
    },
    move: objOps.move,
    copy: objOps.copy,
    test: objOps.test,
    _get: objOps._get
  };
  function getValueByPointer(document2, pointer) {
    if (pointer == "") {
      return document2;
    }
    var getOriginalDestination = { op: "_get", path: pointer };
    applyOperation(document2, getOriginalDestination);
    return getOriginalDestination.value;
  }
  function applyOperation(document2, operation, validateOperation, mutateDocument, banPrototypeModifications, index) {
    if (validateOperation === void 0) {
      validateOperation = false;
    }
    if (mutateDocument === void 0) {
      mutateDocument = true;
    }
    if (banPrototypeModifications === void 0) {
      banPrototypeModifications = true;
    }
    if (index === void 0) {
      index = 0;
    }
    if (validateOperation) {
      if (typeof validateOperation == "function") {
        validateOperation(operation, 0, document2, operation.path);
      } else {
        validator(operation, 0);
      }
    }
    if (operation.path === "") {
      var returnValue = { newDocument: document2 };
      if (operation.op === "add") {
        returnValue.newDocument = operation.value;
        return returnValue;
      } else if (operation.op === "replace") {
        returnValue.newDocument = operation.value;
        returnValue.removed = document2;
        return returnValue;
      } else if (operation.op === "move" || operation.op === "copy") {
        returnValue.newDocument = getValueByPointer(document2, operation.from);
        if (operation.op === "move") {
          returnValue.removed = document2;
        }
        return returnValue;
      } else if (operation.op === "test") {
        returnValue.test = _areEquals(document2, operation.value);
        if (returnValue.test === false) {
          throw new JsonPatchError("Test operation failed", "TEST_OPERATION_FAILED", index, operation, document2);
        }
        returnValue.newDocument = document2;
        return returnValue;
      } else if (operation.op === "remove") {
        returnValue.removed = document2;
        returnValue.newDocument = null;
        return returnValue;
      } else if (operation.op === "_get") {
        operation.value = document2;
        return returnValue;
      } else {
        if (validateOperation) {
          throw new JsonPatchError("Operation `op` property is not one of operations defined in RFC-6902", "OPERATION_OP_INVALID", index, operation, document2);
        } else {
          return returnValue;
        }
      }
    } else {
      if (!mutateDocument) {
        document2 = _deepClone(document2);
      }
      var path = operation.path || "";
      var keys = path.split("/");
      var obj = document2;
      var t = 1;
      var len = keys.length;
      var existingPathFragment = void 0;
      var key = void 0;
      var validateFunction = void 0;
      if (typeof validateOperation == "function") {
        validateFunction = validateOperation;
      } else {
        validateFunction = validator;
      }
      while (true) {
        key = keys[t];
        if (key && key.indexOf("~") != -1) {
          key = unescapePathComponent(key);
        }
        if (banPrototypeModifications && (key == "__proto__" || key == "prototype" && t > 0 && keys[t - 1] == "constructor")) {
          throw new TypeError("JSON-Patch: modifying `__proto__` or `constructor/prototype` prop is banned for security reasons, if this was on purpose, please set `banPrototypeModifications` flag false and pass it to this function. More info in fast-json-patch README");
        }
        if (validateOperation) {
          if (existingPathFragment === void 0) {
            if (obj[key] === void 0) {
              existingPathFragment = keys.slice(0, t).join("/");
            } else if (t == len - 1) {
              existingPathFragment = operation.path;
            }
            if (existingPathFragment !== void 0) {
              validateFunction(operation, 0, document2, existingPathFragment);
            }
          }
        }
        t++;
        if (Array.isArray(obj)) {
          if (key === "-") {
            key = obj.length;
          } else {
            if (validateOperation && !isInteger(key)) {
              throw new JsonPatchError("Expected an unsigned base-10 integer value, making the new referenced value the array element with the zero-based index", "OPERATION_PATH_ILLEGAL_ARRAY_INDEX", index, operation, document2);
            } else if (isInteger(key)) {
              key = ~~key;
            }
          }
          if (t >= len) {
            if (validateOperation && operation.op === "add" && key > obj.length) {
              throw new JsonPatchError("The specified index MUST NOT be greater than the number of elements in the array", "OPERATION_VALUE_OUT_OF_BOUNDS", index, operation, document2);
            }
            var returnValue = arrOps[operation.op].call(operation, obj, key, document2);
            if (returnValue.test === false) {
              throw new JsonPatchError("Test operation failed", "TEST_OPERATION_FAILED", index, operation, document2);
            }
            return returnValue;
          }
        } else {
          if (t >= len) {
            var returnValue = objOps[operation.op].call(operation, obj, key, document2);
            if (returnValue.test === false) {
              throw new JsonPatchError("Test operation failed", "TEST_OPERATION_FAILED", index, operation, document2);
            }
            return returnValue;
          }
        }
        obj = obj[key];
        if (validateOperation && t < len && (!obj || typeof obj !== "object")) {
          throw new JsonPatchError("Cannot perform operation at the desired path", "OPERATION_PATH_UNRESOLVABLE", index, operation, document2);
        }
      }
    }
  }
  function applyPatch(document2, patch, validateOperation, mutateDocument, banPrototypeModifications) {
    if (mutateDocument === void 0) {
      mutateDocument = true;
    }
    if (banPrototypeModifications === void 0) {
      banPrototypeModifications = true;
    }
    if (validateOperation) {
      if (!Array.isArray(patch)) {
        throw new JsonPatchError("Patch sequence must be an array", "SEQUENCE_NOT_AN_ARRAY");
      }
    }
    if (!mutateDocument) {
      document2 = _deepClone(document2);
    }
    var results = new Array(patch.length);
    for (var i = 0, length_1 = patch.length; i < length_1; i++) {
      results[i] = applyOperation(document2, patch[i], validateOperation, true, banPrototypeModifications, i);
      document2 = results[i].newDocument;
    }
    results.newDocument = document2;
    return results;
  }
  function applyReducer(document2, operation, index) {
    var operationResult = applyOperation(document2, operation);
    if (operationResult.test === false) {
      throw new JsonPatchError("Test operation failed", "TEST_OPERATION_FAILED", index, operation, document2);
    }
    return operationResult.newDocument;
  }
  function validator(operation, index, document2, existingPathFragment) {
    if (typeof operation !== "object" || operation === null || Array.isArray(operation)) {
      throw new JsonPatchError("Operation is not an object", "OPERATION_NOT_AN_OBJECT", index, operation, document2);
    } else if (!objOps[operation.op]) {
      throw new JsonPatchError("Operation `op` property is not one of operations defined in RFC-6902", "OPERATION_OP_INVALID", index, operation, document2);
    } else if (typeof operation.path !== "string") {
      throw new JsonPatchError("Operation `path` property is not a string", "OPERATION_PATH_INVALID", index, operation, document2);
    } else if (operation.path.indexOf("/") !== 0 && operation.path.length > 0) {
      throw new JsonPatchError('Operation `path` property must start with "/"', "OPERATION_PATH_INVALID", index, operation, document2);
    } else if ((operation.op === "move" || operation.op === "copy") && typeof operation.from !== "string") {
      throw new JsonPatchError("Operation `from` property is not present (applicable in `move` and `copy` operations)", "OPERATION_FROM_REQUIRED", index, operation, document2);
    } else if ((operation.op === "add" || operation.op === "replace" || operation.op === "test") && operation.value === void 0) {
      throw new JsonPatchError("Operation `value` property is not present (applicable in `add`, `replace` and `test` operations)", "OPERATION_VALUE_REQUIRED", index, operation, document2);
    } else if ((operation.op === "add" || operation.op === "replace" || operation.op === "test") && hasUndefined(operation.value)) {
      throw new JsonPatchError("Operation `value` property is not present (applicable in `add`, `replace` and `test` operations)", "OPERATION_VALUE_CANNOT_CONTAIN_UNDEFINED", index, operation, document2);
    } else if (document2) {
      if (operation.op == "add") {
        var pathLen = operation.path.split("/").length;
        var existingPathLen = existingPathFragment.split("/").length;
        if (pathLen !== existingPathLen + 1 && pathLen !== existingPathLen) {
          throw new JsonPatchError("Cannot perform an `add` operation at the desired path", "OPERATION_PATH_CANNOT_ADD", index, operation, document2);
        }
      } else if (operation.op === "replace" || operation.op === "remove" || operation.op === "_get") {
        if (operation.path !== existingPathFragment) {
          throw new JsonPatchError("Cannot perform the operation at a path that does not exist", "OPERATION_PATH_UNRESOLVABLE", index, operation, document2);
        }
      } else if (operation.op === "move" || operation.op === "copy") {
        var existingValue = { op: "_get", path: operation.from, value: void 0 };
        var error = validate([existingValue], document2);
        if (error && error.name === "OPERATION_PATH_UNRESOLVABLE") {
          throw new JsonPatchError("Cannot perform the operation from a path that does not exist", "OPERATION_FROM_UNRESOLVABLE", index, operation, document2);
        }
      }
    }
  }
  function validate(sequence, document2, externalValidator) {
    try {
      if (!Array.isArray(sequence)) {
        throw new JsonPatchError("Patch sequence must be an array", "SEQUENCE_NOT_AN_ARRAY");
      }
      if (document2) {
        applyPatch(_deepClone(document2), _deepClone(sequence), externalValidator || true);
      } else {
        externalValidator = externalValidator || validator;
        for (var i = 0; i < sequence.length; i++) {
          externalValidator(sequence[i], i, document2, void 0);
        }
      }
    } catch (e) {
      if (e instanceof JsonPatchError) {
        return e;
      } else {
        throw e;
      }
    }
  }
  function _areEquals(a, b) {
    if (a === b)
      return true;
    if (a && b && typeof a == "object" && typeof b == "object") {
      var arrA = Array.isArray(a), arrB = Array.isArray(b), i, length, key;
      if (arrA && arrB) {
        length = a.length;
        if (length != b.length)
          return false;
        for (i = length; i-- !== 0; )
          if (!_areEquals(a[i], b[i]))
            return false;
        return true;
      }
      if (arrA != arrB)
        return false;
      var keys = Object.keys(a);
      length = keys.length;
      if (length !== Object.keys(b).length)
        return false;
      for (i = length; i-- !== 0; )
        if (!b.hasOwnProperty(keys[i]))
          return false;
      for (i = length; i-- !== 0; ) {
        key = keys[i];
        if (!_areEquals(a[key], b[key]))
          return false;
      }
      return true;
    }
    return a !== a && b !== b;
  }

  // node_modules/fast-json-patch/module/duplex.mjs
  var duplex_exports = {};
  __export(duplex_exports, {
    compare: () => compare,
    generate: () => generate,
    observe: () => observe,
    unobserve: () => unobserve
  });
  var beforeDict = /* @__PURE__ */ new WeakMap();
  var Mirror = (
    /** @class */
    /* @__PURE__ */ function() {
      function Mirror2(obj) {
        this.observers = /* @__PURE__ */ new Map();
        this.obj = obj;
      }
      return Mirror2;
    }()
  );
  var ObserverInfo = (
    /** @class */
    /* @__PURE__ */ function() {
      function ObserverInfo2(callback, observer) {
        this.callback = callback;
        this.observer = observer;
      }
      return ObserverInfo2;
    }()
  );
  function getMirror(obj) {
    return beforeDict.get(obj);
  }
  function getObserverFromMirror(mirror, callback) {
    return mirror.observers.get(callback);
  }
  function removeObserverFromMirror(mirror, observer) {
    mirror.observers.delete(observer.callback);
  }
  function unobserve(root, observer) {
    observer.unobserve();
  }
  function observe(obj, callback) {
    var patches = [];
    var observer;
    var mirror = getMirror(obj);
    if (!mirror) {
      mirror = new Mirror(obj);
      beforeDict.set(obj, mirror);
    } else {
      var observerInfo = getObserverFromMirror(mirror, callback);
      observer = observerInfo && observerInfo.observer;
    }
    if (observer) {
      return observer;
    }
    observer = {};
    mirror.value = _deepClone(obj);
    if (callback) {
      observer.callback = callback;
      observer.next = null;
      var dirtyCheck = function() {
        generate(observer);
      };
      var fastCheck = function() {
        clearTimeout(observer.next);
        observer.next = setTimeout(dirtyCheck);
      };
      if (typeof window !== "undefined") {
        window.addEventListener("mouseup", fastCheck);
        window.addEventListener("keyup", fastCheck);
        window.addEventListener("mousedown", fastCheck);
        window.addEventListener("keydown", fastCheck);
        window.addEventListener("change", fastCheck);
      }
    }
    observer.patches = patches;
    observer.object = obj;
    observer.unobserve = function() {
      generate(observer);
      clearTimeout(observer.next);
      removeObserverFromMirror(mirror, observer);
      if (typeof window !== "undefined") {
        window.removeEventListener("mouseup", fastCheck);
        window.removeEventListener("keyup", fastCheck);
        window.removeEventListener("mousedown", fastCheck);
        window.removeEventListener("keydown", fastCheck);
        window.removeEventListener("change", fastCheck);
      }
    };
    mirror.observers.set(callback, new ObserverInfo(callback, observer));
    return observer;
  }
  function generate(observer, invertible) {
    if (invertible === void 0) {
      invertible = false;
    }
    var mirror = beforeDict.get(observer.object);
    _generate(mirror.value, observer.object, observer.patches, "", invertible);
    if (observer.patches.length) {
      applyPatch(mirror.value, observer.patches);
    }
    var temp = observer.patches;
    if (temp.length > 0) {
      observer.patches = [];
      if (observer.callback) {
        observer.callback(temp);
      }
    }
    return temp;
  }
  function _generate(mirror, obj, patches, path, invertible) {
    if (obj === mirror) {
      return;
    }
    if (typeof obj.toJSON === "function") {
      obj = obj.toJSON();
    }
    var newKeys = _objectKeys(obj);
    var oldKeys = _objectKeys(mirror);
    var changed = false;
    var deleted = false;
    for (var t = oldKeys.length - 1; t >= 0; t--) {
      var key = oldKeys[t];
      var oldVal = mirror[key];
      if (hasOwnProperty(obj, key) && !(obj[key] === void 0 && oldVal !== void 0 && Array.isArray(obj) === false)) {
        var newVal = obj[key];
        if (typeof oldVal == "object" && oldVal != null && typeof newVal == "object" && newVal != null && Array.isArray(oldVal) === Array.isArray(newVal)) {
          _generate(oldVal, newVal, patches, path + "/" + escapePathComponent(key), invertible);
        } else {
          if (oldVal !== newVal) {
            changed = true;
            if (invertible) {
              patches.push({ op: "test", path: path + "/" + escapePathComponent(key), value: _deepClone(oldVal) });
            }
            patches.push({ op: "replace", path: path + "/" + escapePathComponent(key), value: _deepClone(newVal) });
          }
        }
      } else if (Array.isArray(mirror) === Array.isArray(obj)) {
        if (invertible) {
          patches.push({ op: "test", path: path + "/" + escapePathComponent(key), value: _deepClone(oldVal) });
        }
        patches.push({ op: "remove", path: path + "/" + escapePathComponent(key) });
        deleted = true;
      } else {
        if (invertible) {
          patches.push({ op: "test", path, value: mirror });
        }
        patches.push({ op: "replace", path, value: obj });
        changed = true;
      }
    }
    if (!deleted && newKeys.length == oldKeys.length) {
      return;
    }
    for (var t = 0; t < newKeys.length; t++) {
      var key = newKeys[t];
      if (!hasOwnProperty(mirror, key) && obj[key] !== void 0) {
        patches.push({ op: "add", path: path + "/" + escapePathComponent(key), value: _deepClone(obj[key]) });
      }
    }
  }
  function compare(tree1, tree2, invertible) {
    if (invertible === void 0) {
      invertible = false;
    }
    var patches = [];
    _generate(tree1, tree2, patches, "", invertible);
    return patches;
  }

  // node_modules/fast-json-patch/index.mjs
  var fast_json_patch_default = Object.assign({}, core_exports, duplex_exports, {
    JsonPatchError: PatchError,
    deepClone: _deepClone,
    escapePathComponent,
    unescapePathComponent
  });

  // src/adapters/FrameworkAdapter.ts
  var BaseFrameworkAdapter = class {
    constructor(eventBridge, objectId) {
      this.eventBridge = eventBridge;
      this.objectId = objectId;
    }
    /**
     * Dispatch an event to the server through the EventBridge.
     * Requirements: 9.3 - WHEN a component event is received on the server, THE PComponent SHALL invoke the registered handler
     * Requirements: 9.4 - THE PComponent SHALL support typed event handlers
     * @param eventType - Type of event to dispatch
     * @param payload - Event payload data
     */
    dispatchEvent(eventType, payload) {
      this.eventBridge.dispatch(this.objectId, eventType, payload);
    }
  };

  // src/adapters/ReactAdapter.ts
  var ReactAdapter = class extends BaseFrameworkAdapter {
    constructor(factory, initialProps, eventBridge, objectId) {
      super(eventBridge, objectId);
      this._root = null;
      this.mounted = false;
      this._component = factory.getReactComponent?.();
      this.props = initialProps;
      this._container = factory.getContainer();
    }
    mount() {
      if (this.mounted) {
        return;
      }
      if (!window.ReactDOMClient) {
        console.error("ReactDOMClient not available - ensure React 18+ is loaded");
        return;
      }
      this._root = window.ReactDOMClient.createRoot(this._container);
      this.mounted = true;
      this.render();
    }
    unmount() {
      if (!this.mounted) {
        return;
      }
      if (this._root) {
        this._root.unmount();
        this._root = null;
      }
      this.mounted = false;
    }
    setProps(props) {
      this.props = props;
      if (this.mounted) {
        this.render();
      }
    }
    applyPatches(patches) {
      const result = applyPatch(this.props, patches, false, false);
      this.props = result.newDocument;
      if (this.mounted) {
        this.render();
      }
    }
    applyBinary(data) {
      const decoded = this.decodeBinary(data);
      this.props = { ...this.props, ...decoded };
      if (this.mounted) {
        this.render();
      }
    }
    isMounted() {
      return this.mounted;
    }
    render() {
      if (!this._root || !window.React) {
        console.error("Cannot render: React or root not available");
        return;
      }
      const propsWithHandlers = {
        ...this.props,
        onRowClick: (data) => {
          this.dispatchEvent("rowClick", data);
        }
      };
      const element = window.React.createElement(this._component, propsWithHandlers);
      this._root.render(element);
    }
    decodeBinary(_data) {
      return {};
    }
  };

  // src/adapters/VueAdapter.ts
  var VueAdapter = class extends BaseFrameworkAdapter {
    constructor(factory, initialProps, eventBridge, objectId) {
      super(eventBridge, objectId);
      this._app = null;
      this.mounted = false;
      this._component = factory.getVueComponent?.();
      this.props = initialProps;
      this._container = factory.getContainer();
    }
    mount() {
      if (this.mounted) {
        return;
      }
      this.mounted = true;
    }
    unmount() {
      if (!this.mounted) {
        return;
      }
      this._app = null;
      this.mounted = false;
    }
    setProps(props) {
      this.props = props;
      if (this.mounted) {
        this.updateReactiveProps();
      }
    }
    applyPatches(patches) {
      const result = applyPatch(this.props, patches, false, false);
      this.props = result.newDocument;
      if (this.mounted) {
        this.updateReactiveProps();
      }
    }
    applyBinary(data) {
      const decoded = this.decodeBinary(data);
      this.props = { ...this.props, ...decoded };
      if (this.mounted) {
        this.updateReactiveProps();
      }
    }
    isMounted() {
      return this.mounted;
    }
    updateReactiveProps() {
    }
    decodeBinary(_data) {
      return {};
    }
  };

  // src/adapters/SvelteAdapter.ts
  var SvelteAdapter = class extends BaseFrameworkAdapter {
    constructor(factory, initialProps, eventBridge, objectId) {
      super(eventBridge, objectId);
      this._instance = null;
      this.mounted = false;
      this._component = factory.getSvelteComponent?.();
      this.props = initialProps;
      this._container = factory.getContainer();
    }
    mount() {
      if (this.mounted) {
        return;
      }
      this.mounted = true;
    }
    unmount() {
      if (!this.mounted) {
        return;
      }
      this._instance = null;
      this.mounted = false;
    }
    setProps(props) {
      this.props = props;
      if (this.mounted) {
        this.updateSvelteProps();
      }
    }
    applyPatches(patches) {
      const result = applyPatch(this.props, patches, false, false);
      this.props = result.newDocument;
      if (this.mounted) {
        this.updateSvelteProps();
      }
    }
    applyBinary(data) {
      const decoded = this.decodeBinary(data);
      this.props = { ...this.props, ...decoded };
      if (this.mounted) {
        this.updateSvelteProps();
      }
    }
    isMounted() {
      return this.mounted;
    }
    updateSvelteProps() {
    }
    decodeBinary(_data) {
      return {};
    }
  };

  // src/adapters/WebComponentAdapter.ts
  var WebComponentAdapter = class extends BaseFrameworkAdapter {
    constructor(factory, initialProps, eventBridge, objectId) {
      super(eventBridge, objectId);
      this.element = null;
      this.mounted = false;
      this.tagName = factory.getTagName?.() ?? "unknown-component";
      this.props = initialProps;
      this.container = factory.getContainer();
    }
    mount() {
      if (this.mounted) {
        return;
      }
      this.element = document.createElement(this.tagName);
      this.applyPropsToElement();
      this.container.appendChild(this.element);
      this.mounted = true;
    }
    unmount() {
      if (!this.mounted) {
        return;
      }
      if (this.element && this.element.parentNode) {
        this.element.parentNode.removeChild(this.element);
      }
      this.element = null;
      this.mounted = false;
    }
    setProps(props) {
      this.props = props;
      if (this.mounted) {
        this.applyPropsToElement();
      }
    }
    applyPatches(patches) {
      const result = applyPatch(this.props, patches, false, false);
      this.props = result.newDocument;
      if (this.mounted) {
        this.applyPropsToElement();
      }
    }
    applyBinary(data) {
      const decoded = this.decodeBinary(data);
      this.props = { ...this.props, ...decoded };
      if (this.mounted) {
        this.applyPropsToElement();
      }
    }
    isMounted() {
      return this.mounted;
    }
    applyPropsToElement() {
      if (!this.element || this.props === null || this.props === void 0) {
        return;
      }
      const propsObj = this.props;
      for (const [key, value] of Object.entries(propsObj)) {
        this.element[key] = value;
      }
    }
    decodeBinary(_data) {
      return {};
    }
  };

  // src/ComponentRegistry.ts
  var ComponentRegistry = class {
    /**
     * Create a new ComponentRegistry.
     * @param eventBridge - EventBridge for dispatching events from adapters
     */
    constructor(eventBridge) {
      this.components = /* @__PURE__ */ new Map();
      this.factories = /* @__PURE__ */ new Map();
      this.eventBridge = eventBridge;
    }
    /**
     * Register a component factory for a given signature.
     * @param signature - Unique component signature
     * @param factory - Factory for creating component instances
     */
    registerFactory(signature, factory) {
      this.factories.set(signature, factory);
    }
    /**
     * Create and register a framework adapter for a component.
     * @param objectId - Unique object ID
     * @param framework - Target framework type
     * @param signature - Component signature
     * @param initialProps - Initial props for the component
     * @returns The created framework adapter
     * @throws Error if no factory is registered for the signature
     */
    createAdapter(objectId, framework, signature, initialProps) {
      const factory = this.factories.get(signature);
      if (!factory) {
        throw new Error(`No factory registered for signature: ${signature}`);
      }
      const adapter = this.createFrameworkAdapter(framework, factory, initialProps, objectId);
      this.components.set(objectId, adapter);
      return adapter;
    }
    /**
     * Get an adapter by object ID.
     * @param objectId - Object ID to look up
     * @returns The adapter or undefined if not found
     */
    get(objectId) {
      return this.components.get(objectId);
    }
    /**
     * Remove an adapter from the registry.
     * @param objectId - Object ID to remove
     */
    remove(objectId) {
      this.components.delete(objectId);
    }
    /**
     * Check if a component exists in the registry.
     * @param objectId - Object ID to check
     */
    has(objectId) {
      return this.components.has(objectId);
    }
    /**
     * Get the number of registered components.
     */
    get size() {
      return this.components.size;
    }
    /**
     * Clear all registered components.
     */
    clear() {
      this.components.clear();
    }
    /**
     * Get all registered factories (for debugging).
     */
    getFactories() {
      return this.factories;
    }
    createFrameworkAdapter(framework, factory, props, objectId) {
      switch (framework) {
        case "react":
          return new ReactAdapter(factory, props, this.eventBridge, objectId);
        case "vue":
          return new VueAdapter(factory, props, this.eventBridge, objectId);
        case "svelte":
          return new SvelteAdapter(factory, props, this.eventBridge, objectId);
        case "webcomponent":
          return new WebComponentAdapter(factory, props, this.eventBridge, objectId);
        default:
          throw new Error(`Unknown framework type: ${framework}`);
      }
    }
  };

  // src/EventBridge.ts
  var EventBridge = class {
    constructor(websocket) {
      this.pendingEvents = [];
      this.flushScheduled = false;
      this.websocket = websocket;
    }
    /**
     * Dispatch an event to the server.
     * Events are batched within the same animation frame.
     * @param objectId - Object ID of the component
     * @param eventType - Type of event
     * @param payload - Event payload data
     */
    dispatch(objectId, eventType, payload) {
      this.pendingEvents.push({ objectId, eventType, payload });
      this.scheduleFlush();
    }
    /**
     * Get the number of pending events.
     */
    get pendingCount() {
      return this.pendingEvents.length;
    }
    /**
     * Force flush all pending events immediately.
     */
    flushNow() {
      this.flush();
    }
    scheduleFlush() {
      if (this.flushScheduled) {
        return;
      }
      this.flushScheduled = true;
      requestAnimationFrame(() => {
        this.flush();
        this.flushScheduled = false;
      });
    }
    flush() {
      if (this.pendingEvents.length === 0) {
        return;
      }
      const message = this.encodeEvents(this.pendingEvents);
      this.websocket.send(message);
      this.pendingEvents = [];
    }
    encodeEvents(events) {
      const encoder = new TextEncoder();
      let totalSize = 4;
      for (const event of events) {
        const eventTypeBytes = encoder.encode(event.eventType);
        const payloadBytes = encoder.encode(JSON.stringify(event.payload));
        totalSize += 4 + 2 + eventTypeBytes.length + 4 + payloadBytes.length;
      }
      const buffer = new ArrayBuffer(totalSize);
      const view = new DataView(buffer);
      let offset = 0;
      view.setUint32(offset, events.length, false);
      offset += 4;
      for (const event of events) {
        const eventTypeBytes = encoder.encode(event.eventType);
        const payloadBytes = encoder.encode(JSON.stringify(event.payload));
        view.setUint32(offset, event.objectId, false);
        offset += 4;
        view.setUint16(offset, eventTypeBytes.length, false);
        offset += 2;
        new Uint8Array(buffer, offset, eventTypeBytes.length).set(eventTypeBytes);
        offset += eventTypeBytes.length;
        view.setUint32(offset, payloadBytes.length, false);
        offset += 4;
        new Uint8Array(buffer, offset, payloadBytes.length).set(payloadBytes);
        offset += payloadBytes.length;
      }
      return buffer;
    }
  };

  // src/ComponentTerminal.ts
  var ComponentTerminal = class {
    constructor(websocket) {
      this.eventBridge = new EventBridge(websocket);
      this.registry = new ComponentRegistry(this.eventBridge);
    }
    /**
     * Register a component factory for a given signature.
     * @param signature - Unique component signature
     * @param factory - Factory for creating component instances
     */
    registerFactory(signature, factory) {
      this.registry.registerFactory(signature, factory);
    }
    /**
     * Handle an incoming component message.
     * @param message - The component message to handle
     */
    handleMessage(message) {
      switch (message.type) {
        case "create":
          this.handleCreate(message);
          break;
        case "update":
          this.handleUpdate(message);
          break;
        case "destroy":
          this.handleDestroy(message);
          break;
      }
    }
    /**
     * Get the event bridge for dispatching events to the server.
     */
    getEventBridge() {
      return this.eventBridge;
    }
    /**
     * Get the component registry.
     */
    getRegistry() {
      return this.registry;
    }
    handleCreate(message) {
      if (!message.framework || !message.signature) {
        console.warn("Invalid create message: missing framework or signature", message);
        return;
      }
      try {
        const adapter = this.registry.createAdapter(
          message.objectId,
          message.framework,
          message.signature,
          message.props
        );
        adapter.mount();
      } catch (error) {
        console.error("Failed to create component:", error);
      }
    }
    handleUpdate(message) {
      const adapter = this.registry.get(message.objectId);
      if (!adapter) {
        console.warn("Update for unknown component:", message.objectId);
        return;
      }
      if (!adapter.isMounted()) {
        console.warn("Update for unmounted component:", message.objectId);
        return;
      }
      if (message.patches) {
        adapter.applyPatches(message.patches);
      } else if (message.binaryData) {
        adapter.applyBinary(message.binaryData);
      } else if (message.props !== void 0) {
        adapter.setProps(message.props);
      }
    }
    handleDestroy(message) {
      const adapter = this.registry.get(message.objectId);
      if (adapter) {
        adapter.unmount();
        this.registry.remove(message.objectId);
      }
    }
  };
  return __toCommonJS(src_exports);
})();
/*! Bundled license information:

fast-json-patch/module/helpers.mjs:
  (*!
   * https://github.com/Starcounter-Jack/JSON-Patch
   * (c) 2017-2022 Joachim Wester
   * MIT licensed
   *)

fast-json-patch/module/duplex.mjs:
  (*!
   * https://github.com/Starcounter-Jack/JSON-Patch
   * (c) 2017-2021 Joachim Wester
   * MIT license
   *)
*/
