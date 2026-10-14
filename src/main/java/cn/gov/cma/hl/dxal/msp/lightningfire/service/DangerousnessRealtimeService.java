package cn.gov.cma.hl.dxal.msp.lightningfire.service;

import cn.gov.cma.hl.dxal.msp.lightningfire.constant.RegionCode;
import cn.gov.cma.hl.dxal.msp.lightningfire.entity.DangerousnessRealtimeStatistics;
import cn.gov.cma.hl.dxal.msp.lightningfire.mapper.DangerousnessRealtimeStatisticsMapper;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.DangerousnessRealtimeCountyVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.DangerousnessRealtimeElementVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.DangerousnessRealtimeOverviewVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.DangerousnessRealtimeRankingItemVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.DangerousnessRealtimeRankingVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 危险性实时分析：所选区域在某一天的危险性概览（等级、分数、四个要素分数）与前八名区县折线图排行。
 *
 * <p>数据源为实时表 dangerousness_realtime_statistics（粒度：日期 × 区县，主键 date + county_code），
 * 一天最多 125 行，接口一次只取一天，不做区间展开，也不与逐日历史评分表
 * {@link cn.gov.cma.hl.dxal.msp.lightningfire.entity.DangerousnessStatistics} 的时间序列拼接
 * （两者日期区间不重叠，拼起来不是一条连续的曲线）。</p>
 *
 * <p>行政区定位按 {@link RegionCode} 的地区名匹配，与承载体暴露度、脆弱性、综合风险评估等模块
 * 按 region_code 取数的做法不同：该表虽然有 county_code 列，但大兴安岭地区的松岭区 / 新林区 / 呼中区
 * 用的是非国标码 232702 / 232703 / 232704，按代码对齐区划枚举会让这三个区落不进大兴安岭地区的范围，
 * 因此用「地市名 + 区县名」定位（同名区县跨地市时靠地市名区分），返回给前端的区划码取区划枚举的国标码。</p>
 *
 * <p>区县范围：选择地市取所辖县区、选择县市区取该县区本身、选择 230000 取全省 125 个县级单元，
 * 统一走 {@link RegionCode#selectable(Integer)}；范围内的县区全部返回，当天没有结果的值为 null，
 * 不补 0（缺测不是低风险）。</p>
 *
 * <p>两个接口共用同一份单日区县结果：概览接口原样返回这份数组，由前端按所选区域的行政级别决定
 * 展示口径（选区县直接取该县那一行，选地市对该市下所辖县区的分数取平均）；
 * 排行接口取它的前 8 条（已按分数降序），折线图的 X 轴即这 8 个区县的顺序。</p>
 */
@Service
@RequiredArgsConstructor
public class DangerousnessRealtimeService {

    private static final String LEVEL_PROVINCE = "province";

    private static final String LEVEL_CITY = "city";

    private static final String LEVEL_COUNTY = "county";

    /**
     * 分数单位：危险性评分是 0—100 百分制，与综合风险评估与区划、环境影响阈值的「分」同一口径。
     */
    private static final String UNIT_SCORE = "分";

    /**
     * 排行折线图展示的区县数量（当日前八名）。
     */
    private static final int RANKING_SIZE = 8;

    private static final String RANKING_TITLE = "风险分数排行（前 " + RANKING_SIZE + "）";

    /**
     * 四个要素的标识、名称与单位：顺序固定为气温、相对湿度、风速、降水，与表内四列及
     * /ltg-env 统计概览的同名要素一致；表内没有单位列，单位沿用页面既有的展示口径。
     */
    private static final String ELEMENT_TEMPERATURE = "temperature";

    private static final String ELEMENT_TEMPERATURE_NAME = "气温";

    private static final String UNIT_TEMPERATURE = "℃";

    private static final String ELEMENT_RELATIVE_HUMIDITY = "relativeHumidity";

    private static final String ELEMENT_RELATIVE_HUMIDITY_NAME = "相对湿度";

    private static final String UNIT_RELATIVE_HUMIDITY = "%";

    private static final String ELEMENT_WIND_SPEED = "windSpeed";

    private static final String ELEMENT_WIND_SPEED_NAME = "风速";

    private static final String UNIT_WIND_SPEED = "m/s";

    private static final String ELEMENT_RAIN = "rain";

    private static final String ELEMENT_RAIN_NAME = "降水";

    private static final String UNIT_RAIN = "mm";

    /**
     * 分数降序（无结果的排最后，同值按区县名升序）：概览数组与前八名排行都用这个顺序，
     * 前端折线图因此不必再排序。
     */
    private static final Comparator<DangerousnessRealtimeCountyVO> BY_SCORE_DESC =
            Comparator.comparing(DangerousnessRealtimeCountyVO::score,
                            Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(DangerousnessRealtimeCountyVO::name);

    private final DangerousnessRealtimeStatisticsMapper dangerousnessRealtimeStatisticsMapper;

    /**
     * 概览统计：所选区域范围内每个区县当天的危险等级、危险性分数与四个要素的分数。
     *
     * <p>不做区域汇总：地市、全省的展示数值由前端按所选行政级别对各区县取平均
     * （与页面「选区县直接展示、选地市取所辖县区平均」的约定一致），接口只保证范围内的县区齐全、
     * 各字段口径一致。当天没有结果的区县照常返回、分数与四个要素为 null（表内有记录但结果为空时
     * 等级为「无数据」），当天一条结果都没有时返回空快照，接口仍返回 200。</p>
     *
     * @param date   统计日期（必填，表内实测只覆盖 2026-09-01—2026-09-30）
     * @param region 所选行政区（全省 / 地市 / 县市区）
     * @return 区县数组 + 范围与有效个数
     */
    public DangerousnessRealtimeOverviewVO overview(LocalDate date, RegionCode region) {
        List<DangerousnessRealtimeCountyVO> counties = counties(date, region);
        long validCountyCount = counties.stream().filter(county -> county.score() != null).count();
        return new DangerousnessRealtimeOverviewVO(date, region.getCode(), region.getLabel(), levelOf(region),
                UNIT_SCORE, counties.size(), (int) validCountyCount,
                counties.toArray(DangerousnessRealtimeCountyVO[]::new));
    }

    /**
     * 风险分数折线图排行：所选区域范围内当天的前八名区县。
     *
     * <p>折线图的 X 轴即返回顺序（分数由高到低），Y 轴为危险性分数，因此不需要时间区间：
     * 一个日期一次排名、一条折线。当天结果为空的区县不参与排名（缺测不是低风险，不占名次），
     * 范围内有效区县不足 8 个时按实际个数返回（选择县市区时只有一个点），
     * 一条有效结果都没有时 items 为空数组，接口仍返回 200。</p>
     *
     * @param date   统计日期（必填，表内实测只覆盖 2026-09-01—2026-09-30）
     * @param region 所选行政区（全省 / 地市 / 县市区）
     * @return 折线图数据（标题、单位与前八名折线点）
     */
    public DangerousnessRealtimeRankingVO scoreRanking(LocalDate date, RegionCode region) {
        List<DangerousnessRealtimeRankingItemVO> items = new ArrayList<>(RANKING_SIZE);
        for (DangerousnessRealtimeCountyVO county : counties(date, region)) {
            // counties 已按分数降序、无结果的排在最后，故遇到第一条无结果即可停止
            if (county.score() == null || items.size() >= RANKING_SIZE) {
                break;
            }
            items.add(new DangerousnessRealtimeRankingItemVO(items.size() + 1, county.code(), county.name(),
                    county.cityName(), county.score(), county.level()));
        }
        return new DangerousnessRealtimeRankingVO(date, region.getCode(), region.getLabel(), levelOf(region),
                RANKING_TITLE, UNIT_SCORE, items.toArray(DangerousnessRealtimeRankingItemVO[]::new));
    }

    /**
     * 所选区域范围内某一天的区县结果：范围内的县区全部占位，表内没有的区县分数为 null。
     *
     * <p>表内出现的县区按「地市名 + 区县名」定位（区划枚举未覆盖的县区名追加在末尾、区划码为 null，
     * 不因缺少代码丢记录）；最后按分数降序排序（无结果的排最后，同值按名称升序），
     * 概览与原行两个接口共用这个顺序。</p>
     */
    private List<DangerousnessRealtimeCountyVO> counties(LocalDate date, RegionCode region) {
        Map<String, Integer> cityCodes = cityCodes();
        // 当日各「地市 + 区县」的记录；主键是 date + county_code，正常情况下一个区县一天只有一行
        Map<String, DangerousnessRealtimeStatistics> rows = new LinkedHashMap<>();
        for (DangerousnessRealtimeStatistics row : dailyRows(date, region)) {
            Integer cityCode = cityCodes.get(row.getCityName());
            if (cityCode == null) {
                continue;
            }
            rows.putIfAbsent(countyKey(cityCode, row.getRegionName()), row);
        }
        List<DangerousnessRealtimeCountyVO> counties = new ArrayList<>();
        for (RegionCode county : RegionCode.selectable(region.getCode())) {
            DangerousnessRealtimeStatistics row =
                    rows.remove(countyKey(county.getCode() / 100 * 100, county.getLabel()));
            counties.add(county(county.getCode(), county.getLabel(), cityNameOf(county), row));
        }
        // 数据里出现、区划枚举未覆盖的县区名（同名跨地市时按键区分过，不会与枚举内的县区混淆）
        rows.values().forEach(row -> counties.add(county(null, row.getRegionName(), row.getCityName(), row)));
        counties.sort(BY_SCORE_DESC);
        return counties;
    }

    /**
     * 本模块的单日取数条件：只取用到的九列（不取 county_code，区划匹配不按它走），
     * 并按所选行政区限定范围。
     *
     * <p>县市区按「所属地市 + 区县名」定位（区划枚举里同名区县分属不同地市，只按名字会串档）；
     * 地市按 city 列定位；全省不加地区条件。地市名与区县名为 NULL 的行无法定位行政区，直接排除。</p>
     */
    private List<DangerousnessRealtimeStatistics> dailyRows(LocalDate date, RegionCode region) {
        LambdaQueryWrapper<DangerousnessRealtimeStatistics> query =
                Wrappers.<DangerousnessRealtimeStatistics>lambdaQuery()
                        .select(DangerousnessRealtimeStatistics::getDate,
                                DangerousnessRealtimeStatistics::getCityName,
                                DangerousnessRealtimeStatistics::getRegionName,
                                DangerousnessRealtimeStatistics::getScore,
                                DangerousnessRealtimeStatistics::getDangerousnessLevel,
                                DangerousnessRealtimeStatistics::getTemperature,
                                DangerousnessRealtimeStatistics::getRelativeHumidity,
                                DangerousnessRealtimeStatistics::getWindSpeed,
                                DangerousnessRealtimeStatistics::getRain)
                        .eq(DangerousnessRealtimeStatistics::getDate, date)
                        .isNotNull(DangerousnessRealtimeStatistics::getCityName)
                        .isNotNull(DangerousnessRealtimeStatistics::getRegionName)
                        .orderByAsc(DangerousnessRealtimeStatistics::getCityName)
                        .orderByAsc(DangerousnessRealtimeStatistics::getRegionName);
        String cityName = cityNameOf(region);
        if (cityName != null) {
            query.eq(DangerousnessRealtimeStatistics::getCityName, cityName);
        }
        if (region.isCounty()) {
            query.eq(DangerousnessRealtimeStatistics::getRegionName, region.getLabel());
        }
        return dangerousnessRealtimeStatisticsMapper.selectList(query);
    }

    /**
     * 单个区县的概览数据：无记录（row 为 null）时分数与等级为 null、四个要素值为 null，
     * 但仍返回该区县，避免「范围内有、结果里没有」。
     */
    private DangerousnessRealtimeCountyVO county(Integer code, String name, String cityName,
                                                DangerousnessRealtimeStatistics row) {
        if (row == null) {
            return new DangerousnessRealtimeCountyVO(code, name, cityName, null, null,
                    elements(null, null, null, null));
        }
        return new DangerousnessRealtimeCountyVO(code, name, cityName,
                row.getScore() == null ? null : row.getScore().doubleValue(), row.getDangerousnessLevel(),
                elements(row.getTemperature(), row.getRelativeHumidity(), row.getWindSpeed(), row.getRain()));
    }

    /**
     * 四个要素的分数，顺序固定：气温、相对湿度、风速、降水（前端按下标取用）。
     */
    private DangerousnessRealtimeElementVO[] elements(Double temperature, Double relativeHumidity,
                                                     Double windSpeed, Double rain) {
        return new DangerousnessRealtimeElementVO[]{
                new DangerousnessRealtimeElementVO(ELEMENT_TEMPERATURE, ELEMENT_TEMPERATURE_NAME,
                        temperature, UNIT_TEMPERATURE),
                new DangerousnessRealtimeElementVO(ELEMENT_RELATIVE_HUMIDITY, ELEMENT_RELATIVE_HUMIDITY_NAME,
                        relativeHumidity, UNIT_RELATIVE_HUMIDITY),
                new DangerousnessRealtimeElementVO(ELEMENT_WIND_SPEED, ELEMENT_WIND_SPEED_NAME,
                        windSpeed, UNIT_WIND_SPEED),
                new DangerousnessRealtimeElementVO(ELEMENT_RAIN, ELEMENT_RAIN_NAME, rain, UNIT_RAIN)};
    }

    /**
     * 地市名到 6 位地市码的对照表；不含省级码（表内没有省级行）。
     */
    private Map<String, Integer> cityCodes() {
        Map<String, Integer> codes = new LinkedHashMap<>();
        for (RegionCode region : RegionCode.values()) {
            if (region != RegionCode.R230000 && !region.isCounty()) {
                codes.put(region.getLabel(), region.getCode());
            }
        }
        return codes;
    }

    /**
     * 行政区所属地市名：县市区取区划码前 4 位对应的地市名，地市取自身名；省级没有地市名（返回 null，
     * 表示不按地市过滤）。区划枚举内没有对应地市时也返回 null。
     */
    private String cityNameOf(RegionCode region) {
        if (!region.isCounty()) {
            return region == RegionCode.R230000 ? null : region.getLabel();
        }
        RegionCode city = RegionCode.fromCode(region.getCode() / 100 * 100);
        return city == null ? null : city.getLabel();
    }

    /**
     * 行政区级别：全省、地市、县市区。
     */
    private String levelOf(RegionCode region) {
        if (region == RegionCode.R230000) {
            return LEVEL_PROVINCE;
        }
        return region.isCounty() ? LEVEL_COUNTY : LEVEL_CITY;
    }

    /**
     * 区县行的索引键：地市码 + 区县名，避免同名区县跨地市串档（如鹤岗市向阳区与佳木斯市向阳区）。
     */
    private String countyKey(int cityCode, String regionName) {
        return "R" + cityCode + " " + regionName;
    }
}
