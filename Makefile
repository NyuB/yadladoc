dev: fmt test

test:
	millw yadladoc.test

ydoc.jar: out/yadladoc_app/assembly.dest/out.jar
ifeq ($(OS),Windows_NT)
	copy out\yadladoc_app\assembly.dest\out.jar ydoc.jar
else
	cp out/yadladoc_app/assembly.dest/out.jar ydoc.jar
endif

doc-check: ydoc.jar
	java -jar ydoc.jar check README.md 
doc-gen: ydoc.jar
	java -jar ydoc.jar run README.md

out/yadladoc_app/assembly.dest/out.jar:
	millw yadladoc_app.assembly

fmt:
	scalafmt .
fmt-check:
	scalafmt --check .

clean:
	millw clean
ifeq (($OS), Windows_NT)
	del ydoc.jar
else
	rm ydoc.jar
endif
