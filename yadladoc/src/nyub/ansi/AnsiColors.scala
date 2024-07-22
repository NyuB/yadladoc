package nyub.ansi

object AnsiColors:
    val RED = "\u001B[31m"
    val LIGHT_RED = "\u001b[91m"
    val GREEN = "\u001B[32m"
    val LIGHT_GREEN = "\u001b[92m"

    val RESET = "\u001b[0m"

    val COLOR_CONTROL_SEQUENCES = List(
      RED,
      LIGHT_RED,
      GREEN,
      LIGHT_GREEN,
      RESET
    )
