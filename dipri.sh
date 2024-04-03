#!/bin/bash

# Parameter checking
# testclass testmethod outputDir type [engine] 
if [ $# -lt 6 ]; then
    echo "Usage: <BENCHMARK> <TEST_CLASS> <TEST_METHOD> <OUT> <TYPE> <START_IDX> <END_IDX>"
    exit 1
fi

BENCHMARK="$1"
TESTCLASS="$2"
TESTMETHOD="$3"
OUT="$4"
TYPE="$5"
START_IDX="$6"
END_IDX="$7"


if [ "$TYPE" = "VH" ];then
    CONDITION=""
elif [ "$TYPE" = "VE" ];then
    CONDITION="-Dmetric=Euclid"
elif [ "$TYPE" = "AH" ];then
    CONDITION="-Dadaptive=true"
elif [ "$TYPE" = "AE" ];then
    CONDITION="-Dadaptive=true -Dmetric=Euclid"
elif [ "$TYPE" = "PH" ];then
    CONDITION="-Dperiod=1m"
elif [ "$TYPE" = "PE" ];then
    CONDITION="-Dperiod=1m -Dmetric=Euclid"
else
    echo "Unknown running config."
    exit 1
fi

OUT_PRE="$OUT/outs/dipri-$TYPE/$BENCHMARK"
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

    mvn jqf:fuzz -Dclass=$TESTCLASS -Dmethod=$TESTMETHOD -Dtime=24h $CONDITION -Dout=$OUT_DIR -Dtarget=/

done
