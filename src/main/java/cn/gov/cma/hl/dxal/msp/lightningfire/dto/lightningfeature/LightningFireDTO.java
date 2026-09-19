package cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 雷击火记录（时间轴 series 的元素）
 */
@Schema(description = "雷击火记录")
public record LightningFireDTO(
        @Schema(description = "发现时间", example = "2025-07-18T13:56:00")
        LocalDateTime discoveredAt,
        @Schema(description = "经度，取值 -180 至 180", example = "122.405833")
        Double longitude,
        @Schema(description = "纬度，取值 -90 至 90", example = "53.300278")
        Double latitude,
        @Schema(description = "火灾面积", example = "3.66")
        Double totalArea,
        @Schema(description = "起火单位", example = "漠河")
        String fireUnit
) {
}
