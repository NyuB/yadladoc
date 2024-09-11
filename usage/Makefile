TEST_RUNNER_IMAGE=ydoc:test-runner
TEST_RUNNER_WORKDIR=/home/usage
ifeq ($(IN_DOCKER), NO)
	WITH_TEST_RUNNER=
	TEST_DEPS=
else
	WITH_TEST_RUNNER=docker run --rm -v $(CURDIR):$(TEST_RUNNER_WORKDIR) -w $(TEST_RUNNER_WORKDIR) $(TEST_RUNNER_IMAGE)
	TEST_DEPS=build-runner
endif
CRAM=cram --shell /bin/bash
TESTS=basics decoration misc
test: $(TEST_DEPS)
	$(WITH_TEST_RUNNER) $(CRAM) $(TESTS)
update: $(TEST_DEPS)
	$(WITH_TEST_RUNNER) $(CRAM) --interactive --yes $(TESTS) 

build-runner: Dockerfile
	docker build -t $(TEST_RUNNER_IMAGE) .