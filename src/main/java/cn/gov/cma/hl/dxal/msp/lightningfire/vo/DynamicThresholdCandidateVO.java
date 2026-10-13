package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 动态阈值：某一年的年度候选行。
 *
 * <p>与 功能三_动态阈值/结果/年度候选与漂移.csv 的一行一一对应，数值原样返回：
 * threshold 为当年候选阈值、baseline 为当期固定基线、delta 为新减旧（保留正负号）、
 * ciLow/ciHigh 为按 7 天时间块重抽样 500 次后的 2.5% / 97.5% 分位（区间很宽表示候选不稳定，
 * 不是当天因子的最小最大值）。</p>
 *
 * <p>operator 是当年候选的比较方向（可能与基线的方向不同，如风速 2019、2020 年为 ge）；
 * availableDays/expectedDays/missingDays 为训练样本的可用天数、计划天数与相差天数；
 * positive 为有火记录格日数；trainStart/trainEnd 为拟合使用的年份区间；
 * evaluationJ 为当年检验的约登指数（2025 年缺当年检验，为 null）；
 * flipRate 是整体历史检验中判定发生变化的格日比例，不能用于当前县地图
 * （当前县当天要用 {@code /dynamic/counties} 的 changedPercent）。</p>
 */
@Schema(description = "动态阈值·年度候选")
public record DynamicThresholdCandidateVO(
        @Schema(description = "候选年份", example = "2024")
        Integer year,
        @Schema(description = "当年候选的比较方向取值：ge=≥、gt=>、le=≤", example = "le")
        String operator,
        @Schema(description = "当年候选的比较方向符号", example = "≤")
        String operatorSymbol,
        @Schema(description = "当年候选阈值（完整值，展示可四舍五入）", example = "8.150763969421387")
        Double threshold,
        @Schema(description = "当期固定基线（与固定阈值模块同值）", example = "8.04280460357666")
        Double baseline,
        @Schema(description = "阈值差值 = 候选阈值 − 固定基线，保留正负号", example = "0.10795936584472798")
        Double delta,
        @Schema(description = "重抽样 500 次得到的 95% 区间下限（不是当天因子最小值）", example = "0.023759366199374198")
        Double ciLow,
        @Schema(description = "重抽样 500 次得到的 95% 区间上限（不是当天因子最大值）", example = "12.262178344726566")
        Double ciHigh,
        @Schema(description = "整体历史检验中判定发生变化的格日比例（0—1）；不能用于当前县地图", example = "0.001427422199004778")
        Double flipRate,
        @Schema(description = "有火记录格日数", example = "157")
        Long positive,
        @Schema(description = "可用训练天数", example = "918")
        Integer availableDays,
        @Schema(description = "计划训练天数", example = "918")
        Integer expectedDays,
        @Schema(description = "缺少训练天数 = 计划天数 − 可用天数", example = "0")
        Integer missingDays,
        @Schema(description = "拟合训练起始年份", example = "2018")
        Integer trainStart,
        @Schema(description = "拟合训练结束年份", example = "2023")
        Integer trainEnd,
        @Schema(description = "当年检验的约登指数 J；缺当年检验时为 null", example = "0.035296929485027606")
        Double evaluationJ,
        @Schema(description = "与固定基线的比较方向是否不同（true 表示当年候选换了方向）", example = "false")
        Boolean directionChanged,
        @Schema(description = "候选状态，恒为 CANDIDATE_NOT_ACTIVE（候选未启用）", example = "CANDIDATE_NOT_ACTIVE")
        String status,
        @Schema(description = "复核建议：HISTORICAL_REPLAY=历史回看、KEEP_BASELINE=维持基线、"
                + "RESEARCH_CANDIDATE=有改善迹象仍需复核、AWAIT_VALIDATION=缺当年检验暂不采用",
                example = "KEEP_BASELINE")
        String adoptionStatus,
        @Schema(description = "复核建议的中文说明", example = "未显示足够改善，维持基线")
        String adoptionLabel) {
}
