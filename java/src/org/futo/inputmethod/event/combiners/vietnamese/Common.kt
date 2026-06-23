package org.futo.inputmethod.event.combiners.vietnamese

/** Code common to both Telex and VNI */
object Common {
    /** get_tone_mark_placement() function from vi-rs/src/editing.rs
     * Get nth character to place tone mark
     *
     * # Rules:
     * 1. If a vowel contains ơ or ê, tone mark goes there
     * 2. If a vowel contains `oa`, `oe`, `oo`, `oy`, tone mark should be on the
     *    second character
     *
     * If the accent style is [`AccentStyle::Old`], then:
     * - 3. For vowel length 3 or vowel length 2 with a final consonant, put it on the second vowel character
     * - 4. Else, put it on the first vowel character
     *
     * Otherwise:
     * - 3. If a vowel has 2 characters, put the tone mark on the first one
     * - 4. Otherwise, put the tone mark on the second vowel character
     */
    fun getToneMarkPosition(
        outputWithoutTone: CharSequence,
        firstVowelIndex: Int,
        vowelCount: Int
    ): Int {
        val specialVowelPairs = setOf("oa", "oe", "oo", "uy", "uo", "ie")

        // If there's only one vowel, then it's guaranteed that the tone mark will go there
        if (vowelCount == 1) return firstVowelIndex

        for (i in firstVowelIndex ..< firstVowelIndex + vowelCount) {
            when (outputWithoutTone[i]) {
                'ơ', 'Ơ' -> return i
                'ê', 'Ê' -> return i
                'â', 'Â' -> return i
            }
        }

        val vowel = outputWithoutTone.slice(firstVowelIndex ..< firstVowelIndex + vowelCount)

        // If there is only one vowel with a diacritic (circumflex, breve, horn, etc.), it should
        // get the tone mark
        val vowelsWithDiacritics = vowel.withIndex().filter { it.value !in VOWELS }
        if (vowelsWithDiacritics.size == 1) {
            return firstVowelIndex + vowelsWithDiacritics[0].index
        }

        // Special vowels require the tone mark to be placed on the second character
        if (specialVowelPairs.any { vowel.contains(it, ignoreCase = true) })
            return firstVowelIndex + 1

        // If a syllable end with 2 character vowel, put it on the first character
        if (firstVowelIndex + vowelCount == outputWithoutTone.length && vowelCount == 2)
            return firstVowelIndex

        // Else, put tone mark on second vowel
        return firstVowelIndex + 1
    }


    val CONSONANTS = setOf(
        'b', 'c', 'd', 'f', 'g', 'h', 'j', 'k', 'l', 'm', 'n', 'p', 'q', 'r', 's', 't', 'v', 'w', 'x', 'z')

    val VOWELS = setOf('a', 'e', 'i', 'o', 'u', 'y', 'A', 'E', 'I', 'O', 'U', 'Y')

    /** A map of characters without accent to character with circumflex accent */
    public val CIRCUMFLEX_MAP = mapOf(
        'a' to 'â',
        'e' to 'ê',
        'o' to 'ô',
        // uppercase
        'A' to 'Â',
        'E' to 'Ê',
        'O' to 'Ô',
    )

    /** A map of characters without accent to character with dyet (D WITH STROKE) accent */
    public val STROKE_MAP = mapOf(
        'd' to 'đ',
        'D' to 'Đ',
    )

    /** A map of characters without accent to character with horn accent */
    public val HORN_MAP = mapOf(
        'u' to 'ư',
        'o' to 'ơ',
        // uppercase
        'U' to 'Ư',
        'O' to 'Ơ',
    )

    /** A map of characters without accent to character with breve accent */
    public val BREVE_MAP = mapOf(
        'a' to 'ă',
        // uppercase
        'A' to 'Ă',
    )

    private val VIET_VOWELS = setOf(
        'a', 'ă', 'â', 'e', 'ê', 'i', 'o', 'ô', 'ơ', 'u', 'ư', 'y',
        'á', 'à', 'ả', 'ã', 'ạ', 'ắ', 'ằ', 'ẳ', 'ẵ', 'ặ', 'ấ', 'ầ', 'ẩ', 'ẫ', 'ậ',
        'é', 'è', 'ẻ', 'ẽ', 'ẹ', 'ế', 'ề', 'ể', 'ễ', 'ệ',
        'í', 'ì', 'ỉ', 'ĩ', 'ị',
        'ó', 'ò', 'ỏ', 'õ', 'ọ', 'ố', 'ồ', 'ổ', 'ỗ', 'ộ', 'ớ', 'ờ', 'ở', 'ỡ', 'ợ',
        'ú', 'ù', 'ủ', 'ũ', 'ụ', 'ứ', 'ừ', 'ử', 'ữ', 'ự',
        'ý', 'ỳ', 'ỷ', 'ỹ', 'ỵ'
    )

    private val VALID_ONSETS = setOf(
        "b", "c", "ch", "d", "đ", "g", "gh", "gi", "h", "k", "kh", "l", "m", "n", "ng", "ngh", "nh",
        "p", "ph", "q", "qu", "r", "s", "t", "th", "tr", "v", "x"
    )

    private val VALID_CODAS = setOf(
        "", "c", "ch", "m", "n", "p", "t", "ng", "nh"
    )

    fun getBaseVowelChar(char: Char): Char {
        val lower = char.lowercaseChar()
        return when (lower) {
            'a', 'á', 'à', 'ả', 'ã', 'ạ', 'ă', 'ắ', 'ằ', 'ẳ', 'ẵ', 'ặ', 'â', 'ấ', 'ầ', 'ẩ', 'ẫ', 'ậ' -> 'a'
            'e', 'é', 'è', 'ẻ', 'ẽ', 'ẹ' -> 'e'
            'ê', 'ế', 'ề', 'ể', 'ễ', 'ệ' -> 'ê'
            'i', 'í', 'ì', 'ỉ', 'ĩ', 'ị' -> 'i'
            'o', 'ó', 'ò', 'ỏ', 'õ', 'ọ', 'ô', 'ố', 'ồ', 'ổ', 'ỗ', 'ộ', 'ơ', 'ớ', 'ờ', 'ở', 'ỡ', 'ợ' -> 'o'
            'u', 'ú', 'ù', 'ủ', 'ũ', 'ụ', 'ư', 'ứ', 'ừ', 'ử', 'ữ', 'ự' -> 'u'
            'y', 'ý', 'ỳ', 'ỷ', 'ỹ', 'ỵ' -> 'y'
            else -> lower
        }
    }

    fun isValidVietnameseSyllable(word: String): Boolean {
        val lowercaseWord = word.lowercase()
        val vowelIndices = lowercaseWord.indices.filter { lowercaseWord[it] in VIET_VOWELS }
        
        if (vowelIndices.isEmpty()) {
            return true
        }

        val firstVowel = vowelIndices.first()
        val lastVowel = vowelIndices.last()

        for (i in firstVowel..lastVowel) {
            if (lowercaseWord[i] !in VIET_VOWELS) {
                return false
            }
        }

        val onset = lowercaseWord.substring(0, firstVowel)
        if (onset.isNotEmpty() && onset !in VALID_ONSETS) {
            return false
        }

        val coda = lowercaseWord.substring(lastVowel + 1)
        if (coda !in VALID_CODAS) {
            return false
        }

        val lastVowelChar = lowercaseWord[lastVowel]
        val lastBaseVowel = getBaseVowelChar(lastVowelChar)
        if ((coda == "nh" || coda == "ch") && lastBaseVowel !in setOf('a', 'e', 'ê', 'i', 'y')) {
            return false
        }

        val firstVowelChar = lowercaseWord[firstVowel]
        val baseVowel = getBaseVowelChar(firstVowelChar)

        when (onset) {
            "k" -> {
                if (baseVowel !in setOf('i', 'e', 'ê', 'y')) return false
            }
            "c" -> {
                if (baseVowel in setOf('i', 'e', 'ê', 'y')) return false
            }
            "gh", "ngh" -> {
                if (baseVowel !in setOf('i', 'e', 'ê')) return false
            }
            "g" -> {
                if (baseVowel in setOf('e', 'ê')) return false
            }
            "ng" -> {
                if (baseVowel in setOf('i', 'e', 'ê')) return false
            }
        }

        return true
    }

    val TELEX_DECOMPOSE_MAP = mapOf(
        // Lowercase
        'á' to "as", 'à' to "af", 'ả' to "ar", 'ã' to "ax", 'ạ' to "aj",
        'â' to "aa", 'ấ' to "aas", 'ầ' to "aaf", 'ẩ' to "aar", 'ẫ' to "aax", 'ậ' to "aaj",
        'ă' to "aw", 'ắ' to "aws", 'ằ' to "awf", 'ẳ' to "awr", 'ẵ' to "awx", 'ặ' to "awj",
        'é' to "es", 'è' to "ef", 'ẻ' to "er", 'ẽ' to "ex", 'ẹ' to "ej",
        'ê' to "ee", 'ế' to "ees", 'ề' to "eef", 'ể' to "eer", 'ễ' to "eex", 'ệ' to "eej",
        'í' to "is", 'ì' to "if", 'ỉ' to "ir", 'ĩ' to "ix", 'ị' to "ij",
        'ó' to "os", 'ò' to "of", 'ỏ' to "or", 'õ' to "ox", 'ọ' to "oj",
        'ô' to "oo", 'ố' to "oos", 'ồ' to "oof", 'ổ' to "oor", 'ỗ' to "oox", 'ộ' to "ooj",
        'ơ' to "ow", 'ớ' to "ows", 'ờ' to "owf", 'ở' to "owr", 'ỡ' to "owx", 'ợ' to "owj",
        'ú' to "us", 'ù' to "uf", 'ủ' to "ur", 'ũ' to "ux", 'ụ' to "uj",
        'ư' to "uw", 'ứ' to "uws", 'ừ' to "uwf", 'ử' to "uwr", 'ữ' to "uwx", 'ự' to "uwj",
        'ý' to "ys", 'ỳ' to "yf", 'ỷ' to "yr", 'ỹ' to "yx", 'ỵ' to "yj",
        'đ' to "dd",
        // Uppercase
        'Á' to "As", 'À' to "Af", 'Ả' to "Ar", 'Ã' to "Ax", 'Ạ' to "Aj",
        'Â' to "Aa", 'Ấ' to "Aas", 'Ầ' to "Aaf", 'Ẩ' to "Aar", 'Ẫ' to "Aax", 'Ậ' to "Aaj",
        'Ă' to "Aw", 'Ắ' to "Aws", 'Ằ' to "Awf", 'Ẳ' to "Awr", 'Ẵ' to "Awx", 'Ặ' to "Awj",
        'É' to "Es", 'È' to "Ef", 'Ẻ' to "Er", 'Ẽ' to "Ex", 'Ẹ' to "Ej",
        'Ê' to "Ee", 'Ế' to "Ees", 'Ề' to "Eef", 'Ể' to "Eer", 'Ễ' to "Eex", 'Ệ' to "Eej",
        'Í' to "Is", 'Ì' to "If", 'Ỉ' to "Ir", 'Ĩ' to "Ix", 'Ị' to "Ij",
        'Ó' to "Os", 'Ò' to "Of", 'Ỏ' to "Or", 'Õ' to "Ox", 'Ọ' to "Oj",
        'Ô' to "Oo", 'Ố' to "Oos", 'Ồ' to "Oof", 'Ổ' to "Oor", 'Ỗ' to "Oox", 'Ộ' to "Ooj",
        'Ơ' to "Ow", 'Ớ' to "Ows", 'Ờ' to "Owf", 'Ở' to "Owr", 'Ỡ' to "Owx", 'Ợ' to "Owj",
        'Ú' to "Us", 'Ù' to "Uf", 'Ủ' to "Ur", 'Ũ' to "Ux", 'Ụ' to "Uj",
        'Ư' to "Uw", 'Ứ' to "Uws", 'Ừ' to "Uwf", 'Ử' to "Uwr", 'Ữ' to "Uwx", 'Ự' to "Uwj",
        'Ý' to "Ys", 'Ỳ' to "Yf", 'Ỷ' to "Yr", 'Ỹ' to "Yx", 'Ỵ' to "Yj",
        'Đ' to "Dd"
    )

    val VNI_DECOMPOSE_MAP = mapOf(
        // Lowercase
        'á' to "a1", 'à' to "a2", 'ả' to "a3", 'ã' to "a4", 'ạ' to "a5",
        'â' to "a6", 'ấ' to "a61", 'ầ' to "a62", 'ẩ' to "a63", 'ẫ' to "a64", 'ậ' to "a65",
        'ă' to "a8", 'ắ' to "a81", 'ằ' to "a82", 'ẳ' to "a83", 'ẵ' to "a84", 'ặ' to "a85",
        'é' to "e1", 'è' to "e2", 'ẻ' to "e3", 'ẽ' to "e4", 'ẹ' to "e5",
        'ê' to "e6", 'ế' to "e61", 'ề' to "e62", 'ể' to "e63", 'ễ' to "e64", 'ệ' to "e65",
        'í' to "i1", 'ì' to "i2", 'ỉ' to "i3", 'ĩ' to "i4", 'ị' to "i5",
        'ó' to "o1", 'ò' to "o2", 'ỏ' to "o3", 'õ' to "o4", 'ọ' to "o5",
        'ô' to "o6", 'ố' to "o61", 'ồ' to "o62", 'ổ' to "o63", 'ỗ' to "o64", 'ộ' to "o65",
        'ơ' to "o7", 'ớ' to "o71", 'ờ' to "o72", 'ở' to "o73", 'ỡ' to "o74", 'ợ' to "o75",
        'ú' to "u1", 'ù' to "u2", 'ủ' to "u3", 'ũ' to "u4", 'ụ' to "u5",
        'ư' to "u7", 'ứ' to "u71", 'ừ' to "u72", 'ử' to "u73", 'ữ' to "u74", 'ự' to "u75",
        'ý' to "y1", 'ỳ' to "y2", 'ỷ' to "y3", 'ỹ' to "y4", 'ỵ' to "y5",
        'đ' to "d9",
        // Uppercase
        'Á' to "A1", 'À' to "A2", 'Ả' to "A3", 'Ã' to "A4", 'Ạ' to "A5",
        'Â' to "A6", 'Ấ' to "A61", 'Ầ' to "A62", 'Ẩ' to "A63", 'Ẫ' to "A64", 'Ậ' to "A65",
        'Ă' to "A8", 'Ắ' to "A81", 'Ằ' to "A82", 'Ẳ' to "A83", 'Ẵ' to "A84", 'Ặ' to "A85",
        'É' to "E1", 'È' to "E2", 'Ẻ' to "E3", 'Ẽ' to "E4", 'Ẹ' to "E5",
        'Ê' to "E6", 'Ế' to "E61", 'Ề' to "E62", 'Ể' to "E63", 'Ễ' to "E64", 'Ệ' to "E65",
        'Í' to "I1", 'Ì' to "I2", 'Ỉ' to "I3", 'Ĩ' to "I4", 'Ị' to "I5",
        'Ó' to "O1", 'Ò' to "O2", 'Ỏ' to "O3", 'Õ' to "O4", 'Ọ' to "O5",
        'Ô' to "O6", 'Ố' to "O61", 'Ồ' to "O62", 'Ổ' to "O63", 'Ỗ' to "O64", 'Ộ' to "O65",
        'Ơ' to "O7", 'Ớ' to "O71", 'Ờ' to "O72", 'Ở' to "O73", 'Ỡ' to "O74", 'Ợ' to "O75",
        'Ú' to "U1", 'Ù' to "U2", 'Ủ' to "U3", 'Ũ' to "U4", 'Ụ' to "U5",
        'Ư' to "U7", 'Ứ' to "U71", 'Ừ' to "U72", 'Ử' to "U73", 'Ữ' to "U74", 'Ự' to "U75",
        'Ý' to "Y1", 'Ỳ' to "Y2", 'Ỷ' to "Y3", 'Ỹ' to "Y4", 'Ỵ' to "Y5",
        'Đ' to "D9"
    )

    fun decomposeTelex(char: Char): String {
        return TELEX_DECOMPOSE_MAP[char] ?: char.toString()
    }

    fun decomposeVni(char: Char): String {
        return VNI_DECOMPOSE_MAP[char] ?: char.toString()
    }

    fun decomposeStringToTelex(word: String): String {
        val result = StringBuilder()
        for (ch in word) {
            result.append(decomposeTelex(ch))
        }
        return result.toString()
    }

    fun decomposeStringToVni(word: String): String {
        val result = StringBuilder()
        for (ch in word) {
            result.append(decomposeVni(ch))
        }
        return result.toString()
    }

    fun isWordSeparator(char: Char): Boolean {
        if (char.isWhitespace()) return true
        val separators = ",.:;!?()[]{}<>\\/|*+=\"'`~^"
        return char in separators
    }

    fun containsInvalidDigits(original: String, isVni: Boolean): Boolean {
        if (!isVni) {
            return original.any { it.isDigit() }
        } else {
            for (i in original.indices) {
                val ch = original[i]
                if (ch.isDigit()) {
                    if (ch == '9' && i > 0 && (original[i-1] == 'd' || original[i-1] == 'D')) {
                        continue
                    }
                    var allRemainingAreDigits = true
                    for (j in i..<original.length) {
                        if (!original[j].isDigit()) {
                            allRemainingAreDigits = false
                            break
                        }
                    }
                    if (allRemainingAreDigits) {
                        continue
                    }
                    return true
                }
            }
            return false
        }
    }
}
