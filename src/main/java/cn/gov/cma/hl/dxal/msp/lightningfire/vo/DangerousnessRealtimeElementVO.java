package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 危险性实时分析：单个要素的分数（概览统计里「四个要素的分数」的一格）。
 *
 * <p>要素与表 dangerousness_realtime_statistics 的四列一一对应，四格的顺序固定为
 * 气温（temperature）→ 相对湿度（relativeHumidity）→ 风速（windSpeed）→ 降水（rain），
 * 与表内列序、/ltg-env 统计概览的同名要素一致；前端按下标取用即可，不必再按 code 匹配。</p>
 *
 * <p>单位沿用页面既有的同名要素展示口径（气温 ℃、相对湿度 %、风速 m/s、降水 mm）；
 * 表内没有单位列，接口只做原样返回，不做换算。{@code value} 为该区县当日该要素的分数，
 * 当日没有结果时为 null（不补 0，与「分数为 0」可区分）。</p>
 */
@Schema(description = "危险性实时分析·要素分数")
public record DangerousnessRealtimeElementVO(
        @Schema(description = "要素标识，与表列名对应：temperature / relativeHumidity / windSpeed / rain",
                example = "temperature")
        String code,
        @Schema(description = "要素名称", example = "气温")
        String name,
        @Schema(description = "该要素的分数；当日无结果时为 null", example = "11.61")
        Double value,
        @Schema(description = "单位（气温 ℃、相对湿度 %、风速 m/s、降水 mm）", example = "℃")
        String unit) {
}
