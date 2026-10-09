package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 综合风险评估与区划：所选行政区的 H/E/V 分项指数（左二 H/E/V 指数柱图）。
 *
 * <p>三项指数与综合风险评估的评分统计是两套口径，页面并列展示但不可互相换算：</p>
 * <ul>
 *   <li>评分统计（{@link RiskAssessmentStatisticsVO}）取逐日评分表 dangerousness_statistics 的
 *       score，是 0—100 的区域评分均值与评分区间占比；</li>
 *   <li>本结果取 H/E/V 三张汇总表，是 0—1 的归一化指数，用于说明评分的构成来源。</li>
 * </ul>
 *
 * <p>方案相关性：危险性 H 与 AHP 方案无关；暴露度 E 与脆弱性 V 的取值随 schemeId（0—6）变化，
 * 但两者不是同一套权重——E 取 E_weights，V 取 V_weights。</p>
 */
@Schema(description = "H/E/V 分项指数")
public record RiskAssessmentComponentsVO(
        @Schema(description = "评价年份", example = "2018")
        Short assessmentYear,
        @Schema(description = "行政区划代码", example = "232700")
        Integer regionCode,
        @Schema(description = "行政区名称", example = "大兴安岭地区")
        String regionName,
        @Schema(description = "行政区级别：province=全省、city=地市、county=县市区", example = "city")
        String regionLevel,
        @Schema(description = "本次取数使用的 AHP 方案号（仅暴露度与脆弱性受影响，危险性不受影响）", example = "0")
        Short schemeId,
        @Schema(description = "H/E/V 分项指数，顺序固定为危险性 H、暴露度 E、脆弱性 V")
        RiskAssessmentComponentVO[] components) {
}
