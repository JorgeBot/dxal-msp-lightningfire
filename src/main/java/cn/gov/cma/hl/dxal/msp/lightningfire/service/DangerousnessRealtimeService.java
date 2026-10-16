package cn.gov.cma.hl.dxal.msp.lightningfire.service;

import cn.gov.cma.hl.dxal.msp.lightningfire.constant.Option;
import cn.gov.cma.hl.dxal.msp.lightningfire.constant.RegionCode;
import cn.gov.cma.hl.dxal.msp.lightningfire.entity.DangerousnessRealtimeStatistics;
import cn.gov.cma.hl.dxal.msp.lightningfire.mapper.DangerousnessRealtimeStatisticsMapper;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.DangerousnessRealtimeCountyDistributionVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.DangerousnessRealtimeCountyVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.DangerousnessRealtimeElementStatVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.DangerousnessRealtimeOverviewVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.DangerousnessRealtimeRankingItemVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.DangerousnessRealtimeRankingVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 危险性实时分析：所选区域在某个日期区间内所选要素的概览统计、各区县要素值与要素排行。
 *
 * <p>数据源为实时表 dangerousness_realtime_statistics（粒度：日期 × 区县，主键 date + county_code），
 * 一天最多 125 行，接口按 since ~ until 一次取回（实测整表 30 天 3750 行，全量读入内存后聚合即可，
 * 不必逐日发查询）。该表与逐日历史评分表
 * {@link cn.gov.cma.hl.dxal.msp.lightningfire.entity.DangerousnessStatistics} 的日期区间不重叠，
 * 两者不能拼成一条连续时间序列。</p>
 *
 * <p>三个接口围绕同一个 {@code element} 取值（见 {@link Option.RealtimeElementOption}：
 * 危险性评分 + 气温 / 相对湿度 / 风速 / 降水），取数口径完全一致：</p>
 * <ul>
 *   <li><b>概览统计</b>：区域内全部「区县 × 日」有效记录的四项统计——最大值、最小值、平均值、
 *       极差（最大值 − 最小值），逐日等权、不按面积或站点数加权，与 /ltg-env 统计概览同一口径；</li>
 *   <li><b>各区县要素值</b>：每县取该要素在区间内的平均值（逐日等权），供地图配色 / 县级横条；
 *       区间只选一天时即该县当天的值；</li>
 *   <li><b>要素排行</b>：单日的区域快照（since = until = 该日期），取该要素当天最高的 8 个区县。</li>
 * </ul>
 *
 * <p>行政区定位按 {@link RegionCode} 的地区名匹配：该表虽然有 county_code 列，但大兴安岭地区的
 * 松岭区 / 新林区 / 呼中区用的是非国标码 232702 / 232703 / 232704，按代码对齐区划枚举会让这三个区
 * 落不进大兴安岭地区的范围，因此用「地市名 + 区县名」定位（同名区县跨地市时靠地市名区分），
 * 返回给前端的区划码取区划枚举的国标码。</p>
 *
 * <p>区县范围：选择地市取所辖县区、选择县市区取该县区本身、选择 230000 取全省 125 个县级单元，
 * 统一走 {@link RegionCode#selectable(Integer)}；范围内的县区全部返回，没有有效记录的值为 null，
 * 不补 0（缺测不是低风险，也不参与任何统计的分母）。</p>
 */
@Service
@RequiredArgsConstructor
public class DangerousnessRealtimeService {

    private static final String LEVEL_PROVINCE = "province";

    private static final String LEVEL_CITY = "city";

    private static final String LEVEL_COUNTY = "county";

    /**
     * 要素排行展示的区县数量（前八名）。
     */
    private static final int RANKING_SIZE = 8;

    /**
     * 要素概览统计四项的标识与名称，顺序固定：最大值、最小值、平均值、极差。
     */
    private static final String STAT_MAX = "max";

    private static final String STAT_MAX_NAME = "最大值";

    private static final String STAT_MIN = "min";

    private static final String STAT_MIN_NAME = "最小值";

    private static final String STAT_AVG = "avg";

    private static final String STAT_AVG_NAME = "平均值";

    private static final String STAT_RANGE = "range";

    private static final String STAT_RANGE_NAME = "极差（最大值 − 最小值）";

    /**
     * 数值降序（无记录的排最后，同值按区县名升序）：各区县要素值与前八名排行都用这个顺序，
     * 前端图表因此不必再排序。
     */
    private static final Comparator<DangerousnessRealtimeCountyVO> BY_VALUE_DESC =
            Comparator.comparing(DangerousnessRealtimeCountyVO::value,
                            Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(DangerousnessRealtimeCountyVO::name);

    private final DangerousnessRealtimeStatisticsMapper dangerousnessRealtimeStatisticsMapper;

    /**
     * 要素概览统计：所选区域在 since ~ until 内所选要素的四项统计。
     *
     * <p>统计样本是区域内全部「区县 × 日」有效记录（不是先按区县取平均再平均）：实测每天每个县区各一行，
     * 两种口径结果一致；按记录统计与 /ltg-env 统计概览同口径，个别县区缺日时也不会让记录少的县区
     * 与其他县区等权。区间内没有任何有效记录时四项统计的值都为 null、有效记录数为 0，接口返回 200。</p>
     *
     * @param since   统计起始日期（含）
     * @param until   统计结束日期（含），需不早于 since（由接口层校验）
     * @param region  所选行政区（全省 / 地市 / 县市区）
     * @param element 要素（危险性评分或四个气象要素之一）
     * @return 要素信息与四项统计
     */
    public DangerousnessRealtimeOverviewVO overview(LocalDate since, LocalDate until, RegionCode region,
                                                    Option.RealtimeElementOption element) {
        List<Double> values = elementValues(since, until, region, element).allValues();
        return new DangerousnessRealtimeOverviewVO(since, until, region.getCode(), region.getLabel(),
                levelOf(region), element.name(), element.getLabel(), element.getUnit(), values.size(),
                statistics(values, element));
    }

    /**
     * 各区县要素值：所选区域内每个区县在 since ~ until 内所选要素的平均值。
     *
     * <p>区间只选一天（since = until）时每县只有一个值，即当天的单日快照，与「每天一个日期」的
     * 页面用法兼容；区间内该县没有有效记录时值为 null、照常返回（countyCount 与 validCountyCount
     * 因此可区分「范围内有多少县」与「有多少县有记录」）。</p>
     *
     * @param since   统计起始日期（含）
     * @param until   统计结束日期（含），需不早于 since（由接口层校验）
     * @param region  所选行政区（全省 / 地市 / 县市区）
     * @param element 要素（危险性评分或四个气象要素之一）
     * @return 区县数组 + 范围与有效个数
     */
    public DangerousnessRealtimeCountyDistributionVO counties(LocalDate since, LocalDate until, RegionCode region,
                                                             Option.RealtimeElementOption element) {
        List<DangerousnessRealtimeCountyVO> counties =
                sortedCounties(region, elementValues(since, until, region, element), element);
        long validCountyCount = counties.stream().filter(county -> county.value() != null).count();
        return new DangerousnessRealtimeCountyDistributionVO(since, until, region.getCode(), region.getLabel(),
                levelOf(region), element.name(), element.getLabel(), element.getUnit(),
                counties.size(), (int) validCountyCount, counties.toArray(DangerousnessRealtimeCountyVO[]::new));
    }

    /**
     * 要素排行：所选区域在 date 当天所选要素最高的 8 个区县。
     *
     * <p>排行榜是单日快照（与概览统计、各区县要素值共用取数口径，只是把区间收成一天），
     * X 轴即返回顺序（数值由高到低），因此不需要时间区间：一个日期一次排名。
     * 当天缺测的区县不参与排名（缺测不是低值，不占名次），范围内有效区县不足 8 个时按实际个数返回
     * （选择县市区时只有一个点），一条有效值都没有时 items 为空数组，接口仍返回 200。</p>
     *
     * @param date    统计日期（必填，表内实测只覆盖 2026-09-01—2026-09-30）
     * @param region  所选行政区（全省 / 地市 / 县市区）
     * @param element 要素（危险性评分或四个气象要素之一）
     * @return 图表标题、单位与排行数据项
     */
    public DangerousnessRealtimeRankingVO ranking(LocalDate date, RegionCode region,
                                                  Option.RealtimeElementOption element) {
        List<DangerousnessRealtimeRankingItemVO> items = new ArrayList<>(RANKING_SIZE);
        for (DangerousnessRealtimeCountyVO county : sortedCounties(region,
                elementValues(date, date, region, element), element)) {
            // 已按数值降序、无记录的排在最后，故遇到第一条无记录即可停止
            if (county.value() == null || items.size() >= RANKING_SIZE) {
                break;
            }
            items.add(new DangerousnessRealtimeRankingItemVO(items.size() + 1, county.code(), county.name(),
                    county.cityName(), county.value()));
        }
        return new DangerousnessRealtimeRankingVO(date, region.getCode(), region.getLabel(), levelOf(region),
                element.name(), element.getLabel(), element.getUnit(),
                element.getLabel() + "排行（前 " + RANKING_SIZE + "）",
                items.toArray(DangerousnessRealtimeRankingItemVO[]::new));
    }

    /**
     * 所选区域在区间内该要素的取数结果：按「地市 + 区县」累计的值，以及全部有效值。
     *
     * <p>只跳过两类记录：该要素为 NULL 的（缺测，不参与任何统计）与地市名不在区划枚举内的
     * （无法定位行政区，正常数据不会出现）。</p>
     */
    private ElementValues elementValues(LocalDate since, LocalDate until, RegionCode region,
                                        Option.RealtimeElementOption element) {
        Map<String, Integer> cityCodes = cityCodes();
        Map<String, CountyValues> counties = new LinkedHashMap<>();
        List<Double> allValues = new ArrayList<>();
        for (DangerousnessRealtimeStatistics row : rows(since, until, region)) {
            Double value = value(row, element);
            Integer cityCode = cityCodes.get(row.getCityName());
            if (value == null || cityCode == null) {
                continue;
            }
            counties.merge(countyKey(cityCode, row.getRegionName()),
                    new CountyValues(row.getRegionName(), row.getCityName(), value, 1L), CountyValues::plus);
            allValues.add(value);
        }
        return new ElementValues(counties, allValues);
    }

    /**
     * 所选区域内各区县的要素值，按数值降序（无记录的排最后）。
     *
     * <p>范围内的县区全部占位（表内没有的为 null）；表内出现、区划枚举未覆盖的县区名追加在末尾、
     * 区划码为 null，不因缺少代码丢记录。区间平均按要素的小数位数抹掉 double 累加的尾差。</p>
     */
    private List<DangerousnessRealtimeCountyVO> sortedCounties(RegionCode region, ElementValues values,
                                                               Option.RealtimeElementOption element) {
        Map<String, CountyValues> byCounty = new LinkedHashMap<>(values.counties());
        List<DangerousnessRealtimeCountyVO> counties = new ArrayList<>();
        for (RegionCode county : RegionCode.selectable(region.getCode())) {
            CountyValues countyValues =
                    byCounty.remove(countyKey(county.getCode() / 100 * 100, county.getLabel()));
            counties.add(new DangerousnessRealtimeCountyVO(county.getCode(), county.getLabel(),
                    cityNameOf(county), round(countyValues == null ? null : countyValues.mean(), element)));
        }
        // 数据里出现、区划枚举未覆盖的县区名（同名跨地市时按键区分过，不会与枚举内的区县混淆）
        byCounty.values().forEach(county -> counties.add(new DangerousnessRealtimeCountyVO(
                null, county.name(), county.cityName(), round(county.mean(), element))));
        counties.sort(BY_VALUE_DESC);
        return counties;
    }

    /**
     * 要素概览统计的四项，顺序固定：最大值、最小值、平均值、极差（最大值 − 最小值）。
     * 没有任何有效值时四项均为 null（极差同理，不补 0）。
     *
     * <p>最大值 / 最小值是源列原值，平均值与极差按要素的小数位数（见
     * {@link Option.RealtimeElementOption#getScale()}）抹掉 double 累加的尾差。</p>
     */
    private DangerousnessRealtimeElementStatVO[] statistics(List<Double> values,
                                                            Option.RealtimeElementOption element) {
        Double max = values.stream().max(Comparator.naturalOrder()).orElse(null);
        Double min = values.stream().min(Comparator.naturalOrder()).orElse(null);
        String unit = element.getUnit();
        return new DangerousnessRealtimeElementStatVO[]{
                new DangerousnessRealtimeElementStatVO(STAT_MAX, STAT_MAX_NAME, round(max, element), unit),
                new DangerousnessRealtimeElementStatVO(STAT_MIN, STAT_MIN_NAME, round(min, element), unit),
                new DangerousnessRealtimeElementStatVO(STAT_AVG, STAT_AVG_NAME,
                        round(mean(values), element), unit),
                new DangerousnessRealtimeElementStatVO(STAT_RANGE, STAT_RANGE_NAME,
                        max == null || min == null ? null : round(max - min, element), unit)};
    }

    /**
     * 按要素的小数位数（源列 numeric 的 scale）四舍五入，抹掉 double 累加的尾差
     * （如 30 天气温平均 7.072761000000004 → 7.072761）；值为 null 时返回 null。
     */
    private Double round(Double value, Option.RealtimeElementOption element) {
        return value == null ? null
                : BigDecimal.valueOf(value).setScale(element.getScale(), RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 本模块的取数条件：只取用到的八列（不取 county_code，区划匹配不按它走，也不取危险等级文案），
     * 并按所选行政区限定范围。
     *
     * <p>县市区按「所属地市 + 区县名」定位（区划枚举里同名区县分属不同地市，只按名字会串档）；
     * 地市按 city 列定位；全省不加地区条件。地市名与区县名为 NULL 的行无法定位行政区，直接排除。
     * 单日查询（排行）传 since = until = 该日期即可。</p>
     */
    private List<DangerousnessRealtimeStatistics> rows(LocalDate since, LocalDate until, RegionCode region) {
        LambdaQueryWrapper<DangerousnessRealtimeStatistics> query =
                Wrappers.<DangerousnessRealtimeStatistics>lambdaQuery()
                        .select(DangerousnessRealtimeStatistics::getDate,
                                DangerousnessRealtimeStatistics::getCityName,
                                DangerousnessRealtimeStatistics::getRegionName,
                                DangerousnessRealtimeStatistics::getScore,
                                DangerousnessRealtimeStatistics::getTemperature,
                                DangerousnessRealtimeStatistics::getRelativeHumidity,
                                DangerousnessRealtimeStatistics::getWindSpeed,
                                DangerousnessRealtimeStatistics::getRain)
                        .ge(DangerousnessRealtimeStatistics::getDate, since)
                        .le(DangerousnessRealtimeStatistics::getDate, until)
                        .isNotNull(DangerousnessRealtimeStatistics::getCityName)
                        .isNotNull(DangerousnessRealtimeStatistics::getRegionName)
                        .orderByAsc(DangerousnessRealtimeStatistics::getDate)
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
     * 行内取所选要素的值：与 {@link Option.RealtimeElementOption} 的五个取值一一对应，
     * 值缺测（该要素列为 NULL）时返回 null，由调用方跳过或留空。
     */
    private Double value(DangerousnessRealtimeStatistics row, Option.RealtimeElementOption element) {
        return switch (element) {
            case score -> row.getScore() == null ? null : row.getScore().doubleValue();
            case temperature -> row.getTemperature();
            case relativeHumidity -> row.getRelativeHumidity();
            case windSpeed -> row.getWindSpeed();
            case rain -> row.getRain();
        };
    }

    /**
     * 一组数值的等权均值，忽略 null；为空（区间内没有有效记录）时返回 null。
     */
    private Double mean(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        double sum = 0D;
        for (Double value : values) {
            sum += value;
        }
        return sum / values.size();
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

    /**
     * 区间内所选要素的取数结果。
     *
     * @param counties  区县键 → 该县区间内的值累计（含名称与所属地市）
     * @param allValues 范围内全部有效记录的值（四项统计的样本）
     */
    private record ElementValues(Map<String, CountyValues> counties, List<Double> allValues) {
    }

    /**
     * 单个区县在区间内所选要素的累计值：名称、所属地市、求和与记录条数（均值按记录数等权）。
     */
    private record CountyValues(String name, String cityName, double sum, long count) {

        /**
         * 合并该区县区间内的多条记录（一天一条）。
         */
        CountyValues plus(CountyValues other) {
            return new CountyValues(name, cityName, sum + other.sum, count + other.count);
        }

        /**
         * 该区县区间内的平均值；没有有效记录时返回 null。
         */
        Double mean() {
            return count == 0L ? null : sum / count;
        }
    }
}
