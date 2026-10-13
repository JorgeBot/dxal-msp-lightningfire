package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 组合环境条件里的一条判定条件（路径内条件为「且」）。
 *
 * <p>结构与单因子阈值行分开：单因子规则是一条线（一个方向 + 一个阈值），
 * 组合条件是多条线组成的一组条件，不能当成单个阈值展示。</p>
 */
@Schema(description = "组合环境条件中的一条判定条件")
public record EnvironmentalThresholdConditionVO(
        @Schema(description = "环境因子 id", example = "relative_humidity")
        String factor,
        @Schema(description = "环境因子名称", example = "相对湿度")
        String factorName,
        @Schema(description = "比较方向取值：ge=≥、gt=>、le=≤", example = "le")
        String operator,
        @Schema(description = "比较方向符号，与 operator 一一对应", example = "≤")
        String operatorSymbol,
        @Schema(description = "阈值（完整值，判断用完整值，展示可四舍五入）", example = "69.88364791870117")
        Double threshold,
        @Schema(description = "阈值单位", example = "%")
        String unit) {
}
