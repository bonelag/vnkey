package org.futo.inputmethod.event.combiners.vietnamese

object Telex {
    val TONES = mapOf(
        'f' to ToneMark.GRAVE,
        'j' to ToneMark.DOT,
        'r' to ToneMark.HOOK,
        's' to ToneMark.ACUTE,
        'x' to ToneMark.TILDE
    )

    val AFTER_VOWEL_MODIFIERS = setOf('f', 'j', 'r', 's', 'w', 'x')

    enum class Diacritic {
        NONE, CIRCUMFLEX, BREVE, HORN, STROKE
    }

    class TelexChar(
        var base: Char,
        var isUpper: Boolean,
        var diacritic: Diacritic = Diacritic.NONE
    )

    private fun getChar(base: Char, diacritic: Diacritic, isUpper: Boolean): Char {
        val lowerChar = when (base) {
            'a' -> when (diacritic) {
                Diacritic.CIRCUMFLEX -> 'â'
                Diacritic.BREVE -> 'ă'
                else -> 'a'
            }
            'e' -> when (diacritic) {
                Diacritic.CIRCUMFLEX -> 'ê'
                else -> 'e'
            }
            'o' -> when (diacritic) {
                Diacritic.CIRCUMFLEX -> 'ô'
                Diacritic.HORN -> 'ơ'
                else -> 'o'
            }
            'u' -> when (diacritic) {
                Diacritic.HORN -> 'ư'
                else -> 'u'
            }
            'd' -> when (diacritic) {
                Diacritic.STROKE -> 'đ'
                else -> 'd'
            }
            else -> base
        }
        return if (isUpper) lowerChar.uppercaseChar() else lowerChar
    }

    /** Tìm nguyên âm để áp dấu mũ (free placement): quét phải→trái, gặp nguyên âm cùng base
     * thì trả về (cho phép cách nhau bởi phụ âm, vd "tienges"→tiếng); gặp nguyên âm KHÁC base
     * thì dừng (nguyên âm khác chặn, giữ "oa"/"ao"/"oao" thành 2 nguyên âm). */
    private fun findCircumflexTarget(chars: List<TelexChar>, base: Char): Int {
        for (k in chars.indices.reversed()) {
            val ch = chars[k]
            if (ch.base in Common.VOWELS) {
                return if (ch.base == base) k else -1
            }
        }
        return -1
    }

    /** Tìm 'd' để áp/khôi phục dấu gạch (đ): quét phải→trái lấy 'd' đầu tiên. */
    private fun findStrokeTarget(chars: List<TelexChar>): Int {
        for (k in chars.indices.reversed()) {
            if (chars[k].base == 'd') return k
        }
        return -1
    }

    public fun telexToVietnamese(input: String): String {
        val chars = mutableListOf<TelexChar>()
        var currentTone: Char? = null
        // Sau khi khôi phục (undo) dấu mũ/gạch, phím thanh NGAY SAU emit literal (vd "aaas"→"aas").
        var blockNextTone = false

        for (c in input) {
            val cLower = c.lowercaseChar()
            val isUpper = c.isUpperCase()
            val hasVowel = chars.any { it.base in Common.VOWELS }

            // Phím thanh ngay sau restoration → literal, rồi tắt cờ.
            if (blockNextTone) {
                blockNextTone = false
                if (cLower in TONES.keys && hasVowel) {
                    chars.add(TelexChar(cLower, isUpper, Diacritic.NONE))
                    continue
                }
                // không phải thanh → xử lý bình thường bên dưới
            }

            // A. Dấu thanh (f/j/r/s/x) — đặt cuối qua getToneMarkPosition (free placement)
            if (cLower in TONES.keys && hasVowel) {
                if (cLower == currentTone) {
                    // gõ lại cùng thanh → bỏ thanh + literal
                    currentTone = null
                    chars.add(TelexChar(cLower, isUpper, Diacritic.NONE))
                } else {
                    currentTone = cLower  // last-wins
                }
                continue
            }

            // B. Dấu mũ (a/e/o) — free placement + restoration
            if (cLower == 'a' || cLower == 'e' || cLower == 'o') {
                val target = findCircumflexTarget(chars, cLower)
                if (target >= 0) {
                    val t = chars[target]
                    when (t.diacritic) {
                        Diacritic.CIRCUMFLEX -> {
                            t.diacritic = Diacritic.NONE
                            chars.add(TelexChar(cLower, isUpper, Diacritic.NONE))
                            blockNextTone = true
                            continue
                        }
                        Diacritic.NONE -> {
                            t.diacritic = Diacritic.CIRCUMFLEX
                            continue
                        }
                        else -> { /* BREVE/HORN/STROKE → rơi xuống append literal */ }
                    }
                }
                chars.add(TelexChar(cLower, isUpper, Diacritic.NONE))
                continue
            }

            // C. Dấu móc/trăng 'w' — free placement + restoration
            // Ưu tiên TẠO dấu khi còn nguyên âm chưa móc/trăng (giữ "uow"/"nguwowfi"→ươ/...);
            // chỉ khi KHÔNG còn gì để tạo mới khôi phục (gỡ + literal w, vd "aww"→"aw").
            if (cLower == 'w') {
                val candidates = chars.filter { it.base == 'u' || it.base == 'o' || it.base == 'a' }
                if (candidates.isEmpty()) {
                    chars.add(TelexChar('u', isUpper, Diacritic.HORN))  // w đứng riêng → ư
                    continue
                }
                val formable = candidates.filter { it.diacritic == Diacritic.NONE || it.diacritic == Diacritic.CIRCUMFLEX }
                if (formable.isNotEmpty()) {
                    val bases = formable.map { it.base }.toSet()
                    when {
                        bases.contains('u') && bases.contains('o') ->
                            for (v in formable) if (v.base == 'u' || v.base == 'o') v.diacritic = Diacritic.HORN
                        bases.contains('o') && bases.contains('a') ->
                            for (v in formable) if (v.base == 'a') v.diacritic = Diacritic.BREVE
                        bases.contains('u') ->
                            formable.first { it.base == 'u' }.diacritic = Diacritic.HORN  // chỉ u đầu tiên
                        bases.contains('o') ->
                            for (v in formable) if (v.base == 'o') v.diacritic = Diacritic.HORN
                        bases.contains('a') ->
                            for (v in formable) if (v.base == 'a') v.diacritic = Diacritic.BREVE
                    }
                    continue
                }
                // không còn nguyên âm để tạo → khôi phục: gỡ móc/trăng + literal w
                for (v in candidates) {
                    if (v.diacritic == Diacritic.HORN || v.diacritic == Diacritic.BREVE) {
                        v.diacritic = Diacritic.NONE
                    }
                }
                chars.add(TelexChar('w', isUpper, Diacritic.NONE))
                continue
            }

            // D. Dấu gạch 'd' (đ) — restoration
            if (cLower == 'd') {
                val target = findStrokeTarget(chars)
                if (target >= 0) {
                    val t = chars[target]
                    when (t.diacritic) {
                        Diacritic.STROKE -> {
                            t.diacritic = Diacritic.NONE
                            chars.add(TelexChar(cLower, isUpper, Diacritic.NONE))
                            blockNextTone = true
                            continue
                        }
                        Diacritic.NONE -> {
                            t.diacritic = Diacritic.STROKE
                            continue
                        }
                        else -> { /* rơi xuống literal */ }
                    }
                }
                chars.add(TelexChar(cLower, isUpper, Diacritic.NONE))
                continue
            }

            // E. Mặc định: thêm ký tự thô
            chars.add(TelexChar(cLower, isUpper, Diacritic.NONE))
        }

        val outputWithoutTone = StringBuilder()
        for (tc in chars) {
            outputWithoutTone.append(getChar(tc.base, tc.diacritic, tc.isUpper))
        }

        if (currentTone == null) {
            return outputWithoutTone.toString()
        }

        val finalVowelIndices = chars.indices.filter { chars[it].base in Common.VOWELS }
        if (finalVowelIndices.isEmpty()) {
            return outputWithoutTone.toString()
        }

        var firstVowelIndex = finalVowelIndices.first()
        var vowelCount = finalVowelIndices.size

        if (chars.size >= 2 && vowelCount > 1) {
            val firstBase = chars[0].base
            val secondBase = chars[1].base
            if ((firstBase == 'g' && secondBase == 'i') || (firstBase == 'q' && secondBase == 'u')) {
                firstVowelIndex++
                vowelCount--
            }
        }

        if (firstVowelIndex < 0 || firstVowelIndex + vowelCount > outputWithoutTone.length) {
            return outputWithoutTone.toString()
        }

        val toneMarkPosition = Common.getToneMarkPosition(outputWithoutTone, firstVowelIndex, vowelCount)
        if (toneMarkPosition !in 0 until outputWithoutTone.length) {
            return outputWithoutTone.toString()
        }

        val targetChar = outputWithoutTone[toneMarkPosition]
        val toneMarkObj = TONES[currentTone]
        if (toneMarkObj != null) {
            val accentedChar = toneMarkObj.map[targetChar] ?: targetChar
            outputWithoutTone.setCharAt(toneMarkPosition, accentedChar)
        }

        return outputWithoutTone.toString()
    }
}
