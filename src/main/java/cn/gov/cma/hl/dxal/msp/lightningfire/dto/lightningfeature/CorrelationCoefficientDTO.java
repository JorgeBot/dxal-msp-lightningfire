package cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature;

/**
 * 各环境因子在统计区间内的相关系数。
 *
 * <p>相关系数已由上游计算完成并按日存放在 correlation_coefficient_statistics 的各因子列中
 * （取值 -1 ~ 1），此处取区间均值：无匹配数据、或区间内整列为 NULL 时统一为 0。
 * 不涉及 lightning_count / mean_intensity 两列。</p>
 */
public record CorrelationCoefficientDTO(
        // 相对湿度
        Double relativeHumidity,
        // 温度
        Double temperature,
        // 降水
        Double rain,
        // 地面高程
        Double dem,
        // 可燃物含水率
        Double fmc,
        // 坡度
        Double slope,
        // 风速
        Double windSpeed
) {
}
