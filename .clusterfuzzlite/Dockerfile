FROM gcr.io/oss-fuzz-base/base-builder-jvm@sha256:c0d8969074d3e54d689aec85ab4b2f43713e29ae88e4e51d60318ab36351dae5
RUN apt-get update && apt-get install -y openjdk-21-jdk && apt-get install -y maven
ENV JAVA_17_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
ENV JAVA_21_HOME="/usr/lib/jvm/java-21-openjdk-amd64"
RUN mkdir ~/.m2
RUN ln -s $SRC/project-to-test/.clusterfuzzlite/toolchains.xml ~/.m2

COPY . $SRC/project-to-test
WORKDIR $SRC/project-to-test
COPY ./.clusterfuzzlite/build.sh $SRC/
