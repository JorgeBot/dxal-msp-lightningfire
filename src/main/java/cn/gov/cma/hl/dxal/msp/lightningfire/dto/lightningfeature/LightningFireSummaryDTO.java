package cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature;

/**
 * 雷击火统计（区间内、可按区域过滤）
 *
 * <p>对应 ForestFireStatisticsMapper.selectLightningFireSummary：过火面积单位 hm²（公顷），
 * 与源档案一致。聚合不带 GROUP BY，恒返回一行，无记录时 SQL 已 COALESCE 为 0，
 * 故三项都不会是 null。</p>
 */
public record LightningFireSummaryDTO(
        // 雷击火事件次数
        Long fireCount,
        // 过火面积合计，单位 hm²
        Double totalArea,
        // 平均过火面积，单位 hm²
        Double avgArea
) {
}
