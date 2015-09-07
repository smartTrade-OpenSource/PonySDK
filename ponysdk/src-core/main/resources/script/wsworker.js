var ws;
var queue = [];
var fileReader = new FileReader();
                                              
fileReader.onload = function() { 
	postMessage( {'cmd':'onmessage', 'message': JSON.parse(fileReader.result)} );
	
    if(queue.length != 0){
         fileReader.readAsText(queue.shift());
    }
}

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
      if( fileReader.readyState !== 1 ){
          fileReader.readAsText(response.data);
      }else{
          queue.push(response.data);
      }
    }
}




