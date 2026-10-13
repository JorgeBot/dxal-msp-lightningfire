package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 功能三：所选环境因子的动态阈值——2019—2025 年逐年候选 + 当前的固定阈值。
 *
 * <p>数据来自交接包提取的常量 {@link cn.gov.cma.hl.dxal.msp.lightningfire.constant.DynamicThreshold}
 * （年度候选与漂移.csv）与 {@link cn.gov.cma.hl.dxal.msp.lightningfire.constant.FixedThreshold}
 * （阈值汇总.csv 的固定阈值），接口不读 CSV、不重新训练。</p>
 *
 * <p>页面用法：年度阈值折线用 candidates 的 year/threshold，折线竖线区间用 ciLow/ciHigh，
 * "固定基线 → 年度候选" 与阈值差值用每条候选的 baseline/delta，训练天数与有火格日用
 * availableDays/expectedDays/missingDays/positive，复核建议用 adoptionStatus/adoptionLabel。
 * 动态窗口是累积历史（expanding），2019—2023 属开发期回放、2024 为回顾性留出检验、
 * 2025 只有候选线没有逐日结果：历史回放不是预报。</p>
 */
@Schema(description = "环境影响阈值·动态阈值与当前固定阈值")
public record DynamicThresholdVO(
        @Schema(description = "环境因子 id", example = "rain")
        String factor,
        @Schema(description = "环境因子名称", example = "日降水")
        String factorName,
        @Schema(description = "阈值单位", example = "mm/d")
        String unit,
        @Schema(description = "当期固定基线的比较方向取值（与固定阈值模块一致）", example = "le")
        String baselineOperator,
        @Schema(description = "当期固定基线的比较方向符号", example = "≤")
        String baselineOperatorSymbol,
        @Schema(description = "动态窗口，恒为 expanding（累积历史）", example = "expanding")
        String window,
        @Schema(description = "当前的固定阈值规则（阈值、单位、置信区间与 2024 检验结果），"
                + "即年度候选表里的 baseline 列")
        EnvironmentalThresholdRuleVO fixedThreshold,
        @Schema(description = "2019—2025 年逐年候选，按年份升序")
        DynamicThresholdCandidateVO[] candidates) {
}
