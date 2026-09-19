package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "雷击火要素数据")
public record LightningElementsVO(
        @Schema(description = "时间轴标签，按 granularity 展开 since ~ until；各要素序列待数据源接入后补充")
        String[] timeAxis
) {
}
