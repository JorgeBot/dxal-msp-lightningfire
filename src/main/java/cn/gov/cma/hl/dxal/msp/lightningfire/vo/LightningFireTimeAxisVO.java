package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature.LightningFireDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "雷击火时间轴数据")
public record LightningFireTimeAxisVO(
        @Schema(description = "时间轴标签，按 granularity 展开 since ~ until，无记录的日期同样占位，如 2024-06-01 / 2024-06 / 2024")
        String[] timeAxis,
        @Schema(description = "区间内的雷击火记录，按发现时间升序，无记录时为空数组")
        LightningFireDTO[] series
) {
}
