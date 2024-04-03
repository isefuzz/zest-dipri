package edu.nju.fuzz.strategy;

import edu.nju.fuzz.guidance.SeedPriGuidance;

/**
 * 在覆盖率引导的灰盒模糊测试的变异种子筛选阶段，将原有的随机选择种子策略更改为自定义的种子筛选策略。
 * <p>
 * 在自适应随机测试ART相关研究中得出的一个结论为距离其他种子距离越远的种子，能覆盖到更多未覆盖过代码，生成的测试用例具有更高的覆盖率，
 * 参考文献《 A Survey on Adaptive Random Testing 》written by Rubing Huang , Member, IEEE, Weifeng Sun, Yinyin Xu,
 * Haibo Chen, Dave Towey , Member, IEEE, and Xin Xia
 * <p>
 * JQF使用的种子覆盖率存储结构Counter将覆盖率表示为数组counts，每覆盖一次某个branch即在该branch在counts数组中的对应的位置数字加1
 * 本策略算法主要针对该counts数组，将该数组看作一个多维向量，种子之间的距离定义为种子counts多维向量之间的距离，使用常用的距离计算公式计算距离
 *
 * @author Binyu Li
 */
public interface DistStrategy {
    /**
     * 种子距离计算方法
     *
     * @param seed1 SeedPriGuidance中定义的DistInput类型的种子1
     * @param seed2 SeedPriGuidance中定义的DistInput类型的种子2
     * @return 两个种子之间的距离
     */
    double compute(SeedPriGuidance.DistInput seed1, SeedPriGuidance.DistInput seed2);

    /**
     * 获取使用的距离计算策略的名称
     *
     * @return 策略名
     */
    String getName();
}
