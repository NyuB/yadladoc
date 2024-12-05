Link resources to test execution directory, TESTDIR is provided by cram and points to the directory containing the run.t file
  $ shopt -s dotglob
  $ ln -s ${TESTDIR}/../ydoc.jar ydoc.jar
  $ ln -s ${TESTDIR}/* .

Run/check on multiple markdown files at once
  $ java -jar ydoc.jar check One.md Two.md
  Checked one.py generated from One.md
  Checked two.py generated from Two.md
  Error [MissingFile]: File 'one.py' is missing
  Error [MissingFile]: File 'two.py' is missing
  [2]
  $ java -jar ydoc.jar run One.md Two.md
  Generated one.py from One.md
  Generated two.py from Two.md
  $ cat one.py
  def test_one_py:
      print("One")
  $ cat two.py
  def test_two_py:
      print("Two")
  $ java -jar ydoc.jar check One.md Two.md
  Checked one.py generated from One.md
  Checked two.py generated from Two.md
Cleanup
  $ rm one.py two.py

Fail and print help message if no files are passed to run/check
  $ java -jar ydoc.jar run
  Usage: yadladoc {run|check|help} [markdown_files, ...]
      run  : generate documentation code from snippets in [markdown_files, ...]
      check: validate that the currrent documentation code is identical to what would be generated by 'run'
      help : print this message
          help properties: print yadladoc properties gathered from env and command line
  Common options:
      --color: use color when printing errors or diffs to the console
  [1]
  $ java -jar ydoc.jar check
  Usage: yadladoc {run|check|help} [markdown_files, ...]
      run  : generate documentation code from snippets in [markdown_files, ...]
      check: validate that the currrent documentation code is identical to what would be generated by 'run'
      help : print this message
          help properties: print yadladoc properties gathered from env and command line
  Common options:
      --color: use color when printing errors or diffs to the console
  [1]


Color option
  $ echo "print('Oops')" > one.py
  $ java -jar ydoc.jar --color check One.md
  Checked one.py generated from One.md
  \x1b[31mError\x1b[0m [MismatchingContent]: File 'one.py' has mismatching content with what would have been generated (esc)
  \x1b[91m-print('Oops')\x1b[0m (esc)
  \x1b[92m+def test_one_py:\x1b[0m (esc)
  \x1b[92m+    print("One")\x1b[0m (esc)
  [2]
Cleanup
  $ rm one.py

Override properties
  $ java -jar ydoc.jar help properties
  'overridable' -> 'original'
  'some' -> 'property'
  $ java -jar ydoc.jar -Doverridable=overriden help properties
  'overridable' -> 'overriden'
  'some' -> 'property'
