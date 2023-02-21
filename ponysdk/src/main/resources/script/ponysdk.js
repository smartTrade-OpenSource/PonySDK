var pony;

document.ponyLoaded = false;

document.onPonyLoadedListeners = [];
document.onPonyLoaded = function (callback) {
    document.onPonyLoadedListeners.push(callback);
};

document.onConnectionLostListeners = [];
document.onConnectionLost = function (callback) {
    document.onConnectionLostListeners.push(callback);
};

document.doReload = function () {
    document.location.reload();
};

function onPonySDKModuleLoaded() {
    console.log("onPonySDKModuleLoaded");

    pony = com.ponysdk.core.terminal.PonySDK.get();
    if (typeof module !== 'undefined' && module.hasOwnProperty('exports')) module.exports.pony = pony;
    else window['pony'] = pony;

    document.ponyLoaded = true;

    for (var i = 0; i < document.onPonyLoadedListeners.length; i++) {
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
    if (textDecoder != null) return textDecoder.decode(arrayBufferView.subarray(byteOffset, byteLength));
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
AbstractAddon.prototype.onInit = function () {
    if (!this.initialized) {
        this.init();
        this.initialized = true;
    }
};

AbstractAddon.prototype.onAttached = function () {
    if (!this.attached) {
        this.jqelement = $(this.element).attr("id", this.widgetID);
        this.initDom();
        this.attached = true;
    }
};

AbstractAddon.prototype.setLogLevel = function (logLevel) {
    this.logLevel = parseInt(logLevel);
};

AbstractAddon.prototype.sendDataToServer = function (params, callback) {
    pony.sendDataToServer(this.id, params, callback);
};

AbstractAddon.prototype.log = function () {
    var args = Array.prototype.splice.call(arguments, 0);
    args.unshift("[" + this.getName() + "]");
    console.log.apply(console, args);
}

AbstractAddon.prototype.getName = function () {
    // Split the Java full qualified name to only get the associated class name
    var splitted = this.javaClass.split(".");
    return splitted[splitted.length - 1];
}

AbstractAddon.prototype.update = function (methodName, arguments) {
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
AbstractAddon.prototype.init = function () { };
AbstractAddon.prototype.initDom = function () { };
AbstractAddon.prototype.onDetached = function () { };
AbstractAddon.prototype.destroy = function () { };

// Addons Management methods
AbstractAddon.defineAddon = function (name, propertiesObj) {
    console.log("registering Addon: " + name);

    var SuperClass = propertiesObj.extend || AbstractAddon;

    var JsClass = function (id, args, widgetID, element) {
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
    var wrap = function (id, args, widgetID, element) {
        return new JsClass(id, args, widgetID, element);
    }

    if (document.ponyLoaded) {
        pony.registerAddOnFactory(name, wrap);
    } else {
        document.onPonyLoaded(function (pony) {
            pony.registerAddOnFactory(name, wrap);
        });
    }
};

var lookup = function (Base, name) {
    var baseFn = undefined;
    var proto = Base.prototype;
    while (!baseFn && proto) {
        baseFn = proto[name];
        proto = proto.prototype;
    }

    return baseFn;
};

var buildSuper = function (Base, name) {
    var baseFn = lookup(Base, name);
    if (baseFn === undefined || typeof baseFn !== 'function') {
        return function () {
            throw new Error('No definition for method ' + name + ' found in hierarchy');
        };
    }

    return baseFn;
};

var buildWrapper = function (Base, method, name) {
    var _super = buildSuper(Base, name);
    return function () {
        var params = Array.prototype.splice.call(arguments, 0);
        var oldSuper = this.super;
        this.super = _super;
        var r = method.apply(this, params);
        this.super = oldSuper;
        return r;
    };
};

var mapProperties = function (SuperClass, objSrc) {
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

var mapStaticProperties = function (objSrc, ClassFunction) {
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
    (function () {
        var defineProperty = (function () {
            // IE 8 only supports `Object.defineProperty` on DOM elements
            try {
                var object = {};
                var $defineProperty = Object.defineProperty;
                var result = $defineProperty(object, object, object) && $defineProperty;
            } catch (error) { }
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
    (function () {
        'use strict'; // needed to support `apply`/`call` with `undefined`/`null`
        var defineProperty = (function () {
            // IE 8 only supports `Object.defineProperty` on DOM elements
            try {
                var object = {};
                var $defineProperty = Object.defineProperty;
                var result = $defineProperty(object, object, object) && $defineProperty;
            } catch (error) { }
            return result;
        }());
        var codePointAt = function (position) {
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

(function () {
    "use strict";

    AbstractAddon.defineAddon("com.ponysdk.core.ui.datagrid2.view.DefaultDataGridView.Addon", {

        initDom: function () {
            var that = this;
            this.heightLimit = 20000000;
            this.programmaticScroll = false;
            this.body = this.jqelement.find('div.pony-grid-body')[0];
            this.subBody = this.jqelement.find('div.pony-grid-sub-body')[0];
            this.pinnedBody = this.jqelement.find('div.pony-grid-pinned-body')[0];
            this.unpinnedBody = this.jqelement.find('div.pony-grid-unpinned-body')[0];
            this.pinnedHeader = this.jqelement.find('div.pony-grid-pinned-header')[0];
            this.unpinnedHeader = this.jqelement.find('div.pony-grid-unpinned-header')[0];
            this.pinnedFooter = this.jqelement.find('div.pony-grid-pinned-footer')[0];
            this.unpinnedFooter = this.jqelement.find('div.pony-grid-unpinned-footer')[0];
            this.header = this.jqelement.find('div.pony-grid-header')[0];
            this.loadingData = this.jqelement.find('div.pony-grid-loading-data')[0];
            this.ratio = 1.0;

            this.absRowCount = 0;
            this.rowHeight = 0;
            this.relRowCount = 0;
            this.firstRowIndex = 0;
            this.headerHeight = 0;
            this.footerHeight = 0;
            this.scrollRatio = 0;
            this.viewHeight = 0;
            this.subBodyWidth = 0;
            this.resizeChecker = setInterval(this.checkHeight.bind(this), 250);

            this.unpinnedFooter.addEventListener("scroll", function () {
                var scrollLeft = that.unpinnedFooter.scrollLeft;
                if (scrollLeft != that.unpinnedHeader.scrollLeft)
                    that.unpinnedHeader.scrollLeft = scrollLeft;
                if (scrollLeft != that.unpinnedBody.scrollLeft)
                    that.unpinnedBody.scrollLeft = scrollLeft;
            });
            this.unpinnedHeader.addEventListener("scroll", function () {
                var scrollLeft = that.unpinnedHeader.scrollLeft;
                if (scrollLeft != that.unpinnedFooter.scrollLeft)
                    that.unpinnedFooter.scrollLeft = scrollLeft;
                if (scrollLeft != that.unpinnedBody.scrollLeft)
                    that.unpinnedBody.scrollLeft = scrollLeft;
            });
            this.unpinnedBody.addEventListener("scroll", function () {
                var scrollLeft = that.unpinnedBody.scrollLeft;
                if (scrollLeft != that.unpinnedHeader.scrollLeft)
                    that.unpinnedHeader.scrollLeft = scrollLeft;
                if (scrollLeft != that.unpinnedFooter.scrollLeft)
                    that.unpinnedFooter.scrollLeft = scrollLeft;
            });
            this.unpinnedHeader.scrollLeft = 0;
            this.body.addEventListener("scroll", $.debounce(100, this.checkPosition.bind(this)));

            window.onresize = function (e) {
                that.refreshVerticalMargins();
                that.refreshScrollBarMargins();

                var scroll = that.scrollRatio * $(that.subBody).height();
                $(that.body).scrollTop(scroll);
            };

            $(this.pinnedBody).on('mouseenter', '.pony-grid-row', this.onRowMouseEnter.bind(this));
            $(this.pinnedBody).on('mouseleave', '.pony-grid-row', this.onRowMouseLeave.bind(this));
            $(this.unpinnedBody).on('mouseenter', '.pony-grid-row', this.onRowMouseEnter.bind(this));
            $(this.unpinnedBody).on('mouseleave', '.pony-grid-row', this.onRowMouseLeave.bind(this));
        },

        listenOnColumnVisibility: function (value) {
            if ('IntersectionObserver' in window) {
                if (value) {
                    this.intersectionObserver = new IntersectionObserver(function (entries) {
                        var columns = [];
                        var visibility = [];
                        entries.forEach(function (e) {
                            columns.push(parseInt(e.target.getAttribute('data-column-id')));
                            visibility.push(e.isIntersecting);
                        });
                        this.sendDataToServer({
                            col: columns,
                            cv: visibility
                        });
                    }.bind(this), {
                        root: this.header,
                        rootMargin: '0px',
                        threshold: 0.0
                    });
                } else if (this.intersectionObserver) {
                    this.intersectionObserver.disconnect();
                    this.intersectionObserver = null;
                }
            }
        },

        destroy: function () {
            clearInterval(this.resizeChecker);
            if ('IntersectionObserver' in window) {
                if (this.intersectionObserver) this.intersectionObserver.disconnect();
            }
        },

        onRowMouseEnter: function (event) {
            var row = event.currentTarget;
            row.setAttribute('pony-hovered', '');
            this.getOppositeRow(row).setAttribute('pony-hovered', '');
        },

        onRowMouseLeave: function (event) {
            var row = event.currentTarget;
            row.removeAttribute('pony-hovered');
            this.getOppositeRow(row).removeAttribute('pony-hovered');
        },

        getOppositeRow: function (r) {
            var row = $(r);
            var index = row.index();
            if (row.parent().is(this.pinnedBody)) {
                return $(this.unpinnedBody).children('.pony-grid-row').eq(index)[0];
            } else {
                return $(this.pinnedBody).children('.pony-grid-row').eq(index)[0];
            }
        },

        checkHeight: function () {
            var visibleHeight = $(this.body).height();
            if (Math.abs(visibleHeight - this.viewHeight) < this.viewHeight * 0.01) return;
            if (this.rowHeight == 0) {
                this.updateRowHeight(0); // Initialize rowHeight...
                if (this.rowHeight == 0) return;
                else { // We need to change the height of all already existing rows
                    for (var i = 1; i < this.relRowCount; i++) {
                        this.updateRowHeight(i);
                    }
                }
            }
            this.showLoading();
            this.viewHeight = visibleHeight;
            var c = ((visibleHeight / this.rowHeight) | 0) * 3;
            this.sendDataToServer({
                rc: c
            });
        },

        checkPosition: function () {
            var marginTop = parseFloat(this.subBody.style.marginTop);
            var marginBottom = parseFloat(this.subBody.style.marginBottom);
            var pos = this.body.scrollTop;
            var contentHeight = $(this.subBody).height();
            var visibleHeight = $(this.body).height();
            var fullHeight = contentHeight + marginTop + marginBottom;
            this.scrollRatio = pos / fullHeight;
            if ((pos <= marginTop + contentHeight * 0.1 && this.firstRowIndex > 0) ||
                ((pos + visibleHeight >= marginTop + contentHeight * 0.9) &&
                    (this.firstRowIndex < this.absRowCount - this.relRowCount))) {
                this.showLoading();
                var r = Math.max(0, (this.absRowCount * this.scrollRatio) - this.relRowCount / 3) | 0;
                this.sendDataToServer({
                    row: r
                });
            }
        },

        updateRowHeight: function (index) {
            var pinnedRow = $(this.pinnedBody).children('div').eq(index)[0];
            var unpinnedRow = $(this.unpinnedBody).children('div').eq(index)[0];
            if (this.rowHeight <= 0) {
                this.rowHeight = Math.max(pinnedRow ? this.getHeight(pinnedRow) : 0, unpinnedRow ? this.getHeight(unpinnedRow) : 0);
            }
            $(pinnedRow).height(this.rowHeight);
            $(unpinnedRow).height(this.rowHeight);
        },

        updateExtendedRowHeight: function (index) {
            var pinnedRow = $(this.pinnedBody).children('div').eq(index)[0];
            var unpinnedRow = $(this.unpinnedBody).children('div').eq(index)[0];
            var height = Math.max(pinnedRow ? this.getHeight(pinnedRow) : 0, unpinnedRow ? this.getHeight(unpinnedRow) : 0);
            $(pinnedRow).height(height);
            $(unpinnedRow).height(height);
        },

        getHeight: function (row) {
            row.style.height = null;
            var height = $(row).height();
            return height;
        },

        onRowUpdated: function (index) {
            this.updateRowHeight(index);
        },

        onDataUpdated: function (absRowCount, relRowCount, firstRowIndex) {
            if (this.absRowCount != absRowCount || this.firstRowIndex != firstRowIndex || this.relRowCount != relRowCount) {
                this.relRowCount = relRowCount;
                this.absRowCount = absRowCount;
                this.firstRowIndex = firstRowIndex;
                this.refreshVerticalMargins();
            }
            this.refreshScrollBarMargins();
        },

        onColumnAdded: function (id, colMinWidth, colMaxWidth, pinned) {
            var e = $(this.header).find("[data-column-id=" + id + "]")[0];
            if (!e) return; // If column is hidden, it won't appear in DOM
            if ('IntersectionObserver' in window) {
                if (this.intersectionObserver) this.intersectionObserver.observe(e);
            }
            var resizerDiv = e.getElementsByClassName('pony-grid-col-resizer')[0];
            if (resizerDiv) {
                var pageX, curColWidth;
                resizerDiv.addEventListener('mousedown', function (event) {
                    pageX = event.pageX;
                    curColWidth = e.offsetWidth;
                });
                document.addEventListener('mousemove', function (event) {
                    if (pageX != undefined) {
                        var diff = event.pageX - pageX;
                        e.style.width = Math.min(colMaxWidth, Math.max(colMinWidth, (curColWidth + diff))) + 'px';
                    }
                });
                document.addEventListener('mouseup', function (event) {
                    if (pageX != undefined) {
                        this.sendDataToServer({
                            col: parseInt(id),
                            cw: parseInt(e.offsetWidth)
                        });
                        pageX = undefined;
                        curColWidth = undefined;
                    }
                }.bind(this));
            }
            if (!pinned) {
                var draggableElement = e.getElementsByClassName('pony-grid-draggable-col')[0];
                if (draggableElement) {
                    draggableElement.draggable = true;
                    draggableElement.ondragover = function (event) {
                        event.preventDefault();
                        console.log("ondragover=" + id);
                    };
                    draggableElement.ondragstart = function (event) {
                        event.dataTransfer.setData('col-id', id);
                        console.log("ondragstart=" + id);
                    };
                    draggableElement.ondrop = function (event) {
                        event.preventDefault();
                        var data = event.dataTransfer.getData('col-id');
                        if (data == undefined) return;
                        console.log("ondrop=" + data);
                        this.sendDataToServer({
                            col: parseInt(data),
                            to: parseInt(id)
                        });
                    }.bind(this);
                }
            }
        },

        scrollToTop: function () {
            $(this.body).scrollTop(0);
        },

        scrollTo: function (index, total) {
            var visibleHeight = $(this.body).height();
            var lineHeight = $(this.subBody).outerHeight(true) / total;
            if ((total - index) * lineHeight < visibleHeight / 2)
                $(this.body).scrollTop(lineHeight * index);
            else
                $(this.body).scrollTop(lineHeight * index - visibleHeight / 2);
        },

        refreshScrollBarMargins: function () {
            var w = $(this.subBody).width();
            if (this.subBodyWidth == w) return;
            this.subBodyWidth = w;
            var scrollBarWidth = ($(this.body).width() - w) + "px";
            this.unpinnedFooter.style.marginRight = scrollBarWidth;
            this.unpinnedHeader.style.marginRight = scrollBarWidth;
        },

        refreshVerticalMargins: function () {
            var marginTop = this.firstRowIndex * this.rowHeight * this.ratio;
            var marginBottom = Math.max(0, (this.absRowCount - (this.firstRowIndex + this.relRowCount)) * this.rowHeight) * this.ratio;
            var scroll = false;
            if (marginTop + marginBottom > this.heightLimit) {
                while (marginTop + marginBottom > this.heightLimit) {
                    marginTop *= 0.5;
                    marginBottom *= 0.5;
                    this.ratio *= 0.5;
                }
                scroll = true;
            } else if (this.ratio <= 0.5 && ((marginTop + marginBottom) * 2.0 < this.heightLimit)) {
                while (this.ratio <= 0.5 && ((marginTop + marginBottom) * 2.0 < this.heightLimit)) {
                    marginTop *= 2.0;
                    marginBottom *= 2.0;
                    this.ratio *= 2.0;
                }
                scroll = true;
            }
            var marginTopPx = marginTop + "px";
            this.subBody.style.marginTop = marginTopPx;
            var marginBottomPx = marginBottom + "px";
            this.subBody.style.marginBottom = marginBottomPx;
            if (scroll && marginTop > 0) {
                var scrollTop = marginTop + $(this.subBody).height() / 3;
                $(this.body).scrollTop(scrollTop);
            }
        },

        showLoading: function () {
            this.loadingData.style.display = null;
        },
        hideLoading: function () {
            this.loadingData.style.display = "none";
        }

    });

})();

(function () {
    "use strict";

    AbstractAddon.defineAddon("com.ponysdk.core.ui.datagrid2.view.DefaultDataGridView.HideScrollBarAddon", {

        initDom: function () {
            var styles = '.pony-grid-hidden-scrollbar::-webkit-scrollbar { display: none; }\n' +
                '.pony-grid-hidden-scrollbar { scrollbar-width: none; -ms-overflow-style: none; }';
            var styleTag = document.createElement('style');
            styleTag.type = 'text/css';
            if (styleTag.styleSheet)
                styleTag.styleSheet.cssText = styles;
            else
                styleTag.appendChild(document.createTextNode(styles));
            this.jqelement[0].appendChild(styleTag);
        }

    });

})();

(function () {
    "use strict"
    AbstractAddon.defineAddon("com.ponysdk.core.ui.infinitescroll.InfiniteScrollAddon", {

        initDom: function () {
            window.test = this;
            this.jqelement.css("overflow-y", "auto");
            this.jqelement.css("overflow-x", "hidden");
            this.container = this.jqelement.find(".is-container");
            this.container.css("width", "100%");
            this.size = 0; // Size of the data
            this.beginIndex = 0; // Start of the viewport for the server
            this.previousBeginIndex = 0;
            this.previousVisibleItems = 0;
            this.jqelement.scroll(e => {
                // Avoid multiple call
                if (this.timeout) window.clearTimeout(this.timeout);
                this.timeout = window.setTimeout(() => this.updateView(), 40);
            });
            this.updateViewport();
        },
        updateViewport: function () {
            var timeout;
            const o = new ResizeObserver(e => {
                window.requestAnimationFrame(() => {
                    if (timeout) clearTimeout(timeout);
                    timeout = setTimeout(this.updateView(), 150);
                });
            });
            o.observe(this.element);
        },

        getTopPosition(element) {
            let $element = $(element);
            return $element.position().top - this.jqelement.position().top;
        },
        setScrollTop: function () {
            this.jqelement.scrollTop(0);
        },

        getBottomPosition(element) {
            let $element = $(element);
            return this.getTopPosition(element) + $element.outerHeight(true) - $element.height() + $element[0].scrollHeight;
        },

        setSize: function (size) {
            this.size = size;
            this.updateView(true);
        },

        updateView: function (changeFullSize) {
            let children = this.container.children();

            if (children.length === 0) {
                this.container.css("margin-top", 0 + "px");
                this.container.css("margin-bottom", 0 + "px");
                this.beginIndex = 0;
                return;
            }

            let topPosition = this.getTopPosition(children.first());
            let bottomPosition = this.getBottomPosition(children.last());
            let itemsSize = Math.abs(bottomPosition - topPosition);
            let height = this.jqelement.height();

            //rounding issue
            let marginOfError = 2;
            if (topPosition - marginOfError <= 0 && bottomPosition + marginOfError >= height) {
                if (!changeFullSize) return;
                let averageItemSize = itemsSize / children.length;
                let marginBottomPx = Math.max(0, (this.size - this.beginIndex - children.length) * averageItemSize);
                this.container.css("margin-bottom", marginBottomPx + "px");
            }

            let scrollBottom = topPosition > 0;
            let averageItemSize = itemsSize / children.length;
            let deltaItems = 0

            if (scrollBottom && topPosition > height || !scrollBottom && bottomPosition < 0) {
                let scrollPosition = this.jqelement.scrollTop();
                this.beginIndex = Math.round(scrollPosition / averageItemSize - children.length / 4);
                this.forcePosition = null;
            } else if (scrollBottom) {
                let child = children.first();
                let topPosition = this.getTopPosition(child);
                this.forcePosition = {
                    index: this.beginIndex,
                    topPosition: topPosition,
                }
                for (let i = children.length - 1; i >= 0; i--) {
                    deltaItems++;
                    let child = children.get(i);
                    let topPosition = this.getTopPosition(child);
                    if (topPosition <= height) {
                        break;
                    }
                }
            } else {
                let child = children.last();
                let topPosition = this.getTopPosition(child);
                this.forcePosition = {
                    index: this.beginIndex + children.length - 1,
                    topPosition: topPosition,
                }
                for (let i = 0; i < children.length; i++) {
                    let child = children.get(i);
                    deltaItems--;
                    let bottomPosition = this.getBottomPosition(child);
                    if (bottomPosition >= 0) {
                        break;
                    }
                }
            }

            deltaItems = Math.round(deltaItems / 2);

            let visibleItems = Math.ceil(height / averageItemSize * 2 / 5) * 5;
            this.beginIndex = Math.max(0, this.beginIndex - deltaItems);
            this.beginIndex = Math.min(this.size - visibleItems, this.beginIndex);

            // Widget is not visible due to items size
            if (this.beginIndex == -Infinity || visibleItems == Infinity) return;
            if (this.beginIndex < 0) {
                this.beginIndex = 0;
            }

            this.marginTopPx = this.beginIndex === 0 ? 0 : this.beginIndex * averageItemSize;
            this.marginBottomPx = (this.size - this.beginIndex - visibleItems) * averageItemSize;

            if (this.marginBottomPx < 0) {
                this.marginBottomPx = 0;
            }
            if (!changeFullSize && this.previousBeginIndex == this.beginIndex && this.previousVisibleItems == visibleItems) return;
            this.previousBeginIndex = this.beginIndex;
            this.previousVisibleItems = visibleItems;
            this.sendDataToServer({
                beginIndex: this.beginIndex,
                maxVisibleItem: visibleItems
            });
        },

        onDraw: function () {
            if (this.marginTopPx != null) {
                this.container.css("margin-top", this.marginTopPx + "px");
                this.container.css("margin-bottom", this.marginBottomPx + "px");
            }
            if (this.forcePosition) {
                let child = this.container.children().get(this.forcePosition.index - this.beginIndex);
                if (child) {
                    let topPosition = this.getTopPosition(child);
                    this.jqelement.scrollTop(this.jqelement.scrollTop() + topPosition - this.forcePosition.topPosition);
                }
                this.forcePosition = null;
            }
            window.setTimeout(() => {
                this.timeout = null;
                this.updateView();
            }, 50);
        },

        showIndex: function (index) {
            let children = this.container.children();
            if (index < this.beginIndex || index >= this.beginIndex + children.length) {
                this.beginIndex = index;
                this.updateView(true);
            } else {
                var child = children[index - this.beginIndex];
                if (child) {
                    var jqeTop = this.jqelement.offset().top;
                    var jqc = $(child);
                    var jqcTop = jqc.offset().top - jqeTop;
                    var jqeScroll = this.jqelement.scrollTop();
                    if (jqcTop < 0) {
                        this.jqelement.scrollTop(jqeScroll - jqc.height());
                    } else {
                        var jqcBot = jqcTop + jqc.height();
                        var jqeBot = jqeScroll + this.jqelement.height();
                        if (jqcBot + jqeScroll >= jqeBot - 2) {
                            this.jqelement.scrollTop(jqeScroll + jqc.height());
                        }
                    }
                }
            }
        }

    });

})();

(function () {
    "use strict"

    AbstractAddon.defineAddon("com.ponysdk.core.ui.dropdown.DropDownContainerAddon", {

        init: function () {
            this.parentId = this.options.parentId;
            this.stickLeft = this.options.stickLeft;
            // stickOutside at true means stickLeft at true
            this.stickOutside = this.options.stickOutside;
            this.spaceAuthorized = true;
            this.visible = false;
            this.mobile = this.isMobile();
            var that = this;
            this.resizeEventListener = function (event) {
                if (that.visible) {
                    that.sendDataToServer({
                        'windowEvent': 'resize'
                    });
                    that.removeListeners();
                }
            }
            this.scrollEventListener = function (event) {
                if (that.visible) {
                    // Cancel event if the target is a nested dropdown
                    // 5 is an optimization to start to loop after : window, document, html, body
                    let path = event.composedPath();
                    if (isNested(path[path.length - 5])) return;

                    var rect = that.element.getBoundingClientRect();
                    if (event.clientX >= rect.left && event.clientX <= rect.right &&
                        event.clientY >= rect.top && event.clientY <= rect.bottom) {
                        // Inside element, nothing to do
                    } else {
                        that.sendDataToServer({
                            'windowEvent': 'scroll'
                        });
                        that.removeListeners();
                    }
                }
            }
            // Return true if the element is a child of that
            // Else return false
            let isNested = function (element) {
                while (element && element.getAttribute('multilvl-parent')) {
                    element = document.getElementById(element.getAttribute('multilvl-parent'));
                    if (!element) break;
                    if (element.id === that.element.id) return true;
                }
                return false;
            }
            this.mouseDownEventListener = function (event) {
                if (that.visible) {
                    // Cancel event if the target is a nested dropdown
                    // 5 is an optimization to start to loop after : window, document, html, body
                    let path = event.composedPath();
                    if (isNested(path[path.length - 5])) return;

                    if (!that.parentElement) that.parentElement = document.getElementById(that.parentId);
                    var parentRect = that.parentElement.getBoundingClientRect();
                    var rect = that.element.getBoundingClientRect();
                    var x = event.clientX;
                    var y = event.clientY;
                    // In select option, event position is 0 so get target position
                    if (x == 0 && y == 0) {
                        var targetBounds = event.target.getBoundingClientRect();
                        var x = targetBounds.left;
                        var y = targetBounds.top;
                    }
                    if (x >= rect.left && x <= rect.right &&
                        y >= rect.top && y <= rect.bottom ||
                        x >= parentRect.left && x <= parentRect.right &&
                        y >= parentRect.top && y <= parentRect.bottom) {
                        // Inside element or parent, nothing to do
                    } else {
                        that.sendDataToServer({
                            'windowEvent': 'click'
                        });
                        that.removeListeners();
                    }
                }
            },
                this.keyDownEventListener = function (event) {
                    if (that.visible) {
                        // Cancel event if the target is a nested dropdown
                        // 5 is an optimization to start to loop after : window, document, html, body
                        let path = event.composedPath();
                        if (isNested(path[path.length - 5])) return;

                        if (["ArrowUp", "ArrowDown"].indexOf(event.code) > -1 ||
                            "Space" == event.code && !that.spaceAuthorized) {
                            event.preventDefault();
                        }
                    }
                }
        },

        addListeners: function () {
            if (!this.mobile) window.addEventListener('resize', this.resizeEventListener);
            window.addEventListener('wheel', this.scrollEventListener, true);
            window.addEventListener('mousedown', this.mouseDownEventListener, true);
            window.addEventListener('keydown', this.keyDownEventListener, true);
        },

        removeListeners: function () {
            if (!this.mobile) window.removeEventListener('resize', this.resizeEventListener);
            window.removeEventListener('wheel', this.scrollEventListener, true);
            window.removeEventListener('mousedown', this.mouseDownEventListener, true);
            window.removeEventListener('keydown', this.keyDownEventListener, true);
        },

        updatePosition: function () {
            if (!this.parentElement) this.parentElement = document.getElementById(this.parentId);
            var offsets = this.parentElement.getBoundingClientRect();
            this.element.style.width = offsets.width + 'px';
            // Down or top display
            var windowBot = window.innerHeight - offsets.top - offsets.height;
            var windowScrollTop = $(window).scrollTop();
            if (windowBot < this.element.offsetHeight && offsets.top > this.element.offsetHeight) {
                // Element takes place above the parent
                let computedTop = offsets.top - this.element.offsetHeight + windowScrollTop;
                if (this.stickOutside) {
                    // If the element sticks outside of the ofset, we align the element bottom border with the offset bottom border
                    computedTop += offsets.height;
                }
                this.element.style.top = computedTop + 'px';
                this.element.setAttribute('vertical-position', 'top');
                this.element.style.height = this.element.offsetHeight + 'px';
            } else {
                // Element takes place beneath the parent
                let computedTop = offsets.top + offsets.height + windowScrollTop;
                if (this.stickOutside) {
                    // If the element sticks outside of the ofset, we align the element top border with the offset top border
                    computedTop -= offsets.height;
                }
                this.element.style.top = computedTop + 'px';
                this.element.setAttribute('vertical-position', 'down');
                this.element.style.height = 'auto';
            }
            // Right or left display
            var windowScrollLeft = $(window).scrollLeft();
            if (this.stickLeft) {
                // Element position led by its left absolute position
                // windowRight defines the remaining space at the right of the parent
                var windowRight = window.innerWidth - offsets.left - offsets.width;
                if (this.stickOutside) {
                    // if stickOutside, maring and padding of the new element should be taken into account in the remaining space
                    windowRight = windowRight - this.element.style.marginLeft - this.element.style.paddingLeft;
                }
                if (windowRight < this.element.offsetWidth && offsets.left > this.element.offsetWidth) {
                    let computedLeft = offsets.left + offsets.width - this.element.offsetWidth + windowScrollLeft;
                    if (this.stickOutside) {
                        // if stickOutside, element takes place at the left of the parent
                        computedLeft = computedLeft - offsets.width - this.element.style.marginRight - this.element.style.paddingRight;
                    }
                    this.element.style.left = computedLeft + 'px';
                    this.element.setAttribute('horizontal-position', 'right');
                } else {
                    let computedLeft = offsets.left + windowScrollLeft;
                    if (this.stickOutside) {
                        // if stickOutside, element takes place at the right of the parent
                        computedLeft = computedLeft + offsets.width + this.element.style.marginLeft + this.element.style.paddingLeft;
                    }
                    this.element.style.left = computedLeft + 'px';
                    this.element.setAttribute('horizontal-position', 'left');
                }
            } else {
                // Element position led by its right absolute position
                // windowLeft defines the remaining space at the left of the parent
                var windowLeft = offsets.left;
                if (windowLeft < this.element.offsetWidth && window.innerWidth - offsets.right > this.element.offsetWidth) {
                    this.element.style.right = window.innerWidth - offsets.right + offsets.width - this.element.offsetWidth - windowScrollLeft + 'px';
                    this.element.setAttribute('horizontal-position', 'left');
                } else {
                    this.element.style.right = window.innerWidth - offsets.right - windowScrollLeft + 'px';
                    this.element.setAttribute('horizontal-position', 'right');
                }
            }
        },

        adjustPosition: function () {
            if (!this.parentElement) this.parentElement = document.getElementById(this.parentId);
            var offsets = this.parentElement.getBoundingClientRect();
            this.element.style.width = offsets.width + 'px';
            // Down or top display
            var windowBot = window.innerHeight - offsets.top - offsets.height;
            var windowScrollTop = $(window).scrollTop();
            if (this.element.getAttribute('vertical-position') == 'top') {
                this.element.style.top = offsets.top - this.element.offsetHeight + windowScrollTop + 'px';
            } else if (this.element.getAttribute('vertical-position') == 'down') {
                this.element.style.top = offsets.top + offsets.height + windowScrollTop + 'px';
            }
            // Right or left display
            var windowScrollLeft = $(window).scrollLeft();
            if (this.stickLeft) {
                var windowRight = window.innerWidth - offsets.left - offsets.width;
                if (this.element.getAttribute('horizontal-position') == 'right') {
                    this.element.style.left = offsets.left + offsets.width - this.element.offsetWidth + windowScrollLeft + 'px';
                } else if (this.element.getAttribute('horizontal-position') == 'left') {
                    this.element.style.left = offsets.left + windowScrollLeft + 'px';
                }
            } else {
                var windowLeft = offsets.left;
                if (this.element.getAttribute('horizontal-position') == 'left') {
                    this.element.style.right = window.innerWidth - offsets.right + offsets.width - this.element.offsetWidth - windowScrollLeft + 'px';
                } else if (this.element.getAttribute('horizontal-position') == 'right') {
                    this.element.style.right = window.innerWidth - offsets.right - windowScrollLeft + 'px';
                }
            }
        },

        setVisible: function (visible) {
            this.visible = visible;
            if (visible) {
                this.addListeners();
            } else {
                this.removeListeners();
            }
        },

        disableSpaceWhenOpened: function () {
            this.spaceAuthorized = false;
        },

        isMobile: function () {
            const toMatch = [
                /Android/i,
                /webOS/i,
                /iPhone/i,
                /iPad/i,
                /iPod/i,
                /BlackBerry/i,
                /Windows Phone/i
            ];
            return toMatch.some((toMatchItem) => {
                return navigator.userAgent.match(toMatchItem);
            });
        }

    });

})();
