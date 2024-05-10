ifeq ($(OS), Windows_NT)
# Project paths
	MILLW=millw
	YDOC_JAR=usage\ydoc.jar
# Shell commands
	CP=copy
	RM=del
else
# Project paths
	MILLW=./millw
	YDOC_JAR=usage/ydoc.jar
# Shell commands
	CP=cp
	RM=rm
endif

.PHONY: dev test ydoc.jar doc-check doc-gen fmt fmt-check clean

dev: fmt test

test:
	$(MILLW) assert_extensions.test + filesystem.test + interpreter.test + yadladoc.test + yadladoc_app.compile

usage-test: $(YDOC_JAR)
	$(MAKE) -C usage test
usage-update: $(YDOC_JAR)
	$(MAKE) -C usage update

$(YDOC_JAR):
	$(MILLW) yadladoc_app.ydocJar

doc-check: $(YDOC_JAR)
	java -jar $(YDOC_JAR) check README.md 
doc-gen: $(YDOC_JAR)
	java -jar $(YDOC_JAR) run README.md

fmt:
	scalafmt .
fmt-check:
	scalafmt --check .

clean:
	$(MILLW) clean
	$(RM) $(YDOC_JAR)
