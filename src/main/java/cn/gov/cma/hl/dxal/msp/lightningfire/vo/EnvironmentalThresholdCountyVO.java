package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 单个区县的当日危险性分数。
 *
 * <p>危险性统计表只有区县名（region 列）与地市名（city 列）、没有行政区划代码，
 * 这里的 {@code code} 由区划枚举按「地市 + 区县名」反查得到：枚举未覆盖的县区名记为 null，
 * 名称与分数照常返回，不因缺少代码丢记录。</p>
 *
 * <p>{@code score} 为该区县当日的评分均值（同日同区县若有多行则取均值）；
 * 当日没有记录时为 null，不补 0。同一份结构同时用于「所选地区每个县的分数」与
 * 「当日分数前五柱状图」的分类项。</p>
 */
@Schema(description = "区县当日危险性分数")
public record EnvironmentalThresholdCountyVO(
        @Schema(description = "6 位行政区划代码；区划枚举未覆盖的县区名为 null", example = "232701")
        Integer code,
        @Schema(description = "区县名称", example = "漠河市")
        String name,
        @Schema(description = "当日分数（0—100 百分制）；当日无记录时为 null，单位见所属图表的 unit",
                example = "63.42")
        Double score) {
}
