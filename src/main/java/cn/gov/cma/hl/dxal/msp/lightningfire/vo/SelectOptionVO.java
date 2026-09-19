package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "select 选项")
public record SelectOptionVO(
        @Schema(description = "展示名称", example = "呼中区") String label,
        @Schema(description = "提交值，作为 region 参数取值", example = "hzh") String value) {
}
