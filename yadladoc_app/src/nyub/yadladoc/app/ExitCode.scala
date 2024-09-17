package nyub.yadladoc.app

opaque type ExitCode = Int

/** Everything went fine */
private val OK_RETURN_CODE: ExitCode = 0

/** Something was wrong with the way the user called yadladoc */
private val INVALID_ARGUMENT_RETURN_CODE: ExitCode = 1

/** Yadladoc found a problem in the documentation files it was applied on */
private val DOCUMENTATION_PROBLEM_RETURN_CODE: ExitCode = 2

private def exitWith(errorCode: ExitCode): Unit =
    System.exit(errorCode)
