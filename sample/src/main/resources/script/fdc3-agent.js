/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 *
 * A demonstration FDC3 2.1 desktop agent for the pages of one browser (docs/decisions/0014-fdc3.md): what a desktop
 * (OpenFin, interop.io, Connectifi…) gives each application as window.fdc3, enough to show the sample's applications
 * talking. Not a product: three user channels, context listeners, intents resolved to the first application that
 * listens, over a BroadcastChannel. It sets window.fdc3 only where no agent did.
 */
(function () {
  "use strict";

  if (window.fdc3 || typeof BroadcastChannel === 'undefined') return;

  var CLAIM_WAIT_MS = 400;
  var CHANNELS = [
    { id: 'fdc3.channel.1', type: 'user', displayMetadata: { name: 'Channel 1', color: '#dc2626', glyph: '1' } },
    { id: 'fdc3.channel.2', type: 'user', displayMetadata: { name: 'Channel 2', color: '#16a34a', glyph: '2' } },
    { id: 'fdc3.channel.3', type: 'user', displayMetadata: { name: 'Channel 3', color: '#2563eb', glyph: '3' } }
  ];

  var bus = new BroadcastChannel('ponysdk-fdc3-demo');
  var instanceId = Math.random().toString(36).slice(2, 10);
  var appId = document.documentElement.getAttribute('data-fdc3-app') || document.title || location.pathname;
  var current = null;                 // the user channel joined, or null
  var lastContexts = {};              // channel id -> { type -> the last context broadcast there }
  var contextListeners = [];          // { type, handler }
  var intentHandlers = {};            // intent -> handler
  var raises = {};                    // request id -> { claimed, resolve, reject, result }
  var deliveries = {};                // request id -> the raiser's instance, for the result

  function channel(id) {
    var found = CHANNELS.filter(function (c) { return c.id === id; })[0];
    if (!found) throw new Error('NoChannelFound');
    return found;
  }

  function remember(channelId, context, source) {
    (lastContexts[channelId] = lastContexts[channelId] || {})[context.type] = { context: context, source: source };
  }

  function deliver(context, source) {
    contextListeners.slice().forEach(function (l) {
      if (l.type === null || l.type === context.type) l.handler(context, { source: { appId: source } });
    });
  }

  function post(message) {
    message.from = instanceId;
    bus.postMessage(message);
  }

  bus.onmessage = function (event) {
    var m = event.data;
    if (m.kind === 'context') {
      remember(m.channel, m.context, m.app);
      if (m.channel === current) deliver(m.context, m.app);
    } else if (m.kind === 'raise') {
      if (intentHandlers[m.intent]) post({ kind: 'claim', request: m.request, to: m.from, app: appId });
    } else if (m.kind === 'claim' && m.to === instanceId) {
      var raise = raises[m.request];
      if (raise && !raise.claimed) {
        raise.claimed = true;
        raise.resolve({ intent: raise.intent, source: { appId: m.app, instanceId: m.from }, getResult: function () { return raise.result; } });
      }
    } else if (m.kind === 'deliver' && m.to === instanceId) {
      var handler = intentHandlers[m.intent];
      Promise.resolve().then(function () { return handler(m.context, { source: { appId: m.app } }); }).then(function (result) {
        post({ kind: 'result', request: m.request, to: m.from, result: result === undefined ? null : result });
      }, function (e) {
        post({ kind: 'result', request: m.request, to: m.from, error: e && e.message ? e.message : String(e) });
      });
    } else if (m.kind === 'result' && m.to === instanceId) {
      var done = raises[m.request];
      delete raises[m.request];
      if (done) done.settle(m);
    }
  };

  function listener(list, item) {
    list.push(item);
    return Promise.resolve({ unsubscribe: function () { var i = list.indexOf(item); if (i >= 0) list.splice(i, 1); } });
  }

  window.fdc3 = {
    getInfo: function () {
      return Promise.resolve({ fdc3Version: '2.1', provider: 'PonySDK demo agent', providerVersion: '1.0',
        appMetadata: { appId: appId, instanceId: instanceId } });
    },
    getUserChannels: function () { return Promise.resolve(CHANNELS.slice()); },
    getCurrentChannel: function () { return Promise.resolve(current === null ? null : channel(current)); },
    joinUserChannel: function (id) {
      channel(id);
      current = id;
      // A listener receives the channel's current context when its application joins it
      var contexts = lastContexts[id] || {};
      Object.keys(contexts).forEach(function (type) { deliver(contexts[type].context, contexts[type].source); });
      return Promise.resolve();
    },
    leaveCurrentChannel: function () { current = null; return Promise.resolve(); },
    broadcast: function (context) {
      if (current === null) return Promise.resolve(); // FDC3: no channel, no broadcast
      remember(current, context, appId);
      post({ kind: 'context', channel: current, context: context, app: appId });
      return Promise.resolve();
    },
    addContextListener: function (type, handler) {
      if (typeof type === 'function') { handler = type; type = null; }
      return listener(contextListeners, { type: type, handler: handler });
    },
    addIntentListener: function (intent, handler) {
      intentHandlers[intent] = handler;
      return Promise.resolve({ unsubscribe: function () { if (intentHandlers[intent] === handler) delete intentHandlers[intent]; } });
    },
    raiseIntent: function (intent, context) {
      var request = instanceId + '-' + Math.random().toString(36).slice(2, 8);
      return new Promise(function (resolve, reject) {
        var raise = { intent: intent, claimed: false, resolve: resolve };
        raise.result = new Promise(function (settleResolve, settleReject) {
          raise.settle = function (m) { if (m.error) settleReject(new Error(m.error)); else settleResolve(m.result === null ? undefined : m.result); };
        });
        raises[request] = raise;
        post({ kind: 'raise', request: request, intent: intent, context: context, app: appId });
        setTimeout(function () {
          if (!raise.claimed) {
            delete raises[request];
            reject(new Error('NoAppsFound'));
          }
        }, CLAIM_WAIT_MS);
      }).then(function (resolution) {
        // The context goes with the delivery: only the claimed application runs its handler
        bus.postMessage({ kind: 'deliver', request: request, to: resolution.source.instanceId, from: instanceId,
          intent: intent, context: context, app: appId });
        return resolution;
      });
    }
  };

  window.dispatchEvent(new Event('fdc3Ready'));
})();
