package nyub.interpreter

class JShellDecoratorService extends ScriptDecoratorService:
    override def id: String = "jshell"
    override def description: String =
        "Java interpreter based on JDK JShell outputing each expression toString() representation"

    override def createDecorator(
        parameters: Map[String, String]
    ): ScriptDecorator =
        ScriptDecorator(
          JShellInterpreter,
          "//> ",
          ScriptDecorator.Config.DEFAULT.eraseStartingWith("//> ")
        )
