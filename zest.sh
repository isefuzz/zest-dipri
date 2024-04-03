#!/bin/bash

# Parameter checking
# testclass testmethod outputDir type [engine] 
if [ $# -lt 5 ]; then
    echo "Usage: <BENCHMARK> <TEST_CLASS> <TEST_METHOD> <OUT> <START_IDX> <END_IDX>"
    exit 1
fi

BENCHMARK="$1"
TESTCLASS="$2"
TESTMETHOD="$3"
OUT="$4"
START_IDX="$5"
END_IDX="$6"



OUT_PRE="$OUT/outs/zest/$BENCHMARK"
if [ ! -d "$OUT_PRE" ]; then
  mkdir -p "$OUT_PRE"
fi

cd ./examples/

for idx in $(seq "$START_IDX" "$END_IDX"); do

    # Prepare out directories for fuzzing
    OUT_DIR="$OUT_PRE/out-$idx"
    if [ -d "$OUT_DIR" ]; then
        rm -rf "$OUT_DIR"
    fi
    mkdir -p "$OUT_DIR"

    mvn jqf:fuzz -Dclass=$TESTCLASS -Dmethod=$TESTMETHOD -Dengine=zest -Dtime=24h -Dout=$OUT_DIR -Dtarget=/

done
