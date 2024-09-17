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
# Shell commands
	CP=cp
	RM=rm
	YDOC_JAR=usage/ydoc.jar
endif

.PHONY: dev test ydoc.jar doc-check doc-gen fmt fmt-check clean

dev: fmt test

test:
	$(MILLW) -j 4 assert_extensions.test + filesystem.test + interpreter.test + yadladoc.test + yadladoc_app.test

usage-test: $(YDOC_JAR)
	$(MAKE) -C usage test
usage-update: $(YDOC_JAR)
	$(MAKE) -C usage update

$(YDOC_JAR):
	$(MILLW) yadladoc_app.ydocJar

JAVA=java
DOC_JAVA_JAR=$(JAVA) -jar $(YDOC_JAR)
doc-check: $(YDOC_JAR)
	$(DOC_JAVA_JAR) --color check README.md
doc-gen: $(YDOC_JAR)
	$(DOC_JAVA_JAR) run README.md

javadoc:
	$(MILLW) assert_extensions.docJar + filesystem.docJar + interpreter.docJar + yadladoc.docJar + yadladoc_app.docJar

fmt:
	scalafmt .
fmt-check:
	scalafmt --check .

fix:
	$(MILLW) assert_extensions.fix + filesystem.fix + interpreter.fix + yadladoc.fix + yadladoc_app.fix
fix-check:
	$(MILLW) assert_extensions.fix --check
	$(MILLW) filesystem.fix --check
	$(MILLW) interpreter.fix --check
	$(MILLW) yadladoc.fix --check
	$(MILLW) yadladoc_app.fix --check

clean:
	$(MILLW) clean
	$(RM) $(YDOC_JAR)
