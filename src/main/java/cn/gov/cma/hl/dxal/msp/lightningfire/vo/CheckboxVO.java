package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "checkbox 选项")
public record CheckboxVO(
        @Schema(description = "提交值，作为 elements 参数取值", example = "precipitation") String key,
        @Schema(description = "展示名称", example = "降水") String label) {
}
