package edu.nju.fuzz.guidance;

import com.google.common.collect.Lists;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Comparator;
import java.util.Date;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * @projectName: jqf
 * @package: edu.nju.fuzz.guidance
 * @className: CovPriGuidance
 * @author: Yang Ding
 * @description:
 * @date: 2023/10/27 16:43
 * @version: 1.0
 */
public class CovPriGuidance extends SeedPriGuidance {
    /**
     * @param testName           测试类+测试方法
     * @param duration           限制时间
     * @param trials             限制次数
     * @param outputDirectory    fuzzing results目录
     * @param seedInputDir
     * @param sourceOfRandomness
     * @param prioritizingPeriod
     * @param isAdaptive
     * @param percentage
     * @throws IOException
     */
    public CovPriGuidance(String testName, Duration duration, Long trials, File outputDirectory, File seedInputDir, Random sourceOfRandomness,
                          Duration prioritizingPeriod, boolean isAdaptive, double percentage) throws IOException {
        super(testName, duration, trials, outputDirectory, seedInputDir, sourceOfRandomness, prioritizingPeriod, isAdaptive, percentage);
        this.name = "CovPri";
        appendLineToFile(seedFile,name);
    }

    @Override
    public void saveSeed(DistInput newSeed) {
        // 将临时种子加入到种子队列
        savedInputs.add(newSeed);
    }

    @Override
    public int pickSeed() {
        Date beforeDate = new Date();
        savedInputs = Lists.newArrayList(savedInputs.stream().
                sorted(Comparator.comparing(DistInput::getNonZeroCoverage)).
                collect(Collectors.toList()));
        Date afterDate = new Date();
        wasteTime += afterDate.getTime() - beforeDate.getTime();
        return savedInputs.size()-1;
    }

    @Override
    protected String getTitle() {
        if (blind) {
            return  "Generator-based random fuzzing (no guidance)" + " ||| MODE: "+this.refreshMode.name()+
                    "\n--------------------------------------------\n";
        } else {
            return  "Semantic Fuzzing with CovPri\n" +
                    "--------------------------\n";
        }
    }

    @Override
    public void infoNewSeedLog() {
        DistInput seed = savedInputs.get(currentParentInputIdx);
        String seedLog = String.format("new : time %s id %d, numOfSelected %d, nonZero Cov. %d, index %d of size %d",
                new Date().getTime() - startTime.getTime(), seed.id, seed.numOfSelected, seed.getNonZeroCoverage(),
                currentParentInputIdx, savedInputs.size());
        appendLineToFile(seedFile, seedLog);
    }

    @Override
    public void infoOverSeedLog() {
        DistInput seed = savedInputs.get(currentParentInputIdx);
        String seedLog = String.format("over: seed %d over, at %d of %d, offspring: %d, %d new inputs generated this round(energy), %d new seeds generated, totally %d seeds has executed",
                seed.id, currentParentInputIdx, savedInputs.size(),seed.offspring,numChildrenGeneratedForCurrentParentInput, mutatedSeedsThisRound, executedSeedSet.size());
        appendLineToFile(seedFile, seedLog);
    }
}
