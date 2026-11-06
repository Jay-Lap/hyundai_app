package com.example.hyundiaboysranger

/** Accept one explicit percentage; ambiguous or out-of-range requests never change settings. */
fun parseTextSizeVoice(text: String): Int? {
    var value = text.replace(Regex("\\s+"), "").trimEnd('.', '!', '?')
    value = value.removePrefix("글자크기").removePrefix("글씨크기")
    for (suffix in listOf("로바꿔줘", "로바꿔주세요", "로설정해줘", "로설정해주세요", "로변경해줘", "으로", "로")) {
        if (suffix == "로" && value.endsWith("프로")) continue
        if (value.endsWith(suffix)) { value = value.removeSuffix(suffix); break }
    }
    value = value.removeSuffix("퍼센트").removeSuffix("프로").removeSuffix("%")
    val numeric = if (value.matches(Regex("[0-9]{3}"))) value.toInt() else {
        val digits = listOf("", "일", "이", "삼", "사", "오", "육", "칠", "팔", "구")
        (100..200).firstOrNull { number ->
            val remainder = number % 100
            val tens = remainder / 10
            val korean = if (number == 200) "이백" else "백" +
                (if (tens == 0) "" else (if (tens == 1) "" else digits[tens]) + "십") + digits[remainder % 10]
            value == korean || (number < 200 && value == "일$korean")
        }
    }
    return numeric?.takeIf { it in 100..200 }
}
