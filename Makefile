ifeq ($(OS), Windows_NT)
# Project paths
	MILLW=millw
# Shell commands
	CP=copy
	RM=del
else
# Project paths
	MILLW=./millw
# Shell commands
	CP=cp
	RM=rm
endif

.PHONY: dev test ydoc.jar doc-check doc-gen fmt fmt-check clean

dev: fmt test

test:
	$(MILLW) yadladoc.test + yadladoc_app.compile

ydoc.jar:
	$(MILLW) yadladoc_app.ydocJar

doc-check: ydoc.jar
	java -jar ydoc.jar check README.md 
doc-gen: ydoc.jar
	java -jar ydoc.jar run README.md

fmt:
	scalafmt .
fmt-check:
	scalafmt --check .

clean:
	$(MILLW) clean
	$(DEL) ydoc.jar
