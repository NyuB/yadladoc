package nyub.yadladoc

enum DocumentationKind:
    case ExampleSnippet(val name: String, val snippet: Snippet)
    case InterpretedSnippet(val interpreterId: String)
    case Raw
