package nyub.interpreter

trait ScriptDecoratorService:
    def id: String
    def description: String
    def createDecorator(parameters: Map[String, String]): ScriptDecorator
