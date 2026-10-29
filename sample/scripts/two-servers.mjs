// Two showcase servers behind a load balancer, on this machine: what a drain does to the pages of a server that
// stops (docs/guide/deployment.md, ADR-0017).
//
//   ./gradlew :sample:runTwoServers                    then open http://localhost:8090/sample/
//   curl localhost:8090/lb                              the servers, their state, their WebSocket connections
//   curl -X POST localhost:8090/lb/stop/a              server a leaves the balancer, then gets SIGTERM, as ECS does:
//                                                      its pages reload on b, in waves, each at its address
//   curl -X POST localhost:8090/lb/start/a             a starts again and takes new pages once its health check is UP
//
// As an Application Load Balancer: a cookie keeps a browser on its server (stickiness), a server leaves the balancer
// before it is stopped (deregistration) or when its health check is not 200, and a page whose server left goes to
// another one. Arguments, given by Gradle: java, the showcase's classpath, its JVM options.

import { spawn } from 'node:child_process';
import { randomBytes } from 'node:crypto';
import fs from 'node:fs';
import http from 'node:http';
import net from 'node:net';

const [java, classpath, ...jvmArgs] = process.argv.slice(2);
const PORT = 8090;
const LOGS = 'build/two-servers';
const HEALTH = '/sample/health';
// One state transfer key for both servers, as a secret store gives every server of an application (ADR-0018)
const STATE_KEY = randomBytes(32).toString('base64');

/** @type {Record<string, {name: string, port: number, process: import('node:child_process').ChildProcess | null,
 *   state: 'stopped' | 'starting' | 'up' | 'draining', sockets: number}>} */
const servers = {
    a: { name: 'a', port: 8091, process: null, state: 'stopped', sockets: 0 },
    b: { name: 'b', port: 8093, process: null, state: 'stopped', sockets: 0 },
};
let turn = 0;

function start(server) {
    if (server.process) return;
    const log = fs.openSync(`${LOGS}/${server.name}.log`, 'a');
    server.process = spawn(java, [...jvmArgs, `-Dponysdk.port=${server.port}`, '-cp', classpath,
        'com.ponysdk.impl.java.ShowcaseMain'], { stdio: ['ignore', log, log], env: { ...process.env, PONYSDK_STATE_KEY: STATE_KEY } });
    server.state = 'starting';
    console.log(`server ${server.name}: starting on ${server.port} (pid ${server.process.pid}, ${LOGS}/${server.name}.log)`);
    server.process.on('exit', code => {
        console.log(`server ${server.name}: stopped (${code ?? 'signal'})`);
        server.process = null;
        server.state = 'stopped';
    });
}

/** Out of the balancer first, then SIGTERM: the server drains its pages to the other one, then stops. */
function stop(server) {
    if (!server.process) return;
    server.state = 'draining';
    console.log(`server ${server.name}: out of the balancer, SIGTERM`);
    server.process.kill('SIGTERM');
}

function checkHealth() {
    for (const server of Object.values(servers)) {
        if (!server.process || server.state === 'draining') continue;
        http.get({ host: '127.0.0.1', port: server.port, path: HEALTH, timeout: 1000 }, res => {
            res.resume();
            if (server.state === 'draining' || !server.process) return;
            const up = res.statusCode === 200;
            if (up && server.state !== 'up') console.log(`server ${server.name}: up`);
            server.state = up ? 'up' : 'starting';
        }).on('error', () => { if (server.state === 'up') server.state = 'starting'; });
    }
}

/** The server of the browser's cookie while it is up, otherwise the next one up. */
function pick(request) {
    const sticky = /(?:^|;\s*)lb=(\w+)/.exec(request.headers.cookie ?? '')?.[1];
    if (servers[sticky]?.state === 'up') return servers[sticky];
    const up = Object.values(servers).filter(s => s.state === 'up');
    return up.length ? up[turn++ % up.length] : null;
}

function admin(request, response) {
    const [, action, name] = /^\/lb(?:\/(stop|start)\/(\w+))?\/?$/.exec(request.url) ?? [];
    const server = servers[name];
    if (action && request.method === 'POST' && server) {
        (action === 'stop' ? stop : start)(server);
    } else if (action || request.method !== 'GET') {
        response.writeHead(404).end();
        return;
    }
    response.writeHead(200, { 'content-type': 'application/json', 'cache-control': 'no-store' });
    response.end(JSON.stringify({ servers: Object.values(servers).map(({ name, port, state, sockets, process }) =>
        ({ name, port, state, sockets, pid: process?.pid ?? null })) }));
}

function proxy(request, response) {
    if (request.url === '/lb' || request.url.startsWith('/lb/')) return admin(request, response);
    const server = pick(request);
    if (!server) {
        response.writeHead(503, { 'content-type': 'text/plain' }).end('No server up');
        return;
    }
    const upstream = http.request({ host: '127.0.0.1', port: server.port, method: request.method, path: request.url,
        headers: request.headers }, res => {
        const headers = { ...res.headers, 'x-served-by': server.name };
        headers['set-cookie'] = [...(res.headers['set-cookie'] ?? []), `lb=${server.name}; Path=/; SameSite=Lax`];
        response.writeHead(res.statusCode, headers);
        res.pipe(response);
    });
    upstream.on('error', () => response.headersSent ? response.destroy() : response.writeHead(502).end());
    request.pipe(upstream);
}

/** A WebSocket goes to the page's server; the bytes are relayed as they are. */
function upgrade(request, socket, head) {
    const server = pick(request);
    if (!server) return socket.destroy();
    const upstream = net.connect(server.port, '127.0.0.1', () => {
        const lines = [`${request.method} ${request.url} HTTP/1.1`];
        for (let i = 0; i < request.rawHeaders.length; i += 2) lines.push(`${request.rawHeaders[i]}: ${request.rawHeaders[i + 1]}`);
        upstream.write(lines.join('\r\n') + '\r\n\r\n');
        if (head.length) upstream.write(head);
        upstream.pipe(socket);
        socket.pipe(upstream);
    });
    server.sockets++;
    let open = true;
    const close = () => {
        if (!open) return;
        open = false;
        server.sockets--;
        socket.destroy();
        upstream.destroy();
    };
    for (const end of [socket, upstream]) end.on('close', close).on('error', close);
}

fs.mkdirSync(LOGS, { recursive: true });
for (const host of ['127.0.0.1', '::1']) {
    // loopback only: the /lb actions stop this machine's processes
    http.createServer(proxy).on('upgrade', upgrade).listen(PORT, host);
}
Object.values(servers).forEach(start);
setInterval(checkHealth, 250);
console.log(`balancer: http://localhost:${PORT}/sample/ ; servers: curl localhost:${PORT}/lb`);

function quit() {
    Object.values(servers).forEach(stop);
    setInterval(() => Object.values(servers).every(s => !s.process) && process.exit(0), 100);
    setTimeout(() => process.exit(0), 10_000);
}
process.on('SIGINT', quit);
process.on('SIGTERM', quit);
