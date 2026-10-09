package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * H/E/V 分项指数的一项。
 *
 * <p>三项都是 0—1 的归一化指数，与综合风险评估的 0—100 评分不是同一量纲，
 * 也不做 AHP 加权合成：本项只是把该区的危险性、暴露度、脆弱性指数并列展示。</p>
 *
 * <p>缺数约定：该年份、该区划没有汇总记录时 value 返回 null 并给出 source，不补 0
 * （例如综合脆弱性只有 2018—2022 有记录，2023—2024 的 V 为 null）。</p>
 */
@Schema(description = "H/E/V 分项指数")
public record RiskAssessmentComponentVO(
        @Schema(description = "指标代码：H=危险性、E=暴露度、V=脆弱性", example = "H")
        String code,
        @Schema(description = "指标名称", example = "危险性 H")
        String name,
        @Schema(description = "指数值（0—1）；该年份、该区划没有记录时为 null", example = "0.5122")
        Double value,
        @Schema(description = "数值单位；指数为无量纲，统一为「指数 0—1」", example = "指数 0—1")
        String unit,
        @Schema(description = "取数来源（表名与列名），便于与评分统计区分",
                example = "dangerousness_annual_summary.mean（逐年 H 均值，与方案无关）")
        String source) {
}
