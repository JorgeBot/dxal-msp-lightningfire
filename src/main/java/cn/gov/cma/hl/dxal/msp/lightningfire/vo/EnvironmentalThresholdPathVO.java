package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 组合环境条件的一条路径：路径内条件为「且」，路径之间为「或」。
 *
 * <p>leaf 为源浅层树叶子编号（仅用于与成果文件对照），条件顺序即成果文件中的顺序。</p>
 */
@Schema(description = "组合环境条件的一条路径（路径内且、路径间或）")
public record EnvironmentalThresholdPathVO(
        @Schema(description = "源浅层树叶子编号", example = "6")
        Integer leaf,
        @Schema(description = "该路径内的条件，全部满足即该路径成立，顺序即成果文件顺序")
        EnvironmentalThresholdConditionVO[] conditions) {
}
