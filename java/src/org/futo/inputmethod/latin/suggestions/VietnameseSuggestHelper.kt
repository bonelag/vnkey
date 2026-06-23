package org.futo.inputmethod.latin.suggestions

import android.content.Context
import android.util.Log
import org.futo.inputmethod.event.combiners.vietnamese.Common
import org.futo.inputmethod.latin.SuggestedWords.SuggestedWordInfo
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

object VietnameseSuggestHelper {

    @Volatile private var cleanSyllables: Set<String> = emptySet()

    @JvmStatic
    fun init(context: Context) {
        try {
            context.assets.open("vietnamese_syllables.txt").use { input ->
                BufferedReader(InputStreamReader(input, "UTF-8")).use { reader ->
                    val content = reader.readLine() ?: ""
                    cleanSyllables = content.split(" ").toSet()
                }
            }
            Log.d("VietnameseSuggestHelper", "Loaded ${cleanSyllables.size} clean syllables from assets")
        } catch (e: Exception) {
            Log.e("VietnameseSuggestHelper", "Failed to load vietnamese_syllables.txt", e)
        }
    }

    data class CorrectionCandidate(val syllable: String, val distance: Double)

    private val KEY_ADJACENCY = mapOf(
        'q' to "wa", 'w' to "qeas", 'e' to "wrsd", 'r' to "etdf", 't' to "ryfg", 'y' to "tygh", 'u' to "yihj", 'i' to "uojk", 'o' to "ipkl", 'p' to "ol",
        'a' to "qwsz", 's' to "weadzx", 'd' to "ersfxc", 'f' to "rtdgcv", 'g' to "tyfhvb", 'h' to "yughjn", 'j' to "uihknm", 'k' to "iojlm", 'l' to "opk",
        'z' to "asx", 'x' to "sdzc", 'c' to "dfxv", 'v' to "fgcb", 'b' to "ghvn", 'n' to "hjbm", 'm' to "jkn"
    )

    private fun isAdjacent(c1: Char, c2: Char): Boolean {
        val l1 = c1.lowercaseChar()
        val l2 = c2.lowercaseChar()
        if (l1 == l2) return true
        return KEY_ADJACENCY[l1]?.contains(l2) == true
    }

    private fun editDistance(s: String, t: String): Double {
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

    @JvmStatic
    fun findCorrections(cleanWord: String): List<CorrectionCandidate> {
        if (cleanWord.isEmpty() || cleanSyllables.isEmpty()) return emptyList()
        val s = cleanWord.lowercase(Locale.ROOT)
        val candidates = mutableListOf<CorrectionCandidate>()
        for (syl in cleanSyllables) {
            if (Math.abs(s.length - syl.length) > 2) continue
            val dist = editDistance(s, syl)
            if (dist <= 2.0) {
                candidates.add(CorrectionCandidate(syl, dist))
            }
        }
        candidates.sortBy { it.distance }
        return candidates.take(5)
    }

    @JvmStatic
    fun tagAndFilterCorrections(res: List<SuggestedWordInfo>, distance: Double): List<SuggestedWordInfo> {
        val penalty = (distance * 50).toInt()
        return res.map { sug ->
            val rankScore = maxOf(1, sug.mScore - penalty)
            val newSug = SuggestedWordInfo(
                sug.mWord,
                sug.mPrevWordsContext,
                rankScore,
                SuggestedWordInfo.KIND_WHITELIST or SuggestedWordInfo.KIND_FLAG_APPROPRIATE_FOR_AUTO_CORRECTION,
                sug.mSourceDict,
                sug.mIndexOfTouchPointOfSecondWord,
                sug.mAutoCommitFirstWordConfidence
            )
            newSug.mOriginatesFromTransformerLM = sug.mOriginatesFromTransformerLM
            newSug.mOriginatesFromSwipeModel = sug.mOriginatesFromSwipeModel
            newSug
        }
    }

    @JvmStatic
    fun applyAutoCorrectionFlags(merged: ArrayList<SuggestedWordInfo>) {
        if (merged.isEmpty()) return
        val top = merged[0]
        val newSug = SuggestedWordInfo(
            top.mWord,
            top.mPrevWordsContext,
            top.mScore + 200,
            SuggestedWordInfo.KIND_WHITELIST or SuggestedWordInfo.KIND_FLAG_APPROPRIATE_FOR_AUTO_CORRECTION,
            top.mSourceDict,
            top.mIndexOfTouchPointOfSecondWord,
            top.mAutoCommitFirstWordConfidence
        )
        newSug.mOriginatesFromTransformerLM = top.mOriginatesFromTransformerLM
        newSug.mOriginatesFromSwipeModel = top.mOriginatesFromSwipeModel
        merged[0] = newSug
    }

    val ACCENTS_TELEX = mapOf(
        'á' to "as", 'ắ' to "aws", 'ấ' to "aas", 'é' to "es", 'ế' to "ees", 'í' to "is", 'ó' to "os", 'ố' to "oos", 'ớ' to "ows", 'ú' to "us", 'ứ' to "uws", 'ý' to "ys",
        'à' to "af", 'ằ' to "awf", 'ầ' to "aaf", 'è' to "ef", 'ề' to "eef", 'ì' to "if", 'ò' to "of", 'ồ' to "oof", 'ờ' to "owf", 'ù' to "uf", 'ừ' to "uwf", 'ỳ' to "yf",
        'ả' to "ar", 'ẳ' to "awr", 'ẩ' to "aar", 'ẻ' to "er", 'ể' to "eer", 'ỉ' to "ir", 'ỏ' to "or", 'ổ' to "oor", 'ở' to "owr", 'ủ' to "ur", 'ử' to "uwr", 'ỷ' to "yr",
        'ã' to "ax", 'ẵ' to "awx", 'ẫ' to "aax", 'ẽ' to "ex", 'ễ' to "eex", 'ĩ' to "ix", 'õ' to "ox", 'ỗ' to "oox", 'ỡ' to "owx", 'ũ' to "ux", 'ữ' to "uwx", 'ỹ' to "yx",
        'ạ' to "aj", 'ặ' to "awj", 'ậ' to "aaj", 'ẹ' to "ej", 'ệ' to "eej", 'ị' to "ij", 'ọ' to "oj", 'ộ' to "ooj", 'ợ' to "owj", 'ụ' to "uj", 'ự' to "uwj", 'ỵ' to "yj",
        'đ' to "dd", 'Đ' to "dd"
    )

    val BASE_CHARS_TELEX = mapOf(
        'â' to "aa", 'ă' to "aw", 'ê' to "ee", 'ô' to "oo", 'ơ' to "ow", 'ư' to "uw",
        'Â' to "aa", 'Ă' to "aw", 'Ê' to "ee", 'Ô' to "oo", 'Ơ' to "ow", 'Ư' to "uw"
    )

    fun removeAccents(str: String): String {
        val sb = StringBuilder(str.length)
        for (char in str) {
            val r = when (char) {
                'á', 'à', 'ả', 'ã', 'ạ', 'ă', 'ắ', 'ằ', 'ẳ', 'ẵ', 'ặ', 'â', 'ấ', 'ầ', 'ẩ', 'ẫ', 'ậ' -> 'a'
                'Á', 'À', 'Ả', 'Ã', 'Ạ', 'Ă', 'Ắ', 'Ằ', 'Ẳ', 'Ẵ', 'Ặ', 'Â', 'Ấ', 'Ầ', 'Ẩ', 'Ẫ', 'Ậ' -> 'A'
                'é', 'è', 'ẻ', 'ẽ', 'ẹ', 'ê', 'ế', 'ề', 'ể', 'ễ', 'ệ' -> 'e'
                'É', 'È', 'Ẻ', 'Ẽ', 'Ẹ', 'Ê', 'Ế', 'Ề', 'Ể', 'Ễ', 'Ệ' -> 'E'
                'í', 'ì', 'ỉ', 'ĩ', 'ị' -> 'i'
                'Í', 'Ì', 'Ỉ', 'Ĩ', 'Ị' -> 'I'
                'ó', 'ò', 'ỏ', 'õ', 'ọ', 'ô', 'ố', 'ồ', 'ổ', 'ỗ', 'ộ', 'ơ', 'ớ', 'ờ', 'ở', 'ỡ', 'ợ' -> 'o'
                'Ó', 'Ò', 'Ỏ', 'Õ', 'Ọ', 'Ô', 'Ố', 'Ồ', 'Ổ', 'Ỗ', 'Ộ', 'Ơ', 'Ớ', 'Ờ', 'Ở', 'Ỡ', 'Ợ' -> 'O'
                'ú', 'ù', 'ủ', 'ũ', 'ụ', 'ư', 'ứ', 'ừ', 'ử', 'ữ', 'ự' -> 'u'
                'Ú', 'Ù', 'Ủ', 'Ũ', 'Ụ', 'Ư', 'Ứ', 'Ừ', 'Ử', 'Ữ', 'Ự' -> 'U'
                'ý', 'ỳ', 'ỷ', 'ỹ', 'ỵ' -> 'y'
                'Ý', 'Ỳ', 'Ỷ', 'Ỹ', 'Ỵ' -> 'Y'
                'đ' -> 'd'
                'Đ' -> 'D'
                else -> char
            }
            sb.append(r)
        }
        return sb.toString()
    }

    fun toTelex(word: String): String {
        val sb = StringBuilder()
        for (char in word) {
            val lowerChar = char.lowercaseChar()
            if (ACCENTS_TELEX.containsKey(lowerChar)) {
                sb.append(ACCENTS_TELEX[lowerChar])
            } else if (BASE_CHARS_TELEX.containsKey(lowerChar)) {
                sb.append(BASE_CHARS_TELEX[lowerChar])
            } else {
                sb.append(lowerChar)
            }
        }
        return sb.toString()
    }

    fun generatePrefixes(cleanWord: String): List<String> {
        if (cleanWord.isEmpty()) return emptyList()
        val results = mutableListOf<String>()
        generateRecursive(cleanWord, 0, StringBuilder(), results)
        return results
    }

    private fun generateRecursive(cleanWord: String, index: Int, current: StringBuilder, results: MutableList<String>) {
        if (index == cleanWord.length) {
            val word = current.toString()
            if (Common.isValidVietnameseSyllable(word)) {
                results.add(word)
            }
            return
        }

        val char = cleanWord[index]
        val variants = getVariants(char)
        for (v in variants) {
            current.append(v)
            generateRecursive(cleanWord, index + 1, current, results)
            current.setLength(current.length - 1)
        }
    }

    private fun getVariants(char: Char): List<Char> {
        return when (char) {
            'a' -> listOf('a', 'á', 'à', 'ả', 'ã', 'ạ', 'â', 'ấ', 'ầ', 'ẩ', 'ẫ', 'ậ', 'ă', 'ắ', 'ằ', 'ẳ', 'ẵ', 'ặ')
            'A' -> listOf('A', 'Á', 'À', 'Ả', 'Ã', 'Ạ', 'Â', 'Ấ', 'Ầ', 'Ẩ', 'Ẫ', 'Ậ', 'Ă', 'Ắ', 'Ằ', 'Ẳ', 'Ẵ', 'Ặ')
            'd' -> listOf('d', 'đ')
            'D' -> listOf('D', 'Đ')
            'e' -> listOf('e', 'é', 'è', 'ẻ', 'ẽ', 'ẹ', 'ê', 'ế', 'ề', 'ể', 'ễ', 'ệ')
            'E' -> listOf('E', 'É', 'È', 'Ẻ', 'Ẽ', 'Ẹ', 'Ê', 'Ế', 'Ề', 'Ể', 'Ễ', 'Ệ')
            'i' -> listOf('i', 'í', 'ì', 'ỉ', 'ĩ', 'ị')
            'I' -> listOf('I', 'Í', 'Ì', 'Ỉ', 'Ĩ', 'Ị')
            'o' -> listOf('o', 'ó', 'ò', 'ỏ', 'õ', 'ọ', 'ô', 'ố', 'ồ', 'ổ', 'ỗ', 'ộ', 'ơ', 'ớ', 'ờ', 'ở', 'ỡ', 'ợ')
            'O' -> listOf('O', 'Ó', 'Ò', 'Ỏ', 'Õ', 'Ọ', 'Ô', 'Ố', 'Ồ', 'Ổ', 'Ỗ', 'Ộ', 'Ơ', 'Ớ', 'Ờ', 'Ở', 'Ỡ', 'Ợ')
            'u' -> listOf('u', 'ú', 'ù', 'ủ', 'ũ', 'ụ', 'ư', 'ứ', 'ừ', 'ử', 'ữ', 'ự')
            'U' -> listOf('U', 'Ú', 'Ù', 'Ủ', 'Ũ', 'Ụ', 'Ư', 'Ứ', 'Ừ', 'Ử', 'Ữ', 'Ự')
            'y' -> listOf('y', 'ý', 'ỳ', 'ỷ', 'ỹ', 'ỵ')
            'Y' -> listOf('Y', 'Ý', 'Ỳ', 'Ỷ', 'Ỹ', 'Ỵ')
            else -> listOf(char)
        }
    }

    @JvmStatic
    fun mergeAndRankVietnamese(
        lists: List<List<SuggestedWordInfo>>,
        typedWord: String
    ): ArrayList<SuggestedWordInfo> {
        val mergedMap = mutableMapOf<String, SuggestedWordInfo>()
        val typedWordTelex = toTelex(typedWord)
        
        for (list in lists) {
            for (sug in list) {
                val sugWordTelex = toTelex(sug.mWord)
                val extraKeys = Math.abs(sugWordTelex.length - typedWordTelex.length)
                var penalty = 0
                if (sugWordTelex.contains('w') && !typedWordTelex.contains('w')) {
                    penalty += 3
                }
                val rankScore = sug.mScore - (extraKeys + penalty) * 30
                
                val key = sug.mWord.lowercase()
                val existing = mergedMap[key]
                if (existing == null || rankScore > existing.mScore) {
                    val newSug = SuggestedWordInfo(
                        sug.mWord,
                        sug.mPrevWordsContext,
                        rankScore,
                        sug.mKindAndFlags,
                        sug.mSourceDict,
                        sug.mIndexOfTouchPointOfSecondWord,
                        sug.mAutoCommitFirstWordConfidence
                    )
                    newSug.mOriginatesFromTransformerLM = sug.mOriginatesFromTransformerLM
                    newSug.mOriginatesFromSwipeModel = sug.mOriginatesFromSwipeModel
                    mergedMap[key] = newSug
                }
            }
        }
        
        val sorted = mergedMap.values.sortedByDescending { it.mScore }
        return ArrayList(sorted)
    }

    @JvmStatic
    fun getOverlapLength(userInput: String, suggestedWord: String): Int {
        if (userInput.isEmpty() || suggestedWord.isEmpty()) return 0
        val lastSpaceIdx = userInput.lastIndexOf(' ')
        if (lastSpaceIdx < 0) return 0
        val prevWords = userInput.substring(0, lastSpaceIdx + 1)
        val cleanPrev = removeAccents(prevWords).lowercase()
        val cleanSug = removeAccents(suggestedWord).lowercase()
        
        val maxLen = minOf(cleanPrev.length, cleanSug.length)
        for (len in maxLen downTo 1) {
            val suffix = cleanPrev.substring(cleanPrev.length - len)
            val prefix = cleanSug.substring(0, len)
            if (suffix == prefix) {
                return len
            }
        }
        return 0
    }
}
