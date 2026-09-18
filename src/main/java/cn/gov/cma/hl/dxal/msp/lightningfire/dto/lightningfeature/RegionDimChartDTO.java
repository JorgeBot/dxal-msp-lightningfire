package cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature;

import java.util.List;

public record RegionDimChartDTO(
        // x 轴地区名
        List<String> region,
        // 各地区地闪次数
        List<Integer> count
) {
}
