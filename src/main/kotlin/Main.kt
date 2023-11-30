import app.App
import kotlinx.coroutines.runBlocking

val app = App()

fun main(args: Array<String>) {
    app.onCreate(args.joinToString())
}