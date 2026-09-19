package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "主导因子条形图数据")
public record LeadingFactorVO(
        @Schema(description = "x 轴因子名称，按权重绝对值降序排列")
        String[] xAxis,
        @Schema(description = "y 轴主导因子权重均值，无数据记 0，顺序与 xAxis 一一对应")
        Double[] yAxis
) {
}
