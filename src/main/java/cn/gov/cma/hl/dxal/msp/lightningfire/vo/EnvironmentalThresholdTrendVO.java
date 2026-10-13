package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 雷击火环境影响阈值：所选区域在 since ~ until 内的危险性分数折线图。
 *
 * <p>数据源为危险性统计表 dangerousness_statistics 的 score 列（0—100 百分制，数值越高危险性越高），
 * 该表按「日期 × 区县 × 地级市」逐日存放评分，没有行政区划代码，故按地区名匹配
 * （见 {@link EnvironmentalThresholdTrendPointVO} 的取数口径）。</p>
 *
 * <p>x 轴恒按 since ~ until 逐日展开（含无记录的日期），保证前端折线连续：无记录的日期同样占位，
 * 其 {@code score} 为 null，不补 0（缺测与「分数 0」不是一回事）。区间内一条记录都没有时，
 * points 为一段全 null 的日期轴，接口仍返回 200。</p>
 */
@Schema(description = "环境影响阈值·分数折线图（逐日）")
public record EnvironmentalThresholdTrendVO(
        @Schema(description = "行政区划代码", example = "232700")
        Integer regionCode,
        @Schema(description = "行政区名称", example = "大兴安岭地区")
        String regionName,
        @Schema(description = "行政区级别：province=全省、city=地市、county=县市区", example = "city")
        String regionLevel,
        @Schema(description = "统计起始日期（含）", example = "2024-01-01")
        LocalDate since,
        @Schema(description = "统计结束日期（含）", example = "2024-12-31")
        LocalDate until,
        @Schema(description = "分数单位，恒为「分」（0—100 百分制）", example = "分")
        String unit,
        @Schema(description = "逐日折线点：since ~ until 的每一天各一点，无记录的日期 score 为 null")
        EnvironmentalThresholdTrendPointVO[] points) {
}
