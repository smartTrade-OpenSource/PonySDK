// The gallery's links (index.html and the pages of each way to integrate PonySDK): no PonySDK in it. The application
// is the showcase, on port 8081 of this host, or the query's app; another site is this host's other name.
(function () {
    'use strict';
    var query = new URLSearchParams(location.search);
    var app = query.get('app') || location.protocol + '//' + location.hostname + ':8081/sample/';
    // 127.0.0.1 and localhost, or [::1], are different sites: the session cookie does not go from one to the other
    var otherHost = location.hostname === '127.0.0.1' ? 'localhost' : '127.0.0.1';
    var otherSite = location.protocol + '//' + otherHost + ':' + location.port + '/portal.html?app=' + encodeURIComponent(app);
    window.gallery = { app: app, otherSite: otherSite };

    // href="{app}…" and href="{other-site}": the application's URLs on this machine
    document.querySelectorAll('a[data-href]').forEach(function (link) {
        link.href = link.getAttribute('data-href').replace('{app}', app).replace('{other-site}', otherSite);
    });
    // <pony-app data-src="{app}…">: its src, once the terminal that defines the element is loaded
    document.querySelectorAll('pony-app[data-src]').forEach(function (element) {
        element.setAttribute('src', element.getAttribute('data-src').replace('{app}', app));
    });
    document.querySelectorAll('iframe[data-src]').forEach(function (frame) {
        frame.src = frame.getAttribute('data-src').replace('{app}', app);
    });
    document.querySelectorAll('[data-text="app"]').forEach(function (node) {
        node.textContent = app;
    });
    // A page that embeds the application loads its terminal, which defines <pony-app>
    if (document.body.hasAttribute('data-embeds')) import(app + 'ponyterminal/terminal.js');
}());
