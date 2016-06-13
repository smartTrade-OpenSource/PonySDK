/* global Widget */
"use strict";
Widget.new("com.ponysdk.sample.client.page.addon.LoggerAddOn", 
{
	
	init: function() {
		console.log("LabelPAddOn created");
	},
	
	log: function() {
		console.log("LabelPAddOn");
	},
	
	logWithText: function(value) {
		console.log("LabelPAddOn : " + value);
	},
	
});