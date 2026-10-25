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
