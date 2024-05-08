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

usage-test: usage/ydoc.jar
	$(MAKE) -C usage test
usage-update: usage/ydoc.jar
	$(MAKE) -C usage update

usage/ydoc.jar: ydoc.jar
	$(MILLW) yadladoc_app.ydocJar

doc-check: usage/ydoc.jar
	java -jar usage/ydoc.jar check README.md 
doc-gen: ydoc.jar
	java -jar usage/ydoc.jar run README.md

fmt:
	scalafmt .
fmt-check:
	scalafmt --check .

clean:
	$(MILLW) clean
	$(RM) ydoc.jar
