package cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature;

/**
 * 雷击火关键要素·单个气象要素的统计（一行一要素）
 *
 * <p>对应 WeatherObservationMapper.selectKeyElementStatistics：在给定日期区间（assessment_date 精确到日）
 * 与区划范围的全部「区县 × 日」记录上聚合。element 为要素标识，取 Option.KeyElementOption 的枚举名
 * （temperature / windSpeed / precipitation / relativeHumidity / wetnessIndex），与统计概览的
 * 要素卡片一一对应。</p>
 *
 * <p>字段口径随要素不同：avgValue 对气温、风速、降水、相对湿度是区间内的日平均值
 * （降水为日平均降水，不是区间累计），对湿润指数是区间内最后一个非空值（年内累计口径）；
 * maxValue / minValue 是区间内日值的最大与最小。
 * 三个聚合均跳过 NULL：某要素在区间内没有任何有效值时对应项为 null，由服务层按「无数据记 0」的
 * 展示口径处理，不在这里补 0，以免与真实的 0 值混淆。</p>
 */
public record KeyElementStatisticDTO(
        // 要素标识，Option.KeyElementOption 的枚举名
        String element,
        // 区间内平均值（降水为日平均降水；湿润指数为区间内最后一个非空值）
        Double avgValue,
        // 区间内最大值
        Double maxValue,
        // 区间内最小值
        Double minValue
) {
}
