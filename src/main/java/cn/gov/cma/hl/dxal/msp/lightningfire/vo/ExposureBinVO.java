package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 指标区间面积：按 0.2 等宽分档的连续值区间统计。
 *
 * <p>分档边界为 [0,0.2,0.4,0.6,0.8,1]：前四档左闭右开，末档含上界 1。
 * 森林覆盖暴露度、居民地面占比换算为百分数区间（如 0%–20%），
 * 道路暴露度密度、综合暴露度按归一化区间（如 0.0–0.2）展示。</p>
 */
@Schema(description = "指标区间面积")
public record ExposureBinVO(
        @Schema(description = "区间标签", example = "0%–20%")
        String label,
        @Schema(description = "区间下界（含）", example = "0.0")
        Double lowerBound,
        @Schema(description = "区间上界；末档含上界，其余为不含", example = "0.2")
        Double upperBound,
        @Schema(description = "落在该区间的面积，km²；无数据为 null", example = "10092.12")
        Double areaKm2) {
}
