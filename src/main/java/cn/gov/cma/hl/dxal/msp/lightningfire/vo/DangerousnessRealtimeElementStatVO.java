package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 危险性实时分析：要素概览统计的一项（四格中的一格）。
 *
 * <p>「要素概览统计」固定四项，顺序为最大值、最小值、平均值、极差（最大值 − 最小值），
 * 由所选要素在区间内、所选区域范围内的全部「区县 × 日」有效记录算出（与 /ltg-env 统计概览口径一致：
 * 不按面积或站点数加权，也不先按区县取平均再平均）。区间内一条有效记录都没有时四项的值都为 null，
 * 不补 0。</p>
 *
 * <p>四项都带单位（与所属要素一致），便于前端直接渲染同一套统计卡片。</p>
 */
@Schema(description = "危险性实时分析·要素概览统计的一项")
public record DangerousnessRealtimeElementStatVO(
        @Schema(description = "统计项标识：max=最大值、min=最小值、avg=平均值、range=极差", example = "max")
        String code,
        @Schema(description = "统计项名称", example = "最大值")
        String name,
        @Schema(description = "统计值；区间内没有有效记录时为 null", example = "16.697367")
        Double value,
        @Schema(description = "单位（与所选要素一致）", example = "℃")
        String unit) {
}
