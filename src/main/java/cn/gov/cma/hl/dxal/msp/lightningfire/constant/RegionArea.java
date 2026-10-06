package cn.gov.cma.hl.dxal.msp.lightningfire.constant;

import lombok.Getter;

/**
 * 区域面积（km²），雷击火点位密度的分母。
 *
 * <p>目前只提供雷击火环境页面分析范围（大兴安岭地区 232700）所辖 7 个县市区，面积取自各县市区行政面积，
 * 合计 80264 km²（漠河市 18367、呼玛县 14285、塔河县 14103、加格达奇区 1587、松岭区 15799、
 * 新林区 8703、呼中区 7420）。这 7 个县市区就是 {@link RegionCode#selectable(Integer)} 在 232700 下给出的全部区划，
 * 页面上无论选地市还是选县区，面积都能凑齐；选到范围之外的区划（如全省 230000）时没有面积口径，
 * 点位密度按「无数据」记 0，不拿部分县区面积当分母充数。需要扩大范围时在这里补一行即可。</p>
 */
@Getter
public enum RegionArea {

    R232761(RegionCode.R232761, 1587),
    R232762(RegionCode.R232762, 15799),
    R232763(RegionCode.R232763, 8703),
    R232764(RegionCode.R232764, 7420),
    R232701(RegionCode.R232701, 18367),
    R232721(RegionCode.R232721, 14285),
    R232722(RegionCode.R232722, 14103),
    ;

    /**
     * 对应的行政区划
     */
    private final RegionCode region;

    /**
     * 区域面积，单位 km²
     */
    private final int areaKm2;

    RegionArea(RegionCode region, int areaKm2) {
        this.region = region;
        this.areaKm2 = areaKm2;
    }

    /**
     * 按区划取面积。
     *
     * @param region 行政区划，为 null 时返回 null
     * @return 面积（km²）；该区划没有面积口径时返回 null
     */
    public static Integer areaKm2(RegionCode region) {
        if (region == null) {
            return null;
        }
        for (RegionArea area : values()) {
            if (area.region == region) {
                return area.areaKm2;
            }
        }
        return null;
    }
}
