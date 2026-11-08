package com.example.hyundiaboysranger

import org.junit.Assert.*
import org.junit.Test

class WriteGuardTest {
    private class FakeStore(var disk: String) {
        var memory = disk
        var result = WriteResult.SUCCESS
        var writes = 0
        fun operation(value: String) = PersistentWrite {
            writes++
            when (result) {
                WriteResult.SUCCESS -> { memory = value; disk = value; WriteResult.SUCCESS }
                WriteResult.FAILED -> WriteResult.FAILED // rejected before touching memory or disk
                WriteResult.UNKNOWN -> { memory = value; WriteResult.UNKNOWN }
            }
        }
    }

    @Test fun successAndDefinitiveFailure() {
        val store = FakeStore("old")
        val guard = WriteGuard()
        store.result = WriteResult.FAILED
        assertEquals(WriteResult.FAILED, guard.execute(store.operation("new")))
        assertEquals("old", store.disk)
        assertFalse(guard.uncertain)
        store.result = WriteResult.SUCCESS
        assertEquals(WriteResult.SUCCESS, guard.execute(store.operation("new")))
        assertEquals("new", store.disk)
    }

    @Test fun unknownBlocksOtherWritesAndRetryUsesOriginalOperation() {
        val store = FakeStore("backup")
        val guard = WriteGuard()
        store.result = WriteResult.UNKNOWN
        assertEquals(WriteResult.UNKNOWN, guard.execute(store.operation("replacement")))
        assertEquals("replacement", store.memory)
        assertEquals("backup", store.disk)
        assertEquals(WriteResult.UNKNOWN, guard.execute(store.operation("different")))
        assertEquals(1, store.writes)
        store.result = WriteResult.FAILED
        assertEquals(WriteResult.UNKNOWN, guard.retry())
        assertTrue(guard.uncertain)
        store.result = WriteResult.SUCCESS
        assertEquals(WriteResult.SUCCESS, guard.retry())
        assertEquals("replacement", store.disk)
        assertFalse(guard.uncertain)
    }

    @Test fun cancellingRetryDoesNotWriteOrPretendToResolveUnknown() {
        val store = FakeStore("damaged original")
        val guard = WriteGuard()
        store.result = WriteResult.UNKNOWN
        guard.execute(store.operation("reset"))
        // User dismisses the retry prompt: no retry is invoked.
        assertTrue(guard.uncertain)
        assertEquals(1, store.writes)
        assertEquals("damaged original", store.disk)
        assertEquals(WriteResult.UNKNOWN, guard.execute(store.operation("overwrite")))
    }

    @Test fun unknownMayAlsoMeanTheDiskWriteAlreadyHappened() {
        val guard = WriteGuard()
        var disk = "old"
        var calls = 0
        val operation = PersistentWrite {
            calls++
            disk = "new"
            if (calls == 1) WriteResult.UNKNOWN else WriteResult.SUCCESS
        }
        assertEquals(WriteResult.UNKNOWN, guard.execute(operation))
        assertEquals("new", disk)
        assertTrue(guard.uncertain)
        assertEquals(WriteResult.SUCCESS, guard.retry())
        assertEquals("new", disk)
    }

    @Test fun exceptionIsUnknownAndBlocksAnotherOperation() {
        val guard = WriteGuard()
        assertEquals(WriteResult.UNKNOWN, guard.execute { throw IllegalStateException() })
        assertEquals(WriteResult.UNKNOWN, guard.execute { fail("must not run"); WriteResult.SUCCESS })
        assertTrue(guard.uncertain)
    }
}
