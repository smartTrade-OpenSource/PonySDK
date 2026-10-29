/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 *
 * The JavaScript of com.ponysdk.core.ui.fdc3.Fdc3 (docs/decisions/0014-fdc3.md): the page's FDC3 desktop agent,
 * window.fdc3, driven from Java. Listed with the application's scripts, after ponysdk.js.
 */
(function () {
  "use strict";

  var READY_TIMEOUT_MS = 5000;

  // The agent of the page: window.fdc3, or the one its fdc3Ready event brings; none in time is null
  function findAgent() {
    if (window.fdc3) return Promise.resolve(window.fdc3);
    return new Promise(function (resolve) {
      var timer = setTimeout(function () { resolve(null); }, READY_TIMEOUT_MS);
      window.addEventListener('fdc3Ready', function () {
        clearTimeout(timer);
        resolve(window.fdc3 || null);
      }, { once: true });
    });
  }

  function errorOf(e) {
    return e && e.message ? e.message : String(e);
  }

  // A result the server sent as text ("null", a number, a quoted string); an object or an array arrives parsed
  function parsed(value) {
    if (typeof value !== 'string') return value;
    try {
      return JSON.parse(value);
    } catch (e) {
      return value;
    }
  }

  AbstractAddon.defineAddon("com.ponysdk.core.ui.fdc3.Fdc3", {

    init: function () {
      var self = this;
      this._contextListeners = {}; // type ("*" for any) -> the agent's Listener promise
      this._intentListeners = {};  // intent -> the agent's Listener promise
      this._intentRequests = {};   // request id -> { resolve, reject }
      this._nextRequest = 0;
      this._agent = findAgent().then(function (fdc3) {
        if (fdc3 === null) {
          self.sendDataToServer({ state: 'unavailable' });
          return null;
        }
        return Promise.all([fdc3.getInfo(), fdc3.getUserChannels(), fdc3.getCurrentChannel()]).then(function (all) {
          self.sendDataToServer({
            state: 'ready',
            info: all[0],
            userChannels: all[1].map(function (c) { return { id: c.id, displayMetadata: c.displayMetadata || {} }; }),
            channel: all[2] ? all[2].id : null
          });
          return fdc3;
        });
      });
    },

    // Runs once the agent is there; nothing without one
    _withAgent: function (action) {
      var self = this;
      this._agent.then(function (fdc3) {
        if (fdc3 !== null) return action(fdc3);
      }).catch(function (e) {
        self.log('FDC3 call failed', e);
      });
    },

    joinUserChannel: function (channelId) {
      var self = this;
      this._withAgent(function (fdc3) {
        return fdc3.joinUserChannel(channelId).then(function () { self.sendDataToServer({ channel: channelId }); });
      });
    },

    leaveCurrentChannel: function () {
      var self = this;
      this._withAgent(function (fdc3) {
        return fdc3.leaveCurrentChannel().then(function () { self.sendDataToServer({ channel: null }); });
      });
    },

    broadcast: function (context) {
      this._withAgent(function (fdc3) { return fdc3.broadcast(context); });
    },

    listenContext: function (type) {
      var self = this;
      var listener = this._agent.then(function (fdc3) {
        if (fdc3 === null) return null;
        return fdc3.addContextListener(type === '*' ? null : type, function (context) {
          self.sendDataToServer({ listener: type, context: context });
        });
      });
      this._contextListeners[type] = listener;
    },

    unlistenContext: function (type) {
      var listener = this._contextListeners[type];
      delete this._contextListeners[type];
      if (listener) listener.then(function (l) { if (l) l.unsubscribe(); });
    },

    raiseIntent: function (id, intent, context) {
      var self = this;
      this._agent.then(function (fdc3) {
        if (fdc3 === null) throw new Error('No FDC3 desktop agent');
        return fdc3.raiseIntent(intent, context);
      }).then(function (resolution) {
        var app = resolution.source ? resolution.source.appId : null;
        var result = resolution.getResult ? resolution.getResult() : Promise.resolve(undefined);
        return result.then(function (value) {
          self.sendDataToServer({ raised: id, app: app, result: value === undefined ? null : value });
        });
      }).catch(function (e) {
        self.sendDataToServer({ raised: id, error: errorOf(e) });
      });
    },

    listenIntent: function (intent) {
      var self = this;
      this._intentListeners[intent] = this._agent.then(function (fdc3) {
        if (fdc3 === null) return null;
        return fdc3.addIntentListener(intent, function (context) {
          // The handler's answer comes from the server: intentResult or intentFailed
          var request = ++self._nextRequest;
          return new Promise(function (resolve, reject) {
            self._intentRequests[request] = { resolve: resolve, reject: reject };
            self.sendDataToServer({ intent: intent, request: request, context: context });
          });
        });
      });
    },

    unlistenIntent: function (intent) {
      var listener = this._intentListeners[intent];
      delete this._intentListeners[intent];
      if (listener) listener.then(function (l) { if (l) l.unsubscribe(); });
    },

    intentResult: function (request, value) {
      var pending = this._intentRequests[request];
      delete this._intentRequests[request];
      if (pending) pending.resolve(parsed(value) === null ? undefined : parsed(value));
    },

    intentFailed: function (request, message) {
      var pending = this._intentRequests[request];
      delete this._intentRequests[request];
      if (pending) pending.reject(new Error(message));
    },

    destroy: function () {
      var self = this;
      Object.keys(this._contextListeners).forEach(function (type) { self.unlistenContext(type); });
      Object.keys(this._intentListeners).forEach(function (intent) { self.unlistenIntent(intent); });
    }
  });
})();
