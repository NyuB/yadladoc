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
YDOC_JAR=usage/ydoc.jar


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

DOCKER_WRAPPER=docker run -v $(CURDIR):/workspace -w /workspace eclipse-temurin:21
ifeq ($(IN_DOCKER), true)
	DOC_JAVA_JAR=$(DOCKER_WRAPPER) java -jar $(YDOC_JAR)
else
	DOC_JAVA_JAR=java -jar $(YDOC_JAR)
endif
doc-check: $(YDOC_JAR)
	$(DOC_JAVA_JAR) check README.md
doc-gen: $(YDOC_JAR)
	$(DOC_JAVA_JAR) run README.md

javadoc:
	millw assert_extensions.docJar + filesystem.docJar + interpreter.docJar + yadladoc.docJar + yadladoc_app.docJar

fmt:
	scalafmt .
fmt-check:
	scalafmt --check .

clean:
	$(MILLW) clean
	$(RM) $(YDOC_JAR)
