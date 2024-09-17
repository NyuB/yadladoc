package nyub.yadladoc.app

private val OK_RETURN_CODE = 0
private val INVALID_ARGUMENT_RETURN_CODE = 1
private val DOCUMENTATION_PROBLEM_RETURN_CODE = 2

@main def main(args: String*) =
    val result = YdocMain().parse(args).run()
    exitWith(result)

private def exitWith(errorCode: Int): Nothing =
    System.exit(errorCode)
    throw IllegalStateException("Unreachable code, should have exited before")
