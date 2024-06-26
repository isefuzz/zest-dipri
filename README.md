## Zest-DiPri: Zest with Distance-based Seed Prioritization.

This is the Java implementation of our paper: *DiPri: Distance-based Seed Prioritization for Greybox Fuzzing*. You can read our [FUZZING'23 paper](https://dl.acm.org/doi/10.1145/3605157.3605172) or TOSEM paper (under reviewed  at the time this document is updated) to learn more about the basic hypothesis and approach designs of DiPri. This prototype is essentially an extension of the famous Java fuzz testing platform JQF with Zest as the fuzzing algorithm. To learn more about JQF+Zest, please refer to their [JQF GitHub repo](https://github.com/rohanpadhye/JQF).

### Approach and Configurations

![overview](./fig/dipri-overview.png)

The above picture shows the high-level work flow of DiPri. Specifically, DiPri splits seed prioritization into two stages, i.e., seed evaluation and seed reordering. Zest-DiPri adopts a distance-based strategy to evaluate and assign priority scores for seeds. The distance-based strategy could by controlled through different prioritization modes and distance measures. The following picture display the configurations supported by Zest-DiPri, which are essentially a group of maven plugin command parameters. Note that the default configurations is tagged with *.

![configs](./fig/configs.png)

**Prioritization modes.** DiPri provides three prioritization modes, namely `VANILLA`, `PERIODICAL`, and `ADAPTIVE`. Zest-DiPri will conduct prioritization at different timings under different modes. The features of the modes are as follows:

- `VANILLA` mode: prioritizes every time the seed queue is updated.
- `PERIODICAL` mode: prioritizes periodically with a preset duration.
- `ADAPTIVE` mode: prioritizes when all last prioritized seeds are picked for input generation.

### Fuzz Targets

The following table shows the selected fuzz targets and their version information in our paper's Java implementation:

| TID  | Target  | Artifact ID                            | Version   | Input Type    |
| ---- | ------- | -------------------------------------- | --------- | ------------- |
| T25  | ant     | org.apache.ant.ant                     | 1.10.2    | XML           |
| T26  | bcel    | org.apache.bcel.bcel                   | 6.2       | Java Bytecode |
| T27  | chess   | org.lichess.scalachess_2.12            | 8.6.8     | String        |
| T28  | closure | com.google.javascript.closure-compiler | v20180204 | JavaScript    |
| T29  | rhino   | org.mozilla.rhino                      | 1.7.8     | JavaScript    |

### Install and Run Zest-DiPri

you can use the following instructions to install and run Zest-DiPri:

```shell
git clone https://github.com/isefuzz/zest-dipri.git
mvn clean install
cd ./examples
mvn jqf:fuzz -Dclass=<fully-qualified-class-name> -Dmethod=<method-name> [dipri-configuration...]
```

for example:

```shell
mvn jqf:fuzz -Dclass=edu.berkeley.cs.jqf.examples.bcel.ParserTest -Dmethod=testWithGenerator
```

### Run with Docker

**prerequisites**

```
mvn clean install
chmod +x ./dipri.sh
chmod +x ./zest.sh
```

**Run a set of experiments: 10 rounds, 24 hours per round**

```shell
./dipri.sh <name> <fully-qualified-class-name> <method-name> <path-to-your-result> <mode> 0 9
```

for example:

```shell
./dipri.sh bcel edu.berkeley.cs.jqf.examples.bcel.ParserTest testWithGenerator path/to/your/result AE 0 9
```

**Run an experiment**

```shell
cd ./examples
mvn jqf:fuzz -Dclass=<fully-qualified-class-name> -Dmethod=<method-name> [dipri-configuration...]
```

for example:

```shell
cd ./examples
mvn jqf:fuzz -Dclass=edu.berkeley.cs.jqf.examples.bcel.ParserTest -Dmethod=testWithGenerator -Dtime=5m
```

