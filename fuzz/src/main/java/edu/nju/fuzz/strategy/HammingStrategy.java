package edu.nju.fuzz.strategy;

import edu.berkeley.cs.jqf.fuzz.util.Counter;
import edu.nju.fuzz.guidance.SeedPriGuidance;

/**
 * 汉明距离策略
 * 参考资料来自百度百科“汉明距离”词条：
 * 汉明距离是使用在数据传输差错控制编码里面的，汉明距离是一个概念，它表示两个（相同长度）字符串对应位置的不同字符的数量，
 * 我们以d（x,y）表示两个字x,y之间的汉明距离。对两个字符串进行异或运算，并统计结果为1的个数，那么这个数就是汉明距离。
 *
 * @author Binyu Li
 */
public class HammingStrategy implements DistStrategy {
    /**
     * 遍历种子Counter中的counts数组，若两个种子在相同branch的访问次数非零状态不一致，则两个种子的汉明距离加1
     *
     * @param seed1 种子1
     * @param seed2 种子2
     * @return 种子1与种子2之间的汉明距离
     */
    @Override
    public double compute(SeedPriGuidance.DistInput seed1, SeedPriGuidance.DistInput seed2) {
        double hammingDistance = 0;
        Counter ct1 = seed1.getCoverage().getCounter();
        Counter ct2 = seed2.getCoverage().getCounter();
        for (int i = 0; i < ct1.size(); i++) {
            int branch1 = ct1.getAtIndex(i);
            int branch2 = ct2.getAtIndex(i);
            if (branch1 == 0 && branch2 != 0 || branch1 != 0 && branch2 == 0) {
                hammingDistance++;
            }
        }
        return hammingDistance/1000.0;
    }

    @Override
    public String getName() {
        return "Hamming";
    }

}
