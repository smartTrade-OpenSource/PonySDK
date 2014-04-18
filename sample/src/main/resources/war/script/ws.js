//

var theWS = window.WebSocket;

function CustomWebSocket(server) {

	var that = this;
	var ws = new theWS(server);

	this.send = function(message) {
		ws.send(message);
	}

	this.close = function() {
		ws.close();
	}

	this.onopen0 = function() {
		console.log('open (CustomWebSocket)');
		that.onopen();
	}

	this.onmessage0 = function(response) {
		if(response.data) {
			var msg = JSON.parse(response.data);
			if(msg['4']!=null) return;
			
			var t1 = window.performance.now();
			pony.executeInstruction(msg);
			var t2 = window.performance.now();
			console.log('executed in ' + (t2 - t1) + ' ms');
		}
	}

	this.onclose0 = function(m) {
		console.log('close (CustomWebSocket)');
		that.onclose(m);
	}

	ws.onopen = this.onopen0;
	ws.onmessage = this.onmessage0;
	ws.onclose = this.onclose0;
}

window.WebSocket = CustomWebSocket;

/*
var stackMessage = false;
var messagesQueue = [];
var scrollTimer;

function attachScrollListener(id) {
	console.log('attaching: ' + id);
	document.getElementById(id).addEventListener('scroll', function() {
		clearTimeout(scrollTimer);
		stackMessage = true;
		scrollTimer = setTimeout(function(){
			stackMessage = false;
			purge();
		},500);
	}, false);
}

function purge() {
	var length = messagesQueue.length;
	console.log('purging #' + length);
	for(i=0; i<length; i++) {
		executeInstruction(messagesQueue[i]);
	}
	messagesQueue = [];
}

function stack(o) {
	messagesQueue[messagesQueue.length] = o;
}

function WorkerWebSocket(server) {

	console.log('Building custom ws:...');

	var that = this;
	var worker = new Worker('script/wsworker.js');
	worker.addEventListener('message', function(e) {
		var data = e.data;
		that.processWorkerMessage(data);
	}, false);
	worker.postMessage({'cmd': 'connect', 'url': server});

	this.processWorkerMessage = function(data) {
		if(data.cmd == 'onopen') {
			that.onopen();
		} else if(data.cmd == 'onclose') {
			that.onclose(data.message);
		} else if(data.cmd == 'onmessage') {
			that.onmessage0(data.message);
		} else if(data.cmd == 'debug') {
			console.log('debug: ' + data.message);
		}
	}

	this.onmessage0 = function(response) {
		//console.log('received ws: ' + JSON.stringify(response));
		//var t1 = window.performance.now();
		if(stackMessage) {
			stack(response);
		} else {
			pony.executeInstruction(response);
		}
		//var t2 = window.performance.now();
		//console.log('executed in ' + (t2 - t1) + ' ms');
	}

	this.send = function(message) {
		worker.postMessage({'cmd': 'send', 'message': message});
	}

	this.close = function() {
		worker.postMessage({'cmd': 'close'});
	}

}

window.WebSocket = WorkerWebSocket;
*/