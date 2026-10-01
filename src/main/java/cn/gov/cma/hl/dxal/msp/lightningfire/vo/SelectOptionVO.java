package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "select 选项")
public record SelectOptionVO(
        @Schema(description = "展示名称", example = "呼中区") String label,
        @Schema(description = "提交值，作为 regionCode 等接口参数取值（区划为 6 位行政区划码）", example = "232764") String value) {
}
