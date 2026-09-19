package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

public record LeadingFactorVO(
        // x 轴因子名称，按权重绝对值降序排列
        String[] xAxis,
        // y 轴主导因子权重均值，无数据记 0，顺序与 xAxis 一一对应
        Double[] yAxis
) {
}
