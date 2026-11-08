package com.example.hyundiaboysranger

import org.junit.Assert.*
import org.junit.Test

class WarningCatalogTest {
    @Test fun ignoresMalformedAndDuplicateRows() {
        val entries = parseWarningCatalog("belt\t벨트\t설명\t확인\nbelt\t중복\t설명\t확인\nmissing\t한 칸\n")
        assertEquals(1, entries.size)
        assertEquals("belt", entries.single().id)
        assertEquals("확인", entries.single().action)
    }
}
