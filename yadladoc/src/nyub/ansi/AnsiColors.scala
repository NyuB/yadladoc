package nyub.ansi

object AnsiColors:
    val RED: String = "\u001B[31m"
    val LIGHT_RED: String = "\u001b[91m"
    val GREEN: String = "\u001B[32m"
    val LIGHT_GREEN: String = "\u001b[92m"

    val RESET: String = "\u001b[0m"

    val COLOR_CONTROL_SEQUENCES: List[String] = List(
      RED,
      LIGHT_RED,
      GREEN,
      LIGHT_GREEN,
      RESET
    )

    object Helpers:
        extension (s: String)
            def red: String = s"$RED$s$RESET"
            def green: String = s"$GREEN$s$RESET"

        extension (sc: StringContext)
            def red(args: Any*): String = sc.s(args*).red
            def green(args: Any*): String = sc.s(args*).green
