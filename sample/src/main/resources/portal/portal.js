// The portal's own script: no PonySDK in it. It loads the application's terminal, which defines <pony-app>, and
// talks to the embedded applications through attributes and events (docs/decisions/0008-embedded-application.md).
(function () {
    'use strict';
    // The showcase, on the same host as this page: same site, so the session cookie goes with each request
    var app = new URLSearchParams(location.search).get('app') || location.protocol + '//' + location.hostname + ':8081/sample/';
    import(app + 'ponyterminal/terminal.js');

    var tickets = [document.getElementById('ticket-1'), document.getElementById('ticket-2')];
    tickets.forEach(function (ticket) {
        // Who the user is, from the portal's own sign-in (docs/decisions/0009-embedding-limits.md): a property, never
        // an attribute. A real portal returns its identity provider's token here, which the application validates
        ticket.credential = function () { return Promise.resolve('alice@northwind'); };
        ticket.setAttribute('src', app);
    });
    document.getElementById('showcase').setAttribute('src', app + '#inputs');

    // Attributes in: the first ticket follows the portal's instrument
    document.getElementById('instrument').addEventListener('change', function (event) {
        tickets[0].setAttribute('instrument', event.target.value);
    });

    // Events in: the tickets put their quantity back
    document.getElementById('reset').addEventListener('click', function () {
        tickets.forEach(function (ticket) { ticket.dispatchEvent(new CustomEvent('reset')); });
    });

    // Events out: each trade lands in the portal's blotter
    var trades = document.getElementById('trades');
    document.addEventListener('trade', function (event) {
        var trade = event.detail;
        var empty = trades.querySelector('.portal-empty');
        if (empty) empty.remove();
        var row = document.createElement('tr');
        [trade.time, event.target.id, trade.side, trade.quantity.toLocaleString('en-US'), trade.instrument, String(trade.price)]
            .forEach(function (value) {
                var cell = document.createElement('td');
                cell.textContent = value;
                row.appendChild(cell);
            });
        row.className = trade.side === 'BUY' ? 'portal-buy' : 'portal-sell';
        trades.insertBefore(row, trades.firstChild);
    });

    // The title and the location an application asks for: the portal's to decide (docs/decisions/0009-embedding-limits.md).
    // A ticket's title captions it; a location opens in a tab of its own, the portal stays
    document.addEventListener('pony-title', function (event) {
        var caption = document.getElementById(event.target.id + '-title');
        if (caption) caption.textContent = event.detail;
    });
    document.addEventListener('pony-location', function (event) {
        window.open(event.detail, '_blank', 'noopener');
    });

    // The Java code behind the tickets: the application's sources, copied beside this page by the build. A region
    // marked "// region <name>" ... "// endregion" shows alone; a file shows from its class's Javadoc, imports left out
    document.querySelectorAll('pre[data-source]').forEach(function (pre) {
        fetch(pre.dataset.source).then(function (response) {
            if (!response.ok) throw new Error(response.status + ' ' + pre.dataset.source);
            return response.text();
        }).then(function (text) {
            var lines = text.split('\n');
            var region = pre.dataset.region;
            var start = region
                ? lines.findIndex(function (line) { return line.trim().indexOf('// region ' + region) === 0; }) + 1
                : lines.findIndex(function (line) { return line.indexOf('/**') === 0; });
            var end = region
                ? lines.findIndex(function (line, i) { return i >= start && line.trim().indexOf('// endregion') === 0; })
                : lines.length;
            lines = lines.slice(start, end);
            var indent = Math.min.apply(null, lines.filter(function (line) { return line.trim(); })
                .map(function (line) { return line.match(/^ */)[0].length; }));
            pre.textContent = lines.map(function (line) { return line.slice(indent); }).join('\n').trim();
        }).catch(function (error) {
            pre.textContent = 'Not loaded: ' + error.message;
        });
    });

    // The applications' connection
    var status = document.getElementById('status');
    document.addEventListener('pony-connection-lost', function (event) {
        status.textContent = 'Connection lost: ' + event.target.id;
        status.dataset.state = 'lost';
    });
    document.addEventListener('pony-reconnected', function (event) {
        status.textContent = 'Reconnected: ' + event.target.id;
        status.dataset.state = 'back';
    });
}());
