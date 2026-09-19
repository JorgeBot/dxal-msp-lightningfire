package cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature;

/**
 * 各环境因子在统计区间内的主导权重（主导因子统计）。
 *
 * <p>权重已由上游计算完成并按日存放在 leading_factor_statistics 的各因子列中，
 * 此处取区间均值：无匹配数据、或区间内整列为 NULL 时统一为 0。</p>
 */
public record LeadingFactorDTO(
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
