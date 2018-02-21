@file:JvmName("BedGraphParser")
package unh

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.ShowHelpException
import com.xenomachina.argparser.default
import com.xenomachina.text.clear
import java.io.File
import java.lang.Math.abs



class MyParser(parser: ArgParser) {
    val bedFile by parser.storing("-b", "--bedFile",
            help = "BedGraph file containing genome coverage.")
    val interval by parser.storing("-n", "--interval",
            help = "Size of sliding window (in bp) over genome").default { "50" }
    val fasta by parser.storing("-f", "--fasta",
            help = "Fasta map for reference.").default { "" }
}

class Interval(var location: String, var start: Int, var stop: Int, var coverage: Double) {
    var intervals = ArrayList<Interval>()
    var times = 1
    operator fun plus(interval: Interval): Interval {
        if (interval.location != location || abs(interval.start - stop) > 5) {
            interval.intervals = this.intervals.let { it += this; it }
            return interval
        }
        stop = interval.stop
//        coverage = (interval.coverage + coverage) / 2
        coverage += interval.coverage
        times += 1
        return this
    }
}



fun parseFile(filename: String): Interval =
        File(filename).bufferedReader().lineSequence()
                .drop(1)
                .map { line ->
                    line.split("\t").run {
                        Interval(this[0], this[1].toInt(), this[2].toInt(), this[3].toDouble())
                    }
                }
                .filter { interval -> interval.coverage > 2 }
                .reduce(Interval::plus)

fun readFasta(filename: String): HashMap<String, String> {
    val fastaMap = HashMap<String, String>()
    var header = ""
    val builder = StringBuilder()

    File(filename).bufferedReader().forEachLine { line ->
        if (line.startsWith(">")) {
            if (header != "") {
                fastaMap[header.trimEnd()] = builder.toString()
                builder.clear()
            }

            header = line
        } else {
            builder.append(line.trimEnd())
        }
    }

    return fastaMap
}

fun extractSequences(p: MyParser, fasta: HashMap<String, String>,
                     results: Map<Int, List<Interval>>) {
    val pattern = "([ATCG]GG)".toRegex()

    val pamCountsByBin: List<Pair<Int, Double>> =
            results.map { (binSize,count) ->
                // For each interval, extract corresponding sequence and count PAM Sites
                val pamAverage = count.map { interval ->
                    fasta[">" + interval.location]!!
                            .substring(interval.start, interval.stop)
                            .run { pattern.findAll(this).count().toDouble() /
                                    (interval.stop - interval.start)
                            }
                }.sum().toDouble() / count.size // Averages counts in the bin

                binSize to pamAverage
            }
                    .sortedBy { it.first }

    pamCountsByBin.forEach(::println)
}

fun cutOutRanges(fasta: HashMap<String, String>, intervals: List<Interval>): Map<String, Int> {
    val pattern = "([ATCG]GG)".toRegex()
    return intervals.groupingBy(Interval::location)
            .fold(
                    initialValue = Pair(0, 0),
                    operation =  { (total, index), interval ->
                        val fastaString = fasta[interval.location]!!
                        val subseq = fastaString.subSequence(index, interval.start)
                        val pamCounts = pattern.findAll(subseq).count()
                        Pair(total + pamCounts, interval.stop)
                    })
            .map { (key,pamPair) -> key to pamPair.first}
            .toMap()
}


fun runBedGraphParser(args: Array<String>) {
//    val myArgs = arrayOf("--bedFile", "Sample_ZS1_human.bedGraph", "--interval", "50",
//            "--fasta", "hg38.fa")
    val myArgs = args
    val parser = ArgParser(myArgs)
    val p = MyParser(parser)
    val intervals = parseFile(p.bedFile)
    val results = intervals.intervals.groupBy { (it.stop - it.start) / p.interval.toInt() }

//    println("Histogram of Fragment Size (bin size: ${p.interval.toInt()})")
//    results.entries
//            .sortedBy { it.key }
//            .forEach { (k,v) -> println("${k * p.interval.toInt()}: ${v.size}") }
//
//    println("\nAverage Coverage of Binned Fragments")
//    results .map { (k, v) -> k to v.sumByDouble { it.coverage / it.times } / v.size }
//            .sortedBy { it.first }
//            .forEach { (k, v) -> println("${k * p.interval.toInt()}: $v") }

    if (p.fasta != "") {
        val fasta = readFasta(p.fasta)
//        extractSequences(p, fasta, results)
        val res = cutOutRanges(fasta, intervals.intervals.apply { add(0, intervals)})
        res.forEach(::println)
    }
}

fun main(args: Array<String>) {
    runBedGraphParser(args)
}

