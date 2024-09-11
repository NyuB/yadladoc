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
