TEST_RUNNER_IMAGE=ydoc:test-runner
TEST_RUNNER_WORKDIR=/home/usage
WITH_TEST_RUNNER=docker run --rm -v $(CURDIR):$(TEST_RUNNER_WORKDIR) -w $(TEST_RUNNER_WORKDIR) $(TEST_RUNNER_IMAGE) 
CRAM=cram --shell /bin/bash
TESTS=basics decoration
test: build-runner
	$(WITH_TEST_RUNNER) $(CRAM) $(TESTS)
update: build-runner
	$(WITH_TEST_RUNNER) $(CRAM) --interactive --yes $(TESTS) 

build-runner: Dockerfile
	docker build -t $(TEST_RUNNER_IMAGE) .