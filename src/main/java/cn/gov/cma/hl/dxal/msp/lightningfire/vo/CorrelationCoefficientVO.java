package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

public record CorrelationCoefficientVO(
        // x 轴因子名称，按相关系数绝对值降序排列
        String[] xAxis,
        // y 轴相关系数，取值 -1 ~ 1，无数据记 0，顺序与 xAxis 一一对应
        Double[] yAxis
) {
}
