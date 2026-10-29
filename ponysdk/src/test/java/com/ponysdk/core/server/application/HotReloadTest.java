/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.server.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import javax.tools.ToolProvider;

import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import com.ponysdk.core.server.servlet.SessionManager;

public class HotReloadTest {

    @TempDir
    Path root;

    private URLClassLoader parent;
    private final AtomicInteger reloads = new AtomicInteger();
    private HotReload hotReload;

    /** Writes and compiles demo.Greeter, whose get() answers the word, at the given modification time. */
    private void greeter(final String word, final long modified) throws IOException {
        compile("demo/Greeter.java", "package demo; public class Greeter implements java.util.function.Supplier<String> {"
                + " public String get() { return \"" + word + "\"; } }");
        Files.setLastModifiedTime(root.resolve("demo/Greeter.class"), FileTime.fromMillis(modified));
    }

    private void compile(final String path, final String source) throws IOException {
        final Path file = root.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, source);
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", root.toString(), file.toString()));
    }

    @BeforeEach
    public void setUp() throws IOException {
        greeter("one", 1_000_000);
        parent = new URLClassLoader(new URL[] { root.toUri().toURL() }, getClass().getClassLoader());
        hotReload = new HotReload(root, "demo.", parent, reloads::incrementAndGet, TimeUnit.HOURS.toMillis(1));
    }

    @AfterEach
    public void tearDown() throws IOException {
        hotReload.close();
        parent.close();
    }

    @SuppressWarnings("unchecked")
    private String greet() throws ReflectiveOperationException {
        final Class<? extends Supplier<String>> type = hotReload.current((Class<Supplier<String>>) (Class<?>) Supplier.class,
            (Class<Supplier<String>>) parent.loadClass("demo.Greeter"));
        return type.getDeclaredConstructor().newInstance().get();
    }

    @Test
    public void aChangeIsTakenOnceItsFilesStoppedChanging() throws Exception {
        assertEquals("one", greet());
        assertSame(parent, hotReload.classLoader(), "the entry point's own until a change");
        assertFalse(hotReload.check(0), "nothing changed");

        greeter("two", 2_000_000);
        assertFalse(hotReload.check(10_000), "a change seen");
        assertFalse(hotReload.check(10_100), "not quiet yet");
        assertEquals("one", greet());
        assertTrue(hotReload.check(10_400));
        assertEquals(1, reloads.get());
        assertEquals("two", greet());
        assertEquals("ponysdk-hot-reload", hotReload.classLoader().getName());
        assertFalse(hotReload.check(20_000), "taken once");
    }

    @Test
    public void onlyThePrefixUnderTheRootIsLoadedAgain() throws Exception {
        compile("other/Thing.java", "package other; public class Thing {}");
        greeter("two", 2_000_000);
        hotReload.check(0);
        hotReload.check(1_000);

        final Class<?> greeter = hotReload.current(Object.class, parent.loadClass("demo.Greeter"));
        final ClassLoader loader = greeter.getClassLoader();
        assertEquals("ponysdk-hot-reload", loader.getName());
        assertSame(greeter, loader.loadClass("demo.Greeter"), "defined once by its loader");
        assertSame(parent.loadClass("other.Thing"), loader.loadClass("other.Thing"), "another package: the parent's");
        assertThrows(ClassNotFoundException.class, () -> loader.loadClass("demo.Missing"), "not under the root: the parent's");

        final Path locked = root.resolve("demo/Locked.class");
        Files.writeString(locked, "unreadable");
        Files.setPosixFilePermissions(locked, PosixFilePermissions.fromString("---------"));
        final ClassNotFoundException unreadable = assertThrows(ClassNotFoundException.class, () -> loader.loadClass("demo.Locked"));
        assertInstanceOf(IOException.class, unreadable.getCause());
    }

    @Test
    public void aClassOfThePrefixFoundOnlyElsewhereIsTheParents() throws Exception {
        final Path elsewhere = Files.createTempDirectory("pony-elsewhere");
        Files.createDirectories(elsewhere.resolve("demo"));
        Files.writeString(elsewhere.resolve("demo/Elsewhere.java"), "package demo; public class Elsewhere {}");
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", elsewhere.toString(),
            elsewhere.resolve("demo/Elsewhere.java").toString()));
        try (URLClassLoader both = new URLClassLoader(new URL[] { root.toUri().toURL(), elsewhere.toUri().toURL() },
            getClass().getClassLoader()); HotReload reloading = new HotReload(root, "demo.", both, () -> {}, TimeUnit.HOURS.toMillis(1))) {
            greeter("two", 2_000_000);
            reloading.check(0);
            reloading.check(1_000);
            final ClassLoader loader = reloading.current(Object.class, both.loadClass("demo.Greeter")).getClassLoader();
            assertSame(both.loadClass("demo.Elsewhere"), loader.loadClass("demo.Elsewhere"));
        }
    }

    @Test
    public void aClassNoLoaderOfTheApplicationKnowsIsRefused() throws Exception {
        final Path elsewhere = Files.createTempDirectory("pony-elsewhere");
        Files.writeString(elsewhere.resolve("Alone.java"), "public class Alone {}");
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", elsewhere.toString(),
            elsewhere.resolve("Alone.java").toString()));
        try (URLClassLoader unrelated = new URLClassLoader(new URL[] { elsewhere.toUri().toURL() }, null)) {
            final Class<?> alone = unrelated.loadClass("Alone");
            assertThrows(IllegalStateException.class, () -> hotReload.current(Object.class, alone));
        }
    }

    @Test
    public void aFileOrADirectoryThatCannotBeReadMeansNoChangeYet() throws Exception {
        Files.createSymbolicLink(root.resolve("demo/Broken.class"), root.resolve("demo/none"));
        assertFalse(hotReload.check(0), "a link to nothing is seen as a file");
        hotReload.check(1_000);
        assertEquals(1, reloads.get());

        try (var files = Files.walk(root)) {
            files.sorted(java.util.Comparator.reverseOrder()).forEach(file -> file.toFile().delete());
        }
        assertFalse(hotReload.check(2_000), "the directory gone: as before");
        assertFalse(hotReload.check(3_000));
    }

    @Test
    public void theScannerTakesAChangeByItself() throws Exception {
        final CountDownLatch reloaded = new CountDownLatch(1);
        try (HotReload scanning = new HotReload(root, "demo.", parent, reloaded::countDown, 5)) {
            greeter("three", 3_000_000);
            assertTrue(reloaded.await(10, TimeUnit.SECONDS));
        }
    }

    @Test
    public void onlyClassesLoadedFromADirectoryAreWatched() throws Exception {
        assertNull(HotReload.of(Test.class, () -> {}), "a jar");
        try (HotReload watching = HotReload.of(HotReloadTest.class, () -> {})) {
            assertNotNull(watching);
        }
        compile("Plain.java", "public class Plain {}");
        try (HotReload watching = HotReload.of(parent.loadClass("Plain"), () -> {})) {
            assertNotNull(watching, "the default package: every class of the directory");
        }
    }

    @Test
    public void pagesReloadByTheDestructionOfTheLiveContexts() {
        final Application application = new Application("hot", Mockito.mock(HttpSession.class), new ApplicationConfiguration());
        final UIContext alive = Mockito.mock(UIContext.class);
        Mockito.when(alive.getID()).thenReturn(1);
        Mockito.when(alive.isAlive()).thenReturn(true);
        final UIContext dead = Mockito.mock(UIContext.class);
        Mockito.when(dead.getID()).thenReturn(2);
        application.registerUIContext(alive);
        application.registerUIContext(dead);
        SessionManager.get().registerApplication(application);
        try {
            HotReload.reloadPages();
            Mockito.verify(alive).destroyByAdministrator();
            Mockito.verify(dead, Mockito.never()).destroyByAdministrator();
        } finally {
            SessionManager.get().unregisterApplication(application);
        }
    }
}
