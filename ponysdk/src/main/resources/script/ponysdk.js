document.ponyLoaded = false;
document.onPonyLoadedListeners = [];
document.onConnectionLostListeners = [];

document.onPonyLoaded = function(callback) {
    document.onPonyLoadedListeners.push(callback);
};

document.onConnectionLost = function(callback) {
    document.onConnectionLostListeners.push(callback);
};

window.webappsdk = {};
var reconnectionInProgress = false;

var Check = function() {};
Check.prototype = {
    errorDetected: false,
    counter: 3,
    delay_retry: 2000,
    delay_heartbeat: 2000,
    currentInitCheck: null,
    currentFailureCheck: null,
    reconnectionInProgress: false,

    initCheck: function () {
        if (window.opener === null || typeof window.opener == "undefined") {
            setTimeout(function() {
                var xmlhttp = new XMLHttpRequest();

                xmlhttp.onreadystatechange = function() {
                    if (xmlhttp.readyState == XMLHttpRequest.DONE) {
                        if(xmlhttp.status == 200) reconnectionCheck.onCheckSuccess();
                        else reconnectionCheck.onCheckError();
                    }
                };

                xmlhttp.open("GET", reconnectionCheck.getCheckUrl(), true);
                xmlhttp.send();
            }, reconnectionCheck.delay_heartbeat);
        }
    },

    failureCheck: function () {
        reconnectionCheck.counter--;
        var reconnectionElement = document.getElementById('reconnection'); // $('#reconnection');
        reconnectionElement.style.display = 'block'; // reconnectionElement.show();
        var reconnectingElement = document.getElementById('reconnecting');
        reconnectingElement.innerHTML = 'Connection to server lost<br>Reconnecting in ' + reconnectionCheck.counter + ' seconds <strong>...</strong>'; // reconnectionElement.html('Connection to server lost<br>Reconnecting in ' + reconnectionCheck.counter + ' seconds <strong>...</strong>');
        if (reconnectionCheck.counter == 0) {
            reconnectingElement.innerHTML = 'Reconnecting...'; // reconnectionElement.html('Reconnecting...');
            reconnectionCheck.reconnectionInProgress = true;
            window.clearInterval(reconnectionCheck.currentFailureCheck);
            reconnectionCheck.errorDetected = false;
            reconnectionCheck.currentInitCheck = setTimeout(reconnectionCheck.initCheck, reconnectionCheck.delay_heartbeat);
        }
    },

    onCheckError: function (data) {
        if (reconnectionCheck.errorDetected) return;

        reconnectionCheck.errorDetected = true;
        console.log("Failure detected");
        notifyConnectionLostListeners();
        reconnectionCheck.counter = 3;

        reconnectionCheck.currentFailureCheck = setInterval(reconnectionCheck.failureCheck, reconnectionCheck.delay_retry);
    },

    getCheckUrl: function () {
        var i = document.URL.indexOf("#");
        var paramSeparator = document.URL.indexOf("?") === -1 ? "?" : "&";
        var url = "";
        if (i === -1) {
            url = document.URL;
        } else {
            url = document.URL.substr(0, i);
        }
        return url + paramSeparator + 'ping=' + new Date().getTime();
    },

    onCheckSuccess: function (data) {
        if (reconnectionCheck.reconnectionInProgress) {
            location.reload();
        } else {
            reconnectionCheck.currentInitCheck = setTimeout(reconnectionCheck.initCheck, reconnectionCheck.delay_heartbeat);
        }
    }
};

var reconnectionCheck;
var pony;

function onPonySDKModuleLoaded() {
    console.log("onPonySDKModuleLoaded");

    pony = new ponysdk();

    document.ponyLoaded = true;

    for(var i=0; i<document.onPonyLoadedListeners.length; i++) {
        try {
            document.onPonyLoadedListeners[i](pony);
        } catch (error) {
            throw "cannot call onPonyLoaded callback: " + document.onPonyLoadedListeners[i] + ", error " + error;
        }
    }

    pony.registerCommunicationError(function(code, message) {
        // When the client signout, we reload the application
        if(code == 1000) location.reload();
    });

    pony.start();

    reconnectionCheck = new Check();
    reconnectionCheck.initCheck();
}

function notifyConnectionLostListeners() {
    for(var i=0; i<document.onConnectionLostListeners.length; i++) {
        try {
            document.onConnectionLostListeners[i]();
        } catch (error) {
            throw "cannot call onConnectionLostListeners callback: " + document.onConnectionLostListeners[i] + ", error " + error;
        }
    }
}

// Decode ArrayBuffer from server
var textDecoder = null;
if ('TextDecoder' in window) textDecoder = new TextDecoder('utf-8');

function decode(buffer) {
    if(textDecoder != null) return textDecoder.decode(buffer);
    else return UTF8.getStringFromBytes(new Uint8Array(buffer));
}


var Addon = function (pony, params) {
    this.logLevel = 0;
    this.name = this.getName();
    this.id = params.id;
    this.args = params.args
    this.widgetId = params.widgetID;
    if (typeof params.widgetElement != 'undefined') {
        this.element = params.widgetElement;
    }
    this.options = {};
    this.initialized = false;
    this.attached = false;
};

Addon.prototype.onInit = function () {
    if (!this.initialized) {
        this.init(this.args);
        this.initialized = true;
    }
};

Addon.prototype.init = function () {
    // Specific behavior
};

Addon.prototype.onAttached = function () {
    if (!this.attached)  {
        this.jqelement = $(this.element).attr("id", this.id);
        this.initDom();
        this.attached = true;
    }
};

Addon.prototype.initDom = function () {
    // Specific behavior
};

Addon.prototype.onDetached = function () {
};

Addon.prototype.sendDataToServer = function (data) {
    pony.sendDataToServer(this.id, data);
};

Addon.prototype.getName = function () {
    if (Object.prototype.toString.call(this.javaClass) === '[object Array]') {
        var names = [];
        for (var i = 0; i < this.javaClass.length; i++) {
            var splitted = this.javaClass[i].split(".");
            names.push(splitted[splitted.length - 1]);
        }
        return names.join("/");
    } else {
        var splitted = this.javaClass.split(".");
        return splitted[splitted.length - 1];
    }
};

Addon.prototype.log = function () {
    var args = Array.prototype.slice.call(arguments, 0);
    var logArgs = [];
    logArgs.push("[" + this.name + "]");
    logArgs.push(args[0]);
    if (args.length == 2) logArgs.push(args[1]);
    else if (args.length > 2) logArgs.push(args.slice(1));
    console.log.apply(console, logArgs);
};

Addon.prototype.setLogLevel = function (logLevel) {
    this.logLevel = parseInt(logLevel);
};

Addon.prototype.update = function (d) {
    var methodName = d['m'];

    if (!this.initialized)
        throw "[" + this.name + "] Tried to call method '" + methodName + "' before init()";

    try {
        if (d.hasOwnProperty('arg')) {
            if (this.logLevel > 1) this.log(methodName, d['arg']);
            this[methodName].apply(this, d['arg']);
        } else {
            if (this.logLevel > 1) this.log(methodName);
            this[methodName].call(this);
        }
    } catch (e) {
        if (this.logLevel > 0) console.log(e.stack);
        throw e;
    }
};

Addon.new = function (javaClass, obj) {
    var clazz = function (name, a, b) {
        Addon.call(this, name, a, b);
    };
    obj.__proto__ = Object.create(Addon.prototype);
    clazz.prototype = Object.create(obj);
    clazz.prototype.javaClass = javaClass;
    clazz.prototype.constructor = Addon;
    if (Object.prototype.toString.call(javaClass) === '[object Array]') {
        for (var i = 0; i < javaClass.length; i++) {
            this.registerTerminalAddon(javaClass[i], clazz);
            if (obj.globalInit != undefined) Addon._initMethods[javaClass[i]] = obj.globalInit.bind(obj);
        }
    } else {
        this.registerTerminalAddon(javaClass, clazz);
        if (obj.globalInit != undefined) Addon._initMethods[javaClass] = obj.globalInit.bind(obj)
    }
};

Addon.globalInit = function (args) {
    throw "[" + this.name + "] abstract function Addon.globalInit() MUST be implemented"
};

Addon._initMethods = {};

Addon.initAddon = function (signature, args) {
    Addon._initMethods[signature](args);
};

Addon.registerTerminalAddon = function (className, jsClass) {
    console.log("registering Addon: " + className);
    var callback = function (params) {
        var addon = new jsClass(pony, params);
        if (typeof addon.properties != "undefined") {
            for (var key in addon.properties) {
                addon[key] = addon.properties[key];
                delete addon.properties;
            }
        }
        return addon;
    };
    if (document.ponyLoaded) {
        pony.registerAddOnFactory(className, callback);
    } else {
        document.onPonyLoaded(function (pony) {
            pony.registerAddOnFactory(className, callback);
        });
    }
};


/*! https://mths.be/fromcodepoint v0.2.1 by @mathias */
if (!String.fromCodePoint) {
    (function () {
        var defineProperty = (function () {
            // IE 8 only supports `Object.defineProperty` on DOM elements
            try {
                var object = {};
                var $defineProperty = Object.defineProperty;
                var result = $defineProperty(object, object, object) && $defineProperty;
            } catch (error) {
            }
            return result;
        }());
        var stringFromCharCode = String.fromCharCode;
        var floor = Math.floor;
        var fromCodePoint = function (_) {
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
                    // https://mathiasbynens.be/notes/javascript-encoding#surrogate-formulae
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

// UTF8 : Manage UTF-8 strings in ArrayBuffers
if (typeof module !== 'undefined' && typeof module.require === 'function') {
    require('string.fromcodepoint');
    require('string.prototype.codepointat');
}

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
}
_UTF8 = undefined;