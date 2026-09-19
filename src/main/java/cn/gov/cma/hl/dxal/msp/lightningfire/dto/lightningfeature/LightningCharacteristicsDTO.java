package cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 地闪特征
 */
@Schema(description = "地闪特征")
public record LightningCharacteristicsDTO(
        @Schema(description = "地闪次数", example = "1234")
        Integer recordCount,
        @Schema(description = "正闪比例，百分比", example = "12.5")
        Float avgPositiveRatioPer,
        @Schema(description = "地闪密度，次/km²", example = "0.123")
        Float avgDensity,
        @Schema(description = "地闪强度", example = "6.789")
        Float avgAbsIntensity
) {
}
