package nyub.interpreter

import java.nio.file.Paths

class CramDecoratorService extends ScriptDecoratorService:
    override def id: String = "cram"
    override def description: String = "cram-like interpreter for bash scripts"
    override def createDecorator(
        parameters: Map[String, String]
    ): ScriptDecorator =
        val bashPath = Paths.get(
          parameters.getOrElse(
            CramDecoratorService.BASH_PATH_PARAMETER_KEY,
            CramDecoratorService.BASH_PATH_DEFAULT
          )
        )
        CramDecorator(bashPath)

object CramDecoratorService:
    val BASH_PATH_PARAMETER_KEY: String = "cram.bash"
    val BASH_PATH_DEFAULT: String = "/bin/bash"
