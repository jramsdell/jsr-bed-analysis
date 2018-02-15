@file:JvmName("BedGraphParser")
package unh

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.SystemExitException
import com.xenomachina.argparser.default
import java.io.File
import java.lang.Math.abs

class MyParser(parser: ArgParser) {
    val bedFile by parser.storing("-b", "--bedFile",
            help = "BedGraph file containing genome coverage.")
    val interval by parser.storing("-n", "--interval",
            help = "Size of sliding window (in bp) over genome").default { "50" }
}

class Interval(var location: String, var start: Int, var stop: Int, var coverage: Double) {
    var intervals = ArrayList<Interval>()
    operator fun plus(interval: Interval): Interval {
        if (interval.location != location || abs(interval.start - stop) > 5) {
            interval.intervals = this.intervals.let { it += this; it }
            return interval
        }
        stop = interval.stop
        coverage = (interval.coverage + coverage) / 2
        return this
    }
}

var mycount = 0


class GraphParser(val filename: String, val interval: Int) {
    val intervals: ArrayList<Interval> = ArrayList()
    var lastInterval: Interval? = null

    fun createIntervals(line: String) {
        val curInterval = line.split("\t").run {
            Interval(this[0], this[1].toInt(), this[2].toInt(), this[3].toDouble())
        }

        if (lastInterval == null) {
            lastInterval = curInterval
            return
        }



    }

    fun parseFile(): Interval =
        File(filename).bufferedReader().lineSequence()
                .drop(1)
                .onEach { mycount += 1 }
                .map { line ->
                    line.split("\t").run {
                        Interval(this[0], this[1].toInt(), this[2].toInt(), this[3].toDouble())
                    }}
                .filter { interval -> interval.coverage > 2 }
                .reduce(Interval::plus)
}

fun main(args: Array<String>) {
    val myArgs = arrayOf("--bedFile", "Sample_ZS1_human.bedGraph")
    val parser = ArgParser(myArgs)
    val p = MyParser(parser)
    val gp = GraphParser(p.bedFile, p.interval.toInt())
    val combined = gp.parseFile()

    val counter = HashMap<Int, Int>()
//    combined.intervals.forEach {
//        val diff = ((it.stop - it.start)/50).toInt()
//        counter[diff] = counter.getOrDefault(diff, 0) + 1
//    }

//    counter.entries.sortedByDescending { it.value }
////            .take(20)
//            .forEach { println("${it.key}: ${it.value}") }
    val results = combined.intervals.groupBy { (it.stop - it.start) / 50 }

    println("Histogram of Fragment Size (bin size: 50)")
    results.entries
            .sortedBy { it.key }
            .forEach { (k,v) -> println("${k.toInt() * 50}: ${v.size}") }

    println("\nAverage Coverage of Binned Fragments")
    results .map { (k, v) -> k to v.sumByDouble { it.coverage } / v.size }
            .sortedBy { it.first }
            .forEach { (k, v) -> println("${k.toInt() * 50}: $v") }



}
