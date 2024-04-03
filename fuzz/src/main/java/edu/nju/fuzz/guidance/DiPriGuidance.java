package edu.nju.fuzz.guidance;

import edu.berkeley.cs.jqf.fuzz.util.IOUtils;
import edu.nju.fuzz.strategy.DistStrategy;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Comparator;
import java.util.Date;
import java.util.Random;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

/**
 * @projectName: jqf
 * @package: edu.nju.fuzz.guidance
 * @className: DiPriGuidance
 * @author: Yang Ding
 * @description: 基于种子距离的调度方法
 * @date: 2023/10/26 18:40
 * @version: 1.0
 */
public class DiPriGuidance extends SeedPriGuidance{
    private final DistStrategy strategy;

    public DiPriGuidance(String testName, Duration duration, Long trials, File outputDirectory, File seedInputDir, Random sourceOfRandomness,
                         Duration prioritizingPeriod, boolean isAdaptive, double percentage, DistStrategy stategy) throws IOException {
        super(testName,duration,trials,outputDirectory,seedInputDir,sourceOfRandomness,prioritizingPeriod,isAdaptive,percentage);
        this.strategy = stategy;
        this.name = "DiPri";
        appendLineToFile(seedFile,name);
    }

    @Override
    public void saveSeed(DistInput newSeed) {
        double newSeedDist = 0.0;
        // 计算新种子的距离
        for (int i = 0; i < savedInputs.size(); i++) {
            DistInput curSeed = savedInputs.get(i);
            double curDist = strategy.compute(curSeed, newSeed);
            newSeedDist += curDist;
            curSeed.seedDistance += curDist;
        }
        newSeed.seedDistance = newSeedDist;
        // 将临时种子加入到种子队列
        savedInputs.add(newSeed);
    }

    @Override
    public int pickSeed() {
        Date beforeDate = new Date();
        savedInputs = Lists.newArrayList(savedInputs.stream().
                sorted(Comparator.comparing(DistInput::getSeedDistance)).
                collect(Collectors.toList()));
        Date afterDate = new Date();
        wasteTime += afterDate.getTime() - beforeDate.getTime();
        return savedInputs.size()-1;
    }

    @Override
    protected String getTitle() {
        if (blind) {
            return  "Generator-based random fuzzing (no guidance)\n" +
                    "--------------------------------------------\n";
        } else {
            return  "Semantic Fuzzing with DiPri" + " || MODE: "+this.refreshMode.name() + " || METRIC: "+this.strategy.getName()+
                    "\n--------------------------\n";
        }
    }

    @Override
    public void infoNewSeedLog() {
        DistInput seed = savedInputs.get(currentParentInputIdx);
        String seedLog = String.format("new : time %s id %d, numOfSelected %d, distance %.2f, index %d of size %d",
                new Date().getTime() - startTime.getTime(), seed.id, seed.numOfSelected, seed.seedDistance,
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
