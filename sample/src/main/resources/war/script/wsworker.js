
var ws;

onmessage = function(e) {
	var data = e.data;
	if(data.cmd == 'connect') {
		connect(data);
	} else if(data.cmd == 'send') {
		send(data);
	} else if(data.cmd == 'close') {
		close(data);
	}
};

function debug(message) {                                                           
	postMessage({'cmd':'debug', 'message':message});
}

function connect(data) {
	ws = new WebSocket(data.url);
	ws.onopen = onopenWs;
	ws.onmessage = onmessageWs;
	ws.onclose = oncloseWs;
}

function close(data) {
	ws.close();
}

function send(data) {
	ws.send(data.message);
}

function onopenWs() {
	postMessage( {'cmd':'onopen'});
}

function oncloseWs(message) {
	postMessage( {'cmd':'onclose', 'message':''});
}

function onmessageWs(message) {
	if(message.data) {
		var msg = JSON.parse(message.data);
		if(msg['4']!=null) return;
		postMessage( {'cmd':'onmessage', 'message':msg});
	}
}
