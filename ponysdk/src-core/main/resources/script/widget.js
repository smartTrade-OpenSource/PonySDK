/* global pony */
"use strict";
/** @class Widget */
var Widget = function(pony, params) {
	this.logLevel = 0;
	this.name = this.getName();
	this.id = params.id;
	this.widgetId = params.widgetID;
	if(typeof params.widgetElement != 'undefined') {
		this.element = params.widgetElement;
	}
	this.options = {};
	this.initialized = false;
};

Widget.prototype.onInit = function() {
    if(!this.initialized) {
        this.init();
    }
    this.initialized = true;
};

/**	 @abstract  */
Widget.prototype.init = function() {
	throw "[" + this.name + "] abstract function Widget.init() MUST be implemented";
};

Widget.prototype.onInitDom = function() {
    this.jqelement = $(this.element).attr("id", this.id);
    this.initDom();
};

Widget.prototype.initDom = function() {
	// Specific behavior
};

Widget.prototype.onAttach = function(attached) {
    this.onInitDom();
	this.sendDataToServer({ att: attached });
	if( attached === false ) this.onDetached();
};

Widget.prototype.onDetached = function() {
};

Widget.prototype.sendDataToServer = function( data ) {
	pony.sendDataToServer(this.id, data);
};

Widget.prototype.getName = function() {
	if(Object.prototype.toString.call(this.javaClass) === '[object Array]') {
		var names = [];
		for(var i=0; i<this.javaClass.length; i++) {
			var splitted = this.javaClass[i].split(".");
			names.push(splitted[splitted.length -1]);
		}
		return names.join("/");
	} else {
		var splitted = this.javaClass.split(".");
		return splitted[splitted.length -1];
	}
};

Widget.prototype.log = function() {
	var args = Array.prototype.slice.call(arguments, 0);
	var logArgs = [];
	logArgs.push("[" + this.name + "]");
	logArgs.push(args[0]);
	if(args.length == 2) logArgs.push(args[1]);
	else if(args.length > 2) logArgs.push(args.slice(1));
	console.log.apply(console, logArgs);
};

Widget.prototype.setLog = function(level) {
	this.logLevel = parseInt(level);
};

Widget.prototype.update = function( d ) {
	if(!d.hasOwnProperty( 'm' ))
		throw  "[" + this.name + "] Couldn't find 'm' property in received data";

	var methodName = d['m'];
	if(!(methodName in  this) || typeof this[methodName] !== 'function')
		throw  "[" + this.name + "] Called method '" + methodName + "' that does not exist";

	if(!this.initialized)
		throw "[" + this.name + "] Tried to call method '" + methodName + "' before init()";

	try {
		if(d.hasOwnProperty('arg')) {
			if(this.logLevel > 0) this.log(methodName, d['arg']);
			this[methodName].apply(this, d['arg']);
		} else {
			if(this.logLevel > 0) this.log(methodName);
			this[methodName].call(this);
		}
	} catch (e) {
		console.log(e.stack);
		throw "[" + this.name + "] Exception (in '" + methodName + "'): " + e;
	}
};

Widget.new = function(javaClass, obj) {
	var clazz = function(name, a, b) { Widget.call(this, name, a, b); }
	obj.__proto__ = Object.create( Widget.prototype );
	clazz.prototype = Object.create( obj );
	clazz.prototype.javaClass = javaClass;
	clazz.prototype.constructor = Widget;
	if(Object.prototype.toString.call(javaClass) === '[object Array]') {
		for(var i=0; i<javaClass.length; i++) {
			this.registerBindedWidget(javaClass[i], clazz);
			if(obj.globalInit != undefined) Widget._initMethods[javaClass[i]] = obj.globalInit.bind(obj);
		}
	} else {
		this.registerBindedWidget(javaClass, clazz);
		if(obj.globalInit != undefined) Widget._initMethods[javaClass] = obj.globalInit.bind(obj)
	}
};

Widget.registerBindedWidget = function(className, jsClass) {
	console.log("registering PAddon: " + className );
	var callback = function(params) {
		var addon = new jsClass(pony, params);
		if(typeof addon.properties != "undefined") {
			for(var key in addon.properties) {
				addon[key] = addon.properties[key];
				delete addon.properties;
			}
		}
		return addon;
	}
	if(document.ponyLoaded) {
		pony.registerAddOnFactory(className, callback);
	} else {
		document.onPonyLoaded(function(pony) {
			pony.registerAddOnFactory(className, callback);
		});
	}
};

Widget.globalInit = function(args) {
    throw "[" + this.name + "] abstract function Widget.globalInit() MUST be implemented"
};

Widget._initMethods = {};

Widget.initAddon = function(signature, args) {
	Widget._initMethods[signature](args);
};
