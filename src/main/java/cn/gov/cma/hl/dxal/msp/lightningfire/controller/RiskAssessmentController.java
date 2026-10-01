package cn.gov.cma.hl.dxal.msp.lightningfire.controller;


import cn.gov.cma.hl.dxal.msp.lightningfire.constant.Option;
import cn.gov.cma.hl.dxal.msp.lightningfire.constant.RegionCode;
import cn.gov.cma.hl.dxal.msp.lightningfire.dto.ResponseDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.service.ExposureAssessmentService;
import cn.gov.cma.hl.dxal.msp.lightningfire.service.RegionCodeService;
import cn.gov.cma.hl.dxal.msp.lightningfire.service.VulnerabilityAssessmentService;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.ExposureStatisticsVO;
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

    private final ExposureAssessmentService exposureAssessmentService;

    private final VulnerabilityAssessmentService vulnerabilityAssessmentService;

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

}
