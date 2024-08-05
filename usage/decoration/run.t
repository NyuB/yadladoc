Link resources to test execution directory, TESTDIR is provided by cram and points to the directory containing the run.t file
  $ shopt -s dotglob
  $ ln -s ${TESTDIR}/../ydoc.jar ydoc.jar
  $ ln -s ${TESTDIR}/* .
Along generated files, yadladoc will also "decorate" code snippets annotated with ydoc.decorator property
  $ cat README.md
  # Decoration example
  
  Here is a code snippet that would be decorated in-place:
  
  ```java ydoc.decorator=jshell
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
  
  ```java ydoc.decorator=jshell
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
  +//> [Riri, Fifi, Loulou]
   l.size()
  +//> 3
   l.get(1)
  +//> "Fifi"
   l.contains("Picsou")
  +//> false
   l.get(-1)
  +//> java.lang.ArrayIndexOutOfBoundsException
   ```
  [2]

An error is reported if the decorator is unknown in both run and check mode:
  $ cat UNKNOWN.md
  # Decoration error example
  
  The following code snippet refers to an unknown `ydoc.decorator`:
  
  ```text ydoc.decorator=unknown-decorator
  line 1
  line 2
  ``` (no-eol)
  $ java -jar ydoc.jar check UNKNOWN.md
  Error [MissingDecoratorError]: Unknown decorator id 'unknown-decorator'
  [2]
  $ java -jar ydoc.jar run UNKNOWN.md
  Error [MissingDecoratorError]: Unknown decorator id 'unknown-decorator'
  [2]
