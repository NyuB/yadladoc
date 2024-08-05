FROM ghcr.io/graalvm/native-image-community:21
WORKDIR /ydoc
ADD usage/ydoc.jar .
RUN native-image -jar ydoc.jar ydoc-linux
