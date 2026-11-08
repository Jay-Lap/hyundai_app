package com.example.hyundiaboysranger

import org.junit.Assert.*
import org.junit.Test

class ReviewIntegrityTest {
    @Test fun incompleteOrInvalidCurrentRecordIsRejected() {
        assertTrue(invalidReviewHeader(mapOf("updated" to 100L), 1))
        assertTrue(invalidReviewHeader(mapOf("complete" to false), 1))
        assertTrue(invalidReviewHeader(mapOf("updated" to 0L, "complete" to true), 1))
        assertTrue(invalidReviewHeader(mapOf("updated" to -10L, "complete" to false), 1))
        assertTrue(invalidReviewHeader(mapOf("updated" to "100", "complete" to false), 1))
    }

    @Test fun legitimateLegacyAndEmptyRecordsRemainReadable() {
        assertFalse(invalidReviewHeader(emptyMap<String, Any?>(), 1))
        assertFalse(invalidReviewHeader(mapOf("consent" to true, "updated" to 100L, "complete" to false), 1))
        assertFalse(invalidReviewHeader(mapOf("schema_version" to 1, "updated" to 100L, "complete" to true), 1))
        assertTrue(invalidReviewHeader(mapOf("schema_version" to 2), 1))
    }
}
