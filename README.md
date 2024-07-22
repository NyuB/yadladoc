# Yadladoc: reliable, tested code snippets in your documentation
![ci-status](https://github.com/NyuB/yadladoc/actions/workflows/ci.yml/badge.svg?event=push&branch=main)

Inspired by [cram](https://bitheap.org/cram/), [knit](https://github.com/Kotlin/kotlinx-knit), [mdoc](https://github.com/scalameta/mdoc), [mdx](https://github.com/realworldocaml/mdx)

## Motivation
Documenting code is a an effort, maintening the documentation can be even harder. This is especially true for code snippets that can 'rot' easily with refactorings, API changes, renaming, removals...

 But code snippets also have considerable values, as a few lines of codes can sometimes illustrate an API usage in a much more consise and expressive way than a higher level description. 
 
 A place where code snippets are guaranteed to be valid is the test suite. Indeed, we can consider the tests as an accurate documentation of our code(assuming they are present) but the suites are often far away from the README, asciidoc folder, or other entrypoint document for a user discovering the code. 
 To have the best of both world, an idea is to blur the barrier between test and documentation. 
 
 Yadladoc tries to follow this path in a way similar to [knit](https://github.com/Kotlin/kotlinx-knit) by generating files from mardkown code snippets. These file would be placed in your test suite folders and executed during CI, ensuring their validity. From the reader point of view, they are just snippet embedded in the visual markdown documentation, keeping the readibility intact.

![Schema](.ydoc/images/idea.png)

## Usage
### Basics
`/!\` Yadladoc requires a `.ydoc/ydoc.properties` file at the place of execution. You can leave it empty for the default configuration

````console ydoc.example=usage/basics/run.t
Yadladoc can be run in two modes 'check' and 'run'
  $ java -jar ydoc.jar --help
  Usage: yadladoc {run|check} <markdown_file>
      run  : generate documentation code from snippets in <markdown_file>
      check: validate that the currrent documentation code is identical to what would be generated by 'run'

For both modes, the input is a markdown file containing code snippets marked with ydoc.example=<name>
  $ cat README.md
  # Test README
  
  Here is a code snippet that would generate an actual test file:
  
  ```java ydoc.example=Test.java
  import org.junit.jupiter.api.Test;
  import static org.junit.jupiter.api.Assertions.assertEquals;
  class Test {
      @Test
      void test() {
          assertEquals(42, 21 * 2);
      }
  }
  
  ```

In 'run' mode, Yadladoc generates actual files based on these code snippets
  $ java -jar ydoc.jar run README.md
  Generated Test.java from README.md
  $ cat Test.java
  import org.junit.jupiter.api.Test;
  import static org.junit.jupiter.api.Assertions.assertEquals;
  class Test {
      @Test
      void test() {
          assertEquals(42, 21 * 2);
      }
  }

In 'check' mode, Yadladoc verifies that the files content indeed match what would be generated from the snippets
  $ java -jar ydoc.jar check README.md
It will fail on files with mismatching content
  $ sed -i 's/assertEquals/assertNumberEquals/g' Test.java
  $ java -jar ydoc.jar check README.md
  Error [MismatchingContent]: File 'Test.java' has mismatching content with what would have been generated
   import org.junit.jupiter.api.Test;
  -import static org.junit.jupiter.api.Assertions.assertNumberEquals;
  +import static org.junit.jupiter.api.Assertions.assertEquals;
   class Test {
       void test() {
  -        assertNumberEquals(42, 21 * 2);
  +        assertEquals(42, 21 * 2);
       }
  [2]

It will fail on missing files
  $ rm Test.java
  $ java -jar ydoc.jar check README.md
  Error [MissingFile]: File 'Test.java' is missing
  [2]
````

### Marking a snippet as an example
yadladoc will use snippets marked with the property `ydoc.example=<name>` and ignore the others.

````markdown
# Example
Here is a snippet that would generate an examples/add.py file

```python ydoc.example=examples/add.py
def add(a: int, b: int) -> int:
        return a + b
```

And here is one that would be ignored:
```python
def hey():
        print("Hey")
```
````

Consecutive snippets with the same `ydoc.example` value will be concatenated in the same generated file

### Templates
Yadladoc generate files from templates. Once an example is parsed from markdown snippets, it's content is injected in the template defined for the related language in `.ydoc/includes/<language>.template`. A custom template can also be specified via the `ydoc.template=<id>` property, to use `.ydoc/includes/<id>.template` instead. Templates can be used to reduce the amount of code in the example, visible to the reader, e.g. by including common imports or setting up the testing skeleton that will made the generated file part of your CI process. 

```template ydoc.example=.ydoc/includes/scala.template
package nyub.assert.examples
import nyub.assert.AssertExtensions
import java.nio.file.Files

class ${{ydoc.exampleName}} extends munit.FunSuite with AssertExtensions:
${{ydoc.snippet}}

end ${{ydoc.exampleName}}

```

Once applied to a code snippet
````markdown
```scala ydoc.example=test.scala
    test("Documented test"):
        42 isEqualTo 42
```
````

would generate
```scala
package nyub.yadladoc.example
import nyub.assert.AssertExtensions
import java.nio.file.Files

class test_scala extends munit.FunSuite with AssertExtensions:
    test("Documented test"):
        42 isEqualTo 42
end test_scala
```

#### Prefix and suffix templates

In addition to the main template file that will be injected with the concatenation of all snippets related to a file, each of these snippets can be prefixed/suffixed by a custom template designated with the properties ``ydoc.example.prefix`` and `ydoc.example.suffix`. For example, adding `ydoc.example.prefix=templateId` to a snippet header would cause it's line to be prefixed by the expansion of `.ydoc/includes/templateId.template`. A property `ydoc.subExampleName` is injected in these templates with the identifier for the current snippet.

### In-place snippet decoration
In addition to generating new files, Yadladoc can alter code snippets in-place, annotating them with execution results. It currently supports the jshell interpreter to annotate java snippets, annotating them with toString() representations of each line.

```java
var list = java.util.List.of("A", "B", "C");
list.get(0)
list.contains("B")
list.get(-1)
```

```java ydoc.interpreter=jshell
var list = java.util.List.of("A", "B", "C");
//> [A, B, C]
list.get(0)
//> "A"
list.contains("B")
//> true
list.get(-1)
//> java.lang.ArrayIndexOutOfBoundsException
```
*Note: the above snippet is itself decorated via yadladoc ;)*

To trigger the in-place decoration of a snippet, add `ydoc.interpreter` property to the snippet header:
````markdown
```java ydoc.interpreter=jshell
var list = java.util.List.of("A", "B", "C");
list.get(0)
list.contains("B")
list.get(-1)
```
````

In check mode, decorated markdown files are checked in the same way as other generated files and compared against the actual markdown file.


## Install
### From source
Run `make usage/ydoc.jar` to produce the executable jar `ydoc.jar` in `usage/`

### From the release jar

Each [github release](https://github.com/NyuB/yadladoc/releases/) contains an executable jar, which is enough to run all of the examples listed in this document. 

### From the release binary
**COMING SOON** expose a native binary with graalVM

## Contribute

Pull requests and issues welcome !

### Entry points
[YadladocSuite](yadladoc/test/src/nyub/yadladoc/YadladocSuite.scala) groups the high level 'end to end' tests an should be a good entrypoint to have an overview of the current features.
