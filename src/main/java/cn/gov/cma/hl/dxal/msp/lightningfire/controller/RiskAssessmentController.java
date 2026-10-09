package cn.gov.cma.hl.dxal.msp.lightningfire.controller;


import cn.gov.cma.hl.dxal.msp.lightningfire.constant.Option;
import cn.gov.cma.hl.dxal.msp.lightningfire.constant.RegionCode;
import cn.gov.cma.hl.dxal.msp.lightningfire.dto.ResponseDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.service.ExposureAssessmentService;
import cn.gov.cma.hl.dxal.msp.lightningfire.service.RegionCodeService;
import cn.gov.cma.hl.dxal.msp.lightningfire.service.RiskAssessmentService;
import cn.gov.cma.hl.dxal.msp.lightningfire.service.VulnerabilityAssessmentService;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.ExposureStatisticsVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.RiskAssessmentComponentsVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.RiskAssessmentStatisticsVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.SelectOptionVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.VulnerabilityStatisticsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.util.Arrays;

/**
 * 风险评估页面
 *
 * <p>regionCode 参数的取值说明、校验与「码 → 区域」转换统一由 {@link RegionCodeService} 提供，
 * 与 /common/region-code 下拉、/ltg-env 各接口共用同一套区划口径。</p>
 */
@RestController
@Tag(name = "风险评估")
@RequestMapping("/risk-assessment")
@RequiredArgsConstructor
@CrossOrigin("*")
public class RiskAssessmentController {

    private static final String YEAR_DESC = "评价年份 2018—2025";
    private static final String INDICATOR_DESC = """
            评估指标：forestFraction=森林覆盖暴露度、settlementFraction=居民地面占比、\
            roadExposure=道路暴露度密度、compositeExposure=综合暴露度；默认 forestFraction，\
            取值可通过 /risk-assessment/exposure/indicator/options 获取""";
    private static final String SCHEME_DESC = "AHP 方案号 0—6，仅 indicator=compositeExposure 时生效；默认 0（AHP基准矩阵）";
    private static final String VULNERABILITY_INDICATOR_DESC = """
            评估指标：vegetationSensitivity=植被燃烧敏感度、\
            fuelLoadIndex=可燃物负荷指数、slopeDifficulty=坡度困难度、roadAccess=道路接近条件、\
            compositeVulnerability=综合脆弱性 V；默认 vegetationSensitivity，\
            取值可通过 /risk-assessment/vulnerability/indicator/options 获取""";
    private static final String VULNERABILITY_SCHEME_DESC = """
            AHP 方案号 0—6，仅 indicator=compositeVulnerability 时生效；\
            默认 0（AHP基准矩阵），取值可通过 /risk-assessment/vulnerability/scheme/options 获取""";

    private static final String RISK_YEAR_DESC = "评价年份 2018—2024（危险性统计表的覆盖范围，2020 年源数据只到 34 天）";

    private static final String RISK_SCHEME_DESC = """
            AHP 方案号 0—6，仅影响暴露度 E 与脆弱性 V 的取值（危险性 H 与方案无关）；\
            默认 0（AHP基准矩阵），取值可通过 /risk-assessment/vulnerability/scheme/options 获取""";

    private final ExposureAssessmentService exposureAssessmentService;

    private final VulnerabilityAssessmentService vulnerabilityAssessmentService;

    private final RiskAssessmentService riskAssessmentService;

    private final RegionCodeService regionCodeService;

    @Operation(summary = "承载体暴露度·评估指标 【OPTIONS】", description = "select 选项，value 为 indicator 参数取值")
    @GetMapping("/exposure/indicator/options")
    public ResponseDTO<SelectOptionVO[]> getExposureIndicatorOptions() {
        SelectOptionVO[] optionVOS = Arrays.stream(Option.ExposureIndicatorOption.values())
                .map(e -> new SelectOptionVO(e.getLabel(), e.name()))
                .toArray(SelectOptionVO[]::new);
        return ResponseDTO.success(optionVOS);
    }

    @Operation(summary = "承载体暴露度·暴露度 AHP 方案 【OPTIONS】")
    @GetMapping("/")
    public ResponseDTO<?> getExposureAHPOptions() {
        SelectOptionVO[] optionVOS = Arrays.stream(Option.ExposureAHPOption.values())
                .map(e -> new SelectOptionVO(e.getLabel(), String.valueOf(e.getId())))
                .toArray(SelectOptionVO[]::new);
        return ResponseDTO.success(optionVOS);
    }

    @Operation(summary = "承载体暴露度·森林覆盖率指标评估",
            description = """
                    指标区间面积（五档）与指标区总面积、类型占比柱状图、指标 mean 对比柱状图；\
                    mean 对比：选择全省返回各市前 5，选择地市返回所辖县区前 5，选择县区返回其本身；\
                    该年份、区域（或方案）没有汇总记录时返回 404，不以 0 或空图表冒充无数据；\
                    regionCode 不是有效区划码时返回 400""")
    @GetMapping("/exposure/statistics")
    public ResponseDTO<ExposureStatisticsVO> getExposureStatistics(
            @Parameter(description = YEAR_DESC, example = "2018")
            @RequestParam("year") Short year,
            @Parameter(description = RegionCodeService.CODE_DESC, example = "232700")
            @RequestParam("regionCode") Integer regionCode,
            @Parameter(description = INDICATOR_DESC)
            @RequestParam(value = "indicator", required = false, defaultValue = "forestFraction")
            Option.ExposureIndicatorOption indicator,
            @Parameter(description = SCHEME_DESC)
            @RequestParam(value = "schemeId", required = false, defaultValue = "0") Short schemeId) {
        RegionCode region = regionCodeService.requireCode(regionCode);
        ExposureStatisticsVO statistics =
                exposureAssessmentService.exposureStatistics(year, region.getCode(), indicator, schemeId);
        if (statistics == null) {
            return ResponseDTO.failure(HttpStatus.NOT_FOUND.value(),
                    "该年份与行政区域没有「" + indicator.getLabel() + "」的统计数据");
        }
        return ResponseDTO.success(statistics);
    }

    @Operation(summary = "承载体暴露度·指标栅格文件 【FILE】",
            description = """
                    返回交接包中的指标栅格，路径为指标根目录 + 年份 + 源文件名：\
                    综合暴露度 E 的文件名取自 exposure_composite_summary 的 source_file\
                    （如 2018/E_s0_02064e59b488.tif，按 schemeId 对应方案，不手写哈希）；\
                    森林覆盖暴露度、居民地面占比、道路暴露度密度的源文件名固定为 {指标}.tif；\
                    无对应文件时返回 404""")
    @GetMapping("/exposure/raster")
    public ResponseEntity<Resource> getExposureRaster(
            @Parameter(description = YEAR_DESC, example = "2018")
            @RequestParam("year") Short year,
            @Parameter(description = INDICATOR_DESC)
            @RequestParam(value = "indicator", required = false, defaultValue = "forestFraction")
            Option.ExposureIndicatorOption indicator,
            @Parameter(description = SCHEME_DESC)
            @RequestParam(value = "schemeId", required = false, defaultValue = "0") Short schemeId) {
        Path file = exposureAssessmentService.exposureRaster(year, indicator, schemeId);
        if (file == null) {
            String scope = indicator == Option.ExposureIndicatorOption.compositeExposure
                    ? "该年份与 AHP 方案（方案 " + schemeId + "）"
                    : "该年份";
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    scope + "没有「" + indicator.getLabel() + "」的栅格文件");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("image/tiff"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(file.getFileName().toString()).build().toString())
                .body(new FileSystemResource(file));
    }

    /**
     * 承载体脆弱性模块：植被燃烧敏感度、可燃物负荷指数、坡度困难度、道路接近条件与综合脆弱性 V。
     *
     * <p>数据源为脆弱性汇总表（vegetation_sensitivity_summary、fuel_load_index_summary、
     * slope_difficulty_summary、road_access_summary、vulnerability_composite_summary），
     * 口径与缺数约定见 VulnerabilityAssessmentService。</p>
     */
    @Operation(summary = "承载体脆弱性·评估指标 【OPTIONS】", description = "select 选项，value 为 indicator 参数取值")
    @GetMapping("/vulnerability/indicator/options")
    public ResponseDTO<SelectOptionVO[]> getVulnerabilityIndicatorOptions() {
        SelectOptionVO[] optionVOS = Arrays.stream(Option.VulnerabilityIndicatorOption.values())
                .map(e -> new SelectOptionVO(e.getLabel(), e.name()))
                .toArray(SelectOptionVO[]::new);
        return ResponseDTO.success(optionVOS);
    }

    @Operation(summary = "承载体脆弱性·综合脆弱性 V 方案 【OPTIONS】",
            description = "select 选项，value 为 schemeId 参数取值；与暴露度共用同一套 AHP 方案号，权重取 V_weights")
    @GetMapping("/vulnerability/scheme/options")
    public ResponseDTO<SelectOptionVO[]> getVulnerabilitySchemeOptions() {
        SelectOptionVO[] optionVOS = Arrays.stream(Option.VulnerabilityAHPOption.values())
                .map(e -> new SelectOptionVO(e.getLabel(), String.valueOf(e.getId())))
                .toArray(SelectOptionVO[]::new);
        return ResponseDTO.success(optionVOS);
    }

    @Operation(summary = "承载体脆弱性·指标评估",
            description = """
                    指标区间面积（五档）与指标区总面积、第二张统计图、指标均值对比柱状图；\
                    第二张图按指标不同：植被燃烧敏感度为五类森林类型面积、可燃物负荷指数为森林载量均值与归一化参考\
                    （t/ha）、坡度困难度与道路接近条件为五档面积（km²）、综合脆弱性为四项分项贡献（无量纲）；\
                    mean 对比：选择全省返回各市前 5，选择地市返回所辖县区前 5，选择县区返回其本身；\
                    数据覆盖：植被燃烧敏感度与坡度困难度 2018—2025（坡度各年共用 2025 年静态底表，\
                    植被敏感度 2025 沿用 2024 年底表）、道路接近条件 2018—2024、\
                    可燃物负荷指数与综合脆弱性 2018—2022（源包缺 2023—2025，不跨年替代）；\
                    该年份、区域（或方案）没有汇总记录时返回 404，不以 0 或空图表冒充无数据；\
                    regionCode 不是有效区划码时返回 400""")
    @GetMapping("/vulnerability/statistics")
    public ResponseDTO<VulnerabilityStatisticsVO> getVulnerabilityStatistics(
            @Parameter(description = YEAR_DESC, example = "2018")
            @RequestParam("year") Short year,
            @Parameter(description = RegionCodeService.CODE_DESC, example = "232700")
            @RequestParam("regionCode") Integer regionCode,
            @Parameter(description = VULNERABILITY_INDICATOR_DESC)
            @RequestParam(value = "indicator", required = false, defaultValue = "vegetationSensitivity")
            Option.VulnerabilityIndicatorOption indicator,
            @Parameter(description = VULNERABILITY_SCHEME_DESC)
            @RequestParam(value = "schemeId", required = false, defaultValue = "0") Short schemeId) {
        RegionCode region = regionCodeService.requireCode(regionCode);
        VulnerabilityStatisticsVO statistics =
                vulnerabilityAssessmentService.vulnerabilityStatistics(year, region.getCode(), indicator, schemeId);
        if (statistics == null) {
            String scope = indicator == Option.VulnerabilityIndicatorOption.compositeVulnerability
                    ? "该年份、行政区域与 AHP 方案（方案 " + schemeId + "）"
                    : "该年份与行政区域";
            return ResponseDTO.failure(HttpStatus.NOT_FOUND.value(),
                    scope + "没有「" + indicator.getLabel() + "」的统计数据");
        }
        return ResponseDTO.success(statistics);
    }

    @Operation(summary = "承载体脆弱性·指标栅格文件 【FILE】",
            description = """
                    返回交接包中的指标栅格，路径为指标根目录 + 年份 + 源文件名，口径与承载体暴露度一致：\
                    综合脆弱性 V 的文件名取自 vulnerability_composite_summary 的 source_file\
                    （如 2018/V_s0_0ee42f26c3f4.tif，按 schemeId 对应方案，不手写哈希）；\
                    植被燃烧敏感度、可燃物负荷指数、坡度困难度、道路接近条件的源文件名固定为 {指标}.tif；\
                    无对应文件时返回 404""")
    @GetMapping("/vulnerability/raster")
    public ResponseEntity<Resource> getVulnerabilityRaster(
            @Parameter(description = YEAR_DESC, example = "2018")
            @RequestParam("year") Short year,
            @Parameter(description = VULNERABILITY_INDICATOR_DESC)
            @RequestParam(value = "indicator", required = false, defaultValue = "vegetationSensitivity")
            Option.VulnerabilityIndicatorOption indicator,
            @Parameter(description = VULNERABILITY_SCHEME_DESC)
            @RequestParam(value = "schemeId", required = false, defaultValue = "0") Short schemeId) {
        Path file = vulnerabilityAssessmentService.vulnerabilityRaster(year, indicator, schemeId);
        if (file == null) {
            String scope = indicator == Option.VulnerabilityIndicatorOption.compositeVulnerability
                    ? "该年份与 AHP 方案（方案 " + schemeId + "）"
                    : "该年份";
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    scope + "没有「" + indicator.getLabel() + "」的栅格文件");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("image/tiff"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(file.getFileName().toString()).build().toString())
                .body(new FileSystemResource(file));
    }

    /**
     * 综合风险评估与区划模块：危险性评分的区域指数、评分区间占比与区县对比。
     *
     * <p>数据源为危险性统计表 dangerousness_statistics 的 score 列（0—100 百分制），该表按
     * 「日期 × 区县 × 地级市」逐日存放评分，region 列是区县名、city 列是地市名，没有行政区划代码，
     * 故本模块按 {@link RegionCode} 的地区名匹配（地市名定位地市，地市名 + 区县名定位区县），
     * 与承载体暴露度（{@code exposure_*_summary}）、承载体脆弱性（{@code *_summary}）
     * 按 region_code 取数的方式不同。</p>
     *
     * <p>与 AHP 无关：区域评分指数是该区在评价年份内有记录日期的评分均值，不按方案号合成，
     * 也不读取承载体暴露度 / 脆弱性的汇总表；因此本模块没有 schemeId 参数，
     * 页面上的方案选择只影响承载体暴露度与脆弱性两个模块。</p>
     */
    @Operation(summary = "综合风险评估与区划·评分统计",
            description = """
                    返回所选行政区在评价年份内的风险评分均值（0—100 百分制）、评分区间记录日占比\
                    （0—100 等宽 20 分五档，前四档左闭右开、末档含上界）与评分均值对比柱状图；\
                    指数口径：县市区为其记录均值，地市为所辖县区均值的等权均值，全省为各地市均值的等权均值\
                    （按行政区等权，表中没有面积列，不做面积加权）；\
                    对比口径：选择全省返回各地市前 5，选择地市返回所辖县区前 5，选择县区返回其本身，\
                    记录缺失的区县不参与排名；\
                    评价年份取自然年，数据源只覆盖 2018—2024（2020 年只有 34 天记录），\
                    该年份或该行政区没有评分记录时返回 404，不以 0 或空图表冒充无数据；\
                    validDays 与 coverage 为所选范围内各行政区有记录日数与完整率的平均值，\
                    只用于提示源数据完整程度；regionCode 不是有效区划码时返回 400""")
    @GetMapping("/statistics")
    public ResponseDTO<RiskAssessmentStatisticsVO> getRiskAssessmentStatistics(
            @Parameter(description = RISK_YEAR_DESC, example = "2018")
            @RequestParam("year") Short year,
            @Parameter(description = RegionCodeService.CODE_DESC, example = "232700")
            @RequestParam("regionCode") Integer regionCode) {
        RegionCode region = regionCodeService.requireCode(regionCode);
        RiskAssessmentStatisticsVO statistics = riskAssessmentService.riskAssessmentStatistics(year, region);
        if (statistics == null) {
            return ResponseDTO.failure(HttpStatus.NOT_FOUND.value(),
                    "该年份与行政区域没有「危险性评分」的统计数据");
        }
        return ResponseDTO.success(statistics);
    }

    @Operation(summary = "综合风险评估与区划·评分区间 【OPTIONS】",
            description = "select 选项，value 为评分区间取值，label 为「风险等级 下界—上界」；"
                    + "评分区间为 0—100 等宽 20 分五档，与统计接口返回的 bins 一一对应")
    @GetMapping("/grade/options")
    public ResponseDTO<SelectOptionVO[]> getRiskGradeOptions() {
        SelectOptionVO[] optionVOS = Arrays.stream(Option.RiskGradeOption.values())
                .map(e -> new SelectOptionVO(e.getBinLabel(), e.name()))
                .toArray(SelectOptionVO[]::new);
        return ResponseDTO.success(optionVOS);
    }

    @Operation(summary = "综合风险评估与区划·危险性 H 年度均值栅格 【FILE】",
            description = """
                    返回交接包中危险性 H 的逐年均值栅格，文件名为 H_mean_{评价年份}.tif，\
                    默认取交接包根目录下的 H_annual_mean_TIFF 目录（可用 file-root.risk-assessment-h 覆盖）；\
                    H 栅格与行政区划、AHP 方案都无关，只有年份参与定位，故不提供 regionCode 与 schemeId 参数；\
                    各年可用日数：2018=359、2019=359、2020=34、2021=361、2022=363、2023=365、2024=366，\
                    2020 年只是这 34 天的均值，不是完整年度观测；\
                    没有该年份的文件时返回 404，不用其他年份的文件冒充""")
    @GetMapping("/hazard/raster")
    public ResponseEntity<Resource> getHazardRaster(
            @Parameter(description = RISK_YEAR_DESC, example = "2018")
            @RequestParam("year") Short year) {
        Path file = riskAssessmentService.hazardRaster(year);
        if (file == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "该年份没有「危险性 H」的年度均值栅格文件");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("image/tiff"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(file.getFileName().toString()).build().toString())
                .body(new FileSystemResource(file));
    }

    @Operation(summary = "综合风险评估与区划·H/E/V 分项指数",
            description = """
                    返回所选行政区在评价年份内的危险性 H、暴露度 E、脆弱性 V 三项指数\
                    （均为 0—1 归一化指数，不按 AHP 加权合成，也不由评分反推）；\
                    取数：H 取 dangerousness_annual_summary.mean（与方案无关）、\
                    E 取 exposure_composite_summary.region_value、V 取 vulnerability_composite_summary.mean，\
                    后两项按 schemeId（0—6）取方案；\
                    数据覆盖：H 与 E 为 2018—2024，V 只有 2018—2022，缺项返回 null 并附 source，不补 0；\
                    三项都取不到时返回 404；regionCode 不是有效区划码时返回 400""")
    @GetMapping("/components")
    public ResponseDTO<RiskAssessmentComponentsVO> getRiskComponents(
            @Parameter(description = RISK_YEAR_DESC, example = "2018")
            @RequestParam("year") Short year,
            @Parameter(description = RegionCodeService.CODE_DESC, example = "232700")
            @RequestParam("regionCode") Integer regionCode,
            @Parameter(description = RISK_SCHEME_DESC)
            @RequestParam(value = "schemeId", required = false, defaultValue = "0") Short schemeId) {
        regionCodeService.requireCode(regionCode);
        RiskAssessmentComponentsVO components =
                riskAssessmentService.riskComponents(year, regionCode, schemeId);
        if (components == null) {
            return ResponseDTO.failure(HttpStatus.NOT_FOUND.value(),
                    "该年份与行政区域没有「危险性 / 暴露度 / 脆弱性」的分项指数");
        }
        return ResponseDTO.success(components);
    }

}
