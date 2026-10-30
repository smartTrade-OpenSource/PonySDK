/**
 * Loads Web Awesome (docs/decisions/0028-web-awesome.md) on the first page that builds one of its elements: the page
 * calls PScript.call(window, "loadWebAwesome"), by name, as the policy of docs/decisions/0015-csp.md asks.
 *
 * The library is served by the application itself (build/generated/resources/webawesome, script/webawesome/**), never
 * by a CDN: one `import()` of the library's own bundled loader, from the origin of this script, which 'self' allows.
 * Its autoloader then defines each <wa-*> element the first time one is in the document, fetching that component's
 * module alone. A page that does not use Web Awesome costs this file (2 KB, 1 KB gzip) and nothing else.
 */
(function () {
    var base = new URL('webawesome/', document.currentScript.src).href; // script/webawesome/, absolute: embedded pages too
    var loading = null;

    function stylesheet(id, href) {
        if (document.getElementById(id)) return;
        var link = document.createElement('link');
        link.id = id;
        link.rel = 'stylesheet';
        link.href = href;
        document.head.appendChild(link);
    }

    window.loadWebAwesome = function () {
        if (loading) return loading;
        // The theme scope of the page's container (class pony-theme) and the bridge that gives its tokens to the library
        stylesheet('pony-theme-scoped', new URL('../../style/pony-theme.css', base).href);
        stylesheet('webawesome-theme', base + 'styles/themes/default.css');
        stylesheet('webawesome-bridge', new URL('../../css/pony-webawesome.css', base).href);
        // The base path first: the autoloader starts when its module is evaluated, and looks the components up there. No
        // local icons are shipped: an icon is the system library's (inline), or a library the application registers.
        loading = import(base + 'utilities/base-path.js').then(function (path) {
            path.setBasePath(base);
            return import(base + 'webawesome.loader.js');
        }).catch(function (error) {
            loading = null; // the next page that asks tries again
            console.error('Web Awesome could not be loaded from ' + base, error);
        });
        return loading;
    };
})();
