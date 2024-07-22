Link resources to test execution directory, TESTDIR is provided by cram and points to the directory containing the run.t file
  $ shopt -s dotglob
  $ ln -s ${TESTDIR}/../ydoc.jar ydoc.jar
  $ ln -s ${TESTDIR}/* .
Along generated files, yadladoc will also "decorate" code snippets annotated with ydoc.interpreter property
  $ cat README.md
  # Decoration example
  
  Here is a code snippet that would be decorated in-place:
  
  ```java ydoc.interpreter=jshell
  var l = java.util.List.of("Riri", "Fifi", "Loulou")
  l.size()
  l.get(1)
  l.contains("Picsou")
  l.get(-1)
  ```
  
  $ cp README.md README.md.not_decorated
  $ java -jar ydoc.jar run README.md
  Generated README.md from README.md
  $ cat README.md
  # Decoration example
  
  Here is a code snippet that would be decorated in-place:
  
  ```java ydoc.interpreter=jshell
  var l = java.util.List.of("Riri", "Fifi", "Loulou")
  //> [Riri, Fifi, Loulou]
  l.size()
  //> 3
  l.get(1)
  //> "Fifi"
  l.contains("Picsou")
  //> false
  l.get(-1)
  //> java.lang.ArrayIndexOutOfBoundsException
  ```
Just as other generated files, in check mode yadladoc will compare the actual markdown file
to the decorated version that would have been generated during a run
  $ cp README.md.not_decorated README.md
  $ rm README.md.not_decorated
  $ java -jar ydoc.jar check README.md
  Error [MismatchingContent]: File 'README.md' has mismatching content with what would have been generated
   var l = java.util.List.of("Riri", "Fifi", "Loulou")
  \x1b[92m+//> [Riri, Fifi, Loulou]\x1b[0m (esc)
   l.size()
  \x1b[92m+//> 3\x1b[0m (esc)
   l.get(1)
  \x1b[92m+//> "Fifi"\x1b[0m (esc)
   l.contains("Picsou")
  \x1b[92m+//> false\x1b[0m (esc)
   l.get(-1)
  \x1b[92m+//> java.lang.ArrayIndexOutOfBoundsException\x1b[0m (esc)
   ```
  [2]
