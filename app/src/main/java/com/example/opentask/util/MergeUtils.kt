package com.example.opentask.util


fun generateConflict(
    f1: String,
    f2: String,
    label1: String = "APP",
    label2: String = "DISK",
    separator: String = "\n"
): String {
    val units1 = if (separator.isEmpty()) f1.map { it.toString() } else f1.split(separator)
    val units2 = if (separator.isEmpty()) f2.map { it.toString() } else f2.split(separator)

    val opcodes = diffOpcodes(units1, units2)
    val result = mutableListOf<String>()

    for (op in opcodes) {
        when (op.tag) {
            "equal" -> result.addAll(units1.subList(op.i1, op.i2))
            else -> {
                result.add("<<<<<<< $label1")
                result.addAll(units1.subList(op.i1, op.i2))
                result.add("=======")
                result.addAll(units2.subList(op.j1, op.j2))
                result.add(">>>>>>> $label2")
            }
        }
    }
    return result.joinToString(separator)
}


data class Opcode(val tag: String, val i1: Int, val i2: Int, val j1: Int, val j2: Int)

// Version générique : marche sur List<String> (lignes) comme sur List<Char> (caractères)
fun <T> diffOpcodes(a: List<T>, b: List<T>): List<Opcode> {
    val n = a.size
    val m = b.size
    val dp = Array(n + 1) { IntArray(m + 1) }

    for (i in n - 1 downTo 0) {
        for (j in m - 1 downTo 0) {
            dp[i][j] = if (a[i] == b[j]) dp[i + 1][j + 1] + 1
                       else maxOf(dp[i + 1][j], dp[i][j + 1])
        }
    }

    val opcodes = mutableListOf<Opcode>()
    var i = 0
    var j = 0
    var equalStartI = 0
    var equalStartJ = 0
    var diffStartI = 0
    var diffStartJ = 0
    var inEqual = true

    fun flushDiff(endI: Int, endJ: Int) {
        if (diffStartI < endI || diffStartJ < endJ) {
            opcodes.add(Opcode("replace", diffStartI, endI, diffStartJ, endJ))
        }
    }

    fun flushEqual(endI: Int, endJ: Int) {
        if (equalStartI < endI) {
            opcodes.add(Opcode("equal", equalStartI, endI, equalStartJ, endJ))
        }
    }

    while (i < n && j < m) {
        if (a[i] == b[j]) {
            if (!inEqual) {
                flushDiff(i, j)
                inEqual = true
                equalStartI = i
                equalStartJ = j
            }
            i++; j++
        } else {
            if (inEqual) {
                flushEqual(i, j)
                inEqual = false
                diffStartI = i
                diffStartJ = j
            }
            if (dp[i + 1][j] >= dp[i][j + 1]) i++ else j++
        }
    }

    if (inEqual) flushEqual(i, j) else flushDiff(i, j)

    if (i < n || j < m) {
        opcodes.add(Opcode("replace", i, n, j, m))
    }

    return opcodes
}

//fun main() {
//    val f1 = "ligne 1\nligne 2 modifiée par A\nligne 3"
//    val f2 = "ligne 1\nligne 2 modifiée par B\nligne 3"
//    println(generateConflict(f1, f2, "version_A", "version_B", separator = "\n"))
//
//    println("---")
//
//    val g1 = "helloworldkotlin"
//    val g2 = "hallowurldkotlin"
//    println(generateConflict(g1, g2, "version_A", "version_B", separator = ""))
//}