package cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature;

/**
 * 雷击火关键要素·单个气象要素的统计（一行一要素）
 *
 * <p>对应 WeatherObservationMapper.selectKeyElementStatistics：在给定年份区间与区划范围的全部
 * 区县年记录上等权聚合。element 为要素标识，取 Option.KeyElementOption 的枚举名
 * （temperature / windSpeed / precipitation / relativeHumidity / wetnessIndex），与统计概览的
 * 要素卡片一一对应。</p>
 *
 * <p>AVG / MAX / MIN 均跳过 NULL：某要素在区间内没有任何有效值时三项均为 null，由服务层按
 * 「无数据记 0」的展示口径处理，不在这里补 0，以免与真实的 0 值混淆。</p>
 */
public record KeyElementStatisticDTO(
        // 要素标识，Option.KeyElementOption 的枚举名
        String element,
        // 区间内平均值
        Double avgValue,
        // 区间内最大值
        Double maxValue,
        // 区间内最小值
        Double minValue
) {
}
