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
    BreakpointListener: () => BreakpointListener,
    ComponentRegistry: () => ComponentRegistry,
    ComponentTerminal: () => ComponentTerminal,
    DataTableRenderer: () => DataTableRenderer,
    EventBridge: () => EventBridge,
    EventForwarder: () => EventForwarder,
    FormHandler: () => FormHandler,
    OverlayController: () => OverlayController,
    ReactAdapter: () => ReactAdapter,
    ResponsiveGridRenderer: () => ResponsiveGridRenderer,
    SvelteAdapter: () => SvelteAdapter,
    ToastQueue: () => ToastQueue,
    VirtualScroller: () => VirtualScroller,
    VueAdapter: () => VueAdapter,
    WebAwesomeLoader: () => WebAwesomeLoader,
    WebComponentAdapter: () => WebComponentAdapter,
    ensureWebAwesomeComponentDefined: () => ensureWebAwesomeComponentDefined,
    getContainer: () => getContainer,
    getTerminal: () => getTerminal,
    getWebAwesomeComponentList: () => getWebAwesomeComponentList,
    getWebAwesomeLoader: () => getWebAwesomeLoader,
    initializeTerminal: () => initializeTerminal,
    isWebAwesomeComponentReady: () => isWebAwesomeComponentReady,
    registerContainer: () => registerContainer,
    registerWebAwesomeComponents: () => registerWebAwesomeComponents
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
      const createRoot = window.ReactDOMClient?.createRoot || window.ReactDOM?.createRoot;
      if (!createRoot) {
        console.error("ReactDOM.createRoot not available - ensure React 18+ is loaded");
        return;
      }
      this._root = createRoot(this._container);
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
        return;
      }
      const propsWithHandlers = {
        ...this.props,
        onRowClick: (data) => {
          console.log("[ReactAdapter] onRowClick triggered, dispatching to server:", data);
          this.dispatchEvent("rowClick", data);
        },
        onCellClick: (data) => {
          this.dispatchEvent("cellClick", data);
        },
        onChange: (data) => {
          this.dispatchEvent("change", data);
        },
        onClick: (data) => {
          this.dispatchEvent("click", data);
        },
        onSelect: (data) => {
          this.dispatchEvent("select", data);
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

  // src/events/EventForwarder.ts
  var CHECKED_TAGS = /* @__PURE__ */ new Set(["wa-checkbox", "wa-switch"]);
  var WA_EVENTS = [
    "wa-change",
    "wa-input",
    "wa-select",
    "wa-close",
    "wa-request-close",
    "wa-focus",
    "wa-blur",
    "wa-show",
    "wa-hide",
    "wa-after-show",
    "wa-after-hide"
  ];
  function extractPayload(eventType, event, element) {
    const tag = element.tagName.toLowerCase();
    const el = element;
    switch (eventType) {
      case "wa-change":
        if (CHECKED_TAGS.has(tag)) {
          return { checked: el.checked ?? false };
        }
        return { value: el.value ?? "" };
      case "wa-input":
        return { value: el.value ?? "" };
      case "wa-select":
        return { value: el.value ?? "" };
      case "wa-request-close": {
        const detail = event.detail;
        const source = typeof detail?.source === "string" ? detail.source : "unknown";
        return { source };
      }
      case "wa-close":
      case "wa-focus":
      case "wa-blur":
      case "wa-show":
      case "wa-hide":
      case "wa-after-show":
      case "wa-after-hide":
        return {};
      default:
        return {};
    }
  }
  var EventForwarder = class {
    constructor(element, eventBridge, objectId) {
      this.listeners = /* @__PURE__ */ new Map();
      this.attached = false;
      this.element = element;
      this.eventBridge = eventBridge;
      this.objectId = objectId;
    }
    /**
     * Attach event listeners for all supported wa-* events.
     * Safe to call multiple times — only attaches once.
     */
    attach() {
      if (this.attached) {
        return;
      }
      for (const eventType of WA_EVENTS) {
        const listener = (event) => {
          const payload = extractPayload(eventType, event, this.element);
          this.eventBridge.dispatch(this.objectId, eventType, payload);
        };
        this.listeners.set(eventType, listener);
        this.element.addEventListener(eventType, listener);
      }
      this.attached = true;
    }
    /**
     * Detach all event listeners. Safe to call multiple times.
     */
    detach() {
      if (!this.attached) {
        return;
      }
      for (const [eventType, listener] of this.listeners) {
        this.element.removeEventListener(eventType, listener);
      }
      this.listeners.clear();
      this.attached = false;
    }
    /**
     * Whether listeners are currently attached.
     */
    isAttached() {
      return this.attached;
    }
  };

  // src/overlay/OverlayController.ts
  var OVERLAY_TAGS = /* @__PURE__ */ new Set(["wa-dialog", "wa-drawer"]);
  var OverlayController = class {
    constructor(element) {
      this.attached = false;
      this.lastOpen = null;
      this.element = element;
    }
    /**
     * Attach the controller. Reads the current `open` state from the element.
     * Safe to call multiple times — only attaches once.
     */
    attach() {
      if (this.attached) {
        return;
      }
      this.lastOpen = this.isElementOpen();
      this.attached = true;
    }
    /**
     * Detach the controller and reset internal state.
     * Safe to call multiple times.
     */
    detach() {
      if (!this.attached) {
        return;
      }
      this.lastOpen = null;
      this.attached = false;
    }
    /**
     * Whether the controller is currently attached.
     */
    isAttached() {
      return this.attached;
    }
    /**
     * Sync the open state from server props to the native element.
     *
     * When `open` changes to `true`, calls `element.show()` to trigger
     * the native Web Awesome open animation with blocking overlay.
     * When `open` changes to `false`, calls `element.hide()` to trigger
     * the native close animation.
     *
     * If the element doesn't have show/hide methods, falls back to
     * setting the `open` property directly.
     *
     * @param open - The desired open state from server props
     */
    syncOpen(open) {
      if (!this.attached) {
        return;
      }
      if (this.lastOpen === open) {
        return;
      }
      this.lastOpen = open;
      const el = this.element;
      if (open) {
        if (typeof el.show === "function") {
          el.show();
        } else {
          el.open = true;
        }
      } else {
        if (typeof el.hide === "function") {
          el.hide();
        } else {
          el.open = false;
        }
      }
    }
    /**
     * Check if this element is an overlay-type component (wa-dialog or wa-drawer).
     */
    static isOverlayElement(element) {
      return OVERLAY_TAGS.has(element.tagName.toLowerCase());
    }
    // ========== Internal ==========
    isElementOpen() {
      const el = this.element;
      return el.open === true;
    }
  };

  // src/WebAwesomeLoader.ts
  var DEFAULT_TIMEOUT_MS = 1e4;
  var PLACEHOLDER_CLASS = "wa-loading-placeholder";
  var WebAwesomeLoader = class {
    constructor(timeoutMs = DEFAULT_TIMEOUT_MS) {
      /** Per-tagName promises so we only wait once per element type. */
      this.pending = /* @__PURE__ */ new Map();
      this.timeoutMs = timeoutMs;
    }
    /**
     * Returns `true` if the given tag name is already registered in the
     * custom elements registry.
     */
    isReady(tagName) {
      return customElements.get(tagName) !== void 0;
    }
    /**
     * Waits until the custom element for `tagName` is defined.
     *
     * - If the element is already registered, resolves immediately.
     * - Otherwise waits via `customElements.whenDefined()` with a timeout.
     * - Rejects with an `Error` if the timeout expires.
     * - Deduplicates concurrent calls for the same tag name.
     */
    async ensureDefined(tagName) {
      if (this.isReady(tagName)) {
        return;
      }
      const existing = this.pending.get(tagName);
      if (existing) {
        return existing;
      }
      const promise = this.waitForDefinition(tagName);
      this.pending.set(tagName, promise);
      try {
        await promise;
      } finally {
        this.pending.delete(tagName);
      }
    }
    /**
     * Shows a lightweight placeholder element inside `container` while the
     * custom element is loading. The placeholder is automatically removed
     * once the element is defined (or on timeout).
     *
     * Returns the placeholder element so callers can customise it further.
     */
    showPlaceholder(container) {
      const placeholder = document.createElement("div");
      placeholder.className = PLACEHOLDER_CLASS;
      placeholder.setAttribute("role", "status");
      placeholder.setAttribute("aria-label", "Loading component\u2026");
      placeholder.textContent = "";
      container.appendChild(placeholder);
      return placeholder;
    }
    /**
     * Removes a previously-added placeholder from its parent.
     */
    removePlaceholder(placeholder) {
      if (placeholder.parentNode) {
        placeholder.parentNode.removeChild(placeholder);
      }
    }
    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------
    waitForDefinition(tagName) {
      return new Promise((resolve, reject) => {
        const timer = setTimeout(() => {
          reject(
            new Error(
              `Timeout: custom element "${tagName}" was not defined within ${this.timeoutMs}ms`
            )
          );
        }, this.timeoutMs);
        customElements.whenDefined(tagName).then(() => {
          clearTimeout(timer);
          resolve();
        }).catch((err) => {
          clearTimeout(timer);
          reject(err);
        });
      });
    }
  };

  // src/WebAwesomeRegistry.ts
  var WA_COMPONENTS = [
    "wa-icon",
    "wa-checkbox",
    "wa-spinner",
    "wa-tree-item",
    "wa-carousel-item",
    "wa-button",
    "wa-animated-image",
    "wa-animation",
    "wa-avatar",
    "wa-badge",
    "wa-breadcrumb-item",
    "wa-breadcrumb",
    "wa-button-group",
    "wa-callout",
    "wa-card",
    "wa-carousel",
    "wa-input",
    "wa-popup",
    "wa-color-picker",
    "wa-comparison",
    "wa-tooltip",
    "wa-copy-button",
    "wa-details",
    "wa-dialog",
    "wa-divider",
    "wa-drawer",
    "wa-dropdown-item",
    "wa-dropdown",
    "wa-format-bytes",
    "wa-format-date",
    "wa-format-number",
    "wa-include",
    "wa-intersection-observer",
    "wa-mutation-observer",
    "wa-tag",
    "wa-select",
    "wa-option",
    "wa-popover",
    "wa-progress-bar",
    "wa-progress-ring",
    "wa-qr-code",
    "wa-radio",
    "wa-radio-group",
    "wa-rating",
    "wa-relative-time",
    "wa-resize-observer",
    "wa-scroller",
    "wa-skeleton",
    "wa-slider",
    "wa-split-panel",
    "wa-switch",
    "wa-tab",
    "wa-tab-panel",
    "wa-tab-group",
    "wa-textarea",
    "wa-tree",
    "wa-zoomable-frame",
    "wa-number-input"
  ];
  var waLoader = new WebAwesomeLoader();
  function createWaFactory(tagName, container) {
    return {
      getTagName: () => tagName,
      getContainer: () => container
    };
  }
  function registerWebAwesomeComponents(terminal2, container) {
    console.log("[WebAwesome] Registering", WA_COMPONENTS.length, "component factories");
    for (const tagName of WA_COMPONENTS) {
      const factory = createWaFactory(tagName, container);
      terminal2.registerFactory(tagName, factory);
    }
    console.log("[WebAwesome] Registration complete");
  }
  async function ensureWebAwesomeComponentDefined(tagName) {
    return waLoader.ensureDefined(tagName);
  }
  function isWebAwesomeComponentReady(tagName) {
    return waLoader.isReady(tagName);
  }
  function getWebAwesomeComponentList() {
    return WA_COMPONENTS;
  }
  function getWebAwesomeLoader() {
    return waLoader;
  }

  // src/adapters/WebComponentAdapter.ts
  var WebComponentAdapter = class extends BaseFrameworkAdapter {
    constructor(factory, initialProps, eventBridge, objectId) {
      super(eventBridge, objectId);
      this.element = null;
      this.mounted = false;
      this.eventForwarder = null;
      this.overlayController = null;
      this.tagName = factory.getTagName?.() ?? "unknown-component";
      this.props = initialProps;
      this.container = factory.getContainer();
    }
    mount() {
      if (this.mounted) {
        return;
      }
      if (this.tagName.startsWith("wa-")) {
        const loader = getWebAwesomeLoader();
        if (!loader.isReady(this.tagName)) {
          const placeholder = loader.showPlaceholder(this.container);
          loader.ensureDefined(this.tagName).then(() => {
            loader.removePlaceholder(placeholder);
            this.performMount();
          }).catch((err) => {
            console.error(`Failed to load Web Awesome component ${this.tagName}:`, err);
            loader.removePlaceholder(placeholder);
            const errorDiv = document.createElement("div");
            errorDiv.className = "wa-load-error";
            errorDiv.textContent = `Failed to load ${this.tagName}`;
            errorDiv.style.color = "red";
            errorDiv.style.padding = "1rem";
            errorDiv.style.border = "1px solid red";
            this.container.appendChild(errorDiv);
          });
          return;
        }
      }
      this.performMount();
    }
    performMount() {
      this.element = document.createElement(this.tagName);
      this.applyPropsToElement();
      this.container.appendChild(this.element);
      this.mounted = true;
      this.eventForwarder = new EventForwarder(this.element, this.eventBridge, this.objectId);
      this.eventForwarder.attach();
      if (OverlayController.isOverlayElement(this.element)) {
        this.overlayController = new OverlayController(this.element);
        this.overlayController.attach();
        const propsObj = this.props;
        if (typeof propsObj["open"] === "boolean") {
          this.overlayController.syncOpen(propsObj["open"]);
        }
      }
    }
    unmount() {
      if (!this.mounted) {
        return;
      }
      if (this.eventForwarder) {
        this.eventForwarder.detach();
        this.eventForwarder = null;
      }
      if (this.overlayController) {
        this.overlayController.detach();
        this.overlayController = null;
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
    /**
     * Get the underlying DOM element.
     * Returns null if the component is not mounted.
     */
    getElement() {
      return this.element;
    }
    /**
     * Handle a slot operation (add or remove a child element).
     * Requirements: 7.2 - Insert child into the corresponding Web Component slot
     * Requirements: 7.3 - Remove child from the slot DOM
     * Requirements: 7.4 - Default slot (null slotName) mounts without slot attribute
     *
     * @param slotOp - The slot operation descriptor
     * @param childElement - The child DOM element to add or remove
     */
    handleSlotOperation(slotOp, childElement) {
      if (!this.mounted || !this.element) {
        console.warn("Cannot handle slot operation on unmounted component:", this.objectId);
        return;
      }
      if (slotOp.operation === "add") {
        if (slotOp.slotName !== null) {
          childElement.setAttribute("slot", slotOp.slotName);
        } else {
          childElement.removeAttribute("slot");
        }
        this.element.appendChild(childElement);
      } else if (slotOp.operation === "remove") {
        if (childElement.parentNode === this.element) {
          this.element.removeChild(childElement);
        }
      }
    }
    applyPropsToElement() {
      if (!this.element || this.props === null || this.props === void 0) {
        return;
      }
      const propsObj = this.props;
      for (const [key, value] of Object.entries(propsObj)) {
        if (typeof value === "boolean") {
          if (value) {
            this.element.setAttribute(key, "");
          } else {
            this.element.removeAttribute(key);
          }
        } else if (value === null || value === void 0) {
          this.element.removeAttribute(key);
        } else if (typeof value === "string" || typeof value === "number") {
          this.element.setAttribute(key, String(value));
        } else {
          this.element[key] = value;
        }
      }
      if (this.overlayController && typeof propsObj["open"] === "boolean") {
        this.overlayController.syncOpen(propsObj["open"]);
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

  // src/form/FormHandler.ts
  var FormHandler = class {
    constructor(formElement, eventBridge, objectId) {
      this.submitListener = null;
      this.formElement = formElement;
      this.eventBridge = eventBridge;
      this.objectId = objectId;
    }
    /**
     * Attach the submit listener to the form element.
     */
    attach() {
      this.submitListener = (e) => {
        e.preventDefault();
        this.handleSubmit();
      };
      this.formElement.addEventListener("submit", this.submitListener);
    }
    /**
     * Detach the submit listener from the form element.
     */
    detach() {
      if (this.submitListener) {
        this.formElement.removeEventListener("submit", this.submitListener);
        this.submitListener = null;
      }
    }
    /**
     * Programmatically trigger form submission (e.g., from a button click handler).
     */
    submit() {
      this.handleSubmit();
    }
    /**
     * Apply server-side validation errors to matching child input components.
     * Sets customValidity on each matching wa-* element and calls reportValidity.
     * Clears errors on fields not present in the error map.
     *
     * @param errors - Map of field name to error messages from the server
     */
    applyServerErrors(errors) {
      const inputs = this.getInputElements();
      for (const input of inputs) {
        const fieldName = this.getFieldName(input);
        const fieldErrors = errors[fieldName];
        if (fieldErrors && fieldErrors.length > 0 && fieldErrors[0]) {
          this.setCustomValidity(input, fieldErrors[0]);
        } else {
          this.setCustomValidity(input, "");
        }
      }
    }
    /**
     * Clear all validation errors on child input elements.
     */
    clearErrors() {
      const inputs = this.getInputElements();
      for (const input of inputs) {
        this.setCustomValidity(input, "");
      }
    }
    // ========== Internal ==========
    handleSubmit() {
      const inputs = this.getInputElements();
      let allValid = true;
      for (const input of inputs) {
        const valid = this.validateInput(input);
        if (!valid) {
          allValid = false;
        }
      }
      if (allValid) {
        const values = this.collectValues(inputs);
        const payload = { values };
        this.eventBridge.dispatch(this.objectId, "submit", payload);
      }
    }
    /**
     * Validate a single input element using native checkValidity/reportValidity.
     * Returns true if valid.
     */
    validateInput(input) {
      const el = input;
      if (typeof el.reportValidity === "function") {
        return el.reportValidity();
      }
      if (typeof el.checkValidity === "function") {
        return el.checkValidity();
      }
      return true;
    }
    /**
     * Set custom validity message on a wa-* input element.
     */
    setCustomValidity(input, message) {
      const el = input;
      if (typeof el.setCustomValidity === "function") {
        el.setCustomValidity(message);
        if (message && typeof el.reportValidity === "function") {
          el.reportValidity();
        }
      }
    }
    /**
     * Get the field name for an input element.
     * Uses the 'name' attribute, falls back to 'label' property, then tag name.
     */
    getFieldName(input) {
      return input.getAttribute("name") || input["label"] || input.tagName.toLowerCase();
    }
    /**
     * Get the field value from an input element.
     */
    getFieldValue(input) {
      const el = input;
      if (input.tagName.toLowerCase() === "wa-checkbox" || input.tagName.toLowerCase() === "wa-switch") {
        return el.checked ?? false;
      }
      return el.value ?? "";
    }
    /**
     * Collect values from all input elements into a name→value map.
     */
    collectValues(inputs) {
      const values = {};
      for (const input of inputs) {
        const name = this.getFieldName(input);
        values[name] = this.getFieldValue(input);
      }
      return values;
    }
    /**
     * Find all child wa-* input elements within the form.
     * Matches elements whose tag name starts with 'wa-' and that have a value property
     * or are known input-type components.
     */
    getInputElements() {
      const allChildren = this.formElement.querySelectorAll("*");
      const inputs = [];
      const inputTags = /* @__PURE__ */ new Set([
        "wa-input",
        "wa-textarea",
        "wa-select",
        "wa-checkbox",
        "wa-radio-group",
        "wa-switch",
        "wa-range",
        "wa-color-picker",
        "wa-rating"
      ]);
      for (const child of allChildren) {
        const tagName = child.tagName.toLowerCase();
        if (inputTags.has(tagName)) {
          inputs.push(child);
        }
      }
      return inputs;
    }
  };

  // src/ComponentTerminal.ts
  var ComponentTerminal = class {
    constructor(websocket) {
      this.formHandlers = /* @__PURE__ */ new Map();
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
        case "slot":
          this.handleSlot(message);
          break;
        case "serverErrors":
          this.handleServerErrors(message);
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
    /**
     * Get the FormHandler for a given form objectId, if one exists.
     */
    getFormHandler(objectId) {
      return this.formHandlers.get(objectId);
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
        if (message.signature === "wa-form" && adapter instanceof WebComponentAdapter) {
          const element = adapter.getElement();
          if (element) {
            const handler = new FormHandler(element, this.eventBridge, message.objectId);
            handler.attach();
            this.formHandlers.set(message.objectId, handler);
          }
        }
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
      const formHandler = this.formHandlers.get(message.objectId);
      if (formHandler) {
        formHandler.detach();
        this.formHandlers.delete(message.objectId);
      }
      const adapter = this.registry.get(message.objectId);
      if (adapter) {
        adapter.unmount();
        this.registry.remove(message.objectId);
      }
    }
    /**
     * Handle server validation errors for a form component.
     * Requirements: 9.3 - Display server validation errors on matching input components
     */
    handleServerErrors(message) {
      const errors = message.serverErrors;
      if (!errors) {
        console.warn("serverErrors message missing errors:", message.objectId);
        return;
      }
      const formHandler = this.formHandlers.get(message.objectId);
      if (!formHandler) {
        console.warn("serverErrors for unknown form:", message.objectId);
        return;
      }
      formHandler.applyServerErrors(errors);
    }
    /**
     * Handle a slot operation message.
     * Requirements: 7.2, 7.3, 7.4 - Slot add/remove with named and default slots
     */
    handleSlot(message) {
      const slotOp = message.slotOperation;
      if (!slotOp) {
        console.warn("Slot message missing slotOperation:", message.objectId);
        return;
      }
      const parentAdapter = this.registry.get(message.objectId);
      if (!parentAdapter) {
        console.warn("Slot operation for unknown parent component:", message.objectId);
        return;
      }
      if (!(parentAdapter instanceof WebComponentAdapter)) {
        console.warn("Slot operations are only supported on WebComponentAdapter:", message.objectId);
        return;
      }
      const childAdapter = this.registry.get(slotOp.childObjectId);
      if (!childAdapter) {
        console.warn("Slot operation references unknown child component:", slotOp.childObjectId);
        return;
      }
      if (!(childAdapter instanceof WebComponentAdapter)) {
        console.warn("Slot child must be a WebComponentAdapter:", slotOp.childObjectId);
        return;
      }
      const childElement = childAdapter.getElement();
      if (!childElement) {
        console.warn("Slot child element is not mounted:", slotOp.childObjectId);
        return;
      }
      parentAdapter.handleSlotOperation(slotOp, childElement);
    }
  };

  // src/ComponentBridge.ts
  var containerRegistry = /* @__PURE__ */ new Map();
  var terminal = null;
  function registerContainer(objectId, container) {
    containerRegistry.set(objectId, container);
  }
  function getContainer(objectId) {
    return containerRegistry.get(objectId);
  }
  function initializeTerminal(websocket) {
    terminal = new ComponentTerminal(websocket);
  }
  function getTerminal() {
    return terminal;
  }
  var FRAMEWORK_MAP = {
    0: "react",
    1: "vue",
    2: "svelte",
    3: "webcomponent"
  };
  var bridgeAPI = {
    handleCreate(objectId, framework, signature, propsJson) {
      if (!terminal) {
        console.warn("ComponentTerminal not initialized for component #" + objectId);
        return;
      }
      const container = containerRegistry.get(objectId);
      if (!container) {
        console.error("Container not registered for component #" + objectId);
        return;
      }
      const frameworkType = FRAMEWORK_MAP[framework];
      if (!frameworkType) {
        console.error("Unknown framework type: " + framework);
        return;
      }
      let props;
      try {
        props = JSON.parse(propsJson);
        const p = props;
        console.log("DEBUG handleCreate props:", {
          objectId,
          signature,
          loading: p?.["loading"],
          loadingType: typeof p?.["loading"],
          disabled: p?.["disabled"],
          disabledType: typeof p?.["disabled"],
          pill: p?.["pill"],
          pillType: typeof p?.["pill"]
        });
      } catch (error) {
        console.error("Failed to parse props JSON for component #" + objectId, error);
        return;
      }
      const factorySignature = `${objectId}-${signature}`;
      terminal.registerFactory(factorySignature, {
        getContainer: () => container,
        getTagName: () => signature
      });
      terminal.handleMessage({
        objectId,
        type: "create",
        framework: frameworkType,
        signature: factorySignature,
        props
      });
    },
    handlePatch(objectId, patchJson) {
      if (!terminal) {
        console.warn("ComponentTerminal not initialized for component #" + objectId);
        return;
      }
      let patches;
      try {
        patches = JSON.parse(patchJson);
      } catch (error) {
        console.error("Failed to parse patch JSON for component #" + objectId, error);
        return;
      }
      terminal.handleMessage({
        objectId,
        type: "update",
        patches
      });
    },
    handleProps(objectId, propsJson) {
      if (!terminal) {
        console.warn("ComponentTerminal not initialized for component #" + objectId);
        return;
      }
      let props;
      try {
        props = JSON.parse(propsJson);
      } catch (error) {
        console.error("Failed to parse props JSON for component #" + objectId, error);
        return;
      }
      terminal.handleMessage({
        objectId,
        type: "update",
        props
      });
    },
    handleBinary(objectId, binaryData) {
      if (!terminal) {
        console.warn("ComponentTerminal not initialized for component #" + objectId);
        return;
      }
      terminal.handleMessage({
        objectId,
        type: "update",
        binaryData
      });
    },
    handleSlotOperation(objectId, slotOperationJson) {
      if (!terminal) {
        console.warn("ComponentTerminal not initialized for component #" + objectId);
        return;
      }
      let slotOperation;
      try {
        slotOperation = JSON.parse(slotOperationJson);
      } catch (error) {
        console.error("Failed to parse slot operation JSON for component #" + objectId, error);
        return;
      }
      terminal.handleMessage({
        objectId,
        type: "slot",
        slotOperation
      });
    },
    handleDestroy(objectId) {
      if (!terminal) {
        return;
      }
      terminal.handleMessage({
        objectId,
        type: "destroy"
      });
      containerRegistry.delete(objectId);
    }
  };
  if (typeof window !== "undefined") {
    window.PonySDK = window.PonySDK || {};
    window.PonySDK.ComponentTerminal = bridgeAPI;
    window.PonySDK.ComponentBridge = {
      registerContainer,
      initializeTerminal
    };
  }

  // src/layout/BreakpointListener.ts
  var BreakpointListener = class {
    constructor(eventBridge, objectId) {
      this.eventBridge = eventBridge;
      this.objectId = objectId;
      this.mobileQuery = window.matchMedia("(max-width: 599px)");
      this.tabletQuery = window.matchMedia("(min-width: 600px) and (max-width: 1023px)");
      this.desktopQuery = window.matchMedia("(min-width: 1024px)");
      this.currentBreakpoint = this.detectCurrentBreakpoint();
      this.mobileHandler = (e) => {
        if (e.matches)
          this.onBreakpointChange("mobile");
      };
      this.tabletHandler = (e) => {
        if (e.matches)
          this.onBreakpointChange("tablet");
      };
      this.desktopHandler = (e) => {
        if (e.matches)
          this.onBreakpointChange("desktop");
      };
      this.setupMediaQueries();
    }
    /**
     * Returns the currently active breakpoint.
     */
    getCurrentBreakpoint() {
      return this.currentBreakpoint;
    }
    /**
     * Removes all media query listeners. Call when disposing.
     */
    destroy() {
      this.mobileQuery.removeEventListener("change", this.mobileHandler);
      this.tabletQuery.removeEventListener("change", this.tabletHandler);
      this.desktopQuery.removeEventListener("change", this.desktopHandler);
    }
    setupMediaQueries() {
      this.mobileQuery.addEventListener("change", this.mobileHandler);
      this.tabletQuery.addEventListener("change", this.tabletHandler);
      this.desktopQuery.addEventListener("change", this.desktopHandler);
    }
    detectCurrentBreakpoint() {
      if (this.mobileQuery.matches)
        return "mobile";
      if (this.tabletQuery.matches)
        return "tablet";
      return "desktop";
    }
    onBreakpointChange(newBreakpoint) {
      if (newBreakpoint === this.currentBreakpoint)
        return;
      this.currentBreakpoint = newBreakpoint;
      this.eventBridge.dispatch(this.objectId, "breakpoint-change", {
        breakpoint: newBreakpoint
      });
    }
  };

  // src/layout/ResponsiveGridRenderer.ts
  var ResponsiveGridRenderer = class {
    /**
     * Generate a complete CSS string for a responsive grid identified by `gridId`.
     *
     * The output includes:
     * - A base grid rule using the default columns/gap
     * - A `@media` rule for each breakpoint overriding columns/gap
     * - Conditional display `@media` rules for hideOnMobile/hideOnTablet/hideOnDesktop
     */
    generateCSS(gridId, props) {
      const selector = `[data-grid-id="${gridId}"]`;
      const parts = [];
      parts.push(
        `${selector} {
  display: grid;
  grid-template-columns: repeat(${props.columns}, 1fr);
  gap: ${props.gap};
}`
      );
      const sorted = Object.entries(props.breakpoints).sort(([, a], [, b]) => a.minWidth - b.minWidth);
      for (const [, config] of sorted) {
        parts.push(this.generateBreakpointMediaQuery(selector, config));
      }
      parts.push(...this.generateConditionalDisplayCSS(selector, props));
      return parts.join("\n\n");
    }
    /**
     * Generate a single `@media` block for a breakpoint configuration.
     */
    generateBreakpointMediaQuery(selector, config) {
      return `@media (min-width: ${config.minWidth}px) {
  ${selector} {
    grid-template-columns: repeat(${config.columns}, 1fr);
    gap: ${config.gap};
  }
}`;
    }
    /**
     * Generate media queries that hide the grid at matching breakpoints.
     */
    generateConditionalDisplayCSS(selector, props) {
      const rules = [];
      if (props.hideOnMobile) {
        rules.push(
          `@media (max-width: 599px) {
  ${selector} {
    display: none;
  }
}`
        );
      }
      if (props.hideOnTablet) {
        rules.push(
          `@media (min-width: 600px) and (max-width: 1023px) {
  ${selector} {
    display: none;
  }
}`
        );
      }
      if (props.hideOnDesktop) {
        rules.push(
          `@media (min-width: 1024px) {
  ${selector} {
    display: none;
  }
}`
        );
      }
      return rules;
    }
  };

  // src/datatable/VirtualScroller.ts
  var VirtualScroller = class {
    /**
     * @param container - Scrollable container element
     * @param rowHeight - Fixed height per row in pixels
     * @param totalRows - Total number of rows in the dataset
     * @param bufferRows - Number of extra rows to render above/below the viewport
     */
    constructor(container, rowHeight, totalRows, bufferRows = 5) {
      this.scrollHandler = null;
      this.onRangeChange = null;
      this.container = container;
      this.rowHeight = rowHeight;
      this.totalRows = totalRows;
      this.bufferRows = bufferRows;
      this.spacerTop = document.createElement("div");
      this.spacerTop.style.width = "100%";
      this.spacerBottom = document.createElement("div");
      this.spacerBottom.style.width = "100%";
    }
    /**
     * Register a callback invoked whenever the visible range changes.
     */
    onVisibleRangeChange(callback) {
      this.onRangeChange = callback;
    }
    /**
     * Attach scroll listener and insert spacer elements into the container.
     */
    attach() {
      this.scrollHandler = () => this.handleScroll();
      this.container.addEventListener("scroll", this.scrollHandler);
      this.handleScroll();
    }
    /**
     * Detach scroll listener.
     */
    detach() {
      if (this.scrollHandler) {
        this.container.removeEventListener("scroll", this.scrollHandler);
        this.scrollHandler = null;
      }
    }
    /**
     * Update the total row count (e.g. after data changes) and recalculate.
     */
    setTotalRows(totalRows) {
      this.totalRows = totalRows;
      this.handleScroll();
    }
    /**
     * Calculate the currently visible range based on scroll position.
     */
    calculateVisibleRange() {
      const scrollTop = this.container.scrollTop;
      const viewportHeight = this.container.clientHeight;
      const firstVisible = Math.floor(scrollTop / this.rowHeight);
      const visibleCount = Math.ceil(viewportHeight / this.rowHeight);
      const start = Math.max(0, firstVisible - this.bufferRows);
      const end = Math.min(this.totalRows, firstVisible + visibleCount + this.bufferRows);
      return {
        start,
        end,
        offsetTop: start * this.rowHeight
      };
    }
    /**
     * Get the total scrollable height for all rows.
     */
    getTotalHeight() {
      return this.totalRows * this.rowHeight;
    }
    /**
     * Get the top spacer element (place before visible rows).
     */
    getSpacerTop() {
      return this.spacerTop;
    }
    /**
     * Get the bottom spacer element (place after visible rows).
     */
    getSpacerBottom() {
      return this.spacerBottom;
    }
    /**
     * Update spacer heights based on the current visible range.
     */
    updateSpacers(range) {
      this.spacerTop.style.height = `${range.offsetTop}px`;
      const bottomHeight = (this.totalRows - range.end) * this.rowHeight;
      this.spacerBottom.style.height = `${Math.max(0, bottomHeight)}px`;
    }
    handleScroll() {
      const range = this.calculateVisibleRange();
      this.updateSpacers(range);
      if (this.onRangeChange) {
        this.onRangeChange(range);
      }
    }
  };

  // src/datatable/DataTableRenderer.ts
  var VIRTUAL_SCROLL_THRESHOLD = 100;
  var DEFAULT_ROW_HEIGHT = 40;
  var DataTableRenderer = class {
    constructor(container, eventBridge, objectId, props) {
      this.tableElement = null;
      this.theadElement = null;
      this.tbodyElement = null;
      this.paginationElement = null;
      this.scrollContainer = null;
      this.virtualScroller = null;
      this.container = container;
      this.eventBridge = eventBridge;
      this.objectId = objectId;
      this.props = props;
      this.selectedRows = new Set(props.selectedRows ?? []);
    }
    /**
     * Build and mount the full table into the container.
     */
    render() {
      this.container.innerHTML = "";
      this.tableElement = document.createElement("table");
      this.tableElement.setAttribute("role", "grid");
      this.renderHeader();
      this.renderBody();
      if (this.useVirtualScroll()) {
        this.setupVirtualScroll();
      } else {
        this.container.appendChild(this.tableElement);
      }
      this.renderPagination();
    }
    /**
     * Update props and re-render affected parts.
     */
    update(props) {
      this.props = props;
      this.selectedRows = new Set(props.selectedRows ?? []);
      if (this.virtualScroller) {
        this.virtualScroller.setTotalRows(props.data.length);
      }
      this.render();
    }
    /**
     * Clean up listeners and virtual scroller.
     */
    destroy() {
      if (this.virtualScroller) {
        this.virtualScroller.detach();
        this.virtualScroller = null;
      }
      this.container.innerHTML = "";
    }
    // ========== Header ==========
    renderHeader() {
      this.theadElement = document.createElement("thead");
      const headerRow = document.createElement("tr");
      for (const col of this.props.columns) {
        const th = document.createElement("th");
        th.setAttribute("data-field", col.field);
        th.textContent = col.header;
        if (col.sortable) {
          th.style.cursor = "pointer";
          th.appendChild(this.createSortIndicator(col.field));
          th.addEventListener("click", () => this.handleSortClick(col.field));
        }
        if (col.width !== void 0) {
          th.style.width = `${col.width}px`;
        }
        headerRow.appendChild(th);
      }
      this.theadElement.appendChild(headerRow);
      this.tableElement.appendChild(this.theadElement);
    }
    createSortIndicator(field) {
      const span = document.createElement("span");
      span.classList.add("sort-indicator");
      if (this.props.sortField === field) {
        span.textContent = this.props.sortDirection === "asc" ? " \u25B2" : " \u25BC";
      } else {
        span.textContent = "";
      }
      return span;
    }
    // ========== Body ==========
    renderBody() {
      this.tbodyElement = document.createElement("tbody");
      if (this.useVirtualScroll()) {
        const range = this.virtualScroller ? this.virtualScroller.calculateVisibleRange() : { start: 0, end: Math.min(this.props.data.length, VIRTUAL_SCROLL_THRESHOLD), offsetTop: 0 };
        this.renderRowRange(range);
      } else {
        for (let i = 0; i < this.props.data.length; i++) {
          const rowData = this.props.data[i];
          if (rowData) {
            this.tbodyElement.appendChild(this.createRow(rowData, i));
          }
        }
      }
      this.tableElement.appendChild(this.tbodyElement);
    }
    renderRowRange(range) {
      if (!this.tbodyElement)
        return;
      this.tbodyElement.innerHTML = "";
      for (let i = range.start; i < range.end && i < this.props.data.length; i++) {
        const rowData = this.props.data[i];
        if (rowData) {
          this.tbodyElement.appendChild(this.createRow(rowData, i));
        }
      }
    }
    createRow(rowData, index) {
      const tr = document.createElement("tr");
      const rowId = this.getRowId(rowData, index);
      tr.setAttribute("data-row-id", rowId);
      if (this.selectedRows.has(rowId)) {
        tr.classList.add("selected");
      }
      tr.addEventListener("click", () => this.handleRowClick(rowId));
      for (const col of this.props.columns) {
        const td = document.createElement("td");
        const value = rowData[col.field];
        td.textContent = value != null ? String(value) : "";
        tr.appendChild(td);
      }
      return tr;
    }
    getRowId(rowData, index) {
      if (rowData["id"] != null) {
        return String(rowData["id"]);
      }
      return String(index);
    }
    // ========== Virtual Scroll ==========
    useVirtualScroll() {
      return this.props.virtualScroll && this.props.data.length > VIRTUAL_SCROLL_THRESHOLD;
    }
    setupVirtualScroll() {
      this.scrollContainer = document.createElement("div");
      this.scrollContainer.style.overflow = "auto";
      this.scrollContainer.style.position = "relative";
      this.scrollContainer.style.maxHeight = "400px";
      this.virtualScroller = new VirtualScroller(
        this.scrollContainer,
        DEFAULT_ROW_HEIGHT,
        this.props.data.length
      );
      this.virtualScroller.onVisibleRangeChange((range) => {
        this.renderRowRange(range);
        this.virtualScroller.updateSpacers(range);
      });
      this.scrollContainer.appendChild(this.virtualScroller.getSpacerTop());
      this.scrollContainer.appendChild(this.tableElement);
      this.scrollContainer.appendChild(this.virtualScroller.getSpacerBottom());
      this.container.appendChild(this.scrollContainer);
      this.virtualScroller.attach();
    }
    // ========== Pagination ==========
    renderPagination() {
      if (this.paginationElement) {
        this.paginationElement.remove();
      }
      this.paginationElement = document.createElement("div");
      this.paginationElement.classList.add("datatable-pagination");
      const totalPages = Math.max(1, Math.ceil(this.props.totalRows / this.props.pageSize));
      const currentPage = this.props.page;
      const prevBtn = document.createElement("button");
      prevBtn.textContent = "Previous";
      prevBtn.disabled = currentPage <= 0;
      prevBtn.addEventListener("click", () => this.handlePageChange(currentPage - 1));
      const pageInfo = document.createElement("span");
      pageInfo.classList.add("page-info");
      pageInfo.textContent = `Page ${currentPage + 1} of ${totalPages}`;
      const nextBtn = document.createElement("button");
      nextBtn.textContent = "Next";
      nextBtn.disabled = currentPage >= totalPages - 1;
      nextBtn.addEventListener("click", () => this.handlePageChange(currentPage + 1));
      this.paginationElement.appendChild(prevBtn);
      this.paginationElement.appendChild(pageInfo);
      this.paginationElement.appendChild(nextBtn);
      this.container.appendChild(this.paginationElement);
    }
    // ========== Event Handlers ==========
    handleSortClick(field) {
      const nextDirection = this.getNextSortDirection(field);
      this.eventBridge.dispatch(this.objectId, "wa-sort", {
        field,
        direction: nextDirection
      });
    }
    handlePageChange(page) {
      this.eventBridge.dispatch(this.objectId, "wa-page-change", { page });
    }
    handleRowClick(rowId) {
      if (this.selectedRows.has(rowId)) {
        this.selectedRows.delete(rowId);
      } else {
        this.selectedRows.add(rowId);
      }
      this.eventBridge.dispatch(this.objectId, "wa-selection-change", {
        selectedRows: Array.from(this.selectedRows)
      });
      this.updateRowSelection(rowId);
    }
    updateRowSelection(rowId) {
      if (!this.tbodyElement)
        return;
      const row = this.tbodyElement.querySelector(`tr[data-row-id="${rowId}"]`);
      if (row) {
        row.classList.toggle("selected", this.selectedRows.has(rowId));
      }
    }
    /**
     * Cycle sort direction: none → asc → desc → none
     */
    getNextSortDirection(field) {
      if (this.props.sortField !== field)
        return "asc";
      switch (this.props.sortDirection) {
        case "asc":
          return "desc";
        case "desc":
          return "";
        default:
          return "asc";
      }
    }
  };

  // src/toast/ToastQueue.ts
  var ToastQueue = class {
    constructor(container) {
      this.queue = [];
      this.activeToast = null;
      this.autoCloseTimer = null;
      this.closeListener = null;
      this.container = container ?? document.body;
    }
    /**
     * Add a toast to the queue. If no toast is currently showing,
     * it displays immediately.
     */
    enqueue(toast) {
      this.queue.push(toast);
      if (!this.activeToast) {
        this.showNext();
      }
    }
    /**
     * Remove all pending and active toasts.
     */
    clear() {
      this.queue = [];
      this.dismissActive();
    }
    /**
     * Number of toasts waiting in the queue (not including the active toast).
     */
    getQueueLength() {
      return this.queue.length;
    }
    /**
     * Whether a toast is currently being displayed.
     */
    get isShowing() {
      return this.activeToast !== null;
    }
    // ========== Internal ==========
    showNext() {
      if (this.queue.length === 0) {
        return;
      }
      const toast = this.queue.shift();
      const el = this.createAlertElement(toast);
      this.activeToast = el;
      this.container.appendChild(el);
      this.closeListener = () => this.onToastClosed();
      el.addEventListener("wa-after-hide", this.closeListener);
      if (toast.duration && toast.duration > 0) {
        this.autoCloseTimer = setTimeout(() => {
          this.dismissActive();
          this.showNext();
        }, toast.duration);
      }
    }
    createAlertElement(toast) {
      const el = document.createElement("wa-alert");
      if (toast.variant) {
        el.setAttribute("variant", toast.variant);
      }
      if (toast.icon) {
        el.setAttribute("icon", toast.icon);
      }
      if (toast.closable) {
        el.setAttribute("closable", "");
      }
      el.setAttribute("open", "");
      el.textContent = toast.message;
      return el;
    }
    onToastClosed() {
      this.clearTimer();
      this.removeActiveElement();
      this.showNext();
    }
    dismissActive() {
      this.clearTimer();
      this.removeActiveElement();
    }
    clearTimer() {
      if (this.autoCloseTimer !== null) {
        clearTimeout(this.autoCloseTimer);
        this.autoCloseTimer = null;
      }
    }
    removeActiveElement() {
      if (this.activeToast) {
        if (this.closeListener) {
          this.activeToast.removeEventListener("wa-after-hide", this.closeListener);
          this.closeListener = null;
        }
        if (this.activeToast.parentNode) {
          this.activeToast.parentNode.removeChild(this.activeToast);
        }
        this.activeToast = null;
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
