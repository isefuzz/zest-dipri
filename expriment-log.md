# Experiment Log
## DiPri 测试脚本

#### bcel

```
./dipri.sh bcel edu.berkeley.cs.jqf.examples.bcel.ParserTest testWithGenerator /hxd/iseFuzz/yd/sp-result AE 0 9
./dipri.sh bcel edu.berkeley.cs.jqf.examples.bcel.ParserTest testWithGenerator /hxd/iseFuzz/yd/sp-result AH 0 9
./dipri.sh bcel edu.berkeley.cs.jqf.examples.bcel.ParserTest testWithGenerator /hxd/iseFuzz/yd/sp-result PE 0 9
./dipri.sh bcel edu.berkeley.cs.jqf.examples.bcel.ParserTest testWithGenerator /hxd/iseFuzz/yd/sp-result PH 0 9
./dipri.sh bcel edu.berkeley.cs.jqf.examples.bcel.ParserTest testWithGenerator /hxd/iseFuzz/yd/sp-result VE 0 9
./dipri.sh bcel edu.berkeley.cs.jqf.examples.bcel.ParserTest testWithGenerator /hxd/iseFuzz/yd/sp-result VH 0 9
```

#### ant

```


./dipri.sh ant edu.berkeley.cs.jqf.examples.ant.ProjectBuilderTest testWithGenerator /hxd/iseFuzz/yd/sp-result AE 0 9
./dipri.sh ant edu.berkeley.cs.jqf.examples.ant.ProjectBuilderTest testWithGenerator /hxd/iseFuzz/yd/sp-result AH 0 9
./dipri.sh ant edu.berkeley.cs.jqf.examples.ant.ProjectBuilderTest testWithGenerator /hxd/iseFuzz/yd/sp-result PE 0 9
./dipri.sh ant edu.berkeley.cs.jqf.examples.ant.ProjectBuilderTest testWithGenerator /hxd/iseFuzz/yd/sp-result PH 0 9
./dipri.sh ant edu.berkeley.cs.jqf.examples.ant.ProjectBuilderTest testWithGenerator /hxd/iseFuzz/yd/sp-result VE 0 9
./dipri.sh ant edu.berkeley.cs.jqf.examples.ant.ProjectBuilderTest testWithGenerator /hxd/iseFuzz/yd/sp-result VH 0 9
```

#### rhino

```
./dipri.sh rhino edu.berkeley.cs.jqf.examples.rhino.CompilerTest testWithGenerator /hxd/iseFuzz/yd/sp-result AE 0 9
./dipri.sh rhino edu.berkeley.cs.jqf.examples.rhino.CompilerTest testWithGenerator /hxd/iseFuzz/yd/sp-result AH 0 9
./dipri.sh rhino edu.berkeley.cs.jqf.examples.rhino.CompilerTest testWithGenerator /hxd/iseFuzz/yd/sp-result PE 0 9
./dipri.sh rhino edu.berkeley.cs.jqf.examples.rhino.CompilerTest testWithGenerator /hxd/iseFuzz/yd/sp-result PH 0 9
./dipri.sh rhino edu.berkeley.cs.jqf.examples.rhino.CompilerTest testWithGenerator /hxd/iseFuzz/yd/sp-result VE 0 9
./dipri.sh rhino edu.berkeley.cs.jqf.examples.rhino.CompilerTest testWithGenerator /hxd/iseFuzz/yd/sp-result VH 0 9
```

#### Closure

```
./dipri.sh closure edu.berkeley.cs.jqf.examples.closure.CompilerTest testWithGenerator /hxd/iseFuzz/yd/sp-result AE 0 9
./dipri.sh closure edu.berkeley.cs.jqf.examples.closure.CompilerTest testWithGenerator /hxd/iseFuzz/yd/sp-result AH 0 9
./dipri.sh closure edu.berkeley.cs.jqf.examples.closure.CompilerTest testWithGenerator /hxd/iseFuzz/yd/sp-result PE 0 9
./dipri.sh closure edu.berkeley.cs.jqf.examples.closure.CompilerTest testWithGenerator /hxd/iseFuzz/yd/sp-result PH 0 9
./dipri.sh closure edu.berkeley.cs.jqf.examples.closure.CompilerTest testWithGenerator /hxd/iseFuzz/yd/sp-result VE 0 9
./dipri.sh closure edu.berkeley.cs.jqf.examples.closure.CompilerTest testWithGenerator /hxd/iseFuzz/yd/sp-result VH 0 9
```

#### chess

```
./dipri.sh chess edu.berkeley.cs.jqf.examples.chess.FENTest testWithGenerator /hxd/iseFuzz/yd/sp-result AE 0 9
./dipri.sh chess edu.berkeley.cs.jqf.examples.chess.FENTest testWithGenerator /hxd/iseFuzz/yd/sp-result AH 0 9
./dipri.sh chess edu.berkeley.cs.jqf.examples.chess.FENTest testWithGenerator /hxd/iseFuzz/yd/sp-result PE 0 9
./dipri.sh chess edu.berkeley.cs.jqf.examples.chess.FENTest testWithGenerator /hxd/iseFuzz/yd/sp-result PH 0 9
./dipri.sh chess edu.berkeley.cs.jqf.examples.chess.FENTest testWithGenerator /hxd/iseFuzz/yd/sp-result VE 0 9
./dipri.sh chess edu.berkeley.cs.jqf.examples.chess.FENTest testWithGenerator /hxd/iseFuzz/yd/sp-result VH 0 9
```

## Baseline 测试脚本

#### bcel

```
./zest.sh bcel edu.berkeley.cs.jqf.examples.bcel.ParserTest testWithGenerator /hxd/iseFuzz/yd/sp-result 0 9
```

#### ant

```
./zest.sh ant edu.berkeley.cs.jqf.examples.ant.ProjectBuilderTest testWithGenerator /hxd/iseFuzz/yd/sp-result 0 9
```

#### rhino

```
./zest.sh rhino edu.berkeley.cs.jqf.examples.rhino.CompilerTest testWithGenerator /hxd/iseFuzz/yd/sp-result 0 9
```

#### closure

```
./zest.sh closure edu.berkeley.cs.jqf.examples.closure.CompilerTest testWithGenerator /hxd/iseFuzz/yd/sp-result 0 9
```

#### chess

```
./zest.sh chess edu.berkeley.cs.jqf.examples.chess.FENTest testWithGenerator /hxd/iseFuzz/yd/sp-result 0 9
```
