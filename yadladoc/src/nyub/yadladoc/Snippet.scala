package nyub.yadladoc

case class Snippet(
    language: Option[Language],
    lines: Iterable[String],
    properties: Properties
)
