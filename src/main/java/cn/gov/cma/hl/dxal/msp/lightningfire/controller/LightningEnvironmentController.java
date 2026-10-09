package cn.gov.cma.hl.dxal.msp.lightningfire.controller;

import cn.gov.cma.hl.dxal.msp.lightningfire.constant.Option;
import cn.gov.cma.hl.dxal.msp.lightningfire.constant.RegionCode;
import cn.gov.cma.hl.dxal.msp.lightningfire.dto.ResponseDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.service.KeyElementOverviewService;
import cn.gov.cma.hl.dxal.msp.lightningfire.service.LightningFeatureService;
import cn.gov.cma.hl.dxal.msp.lightningfire.service.RegionCodeService;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;

/**
 * 雷击火环境
 */
@RestController
@Tag(name = "雷击火环境")
@RequestMapping("/ltg-env")
@RequiredArgsConstructor
@CrossOrigin("*")
public class LightningEnvironmentController {

    private static final String SINCE_DESC = "统计起始日期（含），格式 yyyy-MM-dd";
    private static final String UNTIL_DESC = "统计结束日期（含），格式 yyyy-MM-dd";

    private static final String GRANULARITY_DESC = "时间轴粒度：DAY=天（yyyy-MM-dd）、MONTH=月（yyyy-MM）、YEAR=年（yyyy）；默认 DAY";
    private static final String ELEMENT_DESC = """
            统计概览的要素，单选：lightningCharacteristics=闪电特征（默认）、temperature=气温、windSpeed=风速、\
            precipitation=降水、relativeHumidity=相对湿度、wetnessIndex=湿润指数、lightingFire=雷击火、\
            thunderstorm=雷暴；取值可通过 /ltg-env/key-element/checkboxes 获取""";

    private static final String REGION_DESC = "分析区域：6 位行政区划码，取值可通过 /common/region-code?code=232700 "
            + "获取（如 232701=漠河市）；默认 232700=大兴安岭地区，即页面分析范围的全区域";

    /**
     * 分析区域默认值：大兴安岭地区（232700），即页面分析范围的全区域。
     *
     * <p>雷击火环境页面只覆盖大兴安岭地区所辖县市区，取该区划码作默认值即「不限县区」。
     * regionCode 参数统一取 6 位行政区划码（与各汇总表的 region_code 列、/common/region-code 一致），
     * 服务层按地区名过滤、按区划范围判断，用 {@link RegionCode} 表达区域语义，两者由
     * {@link RegionCode#fromCode(Integer)} 转换。注解属性要求编译期常量，故这里写区划码字面量，
     * 不能写成 {@code String.valueOf(RegionCode.R232700.getCode())}。</p>
     */
    private static final String DEFAULT_REGION_CODE = "232700";

    private final LightningFeatureService lightningFeatureService;

    private final KeyElementOverviewService keyElementOverviewService;

    private final RegionCodeService regionCodeService;

    @Operation(summary = "雷击火关键要素 【OPTIONS】", description = "checkbox 选项，key 为雷击火关键要素的取值，label 为展示名称")
    @GetMapping("/key-element/checkboxes")
    public ResponseDTO<CheckboxVO[]> getKeyElementOptions() {
        CheckboxVO[] optionVOS = Arrays.stream(Option.KeyElementOption.values())
                .map(e -> new CheckboxVO(e.name(), e.getLabel()))
                .toArray(CheckboxVO[]::new);
        return ResponseDTO.success(optionVOS);
    }

    @Operation(summary = "雷击火时间轴与地闪特征 【AXIS】",
            description = """
                    返回 since ~ until 内按 granularity 展开的完整时间轴，以及区间内（可按 regionCode 过滤）的雷击火记录点位，\
                    点位按发现时间升序；区间内无雷击火记录时 series 为空数组""")
    @GetMapping("/time-axis/lightning-fire")
    public ResponseDTO<LightningFireTimeAxisVO> getLightningFireTimeAxis(
            @Parameter(description = SINCE_DESC, example = "2024-01-01")
            @RequestParam("since") LocalDate since,
            @Parameter(description = UNTIL_DESC, example = "2024-12-31")
            @RequestParam("until") LocalDate until,
            @Parameter(description = REGION_DESC, example = "232701")
            @RequestParam(value = "regionCode", required = false, defaultValue = DEFAULT_REGION_CODE) Integer regionCode,
            @Parameter(description = GRANULARITY_DESC)
            @RequestParam(value = "granularity", required = false, defaultValue = "DAY") Option.GranularityOption granularity) {
        return ResponseDTO.success(lightningFeatureService.lightningFireTimeAxis(
                since, until, regionCodeService.requireCode(regionCode), granularity));
    }

    /**
     * 统计概览：按所选要素返回它的 4 个统计格子。
     *
     * <p>闪电特征 4 格取自原「地闪特征」接口的聚合结果，与原 {@code getLightningCharacteristics(since, until, regionCode)}
     * 是同一份数值，故该接口随本次改动一并下线，不再单独暴露；要素统计的口径见
     * {@link KeyElementOverviewService}。</p>
     */
    @Operation(summary = "统计概览 【ARTICLE GRID】",
            description = """
                    按 element 选中的要素返回该要素的 4 个统计格子，顺序固定：闪电特征为地闪次数、地闪密度、\
                    地闪强度、正负极性；雷击火为事件次数、过火面积合计、平均过火面积、点位密度\
                    （面积单位 hm²，密度单位 起/万km²，分母为选区内县市区面积之和，选区内有区划没有面积口径时记 0）；\
                    雷暴为年平均雷暴天数（天）、雷暴密度（天/万km²），后两格为空（该要素只有这两项统计）——\
                    雷暴数据只有 1961—2013 年，这段时间就是它的统计口径，与 since ~ until 无关；\
                    气温、风速、降水、相对湿度为平均、最大、最小，第四格为「最大 − 最小」极差；\
                    湿润指数为区间内最后一个非空值（年内累计口径）、区间内最大 / 最小与距平\
                    （该值 − 同区域多年平均，正值表示比多年平均更湿润）；\
                    气温/风速/降水/相对湿度/湿润指数取自区县逐日气象要素表 weather_observations，\
                    按 assessment_date 在 since ~ until 上精确过滤，在 regionCode 范围内的全部区县日记录上聚合\
                    （不按区县面积或站点数加权）；闪电特征与雷击火按记录日期精确过滤；\
                    区间内没有记录时各格记 0；regionCode 不是有效区划码时返回 400""")
    @GetMapping("/lightning-characteristics")
    public ResponseDTO<ArticleVO[]> getKeyElementOverview(
            @Parameter(description = SINCE_DESC, example = "2018-01-01")
            @RequestParam("since") LocalDate since,
            @Parameter(description = UNTIL_DESC, example = "2025-12-31")
            @RequestParam("until") LocalDate until,
            @Parameter(description = REGION_DESC, example = "232701")
            @RequestParam(value = "regionCode", required = false, defaultValue = DEFAULT_REGION_CODE) Integer regionCode,
            @Parameter(description = ELEMENT_DESC, example = "temperature")
            @RequestParam(value = "element", required = false, defaultValue = "lightningCharacteristics")
            Option.KeyElementOption element) {
        return ResponseDTO.success(keyElementOverviewService.overview(
                since, until, regionCodeService.requireCode(regionCode), element));
    }


    @Operation(summary = "相关系数条形图 【BAR CHART】", description = "区间内各环境因子相关系数的均值，无数据记 0，正负使用不同的颜色")
    @GetMapping("/bar-chart/correlation-coefficient")
    public ResponseDTO<CorrelationCoefficientVO> getCorrelationCoefficientBarChart(
            @Parameter(description = SINCE_DESC, example = "2024-01-01")
            @RequestParam("since") LocalDate since,
            @Parameter(description = UNTIL_DESC, example = "2024-12-31")
            @RequestParam("until") LocalDate until) {
        return ResponseDTO.success(lightningFeatureService.correlationCoefficientBarChart(since, until));
    }

    @Operation(summary = "主导因子条形图 【BAR CHART】", description = "区间内各环境主导因子相关系数的均值，无数据记 0")
    @GetMapping("/bar-chart/leading-factor")
    public ResponseDTO<LeadingFactorVO> getLeadingFactorBarChart(
            @Parameter(description = SINCE_DESC, example = "2024-01-01")
            @RequestParam("since") LocalDate since,
            @Parameter(description = UNTIL_DESC, example = "2024-12-31")
            @RequestParam("until") LocalDate until) {
        return ResponseDTO.success(lightningFeatureService.leadingFactorBarChart(since, until));
    }

}
