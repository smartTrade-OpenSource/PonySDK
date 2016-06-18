var pony = null;
document.ponyLoaded = false;
document.onPonyLoadedListeners = [];
document.onConnectionLostListeners = [];

document.onPonyLoaded = function (callback) {
    document.onPonyLoadedListeners.push(callback);
};

document.onConnectionLost = function (callback) {
    document.onConnectionLostListeners.push(callback);
};

window.webappsdk = {};
var reconnectionInProgress = false;

var Check = function () {
};
Check.prototype = {
    errorDetected: false,
    counter: 3,
    delay_retry: 2000,
    delay_heartbeat: 2000,
    currentInitCheck: null,
    currentFailureCheck: null,
    reconnectionInProgress: false,

    initCheck: function () {
        if (window.opener === null || typeof window.opener == "undefined") {
            setTimeout(function () {
                var xmlhttp = new XMLHttpRequest();

                xmlhttp.onreadystatechange = function () {
                    if (xmlhttp.readyState == XMLHttpRequest.DONE) {
                        if (xmlhttp.status == 200) reconnectionCheck.onCheckSuccess();
                        else reconnectionCheck.onCheckError();
                    }
                };

                xmlhttp.open("GET", reconnectionCheck.getCheckUrl(), true);
                xmlhttp.send();

                /* Avoid JQuery dependencies
                 $.ajax({
                 url: reconnectionCheck.getCheckUrl(),
                 success: reconnectionCheck.onCheckSuccess,
                 error: reconnectionCheck.onCheckError
                 });
                 */
            }, reconnectionCheck.delay_heartbeat);
        }
    },

    failureCheck: function () {
        reconnectionCheck.counter--;
        var reconnectionElement = document.getElementById('reconnection'); // $('#reconnection');
        reconnectionElement.style.display = 'block'; // reconnectionElement.show();
        reconnectionElement.innerHTML = 'Connection to server lost<br>Reconnecting in ' + reconnectionCheck.counter + ' seconds <strong>...</strong>'; // reconnectionElement.html('Connection to server lost<br>Reconnecting in ' + reconnectionCheck.counter + ' seconds <strong>...</strong>');
        if (reconnectionCheck.counter == 0) {
            reconnectionElement.innerHTML = 'Reconnecting...'; // reconnectionElement.html('Reconnecting...');
            reconnectionCheck.reconnectionInProgress = true;
            window.clearInterval(reconnectionCheck.currentFailureCheck);
            reconnectionCheck.errorDetected = false;
            reconnectionCheck.currentInitCheck = setTimeout(reconnectionCheck.initCheck, reconnectionCheck.delay_heartbeat);
        }
    },

    onCheckError: function (data) {
        if (reconnectionCheck.errorDetected) return;

        reconnectionCheck.errorDetected = true;
        console.log("failure detected");
        notifyConnectionLostListeners();
        reconnectionCheck.counter = 3;

        reconnectionCheck.currentFailureCheck = setInterval(reconnectionCheck.failureCheck, reconnectionCheck.delay_retry);
    },

    getCheckUrl: function () {
        var i = document.URL.indexOf("#");
        var url = "";
        if (i === -1) {
            url = document.URL;
        } else {
            url = document.URL.substr(0, i);
        }
        return url + '?ping=' + new Date().getTime();
    },

    onCheckSuccess: function (data) {
        if (reconnectionCheck.reconnectionInProgress) {
            location.reload();
        } else {
            reconnectionCheck.currentInitCheck = setTimeout(reconnectionCheck.initCheck, reconnectionCheck.delay_heartbeat);
        }
    }
};

var reconnectionCheck;

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

    reconnectionCheck = new Check();
    reconnectionCheck.initCheck();
}

function notifyConnectionLostListeners() {
    for (var i = 0; i < document.onConnectionLostListeners.length; i++) {
        try {
            document.onConnectionLostListeners[i]();
        } catch (error) {
            throw "cannot call onConnectionLostListeners callback: " + document.onConnectionLostListeners[i] + ", error " + error;
        }
    }
}

// Decode ArrayBuffer from server
var textDecoder = null;
if ('TextDecoder' in window) textDecoder = new TextDecoder('utf-8');

function decode(buffer) {
    if (textDecoder != null) return textDecoder.decode(buffer);
    else return UTF8.getStringFromBytes(new Uint8Array(buffer));
}
