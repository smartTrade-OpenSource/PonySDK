"use strict";
var Widget = function(pony, params) {
	this.logLevel = 0;
	this.name = this.getName();
	this.id = params.id;
	this.widgetId = params.widgetID;
	if(typeof params.widgetElement != 'undefined') {
		this.element = params.widgetElement;
	}
	this.options = {};
	this.initializated = false;
}

Widget.prototype.onInit = function() {
    if(!this.initializated ) {
        this.init();
    }
    this.initializated = true;
}

Widget.prototype.onAttach = function(attached) {
	this.onInit();
	pony.sendDataToServer(this.id, { attached: attached });
	if( attached === false ) this.onDetached();
}

Widget.prototype.onDetached = function() {
}

Widget.prototype.sendDataToServer = function( data ) {
	pony.sendDataToServer(this.id, data);
}

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
}

Widget.prototype.log = function() {
	var args = Array.prototype.slice.call(arguments, 0);
	var logArgs = [];
	logArgs.push("[" + this.name + "]");
	logArgs.push(args[0]);
	if(args.length == 2) logArgs.push(args[1]);
	else if(args.length > 2) logArgs.push(args.slice(1));
	console.log.apply(console, logArgs);
}

Widget.prototype.setLog = function(level) {
	this.logLevel = parseInt(level);
}

Widget.prototype.update = function( d ) {
	if(!d.hasOwnProperty( 'method' ))
		throw  "[" + this.name + "] Couldn't find 'method' property in received data";

	var methodName = d['method'];
	if(!(methodName in  this) || typeof this[methodName] !== 'function')
		throw  "[" + this.name + "] Called method '" + methodName + "' that does not exist";

	if(!this.initializated)
		throw "[" + this.name + "] Tried to call method '" + methodName + "' before init()";

	try {
		if(d.hasOwnProperty('args')) {
			if(this.logLevel > 0) this.log(methodName, d['args']);
			this[methodName].apply(this, d['args']);
		} else {
			if(this.logLevel > 0) this.log(methodName);
			this[methodName].call(this);
		}
	} catch (e) {
		throw "[" + this.name + "] Exception (in '" + methodName + "'): " + e;
	}
}

Widget.prototype.init = function() {
	throw "[" + this.name + "] abstract function Widget.init() MUST be implemented";
}

Widget.new = function(javaClass, obj) {
	var clazz = function(name, a, b) { Widget.call(this, name, a, b); }
	obj.__proto__ = Object.create( Widget.prototype );
	clazz.prototype = Object.create( obj );
	clazz.prototype.javaClass = javaClass;
	clazz.prototype.constructor = Widget;
	if(Object.prototype.toString.call(javaClass) === '[object Array]') {
		for(var i=0; i<javaClass.length; i++) {
			this.registerBindedWidget(javaClass[i], clazz);
		}
	} else {
		this.registerBindedWidget(javaClass, clazz);
	}
}

Widget.registerBindedWidget = function(className, jsClass) {
	console.log("registering PAddon: " + className );
	var callback = function(params) {
		return new jsClass(pony, params);
	}
	if(document.ponyLoaded) {
		pony.registerAddOnFactory(className, callback);
	} else {
		document.onPonyLoaded(function(pony) {
			pony.registerAddOnFactory(className, callback);
		});
	}
}
