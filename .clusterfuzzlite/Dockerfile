FROM gcr.io/oss-fuzz-base/base-builder-jvm
RUN apt-get update && apt-get install -y openjdk-21 && apt-get install -y maven
COPY . $SRC/rld-build-tools
WORKDIR $SRC/rld-build-tools
COPY ./.clusterfuzzlite/build.sh $SRC/
