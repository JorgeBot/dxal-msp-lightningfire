package cn.gov.cma.hl.dxal.msp.lightningfire.controller;

import cn.gov.cma.hl.dxal.msp.lightningfire.constant.Option;
import cn.gov.cma.hl.dxal.msp.lightningfire.constant.RegionCode;
import cn.gov.cma.hl.dxal.msp.lightningfire.dto.ResponseDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.service.DangerousnessRealtimeService;
import cn.gov.cma.hl.dxal.msp.lightningfire.service.RegionCodeService;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.DangerousnessRealtimeCountyDistributionVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.DangerousnessRealtimeOverviewVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.DangerousnessRealtimeRankingVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.SelectOptionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Arrays;

/**
 * 危险性实时分析页面
 *
 * <p>数据源为实时表 dangerousness_realtime_statistics（粒度：日期 × 区县，主键 date + county_code），
 * 一行一个区县一天的结果：危险性评分与四个气象要素。取数与汇总口径见
 * {@link DangerousnessRealtimeService}。</p>
 *
 * <p>三个取数接口都围绕同一个 {@code element} 参数（见 {@link Option.RealtimeElementOption}，
 * 危险性评分本身也算一个要素）：{@code /overview} 给区域级的四项统计（最大值、最小值、平均值、极差）、
 * {@code /counties} 给每个区县的要素值（地图 / 县级横条）、{@code /ranking} 给单日前八名的排行。
 * 前两个按 since ~ until 取区间，排行是单日快照（一个日期一次排名）。</p>
 *
 * <p>与综合风险评估与区划、环境影响阈值模块的区别：本模块只取实时表的日期区间（实测 2026-09-01—09-30），
 * 与逐日历史评分表 dangerousness_statistics（2018—2024）不是同一批数据、不能拼成一条时间序列；
 * 历史评分的区间查询属于环境影响阈值模块的
 * {@code /environmental-impact-threshold/score-trend}。</p>
 *
 * <p>regionCode 参数的取值说明、校验与「码 → 区域」转换统一由 {@link RegionCodeService} 提供，
 * 与 /common/region-code 下拉、其它模块共用同一套区划口径。</p>
 */
@RestController
@Tag(name = "危险性实时分析")
@RequestMapping("/dangerousness-realtime")
@RequiredArgsConstructor
@CrossOrigin("*")
public class DangerousnessRealtimeController {

    private static final String SINCE_DESC = "统计起始日期（含），格式 yyyy-MM-dd";

    private static final String UNTIL_DESC = "统计结束日期（含），格式 yyyy-MM-dd";

    private static final String DATE_DESC = "统计日期（必填），格式 yyyy-MM-dd";

    private static final String ELEMENT_DESC = """
            要素：score=危险性评分（默认）、temperature=气温、relativeHumidity=相对湿度、\
            windSpeed=风速、rain=降水；三个取数接口（overview / counties / ranking）的 element \
            口径一致，取值可通过 /dangerousness-realtime/element/options 获取""";

    /**
     * 实时表的覆盖范围，用于说明为什么日期参数要显式传入而不是取「今天」。
     */
    private static final String REALTIME_RANGE_DESC =
            "数据源只覆盖 2026-09-01—2026-09-30（每天 125 个县级单元，全省 13 个地市）";

    /**
     * element 的默认值：危险性评分（页面主口径）。注解属性要求编译期常量，故这里写字面量，
     * 不能写成 {@code Option.RealtimeElementOption.score.name()}。
     */
    private static final String DEFAULT_ELEMENT = "score";

    private final DangerousnessRealtimeService dangerousnessRealtimeService;

    private final RegionCodeService regionCodeService;

    @Operation(summary = "危险性实时分析·要素 【OPTIONS】",
            description = "select 选项，value 为 element 参数取值（含危险性评分与四个气象要素），"
                    + "label 为要素名称；各要素的展示单位见取数接口返回的 unit")
    @GetMapping("/element/options")
    public ResponseDTO<SelectOptionVO[]> getElementOptions() {
        SelectOptionVO[] optionVOS = Arrays.stream(Option.RealtimeElementOption.values())
                .map(e -> new SelectOptionVO(e.getLabel(), e.name()))
                .toArray(SelectOptionVO[]::new);
        return ResponseDTO.success(optionVOS);
    }

    /**
     * 要素概览统计：所选要素在区间内的最大值、最小值、平均值与极差。
     *
     * <p>原来「概览统计返回所有区县的风险分数数组」的职责已拆到 {@code /counties}：本接口只给区域级的
     * 四项统计，逐区县数值由前端按所选区域的行政级别另行取值（选区县直接展示该县的值、
     * 选市一级对所辖县区的值取平均后展示）。</p>
     */
    @Operation(summary = "危险性实时分析·要素概览统计 【ARTICLE GRID】",
            description = """
                    返回所选区域在 since ~ until 内所选要素的要素概览统计，固定四项、顺序固定：\
                    最大值、最小值、平均值、极差（最大值 − 最小值）；\
                    统计样本是区域内全部「区县 × 日」有效记录，逐日等权、不按面积或站点数加权\
                    （与 /ltg-env 统计概览同口径），validRecordCount 即参与统计的记录条数；\
                    本接口不做区县明细：逐区县的数值用 /dangerousness-realtime/counties 取，\
                    概览页展示哪个数值由前端按所选区域的行政级别决定（选区县取该县的值、选地市取所辖县区平均）；\
                    区间内该要素一条有效记录都没有时四项统计的值均为 null、validRecordCount 为 0，\
                    接口返回 200 而不是 404——「这段时间还没有结果」本身就是实时页面要展示的状态；""" + REALTIME_RANGE_DESC
                    + "，区间外的日期即返回空统计；since 晚于 until 时返回 400；regionCode 不是有效区划码时返回 400")
    @GetMapping("/overview")
    public ResponseDTO<DangerousnessRealtimeOverviewVO> getOverview(
            @Parameter(description = SINCE_DESC, example = "2026-09-01")
            @RequestParam("since") LocalDate since,
            @Parameter(description = UNTIL_DESC, example = "2026-09-30")
            @RequestParam("until") LocalDate until,
            @Parameter(description = RegionCodeService.CODE_DESC, example = "232700")
            @RequestParam("regionCode") Integer regionCode,
            @Parameter(description = ELEMENT_DESC, example = "temperature")
            @RequestParam(value = "element", required = false, defaultValue = DEFAULT_ELEMENT)
            Option.RealtimeElementOption element) {
        requireRange(since, until);
        RegionCode region = regionCodeService.requireCode(regionCode);
        return ResponseDTO.success(dangerousnessRealtimeService.overview(since, until, region, element));
    }

    /**
     * 各区县要素值：地图配色 / 县级横条 / 点击县弹窗的数据。
     *
     * <p>从概览接口拆出来单独成接口：概览只需四项统计，而地图需要范围内每个区县的数值，
     * 两者取数口径相同（同一个 element、同一段区间），前端按需调用即可。</p>
     */
    @Operation(summary = "危险性实时分析·各区县要素值 【MAP + BAR CHART】",
            description = """
                    返回所选区域在 since ~ until 内每个区县的所选要素值（地图配色 / 县级横条 / 点击县弹窗）：\
                    每县取该要素在区间内的平均值（逐日等权、不按面积或站点数加权），\
                    区间只选一天（since = until）时即该县当天的单日快照；\
                    counties 按数值降序（无记录的 value 为 null、排在最后），\
                    countyCount 是范围内区县总数、validCountyCount 才是区间内有有效记录的区县个数；\
                    区县范围：选择地市返回所辖县区，选择县市区返回该县区本身，选择 230000 返回全省全部县级单元；\
                    数值单位在 unit 字段给出一次（同一要素的区县项单位相同，不在每一项里重复）；\
                    区间内一条有效记录都没有时各区县 value 均为 null、validCountyCount 为 0，\
                    接口返回 200 而不是 404；""" + REALTIME_RANGE_DESC
                    + "，区间外的日期即返回全 null；since 晚于 until 时返回 400；regionCode 不是有效区划码时返回 400")
    @GetMapping("/counties")
    public ResponseDTO<DangerousnessRealtimeCountyDistributionVO> getCounties(
            @Parameter(description = SINCE_DESC, example = "2026-09-01")
            @RequestParam("since") LocalDate since,
            @Parameter(description = UNTIL_DESC, example = "2026-09-30")
            @RequestParam("until") LocalDate until,
            @Parameter(description = RegionCodeService.CODE_DESC, example = "232700")
            @RequestParam("regionCode") Integer regionCode,
            @Parameter(description = ELEMENT_DESC, example = "temperature")
            @RequestParam(value = "element", required = false, defaultValue = DEFAULT_ELEMENT)
            Option.RealtimeElementOption element) {
        requireRange(since, until);
        RegionCode region = regionCodeService.requireCode(regionCode);
        return ResponseDTO.success(dangerousnessRealtimeService.counties(since, until, region, element));
    }

    /**
     * 要素排行：当日前八名区县。
     *
     * <p>排行对象由 element 决定（危险性评分或某个气象要素），不再是固定的风险分数；
     * 柱状图与折线图共用这份数据（X 轴为区县、顺序即名次，Y 轴为该要素值）。</p>
     */
    @Operation(summary = "危险性实时分析·要素排行 【BAR CHART + LINE CHART】",
            description = """
                    返回所选区域在 date 当天所选要素最高的 8 个区县，按数值降序排列\
                    （X 轴为区县、顺序即名次，Y 轴为该要素值，柱状图 / 折线图共用这份数据）：\
                    rank 为名次、name / cityName 为区县与所属地市、value 为该要素当日值，\
                    title 按要素生成（如「危险性评分排行（前 8）」，单位见 unit）；\
                    区县范围与 overview / counties 一致：选择地市返回所辖县区，选择县市区返回该县区本身，\
                    选择 230000 返回全省全部县级单元；\
                    当天该要素缺测的区县不参与排名（缺测不是低值，不占名次），\
                    范围内有效区县不足 8 个时按实际个数返回（选择县市区时只有一个点），\
                    一条有效值都没有时 items 为空数组、接口返回 200 而不是 404；""" + REALTIME_RANGE_DESC
                    + "，区间外的日期即返回空排行；regionCode 不是有效区划码时返回 400")
    @GetMapping("/ranking")
    public ResponseDTO<DangerousnessRealtimeRankingVO> getRanking(
            @Parameter(description = DATE_DESC, example = "2026-09-15")
            @RequestParam("date") LocalDate date,
            @Parameter(description = RegionCodeService.CODE_DESC, example = "230000")
            @RequestParam("regionCode") Integer regionCode,
            @Parameter(description = ELEMENT_DESC, example = "score")
            @RequestParam(value = "element", required = false, defaultValue = DEFAULT_ELEMENT)
            Option.RealtimeElementOption element) {
        RegionCode region = regionCodeService.requireCode(regionCode);
        return ResponseDTO.success(dangerousnessRealtimeService.ranking(date, region, element));
    }

    /**
     * 区间入参校验：since 晚于 until 时记 400，而不是把空区间当成「没有数据」静默返回。
     */
    private void requireRange(LocalDate since, LocalDate until) {
        if (since.isAfter(until)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "开始时间不能晚于结束时间：" + since + " ~ " + until);
        }
    }
}
