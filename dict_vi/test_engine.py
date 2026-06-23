#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Mô phỏng logic VietnameseSuggestionEngine.kt để kiểm tra ranking offline."""
import os, unicodedata

HERE = os.path.dirname(os.path.abspath(__file__))
ASSET = os.path.normpath(os.path.join(HERE, "..", "java", "assets", "vietnamese_words.txt"))

FREQ_W = 1000
DIST_PENALTY = 18000
MAX_DIST = 2.5

ADJ = {
    'q':"wa",'w':"qeas",'e':"wrsd",'r':"etdf",'t':"ryfg",'y':"tygh",'u':"yihj",'i':"uojk",'o':"ipkl",'p':"ol",
    'a':"qwsz",'s':"weadzx",'d':"ersfxc",'f':"rtdgcv",'g':"tyfhvb",'h':"yughjn",'j':"uihknm",'k':"iojlm",'l':"opk",
    'z':"asx",'x':"sdzc",'c':"dfxv",'v':"fgcb",'b':"ghvn",'n':"hjbm",'m':"jkn"
}
def adj(a,b):
    a,b=a.lower(),b.lower()
    return a==b or (a in ADJ and b in ADJ[a])

def edit(s,t):
    m,n=len(s),len(t)
    dp=[[0.0]*(n+1) for _ in range(m+1)]
    for i in range(m+1): dp[i][0]=i
    for j in range(n+1): dp[0][j]=j
    for i in range(1,m+1):
        for j in range(1,n+1):
            c1,c2=s[i-1],t[j-1]
            sub=0.0 if c1==c2 else (0.5 if adj(c1,c2) else 1.0)
            dp[i][j]=min(dp[i-1][j]+1, dp[i][j-1]+1, dp[i-1][j-1]+sub)
            if i>1 and j>1 and s[i-1]==t[j-2] and s[i-2]==t[j-1]:
                dp[i][j]=min(dp[i][j], dp[i-2][j-2]+1)
    return dp[m][n]

def remove_accents(s):
    s=unicodedata.normalize('NFD',s)
    s=''.join(c for c in s if unicodedata.category(c)!='Mn')
    return s.replace('đ','d').replace('Đ','D').lower()

words=[]
with open(ASSET,encoding='utf-8') as f:
    for line in f:
        w,_,fr=line.partition('\t')
        if fr.strip().isdigit():
            words.append((w,int(fr.strip()),remove_accents(w)))

# clean -> best freq
clean_best={}
for w,fr,cl in words:
    if cl not in clean_best or fr>clean_best[cl]: clean_best[cl]=fr

def correct(inp, topn=5):
    cl=remove_accents(inp)
    cand_clean={}
    for c in clean_best:
        if abs(len(c)-len(cl))>2: continue
        if c[0]!=cl[0] and not adj(c[0],cl[0]): continue
        if c==cl: continue
        d=edit(cl,c)
        if d<=MAX_DIST: cand_clean[c]=d
    res=[]
    for w,fr,c in words:
        if c in cand_clean:
            res.append((w,fr,cand_clean[c]))
    res.sort(key=lambda x: -(x[1]*FREQ_W - int(x[2]*DIST_PENALTY)))
    return res[:topn]

for t in ["chaid","chaof","nguoif","ngöi","viet","hoc","caphe","khong","tieg"]:
    print(f"{t!r:10} -> {[w for w,_,_ in correct(t)]}")
