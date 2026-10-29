// The application the portal's pages embed: the showcase on port 8081 of this host, or the query's app when it is on
// this machine. Never a URL of another site's choosing: the pages load the application's terminal from it.
(function () {
    'use strict';
    var fallback = location.protocol + '//' + location.hostname + ':8081/sample/';
    var asked = new URLSearchParams(location.search).get('app');
    var app = fallback;
    if (asked) {
        try {
            var url = new URL(asked);
            var local = ['localhost', '127.0.0.1', '[::1]', location.hostname];
            if ((url.protocol === 'http:' || url.protocol === 'https:') && local.indexOf(url.hostname) >= 0) app = url.href;
        } catch (e) {
            // not a URL: the showcase
        }
    }
    window.portalApp = app;
}());
