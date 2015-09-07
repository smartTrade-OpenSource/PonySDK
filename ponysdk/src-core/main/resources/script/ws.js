window.WebSocket = WorkerWebSocket;

function WorkerWebSocket(server) {
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
		pony.executeInstruction(response);
	}

	this.send = function(message) {
		worker.postMessage({'cmd': 'send', 'message': message});
	}

	this.close = function() {
		worker.postMessage({'cmd': 'close'});
	}

}

