ifeq ($(OS), Windows_NT)
# Project paths
	MILLW=millw
	MILL_APP_ASSEMBLY_JAR=out\yadladoc_app\assembly.dest\out.jar
# Shell commands
	CP=copy
	RM=del
else
# Project paths
	MILLW=./millw
	MILL_APP_ASSEMBLY_JAR=out/yadladoc_app/assembly.dest/out.jar

# Shell commands
	CP=cp
	RM=rm
endif

dev: fmt test

test:
	$(MILLW) yadladoc.test

ydoc.jar:
	$(MILLW) yadladoc_app.assembly
	$(CP) $(MILL_APP_ASSEMBLY_JAR) ydoc.jar

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
