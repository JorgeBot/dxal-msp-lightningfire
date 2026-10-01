package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 概览统计的单个格子
 *
 * <p>统计概览以网格展示，每个要素 4 个格子，一个格子就是一条本记录：展示名称、展示值与单位。
 * 值为字符串：各要素的精度不同（气温 1 位、风速 2 位、湿润指数 3 位），由服务层统一格式化后返回，
 * 前端直接显示，不必再按要素决定小数位。</p>
 */
@Schema(description = "概览统计")
public record ArticleVO(
        @Schema(description = "展示名称", example = "平均气温") String label,
        @Schema(description = "展示值，已按要素精度格式化；区间内没有有效值时记 0", example = "-0.9") String value,
        @Schema(description = "单位", example = "℃") String unit) {
}
