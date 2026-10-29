/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.server.application;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For development ({@link ApplicationConfiguration#setHotReload(boolean)}): the classes of the entry point's package,
 * read from the directory the entry point was loaded from, are loaded again by a new class loader once they change,
 * and the page of each live context reloads where it was, its new context built by the new classes. The framework's
 * classes and those of other packages are not reloaded: an object kept across a reload, a static field of the main
 * class, stays of the old classes.
 * <p>
 * The directory is scanned twice a second; a change is taken once its files have not changed for a quiet period, so
 * that a compilation is taken whole.
 */
public final class HotReload implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(HotReload.class);

    private static final long PERIOD_MILLIS = 500;
    private static final long QUIET_MILLIS = 300;

    private final Path root;
    private final String prefix;
    private final ClassLoader parent;
    private final Runnable reloaded;
    private final Thread scanner;
    private volatile ClassLoader loader;
    /** What the last scan saw of the class files: their paths and modification times. */
    private long fingerprint;
    /** When a change was seen and not taken yet; -1 without one. */
    private long changedAt = -1;

    HotReload(final Path root, final String prefix, final ClassLoader parent, final Runnable reloaded, final long period) {
        this.root = root;
        this.prefix = prefix;
        this.parent = parent;
        this.reloaded = reloaded;
        this.loader = parent;
        this.fingerprint = fingerprint();
        this.scanner = Thread.ofVirtual().name("ponysdk-hot-reload").start(() -> {
            try {
                while (true) {
                    Thread.sleep(period);
                    check(System.currentTimeMillis());
                }
            } catch (final InterruptedException e) {
                // closed
            }
        });
    }

    /**
     * Watches the classes of the entry point's package; null when they are not loaded from a directory (a jar):
     * nothing to watch.
     *
     * @param reloaded what to do once the classes are reloaded: {@link #reloadPages()}
     */
    public static HotReload of(final Class<?> entryPoint, final Runnable reloaded) {
        final Path root = Path.of(URI.create(entryPoint.getProtectionDomain().getCodeSource().getLocation().toString()));
        if (!Files.isDirectory(root)) {
            log.warn("Hot reload: {} is not loaded from a directory, nothing to watch", entryPoint.getName());
            return null;
        }
        final String prefix = entryPoint.getPackageName().isEmpty() ? "" : entryPoint.getPackageName() + ".";
        log.info("Hot reload: the classes of {}* under {}", prefix, root);
        return new HotReload(root, prefix, entryPoint.getClassLoader(), reloaded, PERIOD_MILLIS);
    }

    /** Destroys the live contexts: each client is told and reloads its page, which starts a new context. */
    public static void reloadPages() {
        Drain.live().forEach(UIContext::destroyByAdministrator);
    }

    /** The class loader of the classes as last compiled: the entry point's own until a first change. */
    public ClassLoader classLoader() {
        return loader;
    }

    /** The class as last compiled: the one given until a first change. */
    public <T> Class<? extends T> current(final Class<T> base, final Class<? extends T> type) {
        try {
            return Class.forName(type.getName(), true, loader).asSubclass(base);
        } catch (final ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    /** One scan: true when it took a change, the classes reloaded. */
    boolean check(final long now) {
        final long seen = fingerprint();
        if (seen != fingerprint) {
            fingerprint = seen;
            changedAt = now;
            return false;
        }
        if (changedAt < 0 || now - changedAt < QUIET_MILLIS) return false;
        changedAt = -1;
        loader = new Loader(root, prefix, parent);
        log.info("Hot reload: classes changed, live pages reload");
        reloaded.run();
        return true;
    }

    private long fingerprint() {
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(file -> file.toString().endsWith(".class")).mapToLong(file -> {
                try {
                    return Files.getLastModifiedTime(file).toMillis() * 31 + file.hashCode();
                } catch (final IOException e) {
                    return file.hashCode(); // deleted meanwhile: the next scan sees it gone
                }
            }).sum();
        } catch (final IOException | UncheckedIOException e) {
            return fingerprint; // the directory itself is being rewritten: as if unchanged, the next scan tells
        }
    }

    @Override
    public void close() {
        scanner.interrupt();
    }

    /** Defines the classes of the prefix found under the root itself, again; the others come from the parent. */
    private static final class Loader extends ClassLoader {

        private final Path root;
        private final String prefix;

        Loader(final Path root, final String prefix, final ClassLoader parent) {
            super("ponysdk-hot-reload", parent);
            this.root = root;
            this.prefix = prefix;
        }

        @Override
        protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
            if (!name.startsWith(prefix)) return super.loadClass(name, resolve);
            synchronized (getClassLoadingLock(name)) {
                Class<?> type = findLoadedClass(name);
                if (type == null) {
                    final Path file = root.resolve(name.replace('.', '/') + ".class");
                    if (!Files.isRegularFile(file)) return super.loadClass(name, resolve);
                    final byte[] bytes;
                    try {
                        bytes = Files.readAllBytes(file);
                    } catch (final IOException e) {
                        throw new ClassNotFoundException(name, e);
                    }
                    type = defineClass(name, bytes, 0, bytes.length);
                }
                return type;
            }
        }
    }
}
