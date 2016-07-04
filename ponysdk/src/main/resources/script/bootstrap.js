var pony = null;
document.ponyLoaded = false;
document.onPonyLoadedListeners = [];

document.onPonyLoaded = function (callback) {
    document.onPonyLoadedListeners.push(callback);
};

function onPonySDKModuleLoaded() {
    console.log("onPonySDKModuleLoaded");
    pony = new ponysdk();
    document.ponyLoaded = true;

    for (var i = 0; i < document.onPonyLoadedListeners.length; i++) {
        try {
            document.onPonyLoadedListeners[i](pony);
        } catch (error) {
            throw "cannot call onPonyLoaded callback: " + document.onPonyLoadedListeners[i] + ", error " + error;
        }
    }

    pony.registerCommunicationError(function (code, message) {
        // Do Nothing
    });

    pony.start();
}

//Addon
var Addon = function (pony, params) {
    this.logLevel = 0;
    this.name = this.getName();
    this.id = params.id;
    this.widgetId = params.widgetID;
    if (typeof params.widgetElement != 'undefined') {
        this.element = params.widgetElement;
    }
    this.options = {};
    this.initialized = false;
};

Addon.prototype.onInit = function () {
    if (!this.initialized) {
        this.init();
    }
    this.initialized = true;
};

Addon.prototype.init = function () {
    throw "[" + this.name + "] abstract function Addon.init() MUST be implemented";
};

Addon.prototype.onAttached = function () {
    this.jqelement = $(this.element).attr("id", this.id);
    this.initDom();
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

Addon.prototype.setLog = function (level) {
    this.logLevel = parseInt(level);
};

Addon.prototype.update = function (d) {
    var methodName = d['m'];

    if (!this.initialized)
        throw "[" + this.name + "] Tried to call method '" + methodName + "' before init()";

    try {
        if (d.hasOwnProperty('arg')) {
            if (this.logLevel > 0) this.log(methodName, d['arg']);
            this[methodName].apply(this, d['arg']);
        } else {
            if (this.logLevel > 0) this.log(methodName);
            this[methodName].call(this);
        }
    } catch (e) {
        console.log(e.stack);
        throw "[" + this.name + "] Exception (in '" + methodName + "'): " + e;
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
            this.registerBindedAddon(javaClass[i], clazz);
            if (obj.globalInit != undefined) Addon._initMethods[javaClass[i]] = obj.globalInit.bind(obj);
        }
    } else {
        this.registerBindedAddon(javaClass, clazz);
        if (obj.globalInit != undefined) Addon._initMethods[javaClass] = obj.globalInit.bind(obj)
    }
};

Addon.registerBindedAddon = function (className, jsClass) {
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

Addon.globalInit = function (args) {
    throw "[" + this.name + "] abstract function Addon.globalInit() MUST be implemented"
};

Addon._initMethods = {};

Addon.initAddon = function (signature, args) {
    Addon._initMethods[signature](args);
};


// Decode ArrayBuffer from server
var textDecoder = null;
if ('TextDecoder' in window) textDecoder = new TextDecoder('utf-8');

function decode(buffer) {
    if (textDecoder != null) return textDecoder.decode(buffer);
    else return UTF8.getStringFromBytes(new Uint8Array(buffer));
}


