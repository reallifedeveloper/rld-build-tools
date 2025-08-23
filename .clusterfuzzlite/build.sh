PROJECT_NAME=rld-build-tools
JAVA_VERSION=17

varname=JAVA_${JAVA_VERSION}_HOME
export JAVA_HOME=${!varname}
export PATH=${JAVA_HOME}/bin:${PATH}

mvn clean package dependency:copy-dependencies

mkdir -p ${OUT}/lib
cp target/*.jar ${OUT}/lib
cp target/dependency/*.jar ${OUT}/lib

BUILD_CLASSPATH=$(printf %s: ${OUT}/lib/*.jar)
RUNTIME_CLASSPATH=${BUILD_CLASSPATH}

for fuzzer in $(find ${SRC} -name '*Fuzzer.java'); do
    fuzzer_basename=$(basename -s .java ${fuzzer})
    javac -cp "${BUILD_CLASSPATH}" ${fuzzer}
    find ${SRC} -name ${fuzzer_basename}.class -exec cp {} ${OUT}/ \;

    # Create an execution wrapper that executes Jazzer with the correct arguments.
    echo "#!/bin/sh
# LLVMFuzzerTestOneInput for fuzzer detection.
this_dir=\$(dirname \"\$0\")
LD_LIBRARY_PATH=\"$JVM_LD_LIBRARY_PATH\":\$this_dir \
\$this_dir/jazzer_driver --agent_path=\$this_dir/jazzer_agent_deploy.jar \
--cp=$RUNTIME_CLASSPATH \
--target_class=$fuzzer_basename \
--jvm_args=\"-Xmx2048m:-Djava.awt.headless=true\" \
\$@" > ${OUT}/$fuzzer_basename
    chmod +x ${OUT}/$fuzzer_basename
done
