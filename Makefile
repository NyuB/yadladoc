ifeq ($(OS), Windows_NT)
	MILLW=millw
else
	MILLW=./millw
endif
dev: fmt test

test:
	$(MILLW) yadladoc.test

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
	$(MILLW) yadladoc_app.assembly

fmt:
	scalafmt .
fmt-check:
	scalafmt --check .

clean:
	$(MILLW) clean
ifeq (($OS), Windows_NT)
	del ydoc.jar
else
	rm ydoc.jar
endif
