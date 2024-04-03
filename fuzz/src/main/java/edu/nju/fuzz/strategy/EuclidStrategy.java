package edu.nju.fuzz.strategy;

import edu.berkeley.cs.jqf.fuzz.util.Counter;
import edu.nju.fuzz.guidance.SeedPriGuidance;

/**
 * 欧氏距离策略
 * 参考资料来自百度百科“欧几里得度量”词条：
 * 欧几里得度量（euclidean metric）（也称欧氏距离）是一个通常采用的距离定义，指在m维空间中两个点之间的真实距离，
 * 或者向量的自然长度（即该点到原点的距离）。在二维和三维空间中的欧氏距离就是两点之间的实际距离。
 *
 * @author Binyu Li
 */
public class EuclidStrategy implements DistStrategy {
    /**
     * 根据欧氏距离多维向量计算公式，对种子Counter中的counts数组进行遍历，对两个种子同一branch上的访问次数差值的平方进行求和，
     * 遍历结束后对总距离进行开方得到欧氏距离
     *
     * @param seed1 种子1
     * @param seed2 种子2
     * @return 种子1与种子2之间的欧氏距离
     */
    @Override
    public double compute(SeedPriGuidance.DistInput seed1, SeedPriGuidance.DistInput seed2) {
        double euclidDistance = 0;
        Counter ct1 = seed1.getCoverage().getCounter();
        Counter ct2 = seed2.getCoverage().getCounter();
        for (int i = 0; i < ct1.size(); i++) {
            int branch1 = ct1.getAtIndex(i);
            int branch2 = ct2.getAtIndex(i);
            if (branch1 != 0 || branch2 != 0) {
                euclidDistance += Math.pow(branch1 - branch2, 2);
            }
        }
        return Math.sqrt(euclidDistance)/1000.0;
    }

    @Override
    public String getName() {
        return "Euclid";
    }

}
