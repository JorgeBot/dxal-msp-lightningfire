package cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature;

public record GridDTO(
        // 地闪次数
        Integer recordCount,
        // 正闪比例，百分比
        Float avgPositiveRatioPer,
        // 地闪密度，次/km²
        Float avgDensity,
        // 地闪强度
        Float avgAbsIntensity
) {
}
