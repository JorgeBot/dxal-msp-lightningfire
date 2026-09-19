package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "相关系数条形图数据")
public record CorrelationCoefficientVO(
        @Schema(description = "x 轴因子名称，按相关系数绝对值降序排列")
        String[] xAxis,
        @Schema(description = "y 轴相关系数，取值 -1 ~ 1，无数据记 0，顺序与 xAxis 一一对应")
        Double[] yAxis
) {
}
