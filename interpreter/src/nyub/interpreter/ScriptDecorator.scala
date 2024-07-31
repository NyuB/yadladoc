package nyub.interpreter

trait ScriptDecorator:
    def decorate(lines: Iterable[String]): Iterable[String]
