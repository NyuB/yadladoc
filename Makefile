ifeq ($(OS), Windows_NT)
# Project paths
	MILL=mill
	YDOC_JAR=usage\ydoc.jar
# Shell commands
	CP=copy
	RM=del
else
# Project paths
	MILL=./mill
# Shell commands
	CP=cp
	RM=rm
	YDOC_JAR=usage/ydoc.jar
endif

.PHONY: dev test ydoc.jar doc-check doc-gen fmt fmt-check clean

dev: fmt test

test:
	$(MILL) -j 4 _.test

usage-test: $(YDOC_JAR)
	$(MAKE) -C usage test
usage-update: $(YDOC_JAR)
	$(MAKE) -C usage update

$(YDOC_JAR):
	$(MILL) yadladoc_app.ydocJar

JAVA=java
DOC_JAVA_JAR=$(JAVA) -jar $(YDOC_JAR)
doc-check: $(YDOC_JAR)
	$(DOC_JAVA_JAR) --color check README.md
doc-gen: $(YDOC_JAR)
	$(DOC_JAVA_JAR) run README.md

javadoc:
	$(MILL) _.docJar

fmt:
	scalafmt .
fmt-check:
	scalafmt --check .

fix:
	$(MILL) _.fix
fix-check:
	$(MILL) _.fix --check

clean:
	$(MILL) clean
	$(RM) $(YDOC_JAR)
