package nyub.yadladoc.app

@main def main(args: String*): Unit =
    val result = YdocMain().parse(args).run()
    exitWith(result)
