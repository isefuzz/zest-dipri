package edu.nju.fuzz.guidance;

import edu.berkeley.cs.jqf.fuzz.ei.ZestGuidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.fuzz.util.Coverage;
import edu.berkeley.cs.jqf.fuzz.util.FastNonCollidingCoverage;
import edu.berkeley.cs.jqf.fuzz.util.ICoverage;
import edu.berkeley.cs.jqf.fuzz.util.IOUtils;
import org.eclipse.collections.api.iterator.IntIterator;
import org.eclipse.collections.api.list.primitive.IntList;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.ceil;
import static java.lang.Math.log;

/**
 * @projectName: jqf
 * @package: edu.nju.fuzz.guidance
 * @className: SeedPriGuidance
 * @author: Yang Ding
 * @description: 种子调度Guidance，继承自ZestGuidance
 * @date: 2023/10/26 14:35
 * @version: 1.0
 */
public abstract class SeedPriGuidance extends ZestGuidance {

    protected String name;

    protected final RefreshMode refreshMode;

    /**
     * 无周期情况下为将临时队列合并到种子队列损耗的时间，有周期情况下为合并时间加上队列排序时间
     */
    protected long wasteTime = 0L;

    /**
     * 有周期时的刷新次数
     */
    protected int refreshNums = 0;

    /**
     * 是否为自适应模式
     */
    protected final boolean isAdaptive;

    /**
     * 自适应模式下的百分比
     */
    protected final double percentage;

    /**
     * 种子排序周期，未设置周期情况下的值为-1
     */
    protected final long prioritizingPeriod;

    /**
     * 种子日志文件
     */
    protected File seedFile;

    /**
     * 临时种子队列，用于保存即将计算距离的种子
     */
    protected ArrayList<DistInput> tempInputs = new ArrayList<>();

    /**
     * 将Zest中Input类型成员savedInputs修改为DistInput类型
     */
    protected ArrayList<DistInput> savedInputs = new ArrayList<>();

    /**
     * 将Zest中Input类型成员seedInputs修改为DistInput类型
     */
    protected Deque<DistInput> seedInputs = new ArrayDeque<>();

    /**
     * 将Zest中Input类型成员currentInput修改为DistInput类型
     */
    protected DistInput<?> currentInput;

    /**
     * 将Zest中Input类型成员responsibleInputs修改为DistInput类型
     */
    protected Map<Object, DistInput> responsibleInputs = new HashMap<>(totalCoverage.size());

    /**
     *  运行过的不同种子个数
     */
    protected Set<Integer> executedSeedSet = new HashSet<>();

    /**
     * parent seed在一轮fuzz中新生成的可以加入到种子序列中的seed
     */
    protected int mutatedSeedsThisRound;

    /**
     *
     * @param testName 测试类+测试方法
     * @param duration 限制时间
     * @param trials   限制次数
     * @param outputDirectory fuzzing results目录
     * @param seedInputDir
     * @param sourceOfRandomness
     * @param prioritizingPeriod
     * @param isAdaptive
     * @param percentage
     * @throws IOException
     */
    public SeedPriGuidance(String testName, Duration duration, Long trials, File outputDirectory, File seedInputDir, Random sourceOfRandomness,
                           Duration prioritizingPeriod, boolean isAdaptive, double percentage) throws IOException {
        super(testName, duration, trials, outputDirectory, IOUtils.resolveInputFileOrDirectory(seedInputDir),
                sourceOfRandomness);
        this.isAdaptive = isAdaptive;
        this.percentage = 1 - percentage / 100;
        this.prioritizingPeriod = prioritizingPeriod != null ? prioritizingPeriod.toMillis() : -1;
        // 删除并重新创建先前创建好的plot_data文件
        statsFile.delete();
        this.statsFile = new File(outputDirectory, "plot_data");
        appendLineToFile(statsFile, getStatNames());
        // 删除之前的日志并重新创建种子日志
        this.seedFile = new File(outputDirectory, "seed.log");
        seedFile.delete();
        this.seedFile = new File(outputDirectory, "seed.log");

        if(this.isAdaptive){
            this.refreshMode = RefreshMode.Adaptive;
            appendLineToFile(seedFile,"--------Adaptive--------"+percentage+"-----------------------------------------");
        }
        else if(this.prioritizingPeriod == -1){
            this.refreshMode = RefreshMode.Baseline;
            appendLineToFile(seedFile,"--------BaseLine------------------------------------------------------");
        }
        else{
            this.refreshMode = RefreshMode.Periodic;
            appendLineToFile(seedFile,"--------Periodic--------"+this.prioritizingPeriod+"--------------------------------------");
        }
    }

    @Override
    protected String getStatNames() {
        return "time, cycles_done, cur_path, paths_total, pending_total, " +
                "pending_favs, map_size, unique_crashes, unique_hangs, max_depth, execs_per_sec, valid_inputs, invalid_inputs, valid_cov, all_covered_probes, valid_covered_probes, " +
                "dipri_time, non_dipri_time, total_execs";
    }

    @Override
    protected void displayStats(boolean force) {
        Date now = new Date();
        long intervalMilliseconds = now.getTime() - lastRefreshTime.getTime();
        if (intervalMilliseconds < STATS_REFRESH_TIME_PERIOD && !force) {
            return;
        }
        long interlvalTrials = numTrials - lastNumTrials;
        long intervalExecsPerSec = interlvalTrials * 1000L / intervalMilliseconds;
        double intervalExecsPerSecDouble = interlvalTrials * 1000.0 / intervalMilliseconds;
        lastRefreshTime = now;
        lastNumTrials = numTrials;
        long elapsedMilliseconds = now.getTime() - startTime.getTime();
        long execsPerSec = numTrials * 1000L / elapsedMilliseconds;

        String currentParentInputDesc;
        if (seedInputs.size() > 0 || savedInputs.isEmpty()) {
            currentParentInputDesc = "<seed>";
        } else {
            DistInput currentParentInput = savedInputs.get(currentParentInputIdx);
            currentParentInputDesc = currentParentInputIdx + " ";
            currentParentInputDesc += currentParentInput.isFavored() ? "(favored)" : "(not favored)";
            currentParentInputDesc += " {" + numChildrenGeneratedForCurrentParentInput +
                    "/" + getTargetChildrenForParent(currentParentInput) + " mutations}";
        }

        int nonZeroCount = totalCoverage.getNonZeroCount();
        double nonZeroFraction = nonZeroCount * 100.0 / totalCoverage.size();
        int nonZeroValidCount = validCoverage.getNonZeroCount();
        double nonZeroValidFraction = nonZeroValidCount * 100.0 / validCoverage.size();

        long dipri_time = this.wasteTime/1000;
        long non_dipri_time = (elapsedMilliseconds-wasteTime)/1000;

        if (console != null) {
            if (LIBFUZZER_COMPAT_OUTPUT) {
                console.printf("#%,d\tNEW\tcov: %,d exec/s: %,d L: %,d\n", numTrials, nonZeroValidCount, intervalExecsPerSec, currentInput.size());
            } else if (!QUIET_MODE) {
                console.printf("\033[2J");
                console.printf("\033[H");
                console.printf(this.getTitle() + "\n");
                if (this.testName != null) {
                    console.printf("Test name:            %s\n", this.testName);
                }

                String instrumentationType = "Janala";
                if (this.runCoverage instanceof FastNonCollidingCoverage) {
                    instrumentationType = "Fast";
                }
                console.printf("Instrumentation:      %s\n", instrumentationType);
                console.printf("Results directory:    %s\n", this.outputDirectory.getAbsolutePath());
                console.printf("Elapsed time:         %s (%s)\n", millisToDuration(elapsedMilliseconds),
                        maxDurationMillis == Long.MAX_VALUE ? "no time limit" : ("max " + millisToDuration(maxDurationMillis)));
                console.printf("Number of executions: %,d (%s)\n", numTrials,
                        maxTrials == Long.MAX_VALUE ? "no trial limit" : ("max " + maxTrials));
                console.printf("Valid inputs:         %,d (%.2f%%)\n", numValid, numValid * 100.0 / numTrials);
                console.printf("Unique failures:      %,d\n", uniqueFailures.size());
                console.printf("Queue size:           %,d\n", savedInputs.size());
                console.printf("Current parent input: %s\n", currentParentInputDesc);
                console.printf("Execution speed:      %,d/sec now | %,d/sec overall\n", intervalExecsPerSec, execsPerSec);
                console.printf("Total coverage:       %,d branches (%.2f%% of map)\n", nonZeroCount, nonZeroFraction);
                console.printf("Valid coverage:       %,d branches (%.2f%% of map)\n", nonZeroValidCount, nonZeroValidFraction);
            }
        }

        String plotData = String.format("%d, %d, %d, %d, %d, %d, %.2f%%, %d, %d, %d, %.2f, %d, %d, %.2f%%, %d, %d, %d, %d, %d",
                elapsedMilliseconds/1000, cyclesCompleted, currentParentInputIdx,
                numSavedInputs, 0, 0, nonZeroFraction, uniqueFailures.size(), 0, 0, intervalExecsPerSecDouble,
                numValid, numTrials-numValid, nonZeroValidFraction, nonZeroCount, nonZeroValidCount,
                dipri_time, non_dipri_time, numTrials);
        appendLineToFile(statsFile, plotData);
    }

    /**
     * 仅将原本Input类型变量替换成DistInput，无其他功能上的变化
     */
    protected int getTargetChildrenForParent(DistInput parentInput) {
        // Baseline is a constant
        int target = NUM_CHILDREN_BASELINE;

        // We like inputs that cover many things, so scale with fraction of max
        if (maxCoverage > 0) {
            target = (NUM_CHILDREN_BASELINE * parentInput.nonZeroCoverage) / maxCoverage;
        }

        // We absolutely love favored inputs, so fuzz them more
        if (parentInput.isFavored()) {
            target = target * NUM_CHILDREN_MULTIPLIER_FAVORED;
        }

        return target;
    }

    /**
     * 仅将原本Input类型变量替换成DistInput，无其他功能上的变化
     */
    protected DistInput<?> createFreshDistInput() {
        return new LinearInput();
    }

    /**
     * 仅将原本Input类型变量替换成DistInput，无其他功能上的变化
     */
    @Override
    protected InputStream createParameterStream() {
        // Return an input stream that reads bytes from a linear array
        return new InputStream() {
            int bytesRead = 0;

            @Override
            public int read() throws IOException {
                assert currentInput instanceof LinearInput : "SeedPriGuidance should only mutate LinearInput(s)";

                // For linear inputs, get with key = bytesRead (which is then incremented)
                LinearInput linearInput = (LinearInput) currentInput;
                // Attempt to get a value from the list, or else generate a random value
                int ret = linearInput.getOrGenerateFresh(bytesRead++, random);
                // infoLog("read(%d) = %d", bytesRead, ret);
                return ret;
            }
        };
    }

    /**
     * 引入种子筛选策略，当saveInputs非空且需要选择种子进行变异时，优先选择距离最大的种子
     */
    @Override
    public InputStream getInput() throws GuidanceException {
        conditionallySynchronize(multiThreaded, () -> {
            // Clear coverage stats for this run
            runCoverage.clear();

            // Choose an input to execute based on state of queues
            if (!seedInputs.isEmpty()) {
                // First, if we have some specific seeds, use those
                currentInput = seedInputs.removeFirst();

                // Hopefully, the seeds will lead to new coverage and be added to saved inputs

            } else if (savedInputs.isEmpty()) {
                // If no seeds given try to start with something random
                if (!blind && numTrials > 100_000) {
                    throw new GuidanceException("Too many trials without coverage; " +
                            "likely all assumption violations");
                }

                // Make fresh input using either list or maps
                // infoLog("Spawning new input from thin air");
                currentInput = createFreshDistInput();
            } else {
                int targetNumChildren = getTargetChildrenForParent(savedInputs.get(currentParentInputIdx));
                if (numChildrenGeneratedForCurrentParentInput >= targetNumChildren) {
                    infoOverSeedLog();
                    // 当前seed已经fuzz足够多次，需要从 savedInputs 中选择另一个 seed
                    // 目的：修改currentParentInputIdx，修改numChildrenGeneratedForCurrentParentInput = 0
                    if (this.refreshMode==RefreshMode.Baseline) {
                        // 基线模式：非周期 非自适应
                        // 每次选择新的seed时，将临时队列中种子保存到 savedInputs 中，从中按照“某种原则”选择一个种子
                        saveTempInputs();
                        // 挑选一个种子作为下一个fuzz种子
                        currentParentInputIdx = pickSeed();

                        // 记录信息
                        infoNewSeedLog();
                    } else if (this.refreshMode==RefreshMode.Adaptive) {
                        // 自适应模式逻辑代码：遍历种子队列到达一定比例即可将临时队列中的种子保存
                        if (currentParentInputIdx <= (int) percentage * savedInputs.size()) {
                            // 适应性模式下当种子队列指定百分比的种子被选择过一遍时，清空临时队列并重新排序
                            saveTempInputs();
                            currentParentInputIdx = pickSeed();
                        } else {
                            currentParentInputIdx = currentParentInputIdx==0?(savedInputs.size() - 1):(currentParentInputIdx - 1);
                        }
                        infoNewSeedLog();
                    } else {
                        // 周期模式逻辑代码
                        currentParentInputIdx = currentParentInputIdx==0?(savedInputs.size() - 1):(currentParentInputIdx - 1);
                        infoNewSeedLog();
                    }
                    numChildrenGeneratedForCurrentParentInput = 0;
                    mutatedSeedsThisRound = 0;
                    savedInputs.get(currentParentInputIdx).numOfSelected++;
                }
                // 获取当前需要变异的种子
                DistInput parent = savedInputs.get(currentParentInputIdx);
                if(!executedSeedSet.contains(parent.id)){executedSeedSet.add(parent.id);}
                // 变异
                currentInput = parent.fuzz(random);
                //当前种子被选择次数加1

                numChildrenGeneratedForCurrentParentInput++;
                // Write it to disk for debugging
                try {
                    writeCurrentInputToFile(currentInputFile);
                } catch (IOException ignore) {
                }

                // Start time-counting for timeout handling
                this.runStart = new Date();
                this.branchCount = 0;
            }
        });

        return createParameterStream();
    }

    /**
     * 将临时队列种子保存到种子队列中
     */
    public void saveTempInputs() {
        Date beforeDate = new Date();
        // 当能量耗尽时，将临时队列中的种子逐一添加到种子队列并计算距离
        for (int i = 0; i < tempInputs.size(); i++) {
            saveSeed(tempInputs.get(i));
        }
        // 清空临时队列
        tempInputs.clear();
        Date afterDate = new Date();
        wasteTime += afterDate.getTime() - beforeDate.getTime();
    }

    /**
     * 将临时队列种子逐一计算距离并保存到种子队列，根据是否有周期决定是否寻找最大距离种子
     *
     * @param newSeed   要加入种子队列的种子
     */
    public abstract void saveSeed(DistInput newSeed);

    /**
     * 从种子队列中按照不同原则，选择一个种子
     * @return 所选种子在种子队列中的索引
     */
    public abstract int pickSeed();

    /**
     * 向 seedFile 中记录信息
     */
    public abstract void infoNewSeedLog();

    /**
     * 向 seedFile 中记录信息
     */
    public abstract void infoOverSeedLog();

    /**
     * 增加了判断是否达到下一个周期的代码
     */
    @Override
    public void handleResult(Result result, Throwable error) throws GuidanceException {
        conditionallySynchronize(multiThreaded, () -> {
            // Stop timeout handling
            this.runStart = null;

            // Increment run count
            this.numTrials++;

            boolean valid = result == Result.SUCCESS;

            if (valid) {
                // Increment valid counter
                numValid++;
            }

            if (result == Result.SUCCESS || (result == Result.INVALID && !SAVE_ONLY_VALID)) {

                // Compute a list of keys for which this input can assume responsibility.
                // Newly covered branches are always included.
                // Existing branches *may* be included, depending on the heuristics used.
                // A valid input will steal responsibility from invalid inputs
                IntHashSet responsibilities = computeResponsibilities(valid);

                // Determine if this input should be saved
                List<String> savingCriteriaSatisfied = checkSavingCriteriaSatisfied(result);
                boolean toSave = savingCriteriaSatisfied.size() > 0;

                if (toSave) {
                    String why = String.join(" ", savingCriteriaSatisfied);

                    // Trim input (remove unused keys)
                    currentInput.gc();

                    // It must still be non-empty
                    assert (currentInput.size() > 0) : String.format("Empty input: %s", currentInput.desc);

                    // libFuzzerCompat stats are only displayed when they hit new coverage
                    if (LIBFUZZER_COMPAT_OUTPUT) {
                        displayStats(false);
                    }

                    infoLog("Saving new input (at run %d): " +
                                    "input #%d " +
                                    "of size %d; " +
                                    "reason = %s",
                            numTrials,
                            savedInputs.size(),
                            currentInput.size(),
                            why);

                    // Save input to queue and to disk
                    final String reason = why;
                    GuidanceException.wrap(() -> saveCurrentInput(responsibilities, reason));
                    // 检查是否到达新的周期
                    if (prioritizingPeriod != -1) {
                        // 到达指定周期时再重新计算距离
                        Date now = new Date();
                        if ((now.getTime() - startTime.getTime() - wasteTime) >= prioritizingPeriod * refreshNums) {
                            infoOverSeedLog();
                            refreshNums++;
                            // 将上一个周期新生成的种子全部加入到种子队列中
                            saveTempInputs();

                            currentParentInputIdx = pickSeed();
                            numChildrenGeneratedForCurrentParentInput = 0;
                            savedInputs.get(currentParentInputIdx).numOfSelected++;
                            infoNewSeedLog();
                        }
                    }
                    // Update coverage information
                    updateCoverageFile();
                }
            } else if (result == Result.FAILURE || result == Result.TIMEOUT) {
                String msg = error.getMessage();

                // Get the root cause of the failure
                Throwable rootCause = error;
                while (rootCause.getCause() != null) {
                    rootCause = rootCause.getCause();
                }

                // Attempt to add this to the set of unique failures
                if (uniqueFailures.add(failureDigest(rootCause.getStackTrace()))) {

                    // Trim input (remove unused keys)
                    currentInput.gc();

                    // It must still be non-empty
                    assert (currentInput.size() > 0) : String.format("Empty input: %s", currentInput.desc);

                    // Save crash to disk
                    int crashIdx = uniqueFailures.size() - 1;
                    String saveFileName = String.format("id_%06d", crashIdx);
                    File saveFile = new File(savedFailuresDirectory, saveFileName);
                    GuidanceException.wrap(() -> writeCurrentInputToFile(saveFile));
                    infoLog("%s", "Found crash: " + error.getClass() + " - " + (msg != null ? msg : ""));
                    String how = currentInput.desc;
                    String why = result == Result.FAILURE ? "+crash" : "+hang";
                    infoLog("Saved - %s %s %s", saveFile.getPath(), how, why);

                    if (EXACT_CRASH_PATH != null && !EXACT_CRASH_PATH.equals("")) {
                        File exactCrashFile = new File(EXACT_CRASH_PATH);
                        GuidanceException.wrap(() -> writeCurrentInputToFile(exactCrashFile));
                    }

                    // libFuzzerCompat stats are only displayed when they hit new coverage or crashes
                    if (LIBFUZZER_COMPAT_OUTPUT) {
                        displayStats(false);
                    }
                }
            }

            // displaying stats on every interval is only enabled for AFL-like stats screen
            if (!LIBFUZZER_COMPAT_OUTPUT) {
                displayStats(false);
            }

            // Save input unconditionally if such a setting is enabled
            if (LOG_ALL_INPUTS && (SAVE_ONLY_VALID ? valid : true)) {
                File logDirectory = new File(allInputsDirectory, result.toString().toLowerCase());
                String saveFileName = String.format("id_%09d", numTrials);
                File saveFile = new File(logDirectory, saveFileName);
                GuidanceException.wrap(() -> writeCurrentInputToFile(saveFile));
            }
        });
    }

    /**
     * 将原本的直接将种子加入到队列修改为将种子加入到临时队列
     */
    @Override
    protected void saveCurrentInput(IntHashSet responsibilities, String why) throws IOException {

        // First, save to disk (note: we issue IDs to everyone, but only write to disk  if valid)
        int newInputIdx = numSavedInputs++;
        String saveFileName = String.format("id_%06d", newInputIdx);
        String how = currentInput.desc;
        File saveFile = new File(savedCorpusDirectory, saveFileName);
        writeCurrentInputToFile(saveFile);
        infoLog("Saved - %s %s %s", saveFile.getPath(), how, why);

        // If not using guidance, do nothing else
        if (blind) {
            return;
        }

        // Second, save to queue
        // 先不保存种子，而是保存到临时队列中
        if (!savedInputs.isEmpty()) {
            tempInputs.add(currentInput);
        } else {
            savedInputs.add(currentInput);
        }
        // Third, store basic book-keeping data
        currentInput.id = newInputIdx;
        currentInput.saveFile = saveFile;
        currentInput.coverage = runCoverage.copy();
        currentInput.nonZeroCoverage = runCoverage.getNonZeroCount();
        currentInput.offspring = 0;
        savedInputs.get(currentParentInputIdx).offspring += 1;
        mutatedSeedsThisRound += 1;

        // Fourth, assume responsibility for branches
        currentInput.responsibilities = responsibilities;
        IntIterator iter = responsibilities.intIterator();
        while (iter.hasNext()) {
            int b = iter.next();
            // If there is an old input that is responsible,
            // subsume it
            DistInput oldResponsible = responsibleInputs.get(b);
            if (oldResponsible != null) {
                oldResponsible.responsibilities.remove(b);
                // infoLog("-- Stealing responsibility for %s from input %d", b, oldResponsible.id);
            } else {
                // infoLog("-- Assuming new responsibility for %s", b);
            }
            // We are now responsible
            responsibleInputs.put(b, currentInput);
        }

    }

    /**
     * 仅将原本Input类型变量替换成DistInput，无其他功能上的变化
     */
    @Override
    protected IntHashSet computeResponsibilities(boolean valid) {
        IntHashSet result = new IntHashSet();

        // This input is responsible for all new coverage
        IntList newCoverage = runCoverage.computeNewCoverage(totalCoverage);
        if (newCoverage.size() > 0) {
            result.addAll(newCoverage);
        }

        // If valid, this input is responsible for all new valid coverage
        if (valid) {
            IntList newValidCoverage = runCoverage.computeNewCoverage(validCoverage);
            if (newValidCoverage.size() > 0) {
                result.addAll(newValidCoverage);
            }
        }

        // Perhaps it can also steal responsibility from other inputs
        if (STEAL_RESPONSIBILITY) {
            int currentNonZeroCoverage = runCoverage.getNonZeroCount();
            int currentInputSize = currentInput.size();
            IntHashSet covered = new IntHashSet();
            covered.addAll(runCoverage.getCovered());

            // Search for a candidate to steal responsibility from
            candidate_search:
            for (DistInput candidate : savedInputs) {
                IntHashSet responsibilities = candidate.responsibilities;

                // Candidates with no responsibility are not interesting
                if (responsibilities.isEmpty()) {
                    continue candidate_search;
                }

                // To avoid thrashing, only consider candidates with either
                // (1) strictly smaller total coverage or
                // (2) same total coverage but strictly larger size
                if (candidate.nonZeroCoverage < currentNonZeroCoverage ||
                        (candidate.nonZeroCoverage == currentNonZeroCoverage &&
                                currentInputSize < candidate.size())) {

                    // Check if we can steal all responsibilities from candidate
                    IntIterator iter = responsibilities.intIterator();
                    while (iter.hasNext()) {
                        int b = iter.next();
                        if (covered.contains(b) == false) {
                            // Cannot steal if this input does not cover something
                            // that the candidate is responsible for
                            continue candidate_search;
                        }
                    }
                    // If all of candidate's responsibilities are covered by the
                    // current input, then it can completely subsume the candidate
                    result.addAll(responsibilities);
                }

            }
        }

        return result;
    }

    /**
     * 仅将原本Input类型变量替换成DistInput，无其他功能上的变化
     */
    @Override
    protected void writeCurrentInputToFile(File saveFile) throws IOException {
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(saveFile))) {
            for (Integer b : currentInput) {
                assert (b >= 0 && b < 256);
                out.write(b);
            }
        }

    }

    private static MessageDigest sha1;

    /**
     * 添加ZestGuidance中的私有方法
     */
    private static String failureDigest(StackTraceElement[] stackTrace) {
        if (sha1 == null) {
            try {
                sha1 = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                throw new GuidanceException(e);
            }
        }
        byte[] bytes = sha1.digest(Arrays.deepToString(stackTrace).getBytes());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16)
                    .substring(1));
        }
        return sb.toString();
    }

    /**
     * 在ZestGuidance中的Input类型基础上加入了表示种子之间距离的属性seedDistance
     */
    public static abstract class DistInput<K> implements Iterable<Integer> {

        /**
         * The file where this input is saved.
         *
         * <p>This field is null for inputs that are not saved.</p>
         */
        File saveFile = null;

        /**
         * An ID for a saved input.
         *
         * <p>This field is -1 for inputs that are not saved.</p>
         */
        int id;

        /**
         * The description for this input.
         *
         * <p>This field is modified by the construction and mutation
         * operations.</p>
         */
        String desc;

        /**
         * The run coverage for this input, if the input is saved.
         *
         * <p>This field is null for inputs that are not saved.</p>
         */
        ICoverage coverage = null;

        /**
         * The number of non-zero elements in `coverage`.
         *
         * <p>This field is -1 for inputs that are not saved.</p>
         *
         * <p></p>When this field is non-negative, the information is
         * redundant (can be computed using {@link Coverage#getNonZeroCount()}),
         * but we store it here for performance reasons.</p>
         */
        int nonZeroCoverage = -1;

        /**
         * The number of mutant children spawned from this input that
         * were saved.
         *
         * <p>This field is -1 for inputs that are not saved.</p>
         */
        int offspring = -1;

        /**
         * Distance of each seed
         */
        double seedDistance = 0.0;

        /**
         * counts of this seed is selected
         */
        int numOfSelected = 0;
        /**
         * The set of coverage keys for which this input is
         * responsible.
         *
         * <p>This field is null for inputs that are not saved.</p>
         *
         * <p>Each coverage key appears in the responsibility set
         * of exactly one saved input, and all covered keys appear
         * in at least some responsibility set. Hence, this list
         * needs to be kept in-sync with {@link #responsibleInputs}.</p>
         */
        IntHashSet responsibilities = null;

        /**
         * Create an empty input.
         */
        public DistInput() {
            desc = "random";
        }

        /**
         * Create a copy of an existing input.
         *
         * @param toClone the input map to clone
         */

        public DistInput(DistInput toClone) {
            desc = String.format("src:%06d", toClone.id);
        }

        public double getSeedDistance() {
            return seedDistance;
        }

        public int getNonZeroCoverage(){return nonZeroCoverage;}

        public ICoverage getCoverage() {
            return coverage;
        }

        public abstract int getOrGenerateFresh(K key, Random random);

        public abstract int size();

        public abstract DistInput fuzz(Random random);

        public abstract void gc();

        /**
         * Returns whether this input should be favored for fuzzing.
         *
         * <p>An input is favored if it is responsible for covering
         * at least one branch.</p>
         *
         * @return whether or not this input is favored
         */
        public boolean isFavored() {
            return responsibilities.size() > 0;
        }

        /**
         * Sample from a geometric distribution with given mean.
         * <p>
         * Utility method used in implementing mutation operations.
         *
         * @param random a pseudo-random number generator
         * @param mean   the mean of the distribution
         * @return a randomly sampled value
         */
        public static int sampleGeometric(Random random, double mean) {
            double p = 1 / mean;
            double uniform = random.nextDouble();
            return (int) ceil(log(1 - uniform) / log(1 - p));
        }

    }

    public class LinearInput extends DistInput<Integer> {

        /**
         * A list of byte values (0-255) ordered by their index.
         */
        protected ArrayList<Integer> values;

        /**
         * The number of bytes requested so far
         */
        protected int requested = 0;

        public LinearInput() {
            super();
            this.values = new ArrayList<>();
        }

        public LinearInput(LinearInput other) {
            super(other);
            this.values = new ArrayList<>(other.values);
        }


        @Override
        public int getOrGenerateFresh(Integer key, Random random) {
            // Otherwise, make sure we are requesting just beyond the end-of-list
            // assert (key == values.size());
            if (key != requested) {
                throw new IllegalStateException(String.format("Bytes from linear input out of order. " +
                        "Size = %d, Key = %d", values.size(), key));
            }

            // Don't generate over the limit
            if (requested >= MAX_INPUT_SIZE) {
                return -1;
            }

            // If it exists in the list, return it
            if (key < values.size()) {
                requested++;
                // infoLog("Returning old byte at key=%d, total requested=%d", key, requested);
                return values.get(key);
            }

            // Handle end of stream
            if (GENERATE_EOF_WHEN_OUT) {
                return -1;
            } else {
                // Just generate a random input
                int val = random.nextInt(256);
                values.add(val);
                requested++;
                // infoLog("Generating fresh byte at key=%d, total requested=%d", key, requested);
                return val;
            }
        }

        @Override
        public int size() {
            return values.size();
        }

        /**
         * Truncates the input list to remove values that were never actually requested.
         *
         * <p>Although this operation mutates the underlying object, the effect should
         * not be externally visible (at least as long as the test executions are
         * deterministic).</p>
         */
        @Override
        public void gc() {
            // Remove elements beyond "requested"
            values = new ArrayList<>(values.subList(0, requested));
            values.trimToSize();

            // Inputs should not be empty, otherwise mutations don't work
            if (values.isEmpty()) {
                throw new IllegalArgumentException("Input is either empty or nothing was requested from the input generator.");
            }
        }

        @Override
        public DistInput fuzz(Random random) {
            // Clone this input to create initial version of new child
            LinearInput newInput = new LinearInput(this);

            // Stack a bunch of mutations
            int numMutations = sampleGeometric(random, MEAN_MUTATION_COUNT);
            newInput.desc += ",havoc:" + numMutations;

            boolean setToZero = random.nextDouble() < 0.1; // one out of 10 times

            for (int mutation = 1; mutation <= numMutations; mutation++) {

                // Select a random offset and size
                int offset = random.nextInt(newInput.values.size());
                int mutationSize = sampleGeometric(random, MEAN_MUTATION_SIZE);

                // desc += String.format(":%d@%d", mutationSize, idx);

                // Mutate a contiguous set of bytes from offset
                for (int i = offset; i < offset + mutationSize; i++) {
                    // Don't go past end of list
                    if (i >= newInput.values.size()) {
                        break;
                    }

                    // Otherwise, apply a random mutation
                    int mutatedValue = setToZero ? 0 : random.nextInt(256);
                    newInput.values.set(i, mutatedValue);
                }
            }

            return newInput;
        }

        @Override
        public Iterator<Integer> iterator() {
            return values.iterator();
        }
    }

    public class SeedInput extends LinearInput {
        final File seedFile;
        final InputStream in;

        public SeedInput(File seedFile) throws IOException {
            super();
            this.seedFile = seedFile;
            this.in = new BufferedInputStream(new FileInputStream(seedFile));
            this.desc = "seed";
        }

        @Override
        public int getOrGenerateFresh(Integer key, Random random) {
            int value;
            try {
                value = in.read();
            } catch (IOException e) {
                throw new GuidanceException("Error reading from seed file: " + seedFile.getName(), e);

            }

            // assert (key == values.size())
            if (key != values.size() && value != -1) {
                throw new IllegalStateException(String.format("Bytes from seed out of order. " +
                        "Size = %d, Key = %d", values.size(), key));
            }

            if (value >= 0) {
                requested++;
                values.add(value);
            }

            // If value is -1, then it is returned (as EOF) but not added to the list
            return value;
        }

        @Override
        public void gc() {
            super.gc();
            try {
                in.close();
            } catch (IOException e) {
                throw new GuidanceException("Error closing seed file:" + seedFile.getName(), e);
            }
        }

    }
}
