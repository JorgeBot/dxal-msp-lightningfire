package cn.gov.cma.hl.dxal.msp.lightningfire.controller;

import cn.gov.cma.hl.dxal.msp.lightningfire.constant.Option;
import cn.gov.cma.hl.dxal.msp.lightningfire.dto.ResponseDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature.LightningCharacteristicsDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.service.LightningFeatureService;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Arrays;

/**
 * 雷击火环境
 */
@RestController
@Tag(name = "雷击火环境")
@RequestMapping("/ltg-env")
@RequiredArgsConstructor
public class LightningEnvironmentController {

    private static final String SINCE_DESC = "统计起始日期（含），格式 yyyy-MM-dd";
    private static final String UNTIL_DESC = "统计结束日期（含），格式 yyyy-MM-dd";
    private static final String REGION_DESC = "分析区域：all=大兴安岭地区（全区域）、hzh=呼中区、xlin=新林区、slin=松岭区、"
            + "jgdc=加格达奇区、mh=漠河市、th=塔河县、hm=呼玛县；默认 all，取值可通过 /ltg-env/region/options 获取";
    private static final String GRANULARITY_DESC = "时间轴粒度：DAY=天（yyyy-MM-dd）、MONTH=月（yyyy-MM）、YEAR=年（yyyy）；默认 DAY";
    private static final String ELEMENTS_DESC = "雷击火关键要素，可多选：precipitation=降水、temperature=气温、windSpeed=风速、"
            + "relativeHumidity=相对湿度、soilMoisture=土壤水、fuelMoisture=可燃物含水率、wetnessIndex=湿润指数、"
            + "comprehensiveRisk=综合风险；取值可通过 /ltg-env/key-element/checkboxes 获取";

    private final LightningFeatureService lightningFeatureService;

    @Operation(summary = "雷击火关键要素", description = "checkbox 选项，key 为 /lightning-elements 的 elements 参数取值")
    @GetMapping("/key-element/checkboxes")
    public ResponseDTO<CheckboxVO[]> getKeyElementOptions() {
        CheckboxVO[] optionVOS = Arrays.stream(Option.KeyElementOption.values())
                .map(e -> new CheckboxVO(e.name(), e.getLabel()))
                .toArray(CheckboxVO[]::new);
        return ResponseDTO.success(optionVOS);
    }


    @Operation(summary = "分析区域", description = "select 中的 option 元素，value 为 region 参数取值")
    @GetMapping("/region/options")
    public ResponseDTO<SelectOptionVO[]> getRegionOptions() {
        SelectOptionVO[] optionVOS = Arrays.stream(Option.RegionOption.values())
                .map(e -> new SelectOptionVO(e.getLabel(), e.name()))
                .toArray(SelectOptionVO[]::new);
        return ResponseDTO.success(optionVOS);
    }

    @Operation(summary = "雷击火时间轴与地闪特征",
            description = "返回 since ~ until 内按 granularity 展开的完整时间轴，以及区间内（可按 region 过滤）的雷击火记录点位，"
                    + "点位按发现时间升序；区间内无雷击火记录时 series 为空数组")
    @GetMapping("/time-axis/lightning-fire")
    public ResponseDTO<LightningFireTimeAxisVO> getLightningFireTimeAxis(
            @Parameter(description = SINCE_DESC, example = "2024-01-01")
            @RequestParam("since") LocalDate since,
            @Parameter(description = UNTIL_DESC, example = "2024-12-31")
            @RequestParam("until") LocalDate until,
            @Parameter(description = REGION_DESC)
            @RequestParam(value = "region", required = false, defaultValue = "all") Option.RegionOption region,
            @Parameter(description = GRANULARITY_DESC)
            @RequestParam(value = "granularity", required = false, defaultValue = "DAY") Option.GranularityOption granularity) {
        return ResponseDTO.success(lightningFeatureService.lightningFireTimeAxis(since, until, region, granularity));
    }

    @Operation(summary = "地闪特征",
            description = "区间内（可按 region 过滤）的地闪次数、正闪比例、地闪密度与地闪强度，"
                    + "比例/密度/强度按各地闪记录数加权平均；无数据时各项均为 0")
    @GetMapping("/lightning-characteristics")
    public ResponseDTO<LightningCharacteristicsDTO> getLightningCharacteristics(
            @Parameter(description = SINCE_DESC, example = "2024-01-01")
            @RequestParam("since") LocalDate since,
            @Parameter(description = UNTIL_DESC, example = "2024-12-31")
            @RequestParam("until") LocalDate until,
            @Parameter(description = REGION_DESC)
            @RequestParam(value = "region", required = false, defaultValue = "all") Option.RegionOption region) {
        return ResponseDTO.success(lightningFeatureService.lightningCharacteristics(since, until, region));
    }

    @Operation(summary = "雷击火要素",
            description = "checkbox 每次勾选 push 到要素详情列表中。要素序列数据源尚未接入，当前仅按 since / until / granularity "
                    + "返回时间轴，供前端渲染空图表；region 与 elements 为数据接入后的查询条件，现仅做参数校验")
    @GetMapping("/lightning-elements")
    public ResponseDTO<LightningElementsVO> getLightningElements(
            @Parameter(description = SINCE_DESC, example = "2024-01-01")
            @RequestParam("since") LocalDate since,
            @Parameter(description = UNTIL_DESC, example = "2024-12-31")
            @RequestParam("until") LocalDate until,
            @Parameter(description = REGION_DESC)
            @RequestParam(value = "region", required = false, defaultValue = "all") Option.RegionOption region,
            @Parameter(description = GRANULARITY_DESC)
            @RequestParam(value = "granularity", required = false, defaultValue = "DAY") Option.GranularityOption granularity,
            @Parameter(description = ELEMENTS_DESC, example = "precipitation,temperature")
            @RequestParam("elements") Option.KeyElementOption[] elements) {
        return ResponseDTO.success(lightningFeatureService.lightningElementsTimeAxis(since, until, granularity));
    }

    @Operation(summary = "相关系数条形图", description = "区间内各环境因子相关系数的均值，无数据记 0，正负使用不同的颜色")
    @GetMapping("/bar-chart/correlation-coefficient")
    public ResponseDTO<CorrelationCoefficientVO> getCorrelationCoefficientBarChart(
            @Parameter(description = SINCE_DESC, example = "2024-01-01")
            @RequestParam("since") LocalDate since,
            @Parameter(description = UNTIL_DESC, example = "2024-12-31")
            @RequestParam("until") LocalDate until) {
        return ResponseDTO.success(lightningFeatureService.correlationCoefficientBarChart(since, until));
    }

    @Operation(summary = "主导因子条形图", description = "区间内各环境主导因子相关系数的均值，无数据记 0")
    @GetMapping("/bar-chart/leading-factor")
    public ResponseDTO<LeadingFactorVO> getLeadingFactorBarChart(
            @Parameter(description = SINCE_DESC, example = "2024-01-01")
            @RequestParam("since") LocalDate since,
            @Parameter(description = UNTIL_DESC, example = "2024-12-31")
            @RequestParam("until") LocalDate until) {
        return ResponseDTO.success(lightningFeatureService.leadingFactorBarChart(since, until));
    }


}
