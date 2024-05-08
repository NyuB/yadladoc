FROM debian:bullseye-slim

RUN apt update && apt install -y \
    make \ 
    python3 pip \
    wget &&\
    apt clean &&\
    pip install cram
RUN wget https://download.oracle.com/java/21/latest/jdk-21_linux-x64_bin.deb && dpkg -i jdk-21_linux-x64_bin.deb && rm jdk-21_linux-x64_bin.deb 

