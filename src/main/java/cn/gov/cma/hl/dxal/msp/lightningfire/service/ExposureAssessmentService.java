package cn.gov.cma.hl.dxal.msp.lightningfire.service;

import cn.gov.cma.hl.dxal.msp.lightningfire.constant.Option;
import cn.gov.cma.hl.dxal.msp.lightningfire.entity.ExposureCompositeSummary;
import cn.gov.cma.hl.dxal.msp.lightningfire.entity.ForestFractionSummary;
import cn.gov.cma.hl.dxal.msp.lightningfire.entity.RoadExposureSummary;
import cn.gov.cma.hl.dxal.msp.lightningfire.entity.SettlementFractionSummary;
import cn.gov.cma.hl.dxal.msp.lightningfire.mapper.ExposureCompositeSummaryMapper;
import cn.gov.cma.hl.dxal.msp.lightningfire.mapper.ForestFractionSummaryMapper;
import cn.gov.cma.hl.dxal.msp.lightningfire.mapper.RoadExposureSummaryMapper;
import cn.gov.cma.hl.dxal.msp.lightningfire.mapper.SettlementFractionSummaryMapper;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.ExposureBinVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.ExposureChartItemVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.ExposureChartVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.ExposureStatisticsVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 承载体暴露度：森林覆盖暴露度、居民地面占比、道路暴露度密度与综合暴露度的指标评估。
 *
 * <p>数值全部读取交接包的年度统计汇总表，不重新做栅格统计：</p>
 * <ul>
 *   <li>指标区间面积与指标区总面积：bin_area_km2_1—bin_area_km2_5（0.2 等宽分档）。</li>
 *   <li>类型占比柱状图：森林类型面积（type1_area_km2—type5_area_km2，分母为五类之和）、
 *       居民地与非居民地面积（settlement_area_km2，分母为指标区总面积）、
 *       道路长度（road_length_km）、综合暴露度分项贡献（contrib_forest / contrib_settlement / contrib_road）。</li>
 *   <li>指标均值对比柱状图：mean（综合暴露度取 region_value，本身即选区面积加权均值）。</li>
 * </ul>
 *
 * <p>缺数与 0 区分：列值为 NULL 表示该行政区没有该档 / 该分项的统计，接口返回 null，
 * 不补 0；汇总表中没有该年份、区域或方案的记录时返回 null，由接口层返回失败响应。</p>
 *
 * <p>行政区口径（与汇总表一致）：region_code 为 6 位 GB/T 2260，230000=全省、230100—232700=地市
 * （原 4 位地市码后补 00）、230102 等=县市区；region_level 取 province / city / county。
 * 县区码前 4 位即所属地市（如 232701—232764 属于 232700），据此取所辖县区。</p>
 */
@Service
@RequiredArgsConstructor
public class ExposureAssessmentService {

    private static final String LEVEL_PROVINCE = "province";

    private static final String LEVEL_CITY = "city";

    private static final String LEVEL_COUNTY = "county";

    /**
     * 指标均值对比柱状图展示的行政区数量：选择全省取各市前 5，选择地市取所辖县区前 5。
     */
    private static final int TOP_SIZE = 5;

    /**
     * 指标区间分档边界，与统计 JSON 的 bins 一致：前四档左闭右开，末档含上界 1。
     */
    private static final double[] BIN_BOUNDS = {0D, 0.2D, 0.4D, 0.6D, 0.8D, 1D};

    /**
     * 森林类型名称，与 type1_area_km2—type5_area_km2 依次对应（MCD12Q1 LC_Type1 的 1—5 类）。
     */
    private static final String[] FOREST_TYPE_NAMES = {"常绿针叶林", "常绿阔叶林", "落叶针叶林", "落叶阔叶林", "混交林"};

    private static final String UNIT_SQUARE_KILOMETER = "km²";

    private static final String UNIT_KILOMETER = "km";

    private static final String UNIT_DIMENSIONLESS = "无量纲";

    private final ForestFractionSummaryMapper forestFractionSummaryMapper;

    private final SettlementFractionSummaryMapper settlementFractionSummaryMapper;

    private final RoadExposureSummaryMapper roadExposureSummaryMapper;

    private final ExposureCompositeSummaryMapper exposureCompositeSummaryMapper;

    /**
     * 交接包根目录：index.json 与 {年度}/{指标}.tif 所在目录。
     */
    @Value("${file-root.risk-assessment}")
    private String indicatorDataRoot;

    /**
     * 指标统计：指标区间面积与指标区总面积、类型占比柱状图、指标均值对比柱状图。
     *
     * @param assessmentYear 评价年份
     * @param regionCode     行政区划代码
     * @param indicator      评估指标
     * @param schemeId       AHP 方案号，仅综合暴露度使用
     * @return 统计结果；该年份、区域（或方案）没有记录时返回 null
     */
    public ExposureStatisticsVO exposureStatistics(short assessmentYear, int regionCode,
                                                   Option.ExposureIndicatorOption indicator, short schemeId) {
        RegionStatistics region = regionStatistics(assessmentYear, regionCode, indicator, schemeId);
        if (region == null) {
            return null;
        }
        return new ExposureStatisticsVO(
                assessmentYear,
                assessmentYear,
                indicator.name(),
                indicator.getLabel(),
                regionCode,
                region.regionName(),
                region.regionLevel(),
                bins(indicator, region.binAreas()),
                sum(region.binAreas()),
                region.typeChart(),
                meanChart(assessmentYear, regionCode, indicator, schemeId, region));
    }

    /**
     * 指标栅格文件：交接包根目录下 {年度}/{源文件名}。
     *
     * <p>综合暴露度（E）的栅格文件名带方案哈希（如 2018/E_s0_02064e59b488.tif），取自汇总表
     * exposure_composite_summary 的 source_file 列——该列是源栅格文件名的原值，与 AHP 方案一一对应；
     * 不手写哈希、不经 index.json 查表，也不能用其他方案的文件代替。</p>
     *
     * <p>森林覆盖暴露度、居民地面占比、道路暴露度密度的栅格文件名不带方案，取自年度目录下的同名文件
     * {fieldId}.tif（如 2018/forest_fraction.tif）：这三张汇总表没有 source_file 列，文件名由交接包约定
     * 唯一确定。实测 2018—2025 各年度目录均有这三个同名文件，只有 2025 年缺 road_exposure.tif
     * （与 road_exposure_summary 仅覆盖 2018—2024 一致，该年份返回 404）。</p>
     *
     * @return 栅格文件路径；源文件名取不到、文件不存在或解析结果越出交接包根目录时返回 null
     */
    public Path exposureRaster(short assessmentYear, Option.ExposureIndicatorOption indicator, short schemeId) {
        String sourceFile = indicator == Option.ExposureIndicatorOption.compositeExposure
                ? compositeSourceFile(assessmentYear, schemeId)
                : indicator.getFieldId() + ".tif";
        if (sourceFile == null || sourceFile.isBlank()) {
            return null;
        }
        Path root = Path.of(indicatorDataRoot).toAbsolutePath().normalize();
        Path file = root.resolve(assessmentYear + "/" + sourceFile).normalize();
        return file.startsWith(root) && Files.isRegularFile(file) ? file : null;
    }

    /**
     * 综合暴露度 E 在该年份、方案下的源栅格文件名，取自 exposure_composite_summary.source_file。
     *
     * <p>同一年、同一方案的 139 个行政区共用同一个 source_file（该表只存汇总值，不存栅格本体），
     * 故按区划码取第一行即可；该年份或方案没有记录、或 source_file 为空时返回 null。</p>
     */
    private String compositeSourceFile(short assessmentYear, short schemeId) {
        ExposureCompositeSummary row = exposureCompositeSummaryMapper.selectOne(
                Wrappers.<ExposureCompositeSummary>lambdaQuery()
                        .select(ExposureCompositeSummary::getSourceFile)
                        .eq(ExposureCompositeSummary::getAssessmentYear, assessmentYear)
                        .eq(ExposureCompositeSummary::getSchemeId, schemeId)
                        .isNotNull(ExposureCompositeSummary::getSourceFile)
                        .orderByAsc(ExposureCompositeSummary::getRegionCode)
                        .last("limit 1"));
        return row == null ? null : row.getSourceFile();
    }

    /**
     * 所选行政区的指标记录，无记录时返回 null。
     */
    private RegionStatistics regionStatistics(short assessmentYear, int regionCode,
                                              Option.ExposureIndicatorOption indicator, short schemeId) {
        return switch (indicator) {
            case forestFraction -> forestStatistics(assessmentYear, regionCode);
            case settlementFraction -> settlementStatistics(assessmentYear, regionCode);
            case roadExposure -> roadStatistics(assessmentYear, regionCode);
            case compositeExposure -> compositeStatistics(assessmentYear, regionCode, schemeId);
        };
    }

    /**
     * 森林覆盖暴露度：类型占比为五种森林类型面积，占比分母为五类面积之和（即森林总面积）。
     */
    private RegionStatistics forestStatistics(short assessmentYear, int regionCode) {
        ForestFractionSummary row = forestFractionSummaryMapper.selectOne(Wrappers.<ForestFractionSummary>lambdaQuery()
                .eq(ForestFractionSummary::getAssessmentYear, assessmentYear)
                .eq(ForestFractionSummary::getRegionCode, regionCode));
        if (row == null) {
            return null;
        }
        List<Double> typeAreas = Arrays.asList(row.getType1AreaKm2(), row.getType2AreaKm2(), row.getType3AreaKm2(),
                row.getType4AreaKm2(), row.getType5AreaKm2());
        Double forestArea = sum(typeAreas);
        List<ExposureChartItemVO> items = new ArrayList<>(FOREST_TYPE_NAMES.length);
        for (int i = 0; i < FOREST_TYPE_NAMES.length; i++) {
            Double area = typeAreas.get(i);
            items.add(new ExposureChartItemVO(null, FOREST_TYPE_NAMES[i], area, share(area, forestArea)));
        }
        return new RegionStatistics(row.getRegionName(), row.getRegionLevel(), row.getMean(),
                binAreas(row.getBinAreaKm21(), row.getBinAreaKm22(), row.getBinAreaKm23(),
                        row.getBinAreaKm24(), row.getBinAreaKm25()),
                chart("森林类型面积占比", UNIT_SQUARE_KILOMETER, items));
    }

    /**
     * 居民地面占比：类型占比为居民地与非居民地面积，非居民地 = 指标区总面积 − 居民地面积。
     */
    private RegionStatistics settlementStatistics(short assessmentYear, int regionCode) {
        SettlementFractionSummary row = settlementFractionSummaryMapper.selectOne(Wrappers.<SettlementFractionSummary>lambdaQuery()
                .eq(SettlementFractionSummary::getAssessmentYear, assessmentYear)
                .eq(SettlementFractionSummary::getRegionCode, regionCode));
        if (row == null) {
            return null;
        }
        List<Double> binAreas = binAreas(row.getBinAreaKm21(), row.getBinAreaKm22(), row.getBinAreaKm23(),
                row.getBinAreaKm24(), row.getBinAreaKm25());
        Double settlementArea = row.getSettlementAreaKm2();
        Double validArea = sum(binAreas);
        Double otherArea = settlementArea == null || validArea == null ? null : validArea - settlementArea;
        List<ExposureChartItemVO> items = List.of(
                new ExposureChartItemVO(null, "居民地", settlementArea, share(settlementArea, validArea)),
                new ExposureChartItemVO(null, "非居民地", otherArea, share(otherArea, validArea)));
        return new RegionStatistics(row.getRegionName(), row.getRegionLevel(), row.getMean(), binAreas,
                chart("居民地与非居民地面积占比", UNIT_SQUARE_KILOMETER, items));
    }

    /**
     * 道路暴露度密度：类型占比直接显示道路长度公里数，无占比。
     */
    private RegionStatistics roadStatistics(short assessmentYear, int regionCode) {
        RoadExposureSummary row = roadExposureSummaryMapper.selectOne(Wrappers.<RoadExposureSummary>lambdaQuery()
                .eq(RoadExposureSummary::getAssessmentYear, assessmentYear)
                .eq(RoadExposureSummary::getRegionCode, regionCode));
        if (row == null) {
            return null;
        }
        List<ExposureChartItemVO> items =
                List.of(new ExposureChartItemVO(null, "道路长度", row.getRoadLengthKm(), null));
        return new RegionStatistics(row.getRegionName(), row.getRegionLevel(), row.getMean(),
                binAreas(row.getBinAreaKm21(), row.getBinAreaKm22(), row.getBinAreaKm23(),
                        row.getBinAreaKm24(), row.getBinAreaKm25()),
                chart("道路长度", UNIT_KILOMETER, items));
    }

    /**
     * 综合暴露度 E：类型占比为森林、居民地、道路三项分项贡献，指标均值为 region_value。
     *
     * <p>分项贡献须由三张基础指标栅格在共同有效网格上按选区交叠面积求均值后乘 AHP 权重，
     * 汇总表中缺失或未生成时按 NULL 返回，不由基础指标均值倒推。</p>
     */
    private RegionStatistics compositeStatistics(short assessmentYear, int regionCode, short schemeId) {
        ExposureCompositeSummary row = exposureCompositeSummaryMapper.selectOne(Wrappers.<ExposureCompositeSummary>lambdaQuery()
                .eq(ExposureCompositeSummary::getAssessmentYear, assessmentYear)
                .eq(ExposureCompositeSummary::getRegionCode, regionCode)
                .eq(ExposureCompositeSummary::getSchemeId, schemeId));
        if (row == null) {
            return null;
        }
        List<ExposureChartItemVO> items = List.of(
                new ExposureChartItemVO(null, "森林", row.getContribForest(), null),
                new ExposureChartItemVO(null, "居民地", row.getContribSettlement(), null),
                new ExposureChartItemVO(null, "道路", row.getContribRoad(), null));
        return new RegionStatistics(row.getRegionName(), row.getRegionLevel(), row.getRegionValue(),
                binAreas(row.getBinAreaKm21(), row.getBinAreaKm22(), row.getBinAreaKm23(),
                        row.getBinAreaKm24(), row.getBinAreaKm25()),
                chart("综合暴露度分项贡献", UNIT_DIMENSIONLESS, items));
    }

    /**
     * 指标均值对比柱状图：选择全省返回各市均值前 5，选择地市返回所辖县区均值前 5，选择县区返回其本身。
     *
     * <p>均值为空（没有有效值）的行政区不参与排名；数值按指标展示口径换算
     * （森林覆盖暴露度、居民地面占比乘 100 记 %，其余原值）。</p>
     */
    private ExposureChartVO meanChart(short assessmentYear, int regionCode, Option.ExposureIndicatorOption indicator,
                                      short schemeId, RegionStatistics region) {
        List<Candidate> candidates;
        String title;
        if (LEVEL_PROVINCE.equals(region.regionLevel())) {
            candidates = candidates(assessmentYear, indicator, schemeId, LEVEL_CITY, null);
            title = "地市指标均值对比（前 5）";
        } else if (LEVEL_CITY.equals(region.regionLevel())) {
            candidates = candidates(assessmentYear, indicator, schemeId, LEVEL_COUNTY, regionCode);
            title = "区县指标均值对比（前 5）";
        } else {
            candidates = List.of(new Candidate(regionCode, region.regionName(), region.metric()));
            title = "县区指标均值（所选县区）";
        }
        int scale = indicator.getMeanDisplayScale();
        ExposureChartItemVO[] items = candidates.stream()
                .filter(candidate -> candidate.metric() != null)
                .sorted(Comparator.comparingDouble(Candidate::metric).reversed().thenComparingInt(Candidate::code))
                .limit(TOP_SIZE)
                .map(candidate -> new ExposureChartItemVO(String.valueOf(candidate.code()), candidate.name(),
                        candidate.metric() * scale, null))
                .toArray(ExposureChartItemVO[]::new);
        return chart(title, indicator.getMeanDisplayUnit(), items);
    }

    /**
     * 指标均值对比的候选行政区；parentCityCode 非空时限定为该地市所辖县区。
     */
    private List<Candidate> candidates(short assessmentYear, Option.ExposureIndicatorOption indicator, short schemeId,
                                       String regionLevel, Integer parentCityCode) {
        return switch (indicator) {
            case forestFraction -> forestCandidates(assessmentYear, regionLevel, parentCityCode);
            case settlementFraction -> settlementCandidates(assessmentYear, regionLevel, parentCityCode);
            case roadExposure -> roadCandidates(assessmentYear, regionLevel, parentCityCode);
            case compositeExposure -> compositeCandidates(assessmentYear, schemeId, regionLevel, parentCityCode);
        };
    }

    private List<Candidate> forestCandidates(short assessmentYear, String regionLevel, Integer parentCityCode) {
        LambdaQueryWrapper<ForestFractionSummary> query = Wrappers.<ForestFractionSummary>lambdaQuery()
                .eq(ForestFractionSummary::getAssessmentYear, assessmentYear)
                .eq(ForestFractionSummary::getRegionLevel, regionLevel)
                .orderByAsc(ForestFractionSummary::getRegionCode);
        limitToCity(query, parentCityCode, ForestFractionSummary::getRegionCode);
        return forestFractionSummaryMapper.selectList(query).stream()
                .map(row -> new Candidate(row.getRegionCode(), row.getRegionName(), row.getMean()))
                .toList();
    }

    private List<Candidate> settlementCandidates(short assessmentYear, String regionLevel, Integer parentCityCode) {
        LambdaQueryWrapper<SettlementFractionSummary> query = Wrappers.<SettlementFractionSummary>lambdaQuery()
                .eq(SettlementFractionSummary::getAssessmentYear, assessmentYear)
                .eq(SettlementFractionSummary::getRegionLevel, regionLevel)
                .orderByAsc(SettlementFractionSummary::getRegionCode);
        limitToCity(query, parentCityCode, SettlementFractionSummary::getRegionCode);
        return settlementFractionSummaryMapper.selectList(query).stream()
                .map(row -> new Candidate(row.getRegionCode(), row.getRegionName(), row.getMean()))
                .toList();
    }

    private List<Candidate> roadCandidates(short assessmentYear, String regionLevel, Integer parentCityCode) {
        LambdaQueryWrapper<RoadExposureSummary> query = Wrappers.<RoadExposureSummary>lambdaQuery()
                .eq(RoadExposureSummary::getAssessmentYear, assessmentYear)
                .eq(RoadExposureSummary::getRegionLevel, regionLevel)
                .orderByAsc(RoadExposureSummary::getRegionCode);
        limitToCity(query, parentCityCode, RoadExposureSummary::getRegionCode);
        return roadExposureSummaryMapper.selectList(query).stream()
                .map(row -> new Candidate(row.getRegionCode(), row.getRegionName(), row.getMean()))
                .toList();
    }

    private List<Candidate> compositeCandidates(short assessmentYear, short schemeId, String regionLevel,
                                                Integer parentCityCode) {
        LambdaQueryWrapper<ExposureCompositeSummary> query = Wrappers.<ExposureCompositeSummary>lambdaQuery()
                .eq(ExposureCompositeSummary::getAssessmentYear, assessmentYear)
                .eq(ExposureCompositeSummary::getSchemeId, schemeId)
                .eq(ExposureCompositeSummary::getRegionLevel, regionLevel)
                .orderByAsc(ExposureCompositeSummary::getRegionCode);
        limitToCity(query, parentCityCode, ExposureCompositeSummary::getRegionCode);
        return exposureCompositeSummaryMapper.selectList(query).stream()
                .map(row -> new Candidate(row.getRegionCode(), row.getRegionName(), row.getRegionValue()))
                .toList();
    }

    /**
     * 限定为某地市所辖县区：县区码前 4 位即地市码，区划码落在 [地市码, 地市码+100) 区间内。
     */
    private <T> void limitToCity(LambdaQueryWrapper<T> query, Integer cityCode, SFunction<T, Integer> codeColumn) {
        if (cityCode != null) {
            query.ge(codeColumn, cityCode).lt(codeColumn, cityCode + 100);
        }
    }

    /**
     * 五档区间面积，按分档顺序整理；列值为 NULL 时保留 null。
     */
    private List<Double> binAreas(Double bin1, Double bin2, Double bin3, Double bin4, Double bin5) {
        return Arrays.asList(bin1, bin2, bin3, bin4, bin5);
    }

    /**
     * 五档区间面积输出，标签按指标展示口径：森林覆盖暴露度、居民地面占比为百分数区间，其余为 0—1 区间。
     */
    private ExposureBinVO[] bins(Option.ExposureIndicatorOption indicator, List<Double> binAreas) {
        boolean percent = indicator.getMeanDisplayScale() == 100;
        ExposureBinVO[] bins = new ExposureBinVO[BIN_BOUNDS.length - 1];
        for (int i = 0; i < bins.length; i++) {
            String label = percent
                    ? Math.round(BIN_BOUNDS[i] * 100) + "%–" + Math.round(BIN_BOUNDS[i + 1] * 100) + "%"
                    : BIN_BOUNDS[i] + "–" + BIN_BOUNDS[i + 1];
            bins[i] = new ExposureBinVO(label, BIN_BOUNDS[i], BIN_BOUNDS[i + 1],
                    i < binAreas.size() ? binAreas.get(i) : null);
        }
        return bins;
    }

    /**
     * 数值合计；任一元素为 null（缺测）时返回 null，不按 0 参与求和。
     * 五档面积之和即指标区总面积。
     */
    private Double sum(List<Double> values) {
        double total = 0D;
        for (Double value : values) {
            if (value == null) {
                return null;
            }
            total += value;
        }
        return total;
    }

    /**
     * 占比（%）= 分项值 ÷ 合计 × 100；分项缺测、合计缺测或合计不大于 0 时返回 null。
     */
    private Double share(Double value, Double total) {
        if (value == null || total == null || total <= 0D) {
            return null;
        }
        return value / total * 100D;
    }

    private ExposureChartVO chart(String title, String unit, List<ExposureChartItemVO> items) {
        return chart(title, unit, items.toArray(ExposureChartItemVO[]::new));
    }

    private ExposureChartVO chart(String title, String unit, ExposureChartItemVO[] items) {
        return new ExposureChartVO(title, unit, items);
    }

    /**
     * 所选行政区的指标记录：名称、级别、均值（用于均值对比）与类型占比柱状图。
     */
    private record RegionStatistics(String regionName, String regionLevel, Double metric,
                                    List<Double> binAreas, ExposureChartVO typeChart) {
    }

    /**
     * 均值对比的候选行政区。
     */
    private record Candidate(int code, String name, Double metric) {
    }
}
