package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 折线图上的一个日期点：日期 + 该日所选区域的危险性分数。
 *
 * <p>分数口径与综合风险评估与区划的区域指数一致，只把评价年份换成单日：
 * 县市区取该区县当日记录均值；地市取所辖县区当日均值的等权均值；全省取各地市当日均值的等权均值
 * （按行政区等权，表中没有面积列，不做面积加权）。先按行政区算均值再取均值，
 * 因此记录缺失的县区不会把自己缺的那天按 0 拉低整个地市。</p>
 *
 * <p>该日所选范围内一条记录都没有时 {@code score} 为 null：不补 0，避免把缺测读成低风险。</p>
 */
@Schema(description = "分数折线图日期点")
public record EnvironmentalThresholdTrendPointVO(
        @Schema(description = "日期", example = "2024-05-01")
        LocalDate date,
        @Schema(description = "该日分数（0—100 百分制）；当日无记录时为 null，单位见所属折线图的 unit",
                example = "56.31")
        Double score) {
}
