/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package bench;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;

import jakarta.websocket.CloseReason;
import jakarta.websocket.Session;

import com.ponysdk.driver.PonyBandwithListener;
import com.ponysdk.driver.PonySDKWebDriver;
import com.ponysdk.driver.PonySessionListener;

/**
 * Opens {@code sessions} drivers (the version's own) through a counting TCP proxy in front of the server and prints,
 * every second, {@code cstat <epochMs> <server-to-client TCP bytes> <client-to-server TCP bytes> <decompressed bytes
 * received> <open sessions>}.
 * <p>
 * {@code BenchClient <server port> <proxy port> <sessions> <seconds>}
 */
public final class BenchClient {

    static final LongAdder s2c = new LongAdder(), c2s = new LongAdder(), raw = new LongAdder();
    static final LongAdder open = new LongAdder();

    public static void main(final String[] args) throws Exception {
        final int serverPort = Integer.parseInt(args[0]);
        final int proxyPort = Integer.parseInt(args[1]);
        final int sessions = Integer.parseInt(args[2]);
        final int seconds = Integer.parseInt(args[3]);

        final ServerSocket proxy = new ServerSocket();
        proxy.bind(new InetSocketAddress("127.0.0.1", proxyPort), 512);
        Thread.ofVirtual().start(() -> {
            while (true) {
                try {
                    final Socket client = proxy.accept();
                    final Socket server = new Socket("127.0.0.1", serverPort);
                    client.setTcpNoDelay(true);
                    server.setTcpNoDelay(true);
                    pipe(server, client, s2c);
                    pipe(client, server, c2s);
                } catch (final Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
        });

        final PonyBandwithListener bandwidth = new PonyBandwithListener() {
            @Override
            public void onReceive(final int bytes) {
                raw.add(bytes);
            }

            @Override
            public void onSend(final int bytes) {
            }

            @Override
            public void onReceiveCompressed(final int bytes) {
            }

            @Override
            public void onSendCompressed(final int bytes) {
            }
        };
        final PonySessionListener sessionListener = new PonySessionListener() {
            @Override
            public void onOpen(final Session session) {
                open.increment();
            }

            @Override
            public void onClose(final Session session, final CloseReason closeReason) {
                open.decrement();
            }

            @Override
            public void onError(final Session session, final Throwable thr) {
                System.err.println("session error " + thr);
            }
        };

        final List<PonySDKWebDriver> drivers = new ArrayList<>();
        final String url = "ws://127.0.0.1:" + proxyPort + "/bench/ws";
        for (int i = 0; i < sessions; i++) {
            final PonySDKWebDriver driver = new PonySDKWebDriver(null, bandwidth, sessionListener, true);
            driver.get(url);
            drivers.add(driver);
            Thread.sleep(10);
        }
        System.out.println("opened " + drivers.size());

        final long end = System.currentTimeMillis() + seconds * 1000L;
        long next = (System.currentTimeMillis() / 1000 + 1) * 1000;
        while (next <= end) {
            Thread.sleep(Math.max(0, next - System.currentTimeMillis()));
            System.out.println("cstat " + System.currentTimeMillis() + " " + s2c.sum() + " " + c2s.sum() + " " + raw.sum() + " "
                    + open.sum());
            next += 1000;
        }
        for (final PonySDKWebDriver d : drivers) d.close();
        System.exit(0);
    }

    private static void pipe(final Socket from, final Socket to, final LongAdder counter) {
        Thread.ofVirtual().start(() -> {
            final byte[] buf = new byte[16384];
            try (InputStream in = from.getInputStream(); OutputStream out = to.getOutputStream()) {
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                    counter.add(n);
                }
            } catch (final Exception ignored) {
                // a side closed
            } finally {
                try {
                    from.close();
                    to.close();
                } catch (final Exception ignored) {
                }
            }
        });
    }
}
