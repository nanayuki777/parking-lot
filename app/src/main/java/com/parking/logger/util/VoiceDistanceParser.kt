package com.parking.logger.util

import java.util.Locale
import kotlin.math.pow

/**
 * 解析语音识别出的文本，提取数字与对应的距离字段。
 *
 * 支持两种输入：
 * 1. 带标签：如 "BL 2.5" / "左后 三点五" / "Front point five" → 明确命中某个字段
 * 2. 纯数字："第一个是2.5" / "两点五" → labelIndex 为 null，由调用方按顺序填充
 */
object VoiceDistanceParser {

    /** 距离字段顺序。BL/Back/BR/FR/Front/FL */
    val FIELDS = listOf("BL", "Back", "BR", "FR", "Front", "FL")

    private val LABEL_ALIASES = mapOf(
        "BL" to listOf("bl", "b l", "左后", "左边后", "后左"),
        "Back" to listOf("back", "后", "后面", "背后"),
        "BR" to listOf("br", "b r", "右后", "右边后", "后右"),
        "FR" to listOf("fr", "f r", "右前", "右边前", "前右"),
        "Front" to listOf("front", "前", "前面", "前方"),
        "FL" to listOf("fl", "f l", "左前", "左边前", "前左")
    )

    private val CHINESE_DIGITS = mapOf(
        "零" to 0, "〇" to 0, "一" to 1, "二" to 2, "两" to 2,
        "三" to 3, "四" to 4, "五" to 5, "六" to 6, "七" to 7,
        "八" to 8, "九" to 9, "十" to 10
    )

    private val ENGLISH_DIGITS = mapOf(
        "zero" to 0, "oh" to 0, "one" to 1, "won" to 1, "two" to 2, "to" to 2, "too" to 2,
        "three" to 3, "four" to 4, "for" to 4, "five" to 5, "six" to 6,
        "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10
    )

    data class ParseResult(val labelIndex: Int?, val value: Double?)

    fun parse(text: String): ParseResult {
        val normalized = text.lowercase(Locale.getDefault())
        val labelIndex = matchLabel(normalized)
        val value = extractNumber(normalized)
        return ParseResult(labelIndex, value)
    }

    /** 标签作为独立词命中才返回字段下标，否则 null */
    private fun matchLabel(text: String): Int? {
        for ((canonical, aliases) in LABEL_ALIASES) {
            val idx = FIELDS.indexOf(canonical)
            for (a in aliases) {
                val pattern = Regex("(?<![a-z0-9])${Regex.escape(a)}(?![a-z0-9])")
                if (pattern.containsMatchIn(text)) return idx
            }
        }
        return null
    }

    private fun extractNumber(text: String): Double? {
        // 阿拉伯数字（含小数）
        val arabic = Regex("""\d+(\.\d+)?""").find(text)
        if (arabic != null) return arabic.value.toDoubleOrNull()

        // 中文小数字母：两点五 / 一点三
        Regex("([零〇一二两三四五六七八九十]+)点([零〇一二两三四五六七八九十]+)")
            .find(text)?.let { m ->
                val whole = m.groupValues[1].toChineseInt()
                val frac = m.groupValues[2].toChineseInt()
                val fracStr = frac.toString()
                return whole + frac / 10.0.pow(fracStr.length)
            }

        // 英文口语：two point five
        Regex("([a-z]+)\s+point\s+([a-z]+)").find(text)?.let { m ->
            val whole = m.groupValues[1].toEnglishInt() ?: return null
            val fractional = m.groupValues[2].toEnglishInt() ?: return null
            val fracStr = fractional.toString()
            return whole + fractional / 10.0.pow(fracStr.length)
        }

        // 英文口语：point five（省略整数部分）
        Regex("point\s+([a-z]+)").find(text)?.let { m ->
            val fractional = m.groupValues[1].toEnglishInt() ?: return null
            val fracStr = fractional.toString()
            return fractional / 10.0.pow(fracStr.length)
        }

        // 单一中文数字：二、三
        if (text.length == 1) {
            return CHINESE_DIGITS[text]?.toDouble()
        }

        // 单个英文数字词：five、two
        ENGLISH_DIGITS[text.trim()]?.let { return it.toDouble() }

        return null
    }

    private fun String.toChineseInt(): Int {
        var result = 0
        for (c in this) {
            result = result * 10 + (CHINESE_DIGITS[c.toString()] ?: 0)
        }
        return result
    }

    private fun String.toEnglishInt(): Int? {
        val trimmed = this.trim()
        if (trimmed.isEmpty()) return null
        var result = 0
        for (token in trimmed.split(Regex("\\s+"))) {
            val d = ENGLISH_DIGITS[token] ?: return null
            result = result * 10 + d
        }
        return result
    }
}
