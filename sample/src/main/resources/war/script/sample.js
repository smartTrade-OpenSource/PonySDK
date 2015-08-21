//
function BasicJsAddon(pony, params) {
	console.log('Building new BasicJsAddon with #' + params);
	var self = this;
	self.id = params.id;

	document.onkeydown = function(evt) {
		evt = evt || window.event;
		if (evt.keyCode == 27) {
			var msg = 'Escape has been pressed';
			console.log(msg);
			pony.sendDataToServer(self.id, {'message' : msg});
		}
	};
}

BasicJsAddon.prototype = {
	update: function(data) {
		console.log('#' + this.id + ' received data: ' + data);
	},
	onAttach: function(attached) {
	}
}


function ReverseTextAddon(pony, params) {

	console.log('Building new ReverseTextAddon with #' + params);
	var self = this;
	self.id = params.id;
	self.widgetID = params.widgetID;

	var textbox = params.widgetElement;
	textbox.onblur = function() {
		var p = textbox.value;
		var n = p.split("").reverse().join("");
		textbox.value = n;
	}
}

ReverseTextAddon.prototype = {
	update: function(data) {
	},
	onAttach: function(attached) {
		if(attached) console.log('Widget #' + this.widgetID + ' attached');
	}
}


