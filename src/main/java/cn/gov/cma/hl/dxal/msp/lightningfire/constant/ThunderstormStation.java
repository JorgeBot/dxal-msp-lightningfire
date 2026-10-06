package cn.gov.cma.hl.dxal.msp.lightningfire.constant;

import lombok.Getter;

/**
 * 雷暴观测站：行政区划 ↔ 站号（thunderstorm_statistics.station_code）
 *
 * <p>thunderstorm_statistics 按站存年雷暴日数，表里的地区列是「漠河」「呼玛」这类简称、与
 * {@link RegionCode} 的名称（漠河市、呼玛县）对不上，故用站号做对照：漠河 50442、呼玛 50444、
 * 塔河 50443、加格达奇 50547、松岭 50546、新林 50544、呼中 50545。这 7 个站正好覆盖雷击火环境页面的
 * 分析范围（大兴安岭地区 232700 所辖县市区），选到范围之外的区划时没有对应站点，雷暴统计按「无数据」记 0。</p>
 */
@Getter
public enum ThunderstormStation {

    R50442(RegionCode.R232701, "50442"),
    R50444(RegionCode.R232721, "50444"),
    R50443(RegionCode.R232722, "50443"),
    R50547(RegionCode.R232761, "50547"),
    R50546(RegionCode.R232762, "50546"),
    R50544(RegionCode.R232763, "50544"),
    R50545(RegionCode.R232764, "50545"),
    ;

    /**
     * 站点所在的行政区划
     */
    private final RegionCode region;

    /**
     * 站号，thunderstorm_statistics 中按文本保存
     */
    private final String stationCode;

    ThunderstormStation(RegionCode region, String stationCode) {
        this.region = region;
        this.stationCode = stationCode;
    }

    /**
     * 按区划取雷暴站号。
     *
     * @param region 行政区划，为 null 时返回 null
     * @return 站号；该区划没有雷暴站时返回 null
     */
    public static String stationCode(RegionCode region) {
        if (region == null) {
            return null;
        }
        for (ThunderstormStation station : values()) {
            if (station.region == region) {
                return station.stationCode;
            }
        }
        return null;
    }
}
