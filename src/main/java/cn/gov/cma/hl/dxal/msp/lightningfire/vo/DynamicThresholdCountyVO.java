package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 单个区县在所选日期的动态阈值结果：符合新条件百分比与判定改变百分比。
 *
 * <p>两列都取自县级逐日动态条件分布表，已经是 0—100 的百分比（前端直接加 % 显示）：
 * hitPercent = {@code {因子}_hit_pct}（符合当年候选条件的格网占比）、
 * changedPercent = {@code {因子}_changed_pct}（与旧固定基线条件判定不同的格网占比，
 * 不是「准确率提高」，也不是「符合旧条件」的同义词）。两者一般不互补：
 * 新条件 0% 而变化 100% 表示原先全部符合、现在全部不符合。</p>
 *
 * <p>该县当天没有判定结果（不在 5—9 月研究应用范围、没有含林格网、或用该年只有候选线没有逐日结果）
 * 时两个百分比都为 null，不补 0。</p>
 */
@Schema(description = "区县符合新条件与判定改变百分比")
public record DynamicThresholdCountyVO(
        @Schema(description = "6 位行政区划代码", example = "232722")
        Integer code,
        @Schema(description = "区县名称", example = "塔河县")
        String name,
        @Schema(description = "符合新条件百分比（0—100，前端直接加 % 显示）；无数据时为 null", example = "100.0")
        Double hitPercent,
        @Schema(description = "与旧条件判定不同百分比（0—100）；无数据时为 null", example = "0.0")
        Double changedPercent) {
}
