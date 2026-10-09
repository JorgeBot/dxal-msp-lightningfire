package cn.gov.cma.hl.dxal.msp.lightningfire.service;

import cn.gov.cma.hl.dxal.msp.lightningfire.constant.Option;
import cn.gov.cma.hl.dxal.msp.lightningfire.constant.RegionCode;
import cn.gov.cma.hl.dxal.msp.lightningfire.entity.DangerousnessAnnualSummary;
import cn.gov.cma.hl.dxal.msp.lightningfire.entity.DangerousnessStatistics;
import cn.gov.cma.hl.dxal.msp.lightningfire.entity.ExposureCompositeSummary;
import cn.gov.cma.hl.dxal.msp.lightningfire.entity.VulnerabilityCompositeSummary;
import cn.gov.cma.hl.dxal.msp.lightningfire.mapper.DangerousnessAnnualSummaryMapper;
import cn.gov.cma.hl.dxal.msp.lightningfire.mapper.DangerousnessStatisticsMapper;
import cn.gov.cma.hl.dxal.msp.lightningfire.mapper.ExposureCompositeSummaryMapper;
import cn.gov.cma.hl.dxal.msp.lightningfire.mapper.VulnerabilityCompositeSummaryMapper;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.RiskAssessmentBinVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.RiskAssessmentChartItemVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.RiskAssessmentChartVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.RiskAssessmentComponentVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.RiskAssessmentComponentsVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.RiskAssessmentStatisticsVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 综合风险评估与区划：按评价年份与行政区给出危险性评分的区域指数、评分区间占比与区县对比。
 *
 * <p>数据源为危险性统计表 dangerousness_statistics 的 score 列（0—100 百分制，数值越高越危险），
 * 不读栅格、也不做 AHP 合成：本模块的区域评分指数就是该区在评价年份内有记录日期的评分均值。</p>
 *
 * <p>该表的粒度是「日期 × 区县 × 地级市」，两列地区名各司其职：region 列只放区县名
 * （如漠河市、加格达奇区），city 列放地市名（全省 13 个地市），表内没有省级行。因此：</p>
 * <ul>
 *   <li>县市区：按所选地市 + 区县名定位该区县的记录；</li>
 *   <li>地市：取所辖县区的记录，评分指数为各县区评分均值的等权均值（先按区县算均值，再取均值）；</li>
 *   <li>全省：取各地市的记录（由当地县区记录汇总出地市均值），指数为各地市均值的等权均值；</li>
 *   <li>区县名在各地市间不重名，但仍按「地市 + 区县名」定位，口径与承载体暴露度、脆弱性模块
 *       的「县区码前 4 位即所属地市」一致。</li>
 * </ul>
 *
 * <p>评价年份是该表的自然年；表内覆盖 2018—2024，其中 2020 年只有 34 天记录（源数据缺日），
 * 本模块不补 0、不跨年替代，用 validDays / coverage 提示完整性。区域指数按行政区等权，
 * 表中没有面积列与逐格数据，故不做面积加权。</p>
 *
 * <p>评分区间占比取 0—100 等宽 20 分五档（[0,20,40,60,80,100]，前四档左闭右开、末档含上界），
 * 分母为该选区内有记录的日数，即「有记录日占比」，不是面积占比：危险性统计不提供网格或面积口径，
 * 不能据此推算各级风险面积。</p>
 *
 * <p>validDays 与 coverage（完整率）按所选范围内的行政区取平均：地市、全省不把所辖县区的记录日数
 * 相加，否则两者会随县区数量放大而失去完整性提示的意义。它们只用于提示源数据的完整程度，
 * 不参与评分指数与区间占比的计算（区间占比的分母始终是选区内实际记录条数）。</p>
 */
@Service
@RequiredArgsConstructor
public class RiskAssessmentService {

    private static final String LEVEL_PROVINCE = "province";

    private static final String LEVEL_CITY = "city";

    private static final String LEVEL_COUNTY = "county";

    /**
     * 区县对比柱状图展示的行政区数量：选择全省取各地市前 5，选择地市取所辖县区前 5。
     */
    private static final int TOP_SIZE = 5;

    /**
     * 评分区间边界：0—100 等宽 20 分五档，前四档左闭右开、末档含上界 100；
     * 档位定义与接口选项共用 {@link Option.RiskGradeOption}。
     */
    private static final Option.RiskGradeOption[] GRADES = Option.RiskGradeOption.values();

    private static final String UNIT_SCORE = "分";

    /**
     * 分项指数的单位说明：H、E、V 都是 0—1 的归一化指数，与 0—100 的评分不是同一量纲。
     */
    private static final String UNIT_INDEX = "指数 0—1";

    /**
     * H/E/V 分项指数的指标代码与名称。
     */
    private static final String COMPONENT_HAZARD = "H";

    private static final String COMPONENT_HAZARD_NAME = "危险性 H";

    private static final String COMPONENT_EXPOSURE = "E";

    private static final String COMPONENT_EXPOSURE_NAME = "暴露度 E";

    private static final String COMPONENT_VULNERABILITY = "V";

    private static final String COMPONENT_VULNERABILITY_NAME = "脆弱性 V";

    /**
     * H/E/V 分项指数的数据来源说明，逐项写清取哪张表的哪一列，避免与评分统计混用。
     */
    private static final String HAZARD_SOURCE = "dangerousness_annual_summary.mean（逐年 H 均值，与方案无关）";

    private static final String EXPOSURE_SOURCE = "exposure_composite_summary.region_value（按 schemeId 取方案）";

    private static final String VULNERABILITY_SOURCE =
            "vulnerability_composite_summary.mean（按 schemeId 取方案，仅 2018—2022 有记录）";

    /**
     * 评分均值降序；均值相同时保持行政区名称升序（查询已按名称排序），排名结果稳定。
     */
    private static final Comparator<RegionRecord> BY_SCORE_DESC =
            Comparator.comparing(RegionRecord::score).reversed();

    private final DangerousnessStatisticsMapper dangerousnessStatisticsMapper;

    private final DangerousnessAnnualSummaryMapper dangerousnessAnnualSummaryMapper;

    private final ExposureCompositeSummaryMapper exposureCompositeSummaryMapper;

    private final VulnerabilityCompositeSummaryMapper vulnerabilityCompositeSummaryMapper;

    /**
     * H 逐年均值栅格的存放目录；未配置时按交接包约定取 {@code {file-root.risk-assessment}/H_annual_mean_TIFF}。
     */
    @Value("${file-root.risk-assessment-h:}")
    private String hazardRasterRoot;

    /**
     * 交接包根目录：{@code {年度}/{指标}.tif} 与 H 年度均值目录所在目录。
     */
    @Value("${file-root.risk-assessment}")
    private String indicatorDataRoot;

    /**
     * 危险性 H 的逐年均值栅格文件。
     *
     * <p>交接包把 H 年度均值单独放在 {@code H_annual_mean_TIFF} 目录，文件名为
     * {@code H_mean_{评价年份}.tif}（如 H_mean_2018.tif），与逐日 H 目录 {@code H_daily} 分开；
     * 目录内 README 说明了各年可用日数（2018: 359、2019: 359、2020: 34、2021: 361、2022: 363、
     * 2023: 365、2024: 366），2020 年只是这 34 天的均值，不是完整年度观测。</p>
     *
     * <p>取数顺序：先看 {@code file-root.risk-assessment-h}（若配置且存在），否则按交接包约定取
     * {@code {file-root.risk-assessment}/H_annual_mean_TIFF}，再退回交接包根目录下的
     * {@code H_annual_mean_TIFF}。H 栅格与行政区划、AHP 方案都无关，只有年份参与定位，
     * 故本方法不接受 regionCode 与 schemeId。没有该年份文件时返回 null，由接口层返回 404，
     * 不用其他年份的文件冒充。</p>
     *
     * @return 栅格文件路径；文件不存在或解析结果越出交接包根目录时返回 null
     */
    public Path hazardRaster(short assessmentYear) {
        if (indicatorDataRoot == null || indicatorDataRoot.isBlank()) {
            return null;
        }
        String fileName = "H_mean_" + assessmentYear + ".tif";
        Path handoffRoot = Path.of(indicatorDataRoot).toAbsolutePath().normalize();
        for (Path root : hazardRasterRoots(handoffRoot)) {
            Path file = root.resolve(fileName).normalize();
            if (file.startsWith(handoffRoot) && Files.isRegularFile(file)) {
                return file;
            }
        }
        return null;
    }

    /**
     * 候选的 H 年度均值目录，按优先级排列：显式配置的目录、交接包根目录下的约定子目录。
     *
     * <p>所有候选都限定在交接包根目录之内，避免配置写错时把任意路径当作数据文件返回。</p>
     */
    private List<Path> hazardRasterRoots(Path handoffRoot) {
        List<Path> roots = new ArrayList<>(2);
        if (hazardRasterRoot != null && !hazardRasterRoot.isBlank()) {
            Path configured = Path.of(hazardRasterRoot).toAbsolutePath().normalize();
            if (configured.startsWith(handoffRoot)) {
                roots.add(configured);
            }
        }
        roots.add(handoffRoot.resolve("H_annual_mean_TIFF").normalize());
        return roots;
    }

    /**
     * H/E/V 分项指数：与评分统计输入相同（年份 + 行政区），另可按 schemeId 取暴露度与脆弱度。
     *
     * <p>三项分别取自各自汇总表，不做加权合成，也不由评分反推：</p>
     * <ul>
     *   <li>危险性 H：dangerousness_annual_summary.mean（0—1，与方案无关）；</li>
     *   <li>暴露度 E：exposure_composite_summary.region_value（按 schemeId 取方案，0—1）；</li>
     *   <li>脆弱性 V：vulnerability_composite_summary.mean（按 schemeId 取方案，0—1，
     *       仅 2018—2022 有记录）。</li>
     * </ul>
     *
     * <p>缺数区分：该年份、该区划的任何一项没有记录时该项返回 null（不补 0）；三项都没有记录时
     * 整体返回 null，由接口层返回 404。</p>
     *
     * @param assessmentYear 评价年份（自然年，2018—2024）
     * @param regionCode     行政区划代码
     * @param schemeId       AHP 方案号 0—6，仅暴露度与脆弱性受影响
     * @return H/E/V 分项指数；三项都取不到时返回 null
     */
    public RiskAssessmentComponentsVO riskComponents(short assessmentYear, int regionCode, short schemeId) {
        DangerousnessAnnualSummary hazard = dangerousnessAnnualSummaryMapper.selectOne(
                Wrappers.<DangerousnessAnnualSummary>lambdaQuery()
                        .eq(DangerousnessAnnualSummary::getAssessmentYear, assessmentYear)
                        .eq(DangerousnessAnnualSummary::getRegionCode, regionCode));
        ExposureCompositeSummary exposure = exposureCompositeSummaryMapper.selectOne(
                Wrappers.<ExposureCompositeSummary>lambdaQuery()
                        .eq(ExposureCompositeSummary::getAssessmentYear, assessmentYear)
                        .eq(ExposureCompositeSummary::getRegionCode, regionCode)
                        .eq(ExposureCompositeSummary::getSchemeId, schemeId));
        VulnerabilityCompositeSummary vulnerability = vulnerabilityCompositeSummaryMapper.selectOne(
                Wrappers.<VulnerabilityCompositeSummary>lambdaQuery()
                        .eq(VulnerabilityCompositeSummary::getAssessmentYear, assessmentYear)
                        .eq(VulnerabilityCompositeSummary::getRegionCode, regionCode)
                        .eq(VulnerabilityCompositeSummary::getSchemeId, schemeId));
        if (hazard == null && exposure == null && vulnerability == null) {
            return null;
        }
        RegionCode region = RegionCode.fromCode(regionCode);
        String regionName = firstNonNull(hazard == null ? null : hazard.getRegionName(),
                exposure == null ? null : exposure.getRegionName(),
                vulnerability == null ? null : vulnerability.getRegionName(),
                region == null ? null : region.getLabel());
        String regionLevel = firstNonNull(hazard == null ? null : hazard.getRegionLevel(),
                exposure == null ? null : exposure.getRegionLevel(),
                vulnerability == null ? null : vulnerability.getRegionLevel(),
                region == null ? null : levelOf(region));
        RiskAssessmentComponentVO[] components = {
                new RiskAssessmentComponentVO(COMPONENT_HAZARD, COMPONENT_HAZARD_NAME,
                        hazard == null ? null : hazard.getMean(), UNIT_INDEX, HAZARD_SOURCE),
                new RiskAssessmentComponentVO(COMPONENT_EXPOSURE, COMPONENT_EXPOSURE_NAME,
                        exposure == null ? null : exposure.getRegionValue(), UNIT_INDEX, EXPOSURE_SOURCE),
                new RiskAssessmentComponentVO(COMPONENT_VULNERABILITY, COMPONENT_VULNERABILITY_NAME,
                        vulnerability == null ? null : vulnerability.getMean(), UNIT_INDEX,
                        VULNERABILITY_SOURCE)};
        return new RiskAssessmentComponentsVO(assessmentYear, regionCode, regionName, regionLevel,
                schemeId, components);
    }

    /**
     * 依次返回第一个非 null 值；全部为 null 时返回 null。
     */
    private String firstNonNull(String... values) {
        for (String value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * 行政区级别：全省、地市、县市区，用于汇总表未给出 region_level 时兜底。
     */
    private String levelOf(RegionCode region) {
        if (region == RegionCode.R230000) {
            return LEVEL_PROVINCE;
        }
        return region.isCounty() ? LEVEL_COUNTY : LEVEL_CITY;
    }

    /**
     * 综合风险评估统计：区域评分指数、评分区间占比与区县对比柱状图。
     *
     * @param assessmentYear 评价年份（自然年，2018—2024 有数据）
     * @param region         行政区（全省 / 地市 / 县市区）
     * @return 统计结果；该年份与行政区内没有评分记录时返回 null，由接口层返回失败响应
     */
    public RiskAssessmentStatisticsVO riskAssessmentStatistics(short assessmentYear, RegionCode region) {
        Map<String, RegionRecord> records = regionRecords(assessmentYear);
        if (records.isEmpty()) {
            return null;
        }
        if (region == RegionCode.R230000) {
            List<RegionRecord> cities = records.values().stream()
                    .filter(record -> LEVEL_CITY.equals(record.level()))
                    .toList();
            List<RegionRecord> counties = records.values().stream()
                    .filter(record -> LEVEL_COUNTY.equals(record.level()))
                    .toList();
            return cities.isEmpty() ? null : build(assessmentYear, region, LEVEL_PROVINCE, cities,
                    "地市评分均值对比（前 5）", cities, counties);
        }
        if (region.isCounty()) {
            RegionRecord county = record(records, region);
            if (county == null) {
                return null;
            }
            return build(assessmentYear, region, LEVEL_COUNTY, List.of(county), "县区评分均值（所选县区）",
                    List.of(county), List.of(county));
        }
        RegionRecord city = record(records, region);
        if (city == null) {
            return null;
        }
        List<RegionRecord> counties = records.values().stream()
                .filter(record -> LEVEL_COUNTY.equals(record.level()))
                .filter(record -> Objects.equals(record.cityCode(), region.getCode()))
                .toList();
        return build(assessmentYear, region, LEVEL_CITY, List.of(city),
                "区县评分均值对比（前 5）", counties, counties);
    }

    /**
     * 汇总输出。
     *
     * @param selected 参与评分指数与区间占比的行政区：全省与地市取汇总记录本身，县区取该区县
     * @param charts   对比图候选：全省取各地市，地市取所辖县区，县区取该区县
     * @param units    记录日数与完整率的取平均单位，统一用区县行：全省、地市为所辖县区，
     *                 县区为该区县本身，保证各级的完整率口径一致（全省不会因先按地市取平均
     *                 而把 13 个地市加总成数千个百分点）
     */
    private RiskAssessmentStatisticsVO build(short assessmentYear, RegionCode region, String level,
                                             List<RegionRecord> selected, String title,
                                             List<RegionRecord> charts, List<RegionRecord> units) {
        int expectedDays = expectedDays(assessmentYear);
        long validDays = Math.round(units.stream()
                .mapToLong(RegionRecord::validDays).average().orElseThrow());
        double coverage = units.stream()
                .map(record -> coverage(record.validDays(), expectedDays))
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0D);
        return new RiskAssessmentStatisticsVO(
                assessmentYear,
                region.getCode(),
                region.getLabel(),
                level,
                meanScore(selected),
                bins(selected),
                validDays,
                expectedDays,
                coverage,
                topChart(charts, title));
    }

    /**
     * 所选行政区自身的记录：地市取 city 列汇总行，县区按「地市 + 区县名」定位。
     *
     * <p>县区以区县码前 4 位对应的地市定位，避免同名区县串到别的地市；区划名称与表内名称不一致时
     * 退回按区县名匹配，仍取不到则返回 null（接口按无数据处理）。</p>
     */
    private RegionRecord record(Map<String, RegionRecord> records, RegionCode region) {
        if (!region.isCounty()) {
            return records.get(cityKey(region.getLabel()));
        }
        RegionRecord byCityAndName = records.get(countyKey(region.getCode() / 100 * 100, region.getLabel()));
        if (byCityAndName != null) {
            return byCityAndName;
        }
        return records.values().stream()
                .filter(candidate -> LEVEL_COUNTY.equals(candidate.level()))
                .filter(candidate -> candidate.name().equals(region.getLabel()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 区县评分均值对比柱状图：选择全省返回各地市前 5，选择地市返回所辖县区前 5，选择县区返回其本身。
     *
     * <p>只展示本年份有评分记录的行政区：记录缺失的区县不参与排名，不以 0 参与，
     * 避免把缺测读成低风险。</p>
     */
    private RiskAssessmentChartVO topChart(List<RegionRecord> candidates, String title) {
        RiskAssessmentChartItemVO[] items = candidates.stream()
                .sorted(BY_SCORE_DESC)
                .limit(TOP_SIZE)
                .map(record -> new RiskAssessmentChartItemVO(null, record.name(), record.score(), null))
                .toArray(RiskAssessmentChartItemVO[]::new);
        return new RiskAssessmentChartVO(title, UNIT_SCORE, items);
    }

    /**
     * 区域评分指数：选区内各行政区评分均值的等权均值。
     */
    private Double meanScore(List<RegionRecord> records) {
        return records.stream().mapToDouble(RegionRecord::score).average().orElseThrow();
    }

    /**
     * 评分区间记录日数与占比，分母为选区内有记录的日数。
     */
    private RiskAssessmentBinVO[] bins(List<RegionRecord> records) {
        long[] recordDays = new long[GRADES.length];
        long validDays = 0L;
        for (RegionRecord record : records) {
            for (int i = 0; i < recordDays.length; i++) {
                recordDays[i] += record.binDays()[i];
            }
            validDays += record.validDays();
        }
        RiskAssessmentBinVO[] bins = new RiskAssessmentBinVO[recordDays.length];
        for (int i = 0; i < recordDays.length; i++) {
            Option.RiskGradeOption grade = GRADES[i];
            bins[i] = new RiskAssessmentBinVO(grade.getBinLabel(),
                    (double) grade.getLowerBound(), (double) grade.getUpperBound(),
                    recordDays[i], share(recordDays[i], validDays));
        }
        return bins;
    }

    /**
     * 该评价年份的日历天数（闰年为 366），作为完整覆盖的参考分母。
     */
    private int expectedDays(short assessmentYear) {
        return LocalDate.of(assessmentYear, 1, 1).lengthOfYear();
    }

    /**
     * 完整率（%）= 有记录日数 ÷ 该年日历天数 × 100；日历天数不大于 0 时返回 null。
     */
    private Double coverage(long validDays, int expectedDays) {
        return expectedDays <= 0 ? null : validDays * 100D / expectedDays;
    }

    /**
     * 该年份各行政区的评分记录，地市行与区县行各建索引；没有任何记录时返回空 Map。
     *
     * <p>只取 day、地市名、区县名与评分四列，避免把整表的文本列都读进来；分组、地市汇总、
     * 均值与分档都在内存中完成，与承载体暴露度、脆弱性模块「读汇总数据、不在接口里现算栅格统计」
     * 的做法一致。</p>
     *
     * <p>地市行由该地市下各县区的记录汇总而来：地市评分均值为各县区均值的等权均值，地市有记录日数
     * 为各县区有记录日数之和；返回给接口时再按行政区取平均（见 {@code build}），因此地市、全省的
     * validDays 是「平均一个县区 / 一个地市」的记录日数，而不是全省合计。</p>
     */
    private Map<String, RegionRecord> regionRecords(short assessmentYear) {
        LambdaQueryWrapper<DangerousnessStatistics> query =
                Wrappers.<DangerousnessStatistics>lambdaQuery()
                        .select(DangerousnessStatistics::getRegionName, DangerousnessStatistics::getCityName,
                                DangerousnessStatistics::getScore)
                        .ge(DangerousnessStatistics::getDate, LocalDate.of(assessmentYear, 1, 1))
                        .le(DangerousnessStatistics::getDate, LocalDate.of(assessmentYear, 12, 31))
                        .isNotNull(DangerousnessStatistics::getRegionName)
                        .isNotNull(DangerousnessStatistics::getCityName)
                        .isNotNull(DangerousnessStatistics::getScore)
                        .orderByAsc(DangerousnessStatistics::getCityName)
                        .orderByAsc(DangerousnessStatistics::getRegionName);
        // 地市名 → 地市码：一次建表，避免逐行遍历区划枚举（单年最多 3 万余行）
        Map<String, Integer> cityCodes = cityCodes();
        // 区县累计：键为「地市码 + 区县名」
        Map<String, Long> countyRecordDays = new LinkedHashMap<>();
        Map<String, Double> countyScoreSums = new LinkedHashMap<>();
        Map<String, long[]> countyBinDays = new LinkedHashMap<>();
        Map<String, String> countyNames = new LinkedHashMap<>();
        Map<String, Integer> countyCityCodes = new LinkedHashMap<>();
        for (DangerousnessStatistics row : dangerousnessStatisticsMapper.selectList(query)) {
            String regionName = row.getRegionName();
            String cityName = row.getCityName();
            BigDecimal score = row.getScore();
            if (regionName == null || cityName == null || score == null) {
                continue;
            }
            Integer cityCode = cityCodes.get(cityName);
            if (cityCode == null) {
                continue;
            }
            String key = countyKey(cityCode, regionName);
            double value = score.doubleValue();
            countyRecordDays.merge(key, 1L, Long::sum);
            countyScoreSums.merge(key, value, Double::sum);
            countyBinDays.computeIfAbsent(key, ignored -> new long[GRADES.length])[bin(value)]++;
            countyNames.putIfAbsent(key, regionName);
            countyCityCodes.putIfAbsent(key, cityCode);
        }
        Map<String, RegionRecord> records = new LinkedHashMap<>();
        // 地市累计：由所辖县区行汇总
        Map<Integer, Double> cityScoreSums = new LinkedHashMap<>();
        Map<Integer, Long> cityRecordDays = new LinkedHashMap<>();
        Map<Integer, long[]> cityBinDays = new LinkedHashMap<>();
        Map<Integer, Integer> cityCountyCounts = new LinkedHashMap<>();
        countyRecordDays.forEach((key, days) -> {
            int cityCode = countyCityCodes.get(key);
            double mean = countyScoreSums.get(key) / days;
            records.put(key, new RegionRecord(countyNames.get(key), cityCode, LEVEL_COUNTY, mean, days,
                    countyBinDays.get(key)));
            cityScoreSums.merge(cityCode, mean, Double::sum);
            cityRecordDays.merge(cityCode, days, Long::sum);
            cityCountyCounts.merge(cityCode, 1, Integer::sum);
            long[] cityBins = cityBinDays.computeIfAbsent(cityCode, ignored -> new long[GRADES.length]);
            for (int i = 0; i < cityBins.length; i++) {
                cityBins[i] += countyBinDays.get(key)[i];
            }
        });
        cityScoreSums.forEach((cityCode, sum) -> {
            String cityName = cityLabel(cityCode);
            records.put(cityKey(cityName), new RegionRecord(cityName, cityCode, LEVEL_CITY,
                    sum / cityCountyCounts.get(cityCode), cityRecordDays.get(cityCode),
                    cityBinDays.get(cityCode)));
        });
        return records;
    }

    /**
     * 地市行的索引键。
     */
    private String cityKey(String cityName) {
        return "C " + cityName;
    }

    /**
     * 区县行的索引键：地市码 + 区县名，避免同名区县跨地市串档。
     */
    private String countyKey(int cityCode, String regionName) {
        return "R" + cityCode + " " + regionName;
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
     * 6 位地市码对应的地市名。
     */
    private String cityLabel(int cityCode) {
        RegionCode region = RegionCode.fromCode(cityCode);
        return region == null ? String.valueOf(cityCode) : region.getLabel();
    }

    /**
     * 评分所属区间下标：前四档左闭右开，末档含上界 100；评分超出 0—100 时并入相邻端点档。
     */
    private int bin(double score) {
        for (int i = 0; i < GRADES.length; i++) {
            if (score < GRADES[i].getUpperBound()) {
                return i;
            }
        }
        return GRADES.length - 1;
    }

    /**
     * 占比（%）= 分项值 ÷ 合计 × 100；合计为 0 时返回 null。
     */
    private Double share(long value, long total) {
        return total <= 0L ? null : value * 100D / total;
    }

    /**
     * 单个行政区的评分汇总：名称、所属地市码、级别、评分均值、有记录日数与五档记录日数。
     */
    private record RegionRecord(String name, int cityCode, String level, double score, long validDays,
                                long[] binDays) {
    }
}
