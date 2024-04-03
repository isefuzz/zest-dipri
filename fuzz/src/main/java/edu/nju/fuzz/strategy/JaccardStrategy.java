package edu.nju.fuzz.strategy;

import edu.berkeley.cs.jqf.fuzz.util.Counter;
import edu.nju.fuzz.guidance.SeedPriGuidance;

/**
 * 杰卡德距离策略
 * 参考资料来自百度百科“杰卡德距离”词条：
 * 杰卡德距离(Jaccard Distance) 是用来衡量两个集合差异性的一种指标，它是杰卡德相似系数的补集，被定义为1减去Jaccard相似系数。
 * Jaccard相似指数用来度量两个集合之间的相似性，它被定义为两个集合交集的元素个数除以并集的元素个数。
 *
 * @author Binyu Li
 */
public class JaccardStrategy implements DistStrategy {

    /**
     * 杰卡德距离最大值
     */
    final double MaxJaccardDistance = 1.0;

    /**
     * 遍历两个种子的counts数组，若任一种子在某一branch访问次数非零，则并集大小加一，若两个种子在该branch的访问次数均非零，则交集大小加一，
     *
     * @param seed1 种子1
     * @param seed2 种子2
     * @return 种子1与种子2的杰卡德距离
     */
    @Override
    public double compute(SeedPriGuidance.DistInput seed1, SeedPriGuidance.DistInput seed2) {
        //杰卡德系数
        double jaccardIndex;
        Counter ct1 = seed1.getCoverage().getCounter();
        Counter ct2 = seed2.getCoverage().getCounter();
        //定义交集与并集大小
        int unionSetSize = 0, intersectionSetSize = 0;
        for (int i = 0; i < ct1.size(); i++) {
            int branch1 = ct1.getAtIndex(i);
            int branch2 = ct2.getAtIndex(i);
            if (branch1 != 0 || branch2 != 0) {
                intersectionSetSize++;
                if (branch1 != 0 && branch2 != 0) {
                    unionSetSize++;
                }
            }
        }
        jaccardIndex = (double) unionSetSize / intersectionSetSize;
        return MaxJaccardDistance - jaccardIndex;
    }

    @Override
    public String getName() {
        return "Jaccard";
    }
}
