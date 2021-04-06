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

document.doReload = function() {
    document.location.reload();
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

(function() {
  "use strict";

  AbstractAddon.defineAddon("com.ponysdk.core.ui.datagrid.view.DefaultDataGridView.Addon", {
	
    initDom: function() {
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
        
        if('IntersectionObserver' in window){
            this.intersectionObserver = new IntersectionObserver(function(entries){
                var columns = [];
                var visibility = [];
                entries.forEach(function(e){
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
        }
        
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
        
        this.unpinnedFooter.addEventListener("scroll", function(){
            var scrollLeft = that.unpinnedFooter.scrollLeft;
            if(scrollLeft != that.unpinnedHeader.scrollLeft)
                that.unpinnedHeader.scrollLeft = scrollLeft;
            if(scrollLeft != that.unpinnedBody.scrollLeft)
                that.unpinnedBody.scrollLeft = scrollLeft;
        });
        this.unpinnedHeader.addEventListener("scroll", function(){
            var scrollLeft = that.unpinnedHeader.scrollLeft;
            if(scrollLeft != that.unpinnedFooter.scrollLeft)
                that.unpinnedFooter.scrollLeft = scrollLeft;
            if(scrollLeft != that.unpinnedBody.scrollLeft)
                that.unpinnedBody.scrollLeft = scrollLeft;
        });
        this.unpinnedBody.addEventListener("scroll", function(){
            var scrollLeft = that.unpinnedBody.scrollLeft;
            if(scrollLeft != that.unpinnedHeader.scrollLeft)
                that.unpinnedHeader.scrollLeft = scrollLeft;
            if(scrollLeft != that.unpinnedFooter.scrollLeft)
                that.unpinnedFooter.scrollLeft = scrollLeft;
        });
        this.unpinnedHeader.scrollLeft = 0;
      	this.body.addEventListener("scroll", $.debounce(100, this.checkPosition.bind(this)));
        
		window.onresize = function(e) {
			var rows = $(that.pinnedBody).children('div').length;
			for(var i=0;i<rows;i++){
				that.updateRowHeight(i);
			}
    		that.refreshVerticalMargins();
            that.refreshScrollBarMargins();
			var bodyHeight = $(that.pinnedBody).height();
			var scroll = that.scrollRatio * $(that.subBody).height();
    		$(that.body).scrollTop(scroll);
		};
        
        $(this.pinnedBody).on('mouseenter', '.pony-grid-row', this.onRowMouseEnter.bind(this));
      	$(this.pinnedBody).on('mouseleave', '.pony-grid-row', this.onRowMouseLeave.bind(this));
      	$(this.unpinnedBody).on('mouseenter', '.pony-grid-row', this.onRowMouseEnter.bind(this));
      	$(this.unpinnedBody).on('mouseleave', '.pony-grid-row', this.onRowMouseLeave.bind(this));
    },
    
    destroy: function() {
    	clearInterval(this.resizeChecker);
        if('IntersectionObserver' in window){
            this.intersectionObserver.disconnect();
        }
    },
      
    onRowMouseEnter: function(event) {
        var row = event.currentTarget;
        row.setAttribute('pony-hovered', '');
        this.getOppositeRow(row).setAttribute('pony-hovered', '');
    },
      
    onRowMouseLeave: function(event) {
        var row = event.currentTarget;
        row.removeAttribute('pony-hovered');
        this.getOppositeRow(row).removeAttribute('pony-hovered');
    },
      
    getOppositeRow: function(r) {
        var row = $(r);
        var index = row.index();
        if(row.parent().is(this.pinnedBody)) {
            return $(this.unpinnedBody).children('.pony-grid-row').eq(index)[0];
        } else {
            return $(this.pinnedBody).children('.pony-grid-row').eq(index)[0];
        }
    },
    
    checkHeight: function(){
        var visibleHeight = $(this.body).height();
  		if(Math.abs(visibleHeight - this.viewHeight)<this.viewHeight*0.01) return;
		if(this.rowHeight == 0) return;
        this.loadingData.style.display = null;
		this.viewHeight = visibleHeight;
		var c = ((visibleHeight/this.rowHeight)|0)*3;
		this.sendDataToServer({
          	rc: c
        });
    },

    checkPosition: function(){
    	var marginTop = parseFloat(this.subBody.style.marginTop);
		var marginBottom = parseFloat(this.subBody.style.marginBottom);
		var pos = this.body.scrollTop;
        var contentHeight = $(this.subBody).height();
        var visibleHeight = $(this.body).height();
        var fullHeight = contentHeight + marginTop + marginBottom;
		this.scrollRatio = pos / fullHeight;
		if((pos <= marginTop + contentHeight * 0.1 && this.firstRowIndex > 0) ||
			((pos + visibleHeight >= marginTop + contentHeight * 0.9 ) &&
				(this.firstRowIndex < this.absRowCount - this.relRowCount) )){
            this.loadingData.style.display = null;
			var r = Math.max(0, (this.absRowCount * this.scrollRatio) - this.relRowCount/3) | 0;
			this.sendDataToServer({
				row: r
			});
		}
    },
    
    updateRowHeight: function(index){
    	var pinnedRow = $(this.pinnedBody).children('div').eq(index)[0];
    	var unpinnedRow = $(this.unpinnedBody).children('div').eq(index)[0];
    	if(this.rowHeight <= 0){
    		this.rowHeight = this.adjustHeight(pinnedRow, unpinnedRow);
    	} else {
    		$(pinnedRow).height(this.rowHeight);
    		$(unpinnedRow).height(this.rowHeight);
    	}
    },
    
    updateExtendedRowHeight: function(index){
    	var pinnedRow = $(this.pinnedBody).children('div').eq(index)[0];
    	var unpinnedRow = $(this.unpinnedBody).children('div').eq(index)[0];
    	this.adjustHeight(pinnedRow, unpinnedRow);
    },
    
    adjustHeight: function(pinnedRow, unpinnedRow){
    	pinnedRow.style.height = null;
    	unpinnedRow.style.height = null;
    	var pinnedHeight = $(pinnedRow).height();
    	var unpinnedHeight = $(unpinnedRow).height();
    	var maxHeight = Math.max(pinnedHeight, unpinnedHeight);
    	$(unpinnedRow).height(maxHeight);
    	$(pinnedRow).height(maxHeight);
    	return maxHeight;
    },
    
    onRowUpdated: function(index) {
    	this.updateRowHeight(index);
    },
    
    onDataUpdated: function(absRowCount, relRowCount, firstRowIndex) {
    	if(this.absRowCount != absRowCount || this.firstRowIndex != firstRowIndex || this.relRowCount != relRowCount){
			this.relRowCount = relRowCount;
			this.absRowCount = absRowCount;
			this.firstRowIndex = firstRowIndex;
			this.refreshVerticalMargins();
		}
        this.refreshScrollBarMargins();
    },
      
    onColumnAdded: function(id, colMinWidth, colMaxWidth, pinned){
		var e = $(this.header).find("[data-column-id="+id+"]")[0];
        if('IntersectionObserver' in window){
            this.intersectionObserver.observe(e);
        }
        var resizerDiv = e.getElementsByClassName('pony-grid-col-resizer')[0];
        if(resizerDiv){
            var pageX, curColWidth;
            resizerDiv.addEventListener('mousedown', function (event) {
                pageX = event.pageX;
                curColWidth = e.offsetWidth;
            });
            document.addEventListener('mousemove', function(event) {
                if(pageX != undefined){
                    var diff = event.pageX - pageX;
                    e.style.width = Math.min(colMaxWidth,Math.max(colMinWidth,(curColWidth + diff)))+'px';
                }
            });
            document.addEventListener('mouseup', function(event) {
                if(pageX != undefined){
                    this.sendDataToServer({
                        col: parseInt(id),
                        cw: parseInt(e.offsetWidth)
                    });
                    pageX = undefined; 
                    curColWidth = undefined;
                }
            }.bind(this));
        }
        if(!pinned){
            var draggableElement = e.getElementsByClassName('pony-grid-draggable-col')[0];
            if(draggableElement){
                draggableElement.draggable = true;
                draggableElement.ondragover = function(event){
                    event.preventDefault();
                    console.log("ondragover="+id);
                };
                draggableElement.ondragstart = function(event){
                    event.dataTransfer.setData('col-id', id);
                    console.log("ondragstart="+id);
                };
                draggableElement.ondrop = function(event){
                    event.preventDefault();
                    var data = event.dataTransfer.getData('col-id');
                    if(data == undefined) return;
                    console.log("ondrop="+data);
                    this.sendDataToServer({
                        col: parseInt(data),
                        to: parseInt(id)
                    });
                }.bind(this);
            }
        }
    },        
      
    scrollToTop: function(){
		$(this.body).scrollTop(0);
    },
      
    refreshScrollBarMargins: function(){
        var w = $(this.subBody).width();
        if(this.subBodyWidth == w) return;
        this.subBodyWidth = w;
        var scrollBarWidth = ($(this.body).width() - w)+"px";
        this.unpinnedFooter.style.marginRight = scrollBarWidth;
        this.unpinnedHeader.style.marginRight = scrollBarWidth;
    },
    
    refreshVerticalMargins: function(){
		var marginTop = this.firstRowIndex * this.rowHeight * this.ratio;
		var marginBottom = Math.max(0, (this.absRowCount - (this.firstRowIndex + this.relRowCount))*this.rowHeight) * this.ratio;
        var scroll = false;
        if(marginTop + marginBottom > this.heightLimit){
            while(marginTop + marginBottom > this.heightLimit){
                marginTop *= 0.5;
                marginBottom *= 0.5;
                this.ratio *= 0.5;
            }
            scroll = true;
        } else if(this.ratio <= 0.5 && ((marginTop+marginBottom)*2.0 < this.heightLimit)) {
            while(this.ratio <= 0.5 && ((marginTop+marginBottom)*2.0 < this.heightLimit)){
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
        if(scroll && marginTop > 0){
            var scrollTop = marginTop+$(this.subBody).height()/3;
            $(this.body).scrollTop(scrollTop);
        }
    }

  });

})();

(function() {
  "use strict";

  AbstractAddon.defineAddon("com.ponysdk.core.ui.datagrid.view.DefaultDataGridView.HideScrollBarAddon", {
	
    initDom: function() {        
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

(function() {
  "use strict"

  /**
   * TODO add fake rows
   * show more rows then it could be shown
   */
  AbstractAddon.defineAddon("com.ponysdk.core.ui.infinitescroll.InfiniteScroll", {
    initDom:function() {
      window.test = this;

      this.scrollableBody = this.jqelement;
      this.table = this.jqelement.find(".container");
      this.tbody = this.jqelement.find(".item-body");
      //this.tbody = document.getElementsByClassName("t-body");
      
      this.scrollableBody.width("100%");
      this.scrollableBody.height("100%");
      this.scrollableBody.css("overflow-y", "overlay");
      this.scrollableBody.css("overflow-x", "hidden");
      this.scrollableBody.css("position", "relative");

      this.table.css("width", "100%");
      
  

      this.size = 0; //size of the data
      this.beginIndex = 0; //start of the viewport for the server
      this.throtleRows = 5; //nb row to scroll before update view
      this.started = false;
    
      
      //prevent to much call 
      //this.updateTableHeight = this.updateTableHeight.bind(this);
      //this.updateView = $.debounce(250, this.updateView.bind(this)).bind(this);

      //force compute size t
      this.invalid = true;
      this.firstStart = true;
    },
    
    start:function() {
      this.started = true;
      
    

      
      // default row height
      // will be update by adding rows
      this.rowHeight = 15; 
      
      if(this.firstStart){
        this.firstStart = false;

        //add handlers
        window.addEventListener("resize", function(){
          if(this.started){
            this.updateTableHeight();
          } else {
            this.invalid = true;
          }
        }.bind(this));
        this.scrollableBody.scroll(this.onScroll.bind(this));
        
        var observer = new MutationObserver(function(e){
          var trs = []
          for(var i = 0; i < e.length; i++){
            if(e[i].addedNodes){
              for(var j = 0; j < e[i].addedNodes.length; j++)
              trs.push(e[i].addedNodes[j]);
            }
          }
          if(trs.length > 0)
          this.updateTableHeight(trs);
        }.bind(this));
        observer.observe(this.tbody[0], {childList: true});
      }

      if(this.invalid){
        this.invalid = false;
        this.updateTableHeight();
      }
    },
    
    stop:function(){
      this.started = false;
    },
    
    /**
     * Update table height
     * Called after a window resize event
     * Or after adding a row
     */
    updateTableHeight:function(addedNodes) {
      if(!this.started) return;
      if(!addedNodes){
        console.log(this.tbody);
        var trs = this.tbody.children(".item");
        console.log(trs);
      }   
      else 
        var trs = $(addedNodes);

      
      var height = trs.height();
      if( height != null && height != this.rowHeight ) {
        this.rowHeight = height;
      }

      this.viewportHeightPx = this.scrollableBody.height() ;
      this.visualMaxVisibleItem = Math.ceil( this.viewportHeightPx / this.rowHeight);
      this.throtleRows = Math.floor(this.visualMaxVisibleItem / 2 ) + this.visualMaxVisibleItem % 2;
      this.maxVisibleItem = this.visualMaxVisibleItem + 2 * this.throtleRows;

      this.updateView(true);
    },
    
    /**
     * update the amount of data
     */
    setSize:function(size) {
      this.size = size;
      //force redraw
      this.marginTopPx = undefined;
      this.updateView();
    },
    
    sendInformations:function() {
      if( this.lastBeginIndex != this.beginIndex || this.lastMaxVisibleItem != this.maxVisibleItem ) {
        this.lastBeginIndex = this.beginIndex;
        this.lastMaxVisibleItem = this.maxVisibleItem;
        this.scrollableBody.addClass("widget-circle-loading")
        this.sendDataToServer({
          beginIndex: this.beginIndex,
          maxVisibleItem: this.maxVisibleItem,
        });
      }
    },
    
    onScroll: function() {
      this.updateView();
    },
    
    updateView:function(forceUpdate) {
      if(!this.start)
      return;
      //TODO use dummy rows
      //TODO store next scrolls positions before update

      var scrollTopPx = this.scrollableBody[0].scrollTop;
      
      var topIndex = Math.round(scrollTopPx / this.rowHeight) - this.throtleRows;
      var bottomIndex = topIndex + this.maxVisibleItem;
      if(bottomIndex >= this.size){
        topIndex -= bottomIndex - this.size;
        bottomIndex = this.size;
      }
      if(topIndex < 0){
        bottomIndex -= topIndex;
        topIndex = 0;
      }
      
      var topPositionPx = topIndex * this.rowHeight;
      var marginTopPx = topPositionPx ;
      var marginBottomPx = (this.size - bottomIndex) * this.rowHeight;

      //if size < available space
      if(marginBottomPx < 0) {
        marginBottomPx = 0;
      }

      //prevent too much drawing
      if(forceUpdate || this.marginBottomPx ==  undefined ||
        Math.abs(this.beginIndex - topIndex) >= this.throtleRows ||
        topIndex == 0 && this.beginIndex != 0 ||
        bottomIndex == this.size && this.marginBottomPx != 0) {

        this.beginIndex = topIndex;
        this.marginTopPx = marginTopPx;
        this.marginBottomPx = marginBottomPx;

        this.table.css("margin-top", marginTopPx + "px");
        this.table.css("margin-bottom",  marginBottomPx +"px");


        //even odd pb
        this.sendInformations();
      }
    },

    //callback from server
    onDraw:function() {
      this.scrollableBody.removeClass("widget-circle-loading")
    }
  });
})();