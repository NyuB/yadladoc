TEST_RUNNER_IMAGE=ydoc:test-runner
TEST_RUNNER_WORKDIR=/home/usage
WITH_TEST_RUNNER=docker run --rm -v $(CURDIR):$(TEST_RUNNER_WORKDIR) -w $(TEST_RUNNER_WORKDIR) $(TEST_RUNNER_IMAGE) 

test: build-runner
	$(WITH_TEST_RUNNER) cram basics
update: build-runner
	$(WITH_TEST_RUNNER) cram basics --interactive --yes

build-runner: Dockerfile
	docker build -t $(TEST_RUNNER_IMAGE) .