/* global Widget */
"use strict";
Widget.new("com.ponysdk.sample.client.page.addon.LabelPAddOn", {
	
	init: function() {
		console.log("com.ponysdk.sample.client.page.addon.LabelPAddOn created");
		alert("com.ponysdk.sample.client.page.addon.LabelPAddOn created");
	},
	
	log: function() {
		console.log("com.ponysdk.sample.client.page.addon.LabelPAddOn");
	},
	
	logWithText: function(value) {
		console.log("com.ponysdk.sample.client.page.addon.LabelPAddOn : " + value);
	},
	
});