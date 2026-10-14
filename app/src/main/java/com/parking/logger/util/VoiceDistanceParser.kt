package com.parking.logger.util

import java.util.Locale
import kotlin.math.pow

/**
 * 解析语音识别出的文本，支持两种录入方式：
 * 1. 顺序编号："第一个是2.5" / "第二个跳过" / "第三 1.2" → [targetIndex]=0/1/2...
 * 2. 字段标签："BL 2.5" / "左后 三点五" / "FR 跳过" → [targetIndex] 命中字段下标
 *
 * 无编号/无标签时作为纯数字，由调用方按顺序填入下一个空字段。
 */
object VoiceDistanceParser {

    /** 六个距离字段顺序。BL/Back/BR/FR/Front/FL */
    val FIELDS = listOf("BL", "Back", "BR", "FR", "Front", "FL")

    // 字段标签别名（无编号输入时定位字段用）
    private val LABEL_ALIASES = mapOf(
        "BL" to listOf("bl", "b l", "左后", "左边后", "后左"),
        "Back" to listOf("back", "后", "后面", "背后"),
        "BR" to listOf("br", "b r", "右后", "右边后", "后右"),
        "FR" to listOf("fr", "f r", "右前", "右边前", "前右"),
        "Front" to listOf("front", "前", "前面", "前方"),
        "FL" to listOf("fl", "f l", "左前", "左边前", "前左")
    )

    // 顺序编号词："第一个" -> 0
    private val ORDINAL_WORDS = mapOf(
        "第一个" to 0, "第1个" to 0, "第一" to 0,
        "第二个" to 1, "第2个" to 1, "第二" to 1,
        "第三个" to 2, "第3个" to 2, "第三" to 2,
        "第四个" to 3, "第4个" to 3, "第四" to 3,
        "第五个" to 4, "第5个" to 4, "第五" to 4,
        "第六个" to 5, "第6个" to 5, "第六" to 5
    )

    // 跳过/不填 语义词
    private val SKIP_WORDS = listOf("跳过", "不填", "没有", "不用量", "空缺", "不要", "不计")

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

    /**
     * 解析结果。
     * @param targetIndex 明确命中的字段（编号或标签）。null 表示未指定字段（应按顺序/跳过处理）。
     * @param value 抓取到的数字；null 表示无数字（仅跳过时可为空）。
     * @param skip 该字段是否表示"跳过/不填"。
     */
    data class ParseResult(
        val targetIndex: Int?,
        val value: Double?,
        val skip: Boolean
    )

    fun parse(text: String): ParseResult {
        val normalized = text.lowercase(Locale.getDefault())
        val ordinalIndex = matchOrdinal(normalized)
        val labelIndex = if (ordinalIndex == null) matchLabel(normalized) else null
        val skip = SKIP_WORDS.any { normalized.contains(it) }
        val value = extractNumber(normalized)

        val targetIndex = ordinalIndex ?: labelIndex
        return ParseResult(targetIndex, value, skip)
    }

    /** 匹配"第N个"，返回字段下标 0~5；未命中返回 null */
    private fun matchOrdinal(text: String): Int? {
        for ((word, index) in ORDINAL_WORDS) {
            if (text.contains(word)) {
                // 避免 "第一" 误伤 "第一个"（已按更长词优先：手动保证字长排序）
                return index
            }
        }
        return null
    }

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
        Regex("""\d+(\.\d+)?""").find(text)?.let {
            return it.value.toDoubleOrNull()
        }
        Regex("([零〇一二两三四五六七八九十]+)点([零〇一二两三四五六七八九十]+)")
            .find(text)?.let { m ->
                val whole = m.groupValues[1].toChineseInt()
                val frac = m.groupValues[2].toChineseInt()
                val fracStr = frac.toString()
                return whole + frac / 10.0.pow(fracStr.length)
            }
        Regex("([a-z]+)\\s+point\\s+([a-z]+)").find(text)?.let { m ->
            val whole = m.groupValues[1].toEnglishInt() ?: return null
            val fractional = m.groupValues[2].toEnglishInt() ?: return null
            val fracStr = fractional.toString()
            return whole + fractional / 10.0.pow(fracStr.length)
        }
        Regex("point\\s+([a-z]+)").find(text)?.let { m ->
            val fractional = m.groupValues[1].toEnglishInt() ?: return null
            val fracStr = fractional.toString()
            return fractional / 10.0.pow(fracStr.length)
        }
        if (text.length == 1) {
            return CHINESE_DIGITS[text]?.toDouble()
        }
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
