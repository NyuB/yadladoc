package nyub.yadladoc

enum DocumentationKind:
    case ExampleSnippet(val name: String, val snippet: Snippet)
    case DecoratedSnippet(val decoratorId: String)
    case Raw
