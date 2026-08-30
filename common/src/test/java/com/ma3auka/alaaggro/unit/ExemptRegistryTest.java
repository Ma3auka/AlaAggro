package com.ma3auka.alaaggro.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.ma3auka.alaaggro.core.ExemptRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the global exempt-player registry.
 *
 * Why this matters:
 *   ExemptRegistry is read on every mob tick from multiple server threads
 *   (TickAggroHandler, MobAggroEventHandler) and mutated from the command
 *   handler (AlaAggroCommand). It is the only thing standing between an
 *   exempt player and the entire mob population aggroing on them. Bugs here
 *   are silent — a player who toggled exempt would just keep getting hit
 *   and never know why. We test add/remove semantics, view immutability,
 *   and concurrency because a regression in any of these breaks the
 *   command in production.
 */
final class ExemptRegistryTest {

    @BeforeEach
    void resetState() {
        // ExemptRegistry is a global singleton. Without clearing between tests
        // the suite would have order-dependent failures.
        ExemptRegistry.setChangeListener(null);
        ExemptRegistry.clear();
    }

    @Test
    @DisplayName("add returns true the first time, false on duplicate")
    void add_returnTrueOnceThenFalse() {
        UUID id = UUID.randomUUID();
        assertTrue(ExemptRegistry.add(id), "first add must signal new entry");
        assertFalse(ExemptRegistry.add(id), "duplicate add must signal no-op");
    }

    @Test
    @DisplayName("isExempt reflects add/remove")
    void isExempt_tracksMembership() {
        UUID id = UUID.randomUUID();
        assertFalse(ExemptRegistry.isExempt(id));
        ExemptRegistry.add(id);
        assertTrue(ExemptRegistry.isExempt(id));
        ExemptRegistry.remove(id);
        assertFalse(ExemptRegistry.isExempt(id));
    }

    @Test
    @DisplayName("remove returns true for present, false for absent")
    void remove_signalContract() {
        UUID id = UUID.randomUUID();
        assertFalse(ExemptRegistry.remove(id), "removing absent must return false");
        ExemptRegistry.add(id);
        assertTrue(ExemptRegistry.remove(id), "removing present must return true");
        assertFalse(ExemptRegistry.remove(id), "removing twice must return false");
    }

    @Test
    @DisplayName("clear() empties the registry completely")
    void clear_removesEverything() {
        for (int i = 0; i < 10; i++) ExemptRegistry.add(UUID.randomUUID());
        assertEquals(10, ExemptRegistry.view().size());
        ExemptRegistry.clear();
        assertEquals(0, ExemptRegistry.view().size());
    }

    @Test
    @DisplayName("view() reflects current state")
    void view_isLiveReflection() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        ExemptRegistry.add(a);
        ExemptRegistry.add(b);
        Set<UUID> view = ExemptRegistry.view();
        assertTrue(view.contains(a));
        assertTrue(view.contains(b));
        assertEquals(2, view.size());
    }

    @Test
    @DisplayName("view() is unmodifiable — caller cannot corrupt internal state")
    void view_rejectsMutation() {
        // If view() returned the underlying set, a malicious or buggy caller
        // could clear() it, leaking an exempt player back into aggro range.
        UUID id = UUID.randomUUID();
        ExemptRegistry.add(id);
        Set<UUID> view = ExemptRegistry.view();
        assertThrows(UnsupportedOperationException.class, () -> view.add(UUID.randomUUID()));
        assertThrows(UnsupportedOperationException.class, () -> view.remove(id));
        assertThrows(UnsupportedOperationException.class, view::clear);
    }

    @Test
    @DisplayName("concurrent adds from many threads — no entries lost (thread-safety)")
    void add_isThreadSafe() throws InterruptedException {
        // The registry is backed by ConcurrentHashMap.newKeySet(). If anyone
        // ever swaps it for a plain HashSet, this test will see lost entries
        // or a ConcurrentModificationException.
        int threads = 16;
        int perThread = 250;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        try {
            for (int t = 0; t < threads; t++) {
                pool.submit(() -> {
                    ready.countDown();
                    try { go.await(); } catch (InterruptedException ignored) {}
                    for (int i = 0; i < perThread; i++) {
                        ExemptRegistry.add(UUID.randomUUID());
                    }
                });
            }
            ready.await(2, TimeUnit.SECONDS);
            go.countDown();
        } finally {
            pool.shutdown();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        }
        assertEquals(threads * perThread, ExemptRegistry.view().size(),
                "concurrent adds must not lose entries");
    }

    @Test
    @DisplayName("real changes notify the persistence layer, no-ops do not")
    void changeListener_firesOnRealChangesOnly() {
        // The listener is how an exemption reaches the world save. If it stopped firing, the list
        // would look right all session and be empty after the restart — the exact bug the saved
        // data was added to fix, and one that only shows up hours later.
        java.util.concurrent.atomic.AtomicInteger notifications = new java.util.concurrent.atomic.AtomicInteger();
        ExemptRegistry.setChangeListener(notifications::incrementAndGet);

        UUID id = UUID.randomUUID();
        ExemptRegistry.add(id);
        assertEquals(1, notifications.get(), "adding must notify");

        ExemptRegistry.add(id);
        assertEquals(1, notifications.get(), "a duplicate add changes nothing and must not notify");

        ExemptRegistry.remove(id);
        assertEquals(2, notifications.get(), "removing must notify");

        ExemptRegistry.remove(id);
        assertEquals(2, notifications.get(), "removing an absent entry must not notify");
    }

    @Test
    @DisplayName("replaceAll loads without notifying")
    void replaceAll_doesNotNotify() {
        // replaceAll is the load path. Notifying there would write the file we have just read,
        // and on a large list that means a pointless save on every world load.
        java.util.concurrent.atomic.AtomicInteger notifications = new java.util.concurrent.atomic.AtomicInteger();
        ExemptRegistry.setChangeListener(notifications::incrementAndGet);

        UUID id = UUID.randomUUID();
        ExemptRegistry.replaceAll(Set.of(id));

        assertTrue(ExemptRegistry.isExempt(id), "loaded entries must be present");
        assertEquals(0, notifications.get(), "loading is not a change");
    }
}
