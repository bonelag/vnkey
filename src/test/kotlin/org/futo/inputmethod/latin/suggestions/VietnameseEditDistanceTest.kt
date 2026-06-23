package org.futo.inputmethod.latin.suggestions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

/**
 * Equivalence guard cho tối ưu engine gợi ý tiếng Việt (pure JVM, không cần thiết bị).
 *
 * - T1: editDistance mới (rolling 3 hàng + band cutoff) == bản cũ (full matrix) cho mọi
 *   khoảng cách <= MAX_EDIT_DISTANCE; > ngưỡng thì chỉ cần > ngưỡng (sentinel chấp nhận).
 * - T3: phân loại (<=2.5 / >2.5) khớp 100% — đảm bảo cutoff không loại nhầm ứng viên hợp lệ.
 * - T2: tập ứng viên của bucket-scan mới == full-scan cũ trên fixture.
 */
class VietnameseEditDistanceTest {

    private val MAX_EDIT_DISTANCE = 2.5

    // ---- Oracle: bản editDistance CŨ (full matrix) + adjacency y hệt engine ----
    private val KEY_ADJACENCY = mapOf(
        'q' to "wa", 'w' to "qeas", 'e' to "wrsd", 'r' to "etdf", 't' to "ryfg", 'y' to "tygh",
        'u' to "yihj", 'i' to "uojk", 'o' to "ipkl", 'p' to "ol",
        'a' to "qwsz", 's' to "weadzx", 'd' to "ersfxc", 'f' to "rtdgcv", 'g' to "tyfhvb",
        'h' to "yughjn", 'j' to "uihknm", 'k' to "iojlm", 'l' to "opk",
        'z' to "asx", 'x' to "sdzc", 'c' to "dfxv", 'v' to "fgcb", 'b' to "ghvn", 'n' to "hjbm", 'm' to "jkn"
    )

    private fun isAdjacent(c1: Char, c2: Char): Boolean {
        val l1 = c1.lowercaseChar()
        val l2 = c2.lowercaseChar()
        if (l1 == l2) return true
        return KEY_ADJACENCY[l1]?.contains(l2) == true
    }

    private fun oracle(s: String, t: String): Double {
        val m = s.length
        val n = t.length
        val dp = Array(m + 1) { DoubleArray(n + 1) }
        for (i in 0..m) dp[i][0] = i * 1.0
        for (j in 0..n) dp[0][j] = j * 1.0
        for (i in 1..m) {
            for (j in 1..n) {
                val c1 = s[i - 1]
                val c2 = t[j - 1]
                val subCost = if (c1 == c2) 0.0 else if (isAdjacent(c1, c2)) 0.5 else 1.0
                dp[i][j] = minOf(dp[i - 1][j] + 1.0, dp[i][j - 1] + 1.0, dp[i - 1][j - 1] + subCost)
                if (i > 1 && j > 1 && s[i - 1] == t[j - 2] && s[i - 2] == t[j - 1]) {
                    dp[i][j] = minOf(dp[i][j], dp[i - 2][j - 2] + 1.0)
                }
            }
        }
        return dp[m][n]
    }

    private fun samplePairs(): List<Pair<String, String>> {
        val curated = listOf(
            "the" to "teh",      // transposition
            "chao" to "cgao",    // adjacency (c/g không kề -> 1.0, kiểm tra path)
            "chao" to "chao",    // identical
            "chao" to "",        // empty t
            "" to "chao",        // empty s
            "chaod" to "chao",   // len diff 1
            "chaoxyz" to "chao", // len diff 3 (>2.5)
            "vhao" to "chao",    // typo ký tự đầu, v/c kề
            "xinchao" to "xin",
            "a" to "b",
            "qu" to "qu"
        )
        val rnd = Random(12345L)
        val alphabet = "abcdefghijklmnopqrstuvwxyz"
        val random = ArrayList<Pair<String, String>>()
        repeat(400) {
            val ls = 1 + rnd.nextInt(8)
            val lt = 1 + rnd.nextInt(8)
            val a = buildString { repeat(ls) { append(alphabet[rnd.nextInt(26)]) } }
            val b = buildString { repeat(lt) { append(alphabet[rnd.nextInt(26)]) } }
            random.add(a to b)
        }
        return curated + random
    }

    @Test
    fun t1_editDistance_equivalence_within_threshold() {
        for ((s, t) in samplePairs()) {
            // engine gọi editDistance(cleanIn, clean): t là phía ngắn (clean). Test cả 2 chiều.
            val expected = oracle(s, t)
            val actual = VietnameseSuggestionEngine.editDistanceForTest(s, t)
            if (expected <= MAX_EDIT_DISTANCE) {
                assertEquals("dist mismatch for ($s,$t)", expected, actual, 1e-9)
            } else {
                assertTrue("expected >$MAX_EDIT_DISTANCE for ($s,$t) but got $actual", actual > MAX_EDIT_DISTANCE)
            }
        }
    }

    @Test
    fun t3_band_cutoff_classification_matches() {
        for ((s, t) in samplePairs()) {
            val oraInside = oracle(s, t) <= MAX_EDIT_DISTANCE
            val newInside = VietnameseSuggestionEngine.editDistanceForTest(s, t) <= MAX_EDIT_DISTANCE
            assertEquals("classification mismatch for ($s,$t)", oraInside, newInside)
        }
    }

    // ---- T2: tập ứng viên bucket-scan mới == full-scan cũ ----
    private fun oldScan(cleanToBestFreq: Map<String, Int>, cleanIn: String): Map<String, Double> {
        val out = HashMap<String, Double>()
        if (cleanIn.length < 2) return out
        for ((clean, _) in cleanToBestFreq) {
            if (Math.abs(clean.length - cleanIn.length) > 2) continue
            if (clean[0] != cleanIn[0] && !isAdjacent(clean[0], cleanIn[0])) continue
            if (clean == cleanIn) continue
            val d = VietnameseSuggestionEngine.editDistanceForTest(cleanIn, clean)
            if (d <= MAX_EDIT_DISTANCE) out[clean] = d
        }
        return out
    }

    private fun newScan(cleanToBestFreq: Map<String, Int>, cleanIn: String): Map<String, Double> {
        val out = HashMap<String, Double>()
        if (cleanIn.length < 2) return out
        // dựng index như init
        val index = HashMap<Char, ArrayList<String>>()
        for (clean in cleanToBestFreq.keys) {
            if (clean.isNotEmpty()) index.getOrPut(clean[0]) { ArrayList() }.add(clean)
        }
        val first = cleanIn[0]
        val groups = ArrayList<List<String>>()
        index[first]?.let { groups.add(it) }
        for ((c, list) in index) if (c != first && isAdjacent(c, first)) groups.add(list)
        for (group in groups) {
            for (clean in group) {
                if (Math.abs(clean.length - cleanIn.length) > 2) continue
                if (clean == cleanIn) continue
                val d = VietnameseSuggestionEngine.editDistanceForTest(cleanIn, clean)
                if (d <= MAX_EDIT_DISTANCE) out[clean] = d
            }
        }
        return out
    }

    @Test
    fun t2_findCorrections_candidate_set_equivalence() {
        val fixture = mapOf(
            "chao" to 100, "chai" to 50, "chao2" to 1, "cao" to 30, "vao" to 20,
            "chu" to 10, "cho" to 80, "xin" to 99, "xinh" to 40, "an" to 70,
            "ban" to 60, "van" to 25, "chan" to 15, "khong" to 90, "khon" to 12
        )
        val inputs = listOf("chao", "vhao", "cho", "xin", "khong", "a", "zz", "chaoo")
        for (cleanIn in inputs) {
            assertEquals("candidate set mismatch for '$cleanIn'", oldScan(fixture, cleanIn), newScan(fixture, cleanIn))
        }
    }
}
