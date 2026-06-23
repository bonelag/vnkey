KEY_ADJACENCY = {
    'q': "wa", 'w': "qeas", 'e': "wrsd", 'r': "etdf", 't': "ryfg", 'y': "tygh", 'u': "yihj", 'i': "uojk", 'o': "ipkl", 'p': "ol",
    'a': "qwsz", 's': "weadzx", 'd': "ersfxc", 'f': "rtdgcv", 'g': "tyfhvb", 'h': "yughjn", 'j': "uihknm", 'k': "iojlm", 'l': "opk",
    'z': "asx", 'x': "sdzc", 'c': "dfxv", 'v': "fgcb", 'b': "ghvn", 'n': "hjbm", 'm': "jkn"
}

def is_adjacent(c1, c2):
    l1 = c1.lower()
    l2 = c2.lower()
    if l1 == l2:
        return True
    return l2 in KEY_ADJACENCY.get(l1, "")

def edit_distance(s, t):
    m = len(s)
    n = len(t)
    dp = [[0.0] * (n + 1) for _ in range(m + 1)]
    for i in range(m + 1):
        dp[i][0] = float(i)
    for j in range(n + 1):
        dp[0][j] = float(j)
        
    for i in range(1, m + 1):
        for j in range(1, n + 1):
            c1 = s[i - 1]
            c2 = t[j - 1]
            sub_cost = 0.0 if c1 == c2 else (0.5 if is_adjacent(c1, c2) else 1.0)
            dp[i][j] = min(
                dp[i - 1][j] + 1.0,      # deletion
                dp[i][j - 1] + 1.0,      # insertion
                dp[i - 1][j - 1] + sub_cost # substitution
            )
            if i > 1 and j > 1 and s[i - 1] == t[j - 2] and s[i - 2] == t[j - 1]:
                dp[i][j] = min(dp[i][j], dp[i - 2][j - 2] + 1.0)
    return dp[m][n]

print("chaid vs chao:", edit_distance("chaid", "chao"))
print("chaid vs chai:", edit_distance("chaid", "chai"))
