package nyub.interpreter

/** An interface to instanciate [[ScriptDecorator]]s. Designed for (but not
  * restricted to) SPI usage
  */
trait ScriptDecoratorService:
    /** @return
      *   An unique identifier to represent this decorator
      */
    def id: String

    /** @return
      *   An extended description, including for example the languages this
      *   decorator is suited for
      */
    def description: String

    /** @param parameters
      *   free map to parameterize a given instanciation
      */
    def createDecorator(parameters: Map[String, String]): ScriptDecorator
