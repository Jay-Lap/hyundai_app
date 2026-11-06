package com.example.hyundiaboysranger

import org.junit.Assert.*
import org.junit.Test

class TextSizeVoiceTest {
    @Test fun acceptsNumericAndKoreanPercentages() {
        assertEquals(150, parseTextSizeVoice("글자 크기 150퍼센트로 바꿔줘"))
        assertEquals(200, parseTextSizeVoice("이백 퍼센트"))
        assertEquals(125, parseTextSizeVoice("글씨 크기 백이십오 프로"))
        assertEquals(100, parseTextSizeVoice("100%"))
    }
    @Test fun rejectsAmbiguousAndInvalidCommands() {
        listOf("90%", "201%", "-150%", "150.5%", "150 아니 180", "크게", "", "150%로 하지 마").forEach {
            assertNull(it, parseTextSizeVoice(it))
        }
    }
}
