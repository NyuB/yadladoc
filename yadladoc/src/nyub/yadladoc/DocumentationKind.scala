package nyub.yadladoc

enum DocumentationKind:
    case ExampleSnippet(val name: String, val snippet: Snippet)
    case InterpretedSnippet(val snippet: Snippet)
    case Raw
