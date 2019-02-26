var pony;

document.ponyLoaded = false;

document.onPonyLoadedListeners = [];
document.onPonyLoaded = function(callback) {
    document.onPonyLoadedListeners.push(callback);
};

document.onConnectionLostListeners = [];
document.onConnectionLost = function(callback) {
    document.onConnectionLostListeners.push(callback);
};

function onPonySDKModuleLoaded() {
    console.log("onPonySDKModuleLoaded");

    pony = new com.ponysdk.core.terminal.PonySDK();
    if (typeof module !== 'undefined' && module.hasOwnProperty('exports')) module.exports.pony = pony;
    else window['pony'] = pony;

    document.ponyLoaded = true;

    for(var i = 0 ; i < document.onPonyLoadedListeners.length ; i++) {
        try {
            document.onPonyLoadedListeners[i](pony);
        } catch (error) {
            throw "cannot call onPonyLoaded callback: " + document.onPonyLoadedListeners[i] + ", error " + error;
        }
    }

    pony.start();
}
if (typeof module !== 'undefined' && module.hasOwnProperty('exports')) module.exports.onPonySDKModuleLoaded = onPonySDKModuleLoaded;
else window['onPonySDKModuleLoaded'] = onPonySDKModuleLoaded;

// Decode ArrayBuffer from server
var textDecoder = null;
if ('TextDecoder' in window) textDecoder = new TextDecoder('utf-8');

function decode(arrayBufferView, byteOffset, byteLength) {
    if(textDecoder != null) return textDecoder.decode(arrayBufferView.subarray(byteOffset, byteLength));
    else return UTF8.getStringFromBytes(arrayBufferView, byteOffset, byteLength);
}
if (typeof module !== 'undefined' && module.hasOwnProperty('exports')) module.exports.decode = decode;
else window['decode'] = decode;

function AbstractAddon(id, args, widgetID, element) {
  this.id = id;
  this.widgetID = widgetID;
  this.logLevel = 0;

  if (element) this.element = element;

  // Java constructor parameters
  this.options = args || {};
  this.initialized = false;
  this.attached = false;
};

// AbstractAddon methods
AbstractAddon.prototype.onInit = function() {
  if (!this.initialized) {
    this.init();
    this.initialized = true;
  }
};

AbstractAddon.prototype.onAttached = function() {
  if (!this.attached) {
    this.jqelement = $(this.element).attr("id", this.widgetID);
    this.initDom();
    this.attached = true;
  }
};

AbstractAddon.prototype.setLogLevel = function(logLevel) {
  this.logLevel = parseInt(logLevel);
};

AbstractAddon.prototype.sendDataToServer = function(params, callback) {
  pony.sendDataToServer(this.id, params, callback);
};

AbstractAddon.prototype.log = function() {
  var args = Array.prototype.splice.call(arguments, 0);
  args.unshift("[" + this.getName() + "]");
  console.log.apply(console, args);
}

AbstractAddon.prototype.getName = function() {
  // Split the Java full qualified name to only get the associated class name
  var splitted = this.javaClass.split(".");
  return splitted[splitted.length - 1];
}

AbstractAddon.prototype.update = function(methodName, arguments) {
  if (!this.initialized) {
    throw "[" + this.getName() + "] Tried to call method '" + methodName + "' before init()";
  }

  try {
    if (arguments != null) {
      if (this.logLevel > 1) this.log(methodName, arguments);
      this[methodName].apply(this, arguments);
    } else {
      if (this.logLevel > 1) this.log(methodName);
      this[methodName].call(this);
    }
  } catch (e) {
    throw "[" + this.getName() + "#" + this.id + "] Calling '" + methodName + "' throw an exception : " + e.message + "\n";
  }
};

// SubClasses should override these methods to implement specific behaviour
AbstractAddon.prototype.init = function() {};
AbstractAddon.prototype.initDom = function() {};
AbstractAddon.prototype.onDetached = function() {};
AbstractAddon.prototype.destroy = function() {};

// Addons Management methods
AbstractAddon.defineAddon = function(name, propertiesObj) {
  console.log("registering Addon: " + name);

  var SuperClass = propertiesObj.extend || AbstractAddon;

  var JsClass = function(id, args, widgetID, element) {
    SuperClass.apply(this, [id, args, widgetID, element]);
    if (propertiesObj.ctor) propertiesObj.ctor.call(this, id, args, widgetID, element);
  };

  JsClass.prototype = Object.create(SuperClass.prototype, mapProperties(SuperClass, propertiesObj));
  JsClass.prototype.constructor = JsClass;
  mapStaticProperties(propertiesObj, JsClass);
  JsClass.prototype.javaClass = name;
  JsClass.prototype.jsClass = JsClass;

  AbstractAddon[name] = JsClass;

  // Wrap the constructor method since GWT uses the function as a method and not a constructor
  var wrap = function(id, args, widgetID, element) {
    return new JsClass(id, args, widgetID, element);
  }

  if (document.ponyLoaded) {
    pony.registerAddOnFactory(name, wrap);
  } else {
    document.onPonyLoaded(function(pony) {
      pony.registerAddOnFactory(name, wrap);
    });
  }
};

var lookup = function(Base, name) {
  var baseFn = undefined;
  var proto = Base.prototype;
  while (!baseFn && proto) {
    baseFn = proto[name];
    proto = proto.prototype;
  }

  return baseFn;
};

var buildSuper = function(Base, name) {
  var baseFn = lookup(Base, name);
  if (baseFn === undefined || typeof baseFn !== 'function') {
    return function() {
      throw new Error('No definition for method ' + name + ' found in hierarchy');
    };
  }

  return baseFn;
};

var buildWrapper = function(Base, method, name) {
  var _super = buildSuper(Base, name);
  return function() {
    var params = Array.prototype.splice.call(arguments, 0);
    var oldSuper = this.super;
    this.super = _super;
    var r = method.apply(this, params);
    this.super = oldSuper;
    return r;
  };
};

var mapProperties = function(SuperClass, objSrc) {
  var result = Object.create(Object.prototype);

  for (var property in objSrc) {
    if (property === 'statics' || property === 'extend') {
      continue;
    }

    var propertyConfig = Object.create(Object.prototype);
    propertyConfig.enumerable = true;
    propertyConfig.configurable = false;
    if (typeof objSrc[property] === 'function') {
      propertyConfig.writable = false;
      propertyConfig.value = buildWrapper(SuperClass, objSrc[property], property);
    } else {
      propertyConfig.writable = true;
      propertyConfig.value = objSrc[property];
    }
    result[property] = propertyConfig;
  }
  return result;
};

var mapStaticProperties = function(objSrc, ClassFunction) {
  var staticProperties = objSrc["statics"];
  for (var staticProperty in staticProperties) {
    var value = staticProperties[staticProperty];

    if (typeof staticProperty == 'function') {
      ClassFunction[staticProperty] = value.bind(ClassFunction);
    } else {
      ClassFunction[staticProperty] = value;
    }
  }
};

if (typeof module !== 'undefined' && module.hasOwnProperty('exports')) module.exports = AbstractAddon;
else window['AbstractAddon'] = AbstractAddon;

/*! http://mths.be/fromcodepoint v0.2.1 by @mathias */
if (!String.fromCodePoint) {
    (function() {
        var defineProperty = (function() {
            // IE 8 only supports `Object.defineProperty` on DOM elements
            try {
                var object = {};
                var $defineProperty = Object.defineProperty;
                var result = $defineProperty(object, object, object) && $defineProperty;
            } catch(error) {}
            return result;
        }());
        var stringFromCharCode = String.fromCharCode;
        var floor = Math.floor;
        var fromCodePoint = function(_) {
            var MAX_SIZE = 0x4000;
            var codeUnits = [];
            var highSurrogate;
            var lowSurrogate;
            var index = -1;
            var length = arguments.length;
            if (!length) {
                return '';
            }
            var result = '';
            while (++index < length) {
                var codePoint = Number(arguments[index]);
                if (
                    !isFinite(codePoint) || // `NaN`, `+Infinity`, or `-Infinity`
                    codePoint < 0 || // not a valid Unicode code point
                    codePoint > 0x10FFFF || // not a valid Unicode code point
                    floor(codePoint) != codePoint // not an integer
                ) {
                    throw RangeError('Invalid code point: ' + codePoint);
                }
                if (codePoint <= 0xFFFF) { // BMP code point
                    codeUnits.push(codePoint);
                } else { // Astral code point; split in surrogate halves
                    // http://mathiasbynens.be/notes/javascript-encoding#surrogate-formulae
                    codePoint -= 0x10000;
                    highSurrogate = (codePoint >> 10) + 0xD800;
                    lowSurrogate = (codePoint % 0x400) + 0xDC00;
                    codeUnits.push(highSurrogate, lowSurrogate);
                }
                if (index + 1 == length || codeUnits.length > MAX_SIZE) {
                    result += stringFromCharCode.apply(null, codeUnits);
                    codeUnits.length = 0;
                }
            }
            return result;
        };
        if (defineProperty) {
            defineProperty(String, 'fromCodePoint', {
                'value': fromCodePoint,
                'configurable': true,
                'writable': true
            });
        } else {
            String.fromCodePoint = fromCodePoint;
        }
    }());
}

/*! http://mths.be/codepointat v0.2.0 by @mathias */
if (!String.prototype.codePointAt) {
    (function() {
        'use strict'; // needed to support `apply`/`call` with `undefined`/`null`
        var defineProperty = (function() {
            // IE 8 only supports `Object.defineProperty` on DOM elements
            try {
                var object = {};
                var $defineProperty = Object.defineProperty;
                var result = $defineProperty(object, object, object) && $defineProperty;
            } catch(error) {}
            return result;
        }());
        var codePointAt = function(position) {
            if (this == null) {
                throw TypeError();
            }
            var string = String(this);
            var size = string.length;
            // `ToInteger`
            var index = position ? Number(position) : 0;
            if (index != index) { // better `isNaN`
                index = 0;
            }
            // Account for out-of-bounds indices:
            if (index < 0 || index >= size) {
                return undefined;
            }
            // Get the first code unit
            var first = string.charCodeAt(index);
            var second;
            if ( // check if it�s the start of a surrogate pair
                first >= 0xD800 && first <= 0xDBFF && // high surrogate
                size > index + 1 // there is a next code unit
            ) {
                second = string.charCodeAt(index + 1);
                if (second >= 0xDC00 && second <= 0xDFFF) { // low surrogate
                    // http://mathiasbynens.be/notes/javascript-encoding#surrogate-formulae
                    return (first - 0xD800) * 0x400 + second - 0xDC00 + 0x10000;
                }
            }
            return first;
        };
        if (defineProperty) {
            defineProperty(String.prototype, 'codePointAt', {
                'value': codePointAt,
                'configurable': true,
                'writable': true
            });
        } else {
            String.prototype.codePointAt = codePointAt;
        }
    }());
}

// https://github.com/nfroidure/UTF8.js v1.0.0
// UTF8 : Manage UTF-8 strings in ArrayBuffers
var _UTF8 = {
    // non UTF8 encoding detection (cf README file for details)
    'isNotUTF8': function (bytes, byteOffset, byteLength) {
        try {
            UTF8.getStringFromBytes(bytes, byteOffset, byteLength, true);
        } catch (e) {
            return true;
        }
        return false;
    },
    // UTF8 decoding functions
    'getCharLength': function (theByte) {
        // 4 bytes encoded char (mask 11110000)
        if (0xF0 == (theByte & 0xF0)) {
            return 4;
            // 3 bytes encoded char (mask 11100000)
        } else if (0xE0 == (theByte & 0xE0)) {
            return 3;
            // 2 bytes encoded char (mask 11000000)
        } else if (0xC0 == (theByte & 0xC0)) {
            return 2;
            // 1 bytes encoded char
        } else if (theByte == (theByte & 0x7F)) {
            return 1;
        }
        return 0;
    },
    'getCharCode': function (bytes, byteOffset, charLength) {
        var charCode = 0, mask = '';
        byteOffset = byteOffset || 0;
        // Retrieve charLength if not given
        charLength = charLength || UTF8.getCharLength(bytes[byteOffset]);
        if (charLength == 0) {
            throw new Error(bytes[byteOffset].toString(2) + ' is not a significative' +
                ' byte (offset:' + byteOffset + ').');
        }
        // Return byte value if charlength is 1
        if (1 === charLength) {
            return bytes[byteOffset];
        }
        // Test UTF8 integrity
        mask = '00000000'.slice(0, charLength) + 1 + '00000000'.slice(charLength + 1);
        if (bytes[byteOffset] & (parseInt(mask, 2))) {
            throw Error('Index ' + byteOffset + ': A ' + charLength + ' bytes' +
                ' encoded char' + ' cannot encode the ' + (charLength + 1) + 'th rank bit to 1.');
        }
        // Reading the first byte
        mask = '0000'.slice(0, charLength + 1) + '11111111'.slice(charLength + 1);
        charCode += (bytes[byteOffset] & parseInt(mask, 2)) << ((--charLength) * 6);
        // Reading the next bytes
        while (charLength) {
            if (0x80 !== (bytes[byteOffset + 1] & 0x80)
                || 0x40 === (bytes[byteOffset + 1] & 0x40)) {
                throw Error('Index ' + (byteOffset + 1) + ': Next bytes of encoded char'
                    + ' must begin with a "10" bit sequence.');
            }
            charCode += ((bytes[++byteOffset] & 0x3F) << ((--charLength) * 6));
        }
        return charCode;
    },
    'getStringFromBytes': function (bytes, byteOffset, byteLength, strict) {
        var charLength, chars = [];
        byteOffset = byteOffset | 0;
        byteLength = ('number' === typeof byteLength ?
                byteLength :
            bytes.byteLength || bytes.length
        );
        for (; byteOffset < byteLength; byteOffset++) {
            charLength = UTF8.getCharLength(bytes[byteOffset]);
            if (byteOffset + charLength > byteLength) {
                if (strict) {
                    throw Error('Index ' + byteOffset + ': Found a ' + charLength +
                        ' bytes encoded char declaration but only ' +
                        (byteLength - byteOffset) + ' bytes are available.');
                }
            } else {
                chars.push(String.fromCodePoint(
                    UTF8.getCharCode(bytes, byteOffset, charLength, strict)
                ));
            }
            byteOffset += charLength - 1;
        }
        return chars.join('');
    },
    // UTF8 encoding functions
    'getBytesForCharCode': function (charCode) {
        if (charCode < 128) {
            return 1;
        } else if (charCode < 2048) {
            return 2;
        } else if (charCode < 65536) {
            return 3;
        } else if (charCode < 2097152) {
            return 4;
        }
        throw new Error('CharCode ' + charCode + ' cannot be encoded with UTF8.');
    },
    'setBytesFromCharCode': function (charCode, bytes, byteOffset, neededBytes) {
        charCode = charCode | 0;
        bytes = bytes || [];
        byteOffset = byteOffset | 0;
        neededBytes = neededBytes || UTF8.getBytesForCharCode(charCode);
        // Setting the charCode as it to bytes if the byte length is 1
        if (1 == neededBytes) {
            bytes[byteOffset] = charCode;
        } else {
            // Computing the first byte
            bytes[byteOffset++] =
                (parseInt('1111'.slice(0, neededBytes), 2) << 8 - neededBytes) +
                (charCode >>> ((--neededBytes) * 6));
            // Computing next bytes
            for (; neededBytes > 0;) {
                bytes[byteOffset++] = ((charCode >>> ((--neededBytes) * 6)) & 0x3F) | 0x80;
            }
        }
        return bytes;
    },
    'setBytesFromString': function (string, bytes, byteOffset, byteLength, strict) {
        string = string || '';
        bytes = bytes || [];
        byteOffset = byteOffset | 0;
        byteLength = ('number' === typeof byteLength ?
                byteLength :
            bytes.byteLength || Infinity
        );
        for (var i = 0, j = string.length; i < j; i++) {
            var neededBytes = UTF8.getBytesForCharCode(string[i].codePointAt(0));
            if (strict && byteOffset + neededBytes > byteLength) {
                throw new Error('Not enought bytes to encode the char "' + string[i] +
                    '" at the offset "' + byteOffset + '".');
            }
            UTF8.setBytesFromCharCode(string[i].codePointAt(0),
                bytes, byteOffset, neededBytes, strict);
            byteOffset += neededBytes;
        }
        return bytes;
    }
};


if (typeof module !== 'undefined' && module.hasOwnProperty('exports')) {
    var UTF8 = _UTF8;
    module.exports = UTF8;
} else {
    UTF8 = _UTF8;
    window['UTF8'] = UTF8;
}
_UTF8 = undefined;
