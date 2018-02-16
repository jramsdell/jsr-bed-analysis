package unh

import com.xenomachina.argparser.ShowHelpException

fun main(args: Array<String>) {
    try {
        runBedGraphParser(args)
    } catch (e: ShowHelpException) {
        System.out.println("QWEQWE")
        e.printAndExit()
    }
}