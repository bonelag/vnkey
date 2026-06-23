package org.futo.inputmethod.latin.suggestions

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import org.futo.inputmethod.event.combiners.vietnamese.Common
import org.futo.inputmethod.latin.Dictionary
import org.futo.inputmethod.latin.SuggestedWords.SuggestedWordInfo
import org.futo.inputmethod.latin.uix.VIETNAMESE_AUTOCORRECT_LEVEL
import org.futo.inputmethod.latin.uix.getSettingBlocking
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

/**
 * Engine gợi ý + tự sửa lỗi tiếng Việt thuần Kotlin (không dùng bộ giải mã không gian JNI).
 *
 * Nạp toàn bộ từ điển (word + frequency) từ asset `vietnamese_words.txt` vào RAM, rồi:
 *  - Gợi ý hoàn thành theo tiền tố Telex/không-dấu, xếp hạng theo (ít phím gõ thêm, tần suất cao).
 *  - Sửa lỗi bằng khoảng cách soạn thảo (có tính phím kề), xếp hạng CÂN THEO TẦN SUẤT
 *    (vd "chaid" -> "chào" thắng "chai" vì chào phổ biến hơn).
 *
 * Mức tự sửa (autocorrect on space) do người dùng chọn qua setting 3 mức:
 *  0 = Thấp, 1 = Cân bằng (mặc định), 2 = Nghiêm ngặt.
 */
object VietnameseSuggestionEngine {

    private const val TAG = "VietnameseSuggestEngine"

    const val LEVEL_LOW = 0
    const val LEVEL_BALANCED = 1
    const val LEVEL_STRICT = 2

    // ---- Hằng số xếp hạng (điểm càng cao càng ưu tiên; mScore là Int) ----
    // Hoàn thành tiền tố luôn nằm trên nhóm sửa lỗi (base cao hơn) — gõ dở một từ thật thì
    // ưu tiên hoàn thành nó, không "sửa".
    private const val COMPLETION_BASE = 1_000_000
    private const val CORRECTION_BASE = 500_000
    private const val FREQ_W = 1_000          // trọng số tần suất
    private const val EXTRA_KEY_PENALTY = 40_000   // mỗi phím Telex gõ thêm khi hoàn thành
    private const val W_PENALTY = 40_000      // phạt khi gợi ý có ơ/ư/ă (telex 'w') mà input không gõ
    private const val DIST_PENALTY = 18_000   // phạt cho mỗi 1.0 khoảng cách soạn thảo khi sửa lỗi
    private const val BIGRAM_W = 50_000       // thưởng theo ngữ cảnh từ trước (xin -> chào)
    private const val AUTOCORRECT_SCORE = 2_000_000  // điểm cho ứng viên tự-sửa (đứng đầu)

    private const val MAX_SUGGESTIONS = 8
    private const val MAX_EDIT_DISTANCE = 2.5

    // ---- Ngưỡng khoảng cách cho phép tự sửa theo từng mức ----
    private const val DIST_LOW = 1.5
    private const val DIST_BALANCED = 2.0
    private const val DIST_STRICT = 2.5

    private class Entry(val word: String, val freq: Int, val clean: String, val telex: String)

    @Volatile private var loaded = false
    @Volatile private var appContext: Context? = null
    // Các collection dùng volatile-swap: init dựng vào local rồi publish, set `loaded` cuối cùng.
    // getSuggestions đọc `loaded` (volatile) trước nên thấy map đã build xong (JMM happens-before).
    @Volatile private var entries: ArrayList<Entry> = ArrayList()
    // bucket theo ký tự không-dấu đầu tiên để giảm số lần quét mỗi lần gõ
    @Volatile private var byFirstClean: HashMap<Char, ArrayList<Entry>> = HashMap()
    // tập âm tiết không-dấu phân biệt + tần suất cao nhất, dùng cho sửa lỗi
    @Volatile private var cleanToBestFreq: HashMap<String, Int> = HashMap()
    @Volatile private var exactWords: HashSet<String> = HashSet()
    // ngữ cảnh: từ-trước (thường) -> { từ-kế (thường) -> tần suất bigram }
    @Volatile private var bigrams: HashMap<String, HashMap<String, Int>> = HashMap()
    // âm tiết không-dấu phân biệt nhóm theo ký tự đầu (keyset trùng cleanToBestFreq) —
    // cho findCorrections quét bucket ký tự đầu + ký tự kề thay vì toàn bộ map.
    @Volatile private var cleanByFirstChar: HashMap<Char, ArrayList<String>> = HashMap()

    @JvmStatic
    fun init(context: Context) {
        appContext = context.applicationContext
        if (loaded) return
        try {
            // Dựng vào collection LOCAL, publish nguyên tử khi xong (xem ghi chú field).
            val lEntries = ArrayList<Entry>(32000)
            val lByFirstClean = HashMap<Char, ArrayList<Entry>>()
            val lCleanToBestFreq = HashMap<String, Int>()
            val lExactWords = HashSet<String>()
            val lBigrams = HashMap<String, HashMap<String, Int>>()
            val lCleanByFirstChar = HashMap<Char, ArrayList<String>>()
            context.assets.open("vietnamese_words.txt").use { input ->
                BufferedReader(InputStreamReader(input, "UTF-8")).use { reader ->
                    reader.forEachLine { line ->
                        val tab = line.indexOf('\t')
                        if (tab <= 0) return@forEachLine
                        val word = line.substring(0, tab)
                        val freq = line.substring(tab + 1).trim().toIntOrNull() ?: return@forEachLine
                        val clean = VietnameseSuggestHelper.removeAccents(word).lowercase(Locale.ROOT)
                        val telex = VietnameseSuggestHelper.toTelex(word)
                        val e = Entry(word, freq, clean, telex)
                        lEntries.add(e)
                        lExactWords.add(word.lowercase(Locale.ROOT))
                        if (clean.isNotEmpty()) {
                            lByFirstClean.getOrPut(clean[0]) { ArrayList() }.add(e)
                        }
                        val prev = lCleanToBestFreq[clean]
                        if (prev == null) {
                            // first-sight: thêm clean phân biệt vào index theo ký tự đầu
                            if (clean.isNotEmpty()) {
                                lCleanByFirstChar.getOrPut(clean[0]) { ArrayList() }.add(clean)
                            }
                            lCleanToBestFreq[clean] = freq
                        } else if (freq > prev) {
                            lCleanToBestFreq[clean] = freq
                        }
                    }
                }
            }
            context.assets.open("vietnamese_bigrams.txt").use { input ->
                BufferedReader(InputStreamReader(input, "UTF-8")).use { reader ->
                    reader.forEachLine { line ->
                        val p1 = line.indexOf('\t')
                        if (p1 <= 0) return@forEachLine
                        val p2 = line.indexOf('\t', p1 + 1)
                        if (p2 <= p1) return@forEachLine
                        val prev = line.substring(0, p1).lowercase(Locale.ROOT)
                        val next = line.substring(p1 + 1, p2).lowercase(Locale.ROOT)
                        val freq = line.substring(p2 + 1).trim().toIntOrNull() ?: return@forEachLine
                        lBigrams.getOrPut(prev) { HashMap() }[next] = freq
                    }
                }
            }
            // publish — gán field volatile, đặt `loaded = true` SAU CÙNG
            entries = lEntries
            byFirstClean = lByFirstClean
            cleanToBestFreq = lCleanToBestFreq
            exactWords = lExactWords
            bigrams = lBigrams
            cleanByFirstChar = lCleanByFirstChar
            loaded = true
            Log.d(TAG, "Loaded ${lEntries.size} words, ${lBigrams.size} bigram contexts")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load Vietnamese assets", e)
        }
    }

    // ======================= API chính =======================

    /**
     * @param typedWord  văn bản đang soạn (output combiner Telex, đã có dấu)
     * @param prevWord   từ ngay trước (ngữ cảnh bigram), có thể null/rỗng
     * @param sourceDict dictionary nguồn để gắn vào SuggestedWordInfo (không-null)
     */
    @JvmStatic
    fun getSuggestions(typedWord: String, prevWord: String?, sourceDict: Dictionary?): ArrayList<SuggestedWordInfo> {
        val out = ArrayList<SuggestedWordInfo>()
        if (!loaded || typedWord.isEmpty()) return out

        val level = appContext?.getSettingBlocking(VIETNAMESE_AUTOCORRECT_LEVEL) ?: LEVEL_BALANCED

        val cleanIn = VietnameseSuggestHelper.removeAccents(typedWord).lowercase(Locale.ROOT)
        val telexIn = VietnameseSuggestHelper.toTelex(typedWord)
        if (cleanIn.isEmpty()) return out

        val context = if (prevWord.isNullOrEmpty()) null
            else bigrams[prevWord.lowercase(Locale.ROOT)]

        // ---------- 1. Hoàn thành theo tiền tố ----------
        val scored = HashMap<String, Int>()   // word -> điểm tốt nhất
        val bucket = byFirstClean[cleanIn[0]]
        var hasPrefixOfReal = false
        if (bucket != null) {
            for (e in bucket) {
                val matchTelex = e.telex.startsWith(telexIn)
                val matchClean = e.clean.startsWith(cleanIn)
                if (!matchTelex && !matchClean) continue
                hasPrefixOfReal = true
                if (e.word.equals(typedWord, ignoreCase = true)) continue  // khỏi gợi lại y hệt
                val extraKeys = (e.telex.length - telexIn.length).coerceAtLeast(0)
                val wPenalty = if (e.telex.contains('w') && !telexIn.contains('w')) 1 else 0
                val score = COMPLETION_BASE + e.freq * FREQ_W -
                        extraKeys * EXTRA_KEY_PENALTY - wPenalty * W_PENALTY +
                        bigramBoost(context, e.word)
                val prev = scored[e.word]
                if (prev == null || score > prev) scored[e.word] = score
            }
        }

        // ---------- 2. Sửa lỗi (cân theo tần suất + ngữ cảnh) ----------
        val corrections = findCorrections(cleanIn)   // list (word, dist, freq) đã xếp hạng
        for (c in corrections) {
            val score = CORRECTION_BASE + c.freq * FREQ_W - (c.dist * DIST_PENALTY).toInt() +
                    bigramBoost(context, c.word)
            val prev = scored[c.word]
            if (prev == null || score > prev) scored[c.word] = score
        }

        // ---------- 3. Quyết định tự sửa ----------
        val typedIsRealWord = exactWords.contains(typedWord.lowercase(Locale.ROOT))
        val validShape = Common.isValidVietnameseSyllable(typedWord)
        val hasVowel = cleanIn.any { it in "aeiouy" }
        // chọn ứng viên sửa lỗi theo điểm CUỐI (gồm cả ngữ cảnh bigram), không theo freq thuần
        val best = corrections.maxByOrNull {
            it.freq * FREQ_W - (it.dist * DIST_PENALTY).toInt() + bigramBoost(context, it.word)
        }
        var autoWord: String? = null
        if (best != null && hasVowel && !typedIsRealWord &&
            shouldAutoCorrect(level, validShape, hasPrefixOfReal, best.dist)
        ) {
            autoWord = best.word
        }

        // ---------- 4. Dựng kết quả ----------
        val sorted = scored.entries.sortedByDescending { it.value }
        var added = 0
        for (entry in sorted) {
            if (autoWord != null && entry.key.equals(autoWord, ignoreCase = true)) continue
            out.add(makeInfo(entry.key, entry.value, false, sourceDict))
            if (++added >= MAX_SUGGESTIONS) break
        }
        if (autoWord != null) {
            // chèn ứng viên tự-sửa lên đầu với cờ whitelist + appropriate-for-autocorrection
            out.add(0, makeInfo(autoWord, AUTOCORRECT_SCORE, true, sourceDict))
        }
        return out
    }

    // ======================= Helpers =======================

    private fun bigramBoost(context: Map<String, Int>?, word: String): Int {
        if (context == null) return 0
        val f = context[word.lowercase(Locale.ROOT)] ?: return 0
        return f * BIGRAM_W
    }

    private fun shouldAutoCorrect(
        level: Int,
        validShape: Boolean,
        hasPrefixOfReal: Boolean,
        dist: Double
    ): Boolean {
        return when (level) {
            LEVEL_LOW ->
                // chỉ sửa typo rõ ràng (không phải âm tiết hợp lệ) và rất gần
                !validShape && dist <= DIST_LOW
            LEVEL_STRICT ->
                // mạnh nhất: sửa cả thiếu/sai dấu
                if (!validShape) dist <= DIST_STRICT
                else if (!hasPrefixOfReal) dist <= DIST_BALANCED
                else dist <= DIST_LOW
            else -> // LEVEL_BALANCED
                if (!validShape) dist <= DIST_BALANCED
                else !hasPrefixOfReal && dist <= DIST_LOW
        }
    }

    private fun makeInfo(word: String, score: Int, autocorrect: Boolean, sourceDict: Dictionary?): SuggestedWordInfo {
        val kind = if (autocorrect) {
            SuggestedWordInfo.KIND_WHITELIST or SuggestedWordInfo.KIND_FLAG_APPROPRIATE_FOR_AUTO_CORRECTION
        } else {
            SuggestedWordInfo.KIND_CORRECTION
        }
        return SuggestedWordInfo(
            word,
            "" /* prevWordsContext */,
            score.coerceAtLeast(1),
            kind,
            sourceDict,
            SuggestedWordInfo.NOT_AN_INDEX,
            SuggestedWordInfo.NOT_A_CONFIDENCE
        )
    }

    private class Corr(val word: String, val dist: Double, val freq: Int)

    /**
     * Tìm các từ thật gần [cleanIn] nhất, xếp hạng cân theo tần suất:
     * score = freq*FREQ_W - dist*DIST_PENALTY  (chào thắng chai dù xa hơn 0.5).
     */
    private fun findCorrections(cleanIn: String): List<Corr> {
        if (cleanIn.length < 2) return emptyList()
        // 1. tìm các âm tiết không-dấu gần nhất — chỉ quét bucket ký tự đầu + ký tự kề
        // (đúng tập mà bộ lọc first-char/adjacency cũ giữ lại), không quét toàn map.
        data class CleanCand(val clean: String, val dist: Double)
        val cleanCands = ArrayList<CleanCand>()
        val first = cleanIn[0]
        val index = cleanByFirstChar
        val groups = ArrayList<List<String>>(8)
        index[first]?.let { groups.add(it) }
        for ((c, list) in index) {
            if (c != first && isAdjacent(c, first)) groups.add(list)
        }
        for (group in groups) {
            for (clean in group) {
                if (kotlin.math.abs(clean.length - cleanIn.length) > 2) continue
                if (clean == cleanIn) continue
                val d = editDistance(cleanIn, clean)
                if (d <= MAX_EDIT_DISTANCE) cleanCands.add(CleanCand(clean, d))
            }
        }
        if (cleanCands.isEmpty()) return emptyList()
        val cleanSet = cleanCands.associate { it.clean to it.dist }

        // 2. nở ra các từ thật (có dấu) thuộc những âm tiết đó, gộp điểm cân tần suất
        val result = ArrayList<Corr>()
        val firstChar = cleanIn[0]
        val bucket = byFirstClean[firstChar] ?: emptyList()
        // gồm cả bucket phím-kề cho typo đầu từ
        val extraBuckets = byFirstClean.filterKeys { it != firstChar && isAdjacent(it, firstChar) }.values
        val pools = ArrayList<List<Entry>>()
        pools.add(bucket)
        pools.addAll(extraBuckets)
        for (pool in pools) {
            for (e in pool) {
                val d = cleanSet[e.clean] ?: continue
                result.add(Corr(e.word, d, e.freq))
            }
        }
        result.sortByDescending { it.freq * FREQ_W - (it.dist * DIST_PENALTY).toInt() }
        return result.take(MAX_SUGGESTIONS)
    }

    // ---- khoảng cách soạn thảo có tính phím kề (giống VietnameseSuggestHelper) ----
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

    // Damerau-Levenshtein có trọng số phím kề (subCost 0.0/0.5/1.0).
    // Dùng rolling 3 hàng tái dùng qua ThreadLocal (transposition đọc hàng i-2),
    // KHÔNG cấp phát ma trận mỗi lần gọi. Có band cutoff: distance > MAX_EDIT_DISTANCE
    // trả sentinel (findCorrections loại sẵn) — kết quả <= MAX_EDIT_DISTANCE giữ y hệt bản cũ.
    private const val MAX_DP_LEN = 24

    private val dpRows = ThreadLocal.withInitial {
        Array(3) { DoubleArray(MAX_DP_LEN + 1) }
    }

    private fun editDistance(s: String, t: String): Double {
        val m = s.length
        val n = t.length
        if (m == 0) return n.toDouble()
        if (n == 0) return m.toDouble()
        // t là phía âm tiết không-dấu (ngắn); n vượt buffer thì dùng đường chậm (gần như không xảy ra)
        if (n > MAX_DP_LEN) return editDistanceFull(s, t)

        val rows = dpRows.get()
        var prev2 = rows[0]
        var prev1 = rows[1]
        var cur = rows[2]

        for (j in 0..n) prev1[j] = j * 1.0

        for (i in 1..m) {
            cur[0] = i * 1.0
            var rowMin = cur[0]
            val c1 = s[i - 1]
            for (j in 1..n) {
                val c2 = t[j - 1]
                val subCost = if (c1 == c2) 0.0 else if (isAdjacent(c1, c2)) 0.5 else 1.0
                var v = minOf(
                    prev1[j] + 1.0,
                    cur[j - 1] + 1.0,
                    prev1[j - 1] + subCost
                )
                if (i > 1 && j > 1 && c1 == t[j - 2] && s[i - 2] == c2) {
                    v = minOf(v, prev2[j - 2] + 1.0)
                }
                cur[j] = v
                if (v < rowMin) rowMin = v
            }
            // row-min đơn điệu (mọi transition cộng cost >= 0): vượt ngưỡng thì không cứu lại được
            if (rowMin > MAX_EDIT_DISTANCE) return MAX_EDIT_DISTANCE + 1.0
            // rotate: prev2 <- prev1, prev1 <- cur, cur <- prev2 cũ (tái dùng)
            val tmp = prev2
            prev2 = prev1
            prev1 = cur
            cur = tmp
        }
        return prev1[n]
    }

    // Đường dự phòng khi n > MAX_DP_LEN: bản đầy đủ (giữ giá trị tuyệt đối, không cutoff).
    private fun editDistanceFull(s: String, t: String): Double {
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
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1.0,
                    dp[i][j - 1] + 1.0,
                    dp[i - 1][j - 1] + subCost
                )
                if (i > 1 && j > 1 && s[i - 1] == t[j - 2] && s[i - 2] == t[j - 1]) {
                    dp[i][j] = minOf(dp[i][j], dp[i - 2][j - 2] + 1.0)
                }
            }
        }
        return dp[m][n]
    }

    @VisibleForTesting
    internal fun editDistanceForTest(s: String, t: String): Double = editDistance(s, t)
}
