import app.App

fun main(args: Array<String>) {
    val app = App()
    app.onCreate(args.joinToString(" "))
}