package cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature;

import java.util.List;

public record TimeDimChartDTO(
        // x 轴时间标签
        List<String> series,
        // 各时间点地闪次数
        List<Integer> count,
        // 最大值所在下标
        Integer maxAt,
        // 最大值
        Integer maxValue
) {
}
