package cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature;

public record TimeSeriesDTO(
        // 时间标签，如 2024-06-01 / 2024-06 / 2024
        String period,
        // 该时间段地闪记录总数
        Integer count
) {
}
