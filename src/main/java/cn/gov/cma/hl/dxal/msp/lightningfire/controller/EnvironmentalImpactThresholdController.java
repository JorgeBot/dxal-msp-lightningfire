package cn.gov.cma.hl.dxal.msp.lightningfire.controller;

import cn.gov.cma.hl.dxal.msp.lightningfire.constant.Option;
import cn.gov.cma.hl.dxal.msp.lightningfire.constant.RegionCode;
import cn.gov.cma.hl.dxal.msp.lightningfire.dto.ResponseDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.service.DynamicThresholdService;
import cn.gov.cma.hl.dxal.msp.lightningfire.service.EnvironmentalImpactThresholdService;
import cn.gov.cma.hl.dxal.msp.lightningfire.service.FixedThresholdService;
import cn.gov.cma.hl.dxal.msp.lightningfire.service.RegionCodeService;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.DynamicThresholdCountyDistributionVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.DynamicThresholdVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.EnvironmentalThresholdCountyDistributionVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.EnvironmentalThresholdDailyVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.EnvironmentalThresholdPercentTrendVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.EnvironmentalThresholdRuleVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.EnvironmentalThresholdTrendVO;
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
 * 雷击火环境影响阈值页面
 *
 * <p>三套数据源，按页面模块分工：</p>
 * <ul>
 *   <li><b>危险性评分</b>（{@code /score-trend}、{@code /daily-score}）：危险性统计表
 *       dangerousness_statistics 的 score 列（0—100 百分制），按「日期 × 区县 × 地级市」取数，
 *       区域分数先按区县算均值、再按行政区等权取均值（见 {@link EnvironmentalImpactThresholdService}）；</li>
 *   <li><b>固定阈值与条件百分比</b>（{@code /factor/options}、{@code /threshold}、
 *       {@code /condition/trend}、{@code /condition/counties}）：固定尺子来自交接包提取的常量
 *       {@link cn.gov.cma.hl.dxal.msp.lightningfire.constant.FixedThreshold}
 *       （阈值汇总.csv + 固定阈值参数与检验.json），判定结果来自县级逐日条件分布表
 *       condition_distribution_daily 的 {@code {因子}_hit_pct}（已经是 0—100 的百分比），
 *       多县按可判断格网数加权、月 / 年按有效日比例平均（见 {@link FixedThresholdService}）；</li>
 *   <li><b>动态阈值</b>（{@code /dynamic/factor/options}、{@code /dynamic/threshold}、
 *       {@code /dynamic/counties}）：年度候选线（2019—2025）来自交接包提取的常量
 *       {@link cn.gov.cma.hl.dxal.msp.lightningfire.constant.DynamicThreshold}（年度候选与漂移.csv），
 *       逐日结果来自县级逐日动态条件分布表 dynamic_condition_distribution_daily 的
 *       {@code {因子}_hit_pct}（符合新条件）与 {@code {因子}_changed_pct}（与旧条件判定不同），
 *       多县同样按可判断格网数加权（见 {@link DynamicThresholdService}）。</li>
 * </ul>
 *
 * <p>regionCode 参数的取值说明、校验与「码 → 区域」转换统一由 {@link RegionCodeService} 提供，
 * 与 /common/region-code 下拉、/risk-assessment 各接口共用同一套区划口径。</p>
 */
@RestController
@Tag(name = "雷击火环境影响阈值")
@RequestMapping("/environmental-impact-threshold")
@RequiredArgsConstructor
@CrossOrigin("*")
public class EnvironmentalImpactThresholdController {

    private static final String SINCE_DESC = "统计起始日期（含），格式 yyyy-MM-dd";

    private static final String UNTIL_DESC = "统计结束日期（含），格式 yyyy-MM-dd";

    private static final String DATE_DESC = "统计日期（必填），格式 yyyy-MM-dd";

    /**
     * 危险性统计表的覆盖范围，用于说明为什么日期参数要显式传入而不是取「今天」。
     */
    private static final String SCORE_RANGE_DESC = "数据源只覆盖 2018-01-01—2024-12-31（2020 年只有 34 天记录）";

    private static final String FACTOR_DESC = """
            环境因子：lightning_count=雷电记录数、mean_abs_intensity=雷电平均绝对强度、temperature=气温、\
            wind_speed=风速、relative_humidity=相对湿度、rain=日降水、fmc=可燃物含水率FMC、dem=高程、\
            slope=坡度、combined=组合环境条件（组内且、组间或）；\
            取值可通过 /environmental-impact-threshold/factor/options 获取""";

    private static final String CONDITION_GRANULARITY_DESC =
            "时间轴粒度：DAY=天（yyyy-MM-dd）、MONTH=月（yyyy-MM）、YEAR=年（yyyy）；默认 DAY";

    /**
     * 条件分布表的可用范围：只在 5—9 月研究应用范围内有判定结果，其余时间比例列为 NULL。
     */
    private static final String CONDITION_RANGE_DESC =
            "数据源只在 5—9 月研究应用范围内有判定结果（2018-05-01—2024-09-30，全省 125 个县级单元），"
                    + "其余时间无数据（比例列为 NULL，不补 0）";

    private static final String DYNAMIC_FACTOR_DESC = """
            环境因子：lightning_count=雷电记录数、mean_abs_intensity=雷电平均绝对强度、temperature=气温、\
            wind_speed=风速、relative_humidity=相对湿度、rain=日降水、fmc=可燃物含水率FMC、dem=高程、\
            slope=坡度；动态阈值只有九个单因子，没有 combined（组合环境条件只在固定阈值模块）；\
            取值可通过 /environmental-impact-threshold/dynamic/factor/options 获取""";

    /**
     * 逐日动态结果的可用范围：比固定阈值表晚一年起算，2025 只有候选线。
     */
    private static final String DYNAMIC_RANGE_DESC =
            "逐日动态结果只覆盖 2019-05-01—2024-09-30 的 5—9 月（全省 125 个县级单元）；"
                    + "2025 年只有候选线、没有逐日结果（缺 FMC），其余时间无数据（百分比为 NULL，不补 0）";

    private final EnvironmentalImpactThresholdService environmentalImpactThresholdService;

    private final FixedThresholdService fixedThresholdService;

    private final DynamicThresholdService dynamicThresholdService;

    private final RegionCodeService regionCodeService;

    @Operation(summary = "危险性·分数折线图 【LINE CHART】",
            description = """
                    返回所选行政区在 since ~ until 内逐日的危险性分数折线图：\
                    x 轴按天展开 since ~ until（无记录的日期同样占位，保证折线连续），\
                    y 轴为该日区域分数（0—100 百分制）；\
                    逐日分数口径：县市区取该区县当日记录均值，地市取所辖县区当日均值的等权均值，\
                    全省取各地市当日均值的等权均值（先区县后地市，按行政区等权，不做面积加权）；\
                    无记录的日期 score 为 null，不补 0；区间内一条记录都没有时返回一段全 null 的日期轴（200），\
                    不用 0 或空数组冒充无数据；""" + SCORE_RANGE_DESC
                    + "；since 晚于 until 时返回 400；regionCode 不是有效区划码时返回 400")
    @GetMapping("/score-trend")
    public ResponseDTO<EnvironmentalThresholdTrendVO> getScoreTrend(
            @Parameter(description = SINCE_DESC, example = "2024-01-01")
            @RequestParam("since") LocalDate since,
            @Parameter(description = UNTIL_DESC, example = "2024-12-31")
            @RequestParam("until") LocalDate until,
            @Parameter(description = RegionCodeService.CODE_DESC, example = "232700")
            @RequestParam("regionCode") Integer regionCode) {
        if (since.isAfter(until)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "开始时间不能晚于结束时间：" + since + " ~ " + until);
        }
        RegionCode region = regionCodeService.requireCode(regionCode);
        return ResponseDTO.success(environmentalImpactThresholdService.scoreTrend(since, until, region));
    }

    @Operation(summary = "危险性·当日区县分数 【BAR CHART + ARTICLE GRID】",
            description = """
                    返回所选行政区在 date 当天的区县分数快照：当日有效区县个数、区县分数的最小值与最大值\
                    （含对应区县名）、当日分数最高值前五柱状图、所选地区每个县的分数；\
                    区县范围：选择地市返回所辖县区，选择县市区返回该县区本身，选择 230000 返回全省全部县区；\
                    当日没有记录的县区照常返回、score 为 null（不补 0），有效区县个数即其中分数非 null 的个数；\
                    counties 按分数降序（无记录的排最后），topChart.items 就是它的前 5 条；\
                    该日一条记录都没有时返回空快照（有效区县个数 0、最小 / 最大为 null、items 为空数组）\
                    而不是 404——「当天没数据」本身就是该页面要展示的状态；""" + SCORE_RANGE_DESC
                    + "，区间外的日期即返回空快照；regionCode 不是有效区划码时返回 400")
    @GetMapping("/daily-score")
    public ResponseDTO<EnvironmentalThresholdDailyVO> getDailyScore(
            @Parameter(description = DATE_DESC, example = "2024-05-01")
            @RequestParam("date") LocalDate date,
            @Parameter(description = RegionCodeService.CODE_DESC, example = "232700")
            @RequestParam("regionCode") Integer regionCode) {
        RegionCode region = regionCodeService.requireCode(regionCode);
        return ResponseDTO.success(environmentalImpactThresholdService.dailyScore(date, region));
    }

    @Operation(summary = "环境影响阈值·环境因子 【OPTIONS】",
            description = "select 选项，value 为 factor 参数取值（与成果文件因子 id 一致），label 为因子名称；"
                    + "顺序与 因子字典.csv、阈值汇总.csv 一致，combined（组合环境条件）排在最后")
    @GetMapping("/factor/options")
    public ResponseDTO<SelectOptionVO[]> getFactorOptions() {
        SelectOptionVO[] optionVOS = Arrays.stream(Option.EnvironmentalFactorOption.values())
                .map(e -> new SelectOptionVO(e.getLabel(), e.name()))
                .toArray(SelectOptionVO[]::new);
        return ResponseDTO.success(optionVOS);
    }

    @Operation(summary = "固定阈值·固定阈值规则 【ARTICLE GRID】",
            description = """
                    返回所选环境因子的固定条件界线与检验结果，数值已从交接包提取为项目常量，\
                    接口不读 CSV/JSON、不重新训练：\
                    方向 operator（ge=≥、gt=>、le=≤）与符号、阈值 threshold（完整值，展示可四舍五入）、\
                    单位 unit、95% 置信区间 ciLower/ciUpper、成果状态 status，\
                    以及 2024 年完整格网 holdout 检验结果 metrics2024（pod 有火记录中符合比例、\
                    fpr 无火记录中也符合比例、j=POD−FPR，均为 0—1，前端乘 100 显示百分比）；\
                    combined（组合环境条件）没有单列阈值：threshold/unit/ci 为 null，paths 返回三条路径\
                    （路径内条件「且」、路径间「或」，不能用九条线简单求交代替）；\
                    阈值只适用于 5—9 月研究应用范围，是参考条件而不是报警线，\
                    status=WEAK_OR_UNSUPPORTED 的三条（雷电记录数、雷电平均绝对强度、日降水）不能当报警线；\
                    metrics2024 是历史回看检验，不是所选县当天的准确率""")
    @GetMapping("/threshold")
    public ResponseDTO<EnvironmentalThresholdRuleVO> getThreshold(
            @Parameter(description = FACTOR_DESC, example = "rain")
            @RequestParam(value = "factor", required = false, defaultValue = "lightning_count")
            Option.EnvironmentalFactorOption factor) {
        return ResponseDTO.success(fixedThresholdService.rule(factor));
    }

    @Operation(summary = "固定阈值·条件百分比折线图 【LINE CHART】",
            description = """
                    返回所选行政区内所选环境因子在 since ~ until 内逐时间点的达到条件百分比（0—100，\
                    前端直接加 % 显示）：x 轴恒按 granularity 展开 since ~ until（无数据的时间点同样占位）；\
                    区域当天百分比 = Σ(县比例 × 该县可判断格网数) ÷ Σ可判断格网数，\
                    等价于「符合数之和 ÷ 可判断数之和」，不是各县百分比简单平均；\
                    月 / 年粒度为该周期内有效日区域比例的均值，points.validDays 为参与平均的有效日数；\
                    没有有效日的周期 percent 为 null（缺测断线，不补 0）；""" + CONDITION_RANGE_DESC
                    + "；" + CONDITION_GRANULARITY_DESC
                    + "；since 晚于 until 时返回 400；regionCode 不是有效区划码时返回 400")
    @GetMapping("/condition/trend")
    public ResponseDTO<EnvironmentalThresholdPercentTrendVO> getConditionTrend(
            @Parameter(description = SINCE_DESC, example = "2024-05-01")
            @RequestParam("since") LocalDate since,
            @Parameter(description = UNTIL_DESC, example = "2024-09-30")
            @RequestParam("until") LocalDate until,
            @Parameter(description = RegionCodeService.CODE_DESC, example = "232700")
            @RequestParam("regionCode") Integer regionCode,
            @Parameter(description = FACTOR_DESC, example = "rain")
            @RequestParam(value = "factor", required = false, defaultValue = "lightning_count")
            Option.EnvironmentalFactorOption factor,
            @Parameter(description = CONDITION_GRANULARITY_DESC)
            @RequestParam(value = "granularity", required = false, defaultValue = "DAY")
            Option.GranularityOption granularity) {
        if (since.isAfter(until)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "开始时间不能晚于结束时间：" + since + " ~ " + until);
        }
        RegionCode region = regionCodeService.requireCode(regionCode);
        return ResponseDTO.success(fixedThresholdService.conditionTrend(since, until, region, factor, granularity));
    }

    @Operation(summary = "固定阈值·各县条件百分比 【BAR CHART + MAP】",
            description = """
                    返回所选区域内所有区县在所选日期的达到条件百分比（地图配色 / 县级横条 / 点击县弹窗）：\
                    counties 按百分比降序（无数据的县 percent 为 null、排在最后、validDays 为 0），\
                    区域内「无数据」与「0%（有数据但没有格网符合）」可区分；\
                    区县范围：选择地市返回所辖县区，选择县市区返回该县区本身，选择 230000 返回全省全部县级单元；\
                    regionPercent 为区域合计：DAY 为按可判断格网数加权的各县比例，\
                    MONTH / YEAR 为周期内有效日区域比例的均值；\
                    granularity=DAY（默认）取所选日期当天，与「地图仍显示所选单日」一致；\
                    MONTH / YEAR 取该日期所在月 / 年内该县有效日比例的平均值（每县一个值）；\
                    区域内一个有效数据都没有时 regionPercent 为 null，不返回 404；""" + CONDITION_RANGE_DESC
                    + "；regionCode 不是有效区划码时返回 400")
    @GetMapping("/condition/counties")
    public ResponseDTO<EnvironmentalThresholdCountyDistributionVO> getCountyConditions(
            @Parameter(description = DATE_DESC, example = "2024-08-20")
            @RequestParam("date") LocalDate date,
            @Parameter(description = RegionCodeService.CODE_DESC, example = "232700")
            @RequestParam("regionCode") Integer regionCode,
            @Parameter(description = FACTOR_DESC, example = "rain")
            @RequestParam(value = "factor", required = false, defaultValue = "lightning_count")
            Option.EnvironmentalFactorOption factor,
            @Parameter(description = CONDITION_GRANULARITY_DESC)
            @RequestParam(value = "granularity", required = false, defaultValue = "DAY")
            Option.GranularityOption granularity) {
        RegionCode region = regionCodeService.requireCode(regionCode);
        return ResponseDTO.success(fixedThresholdService.countyConditions(date, region, factor, granularity));
    }

    @Operation(summary = "动态阈值·环境因子 【OPTIONS】",
            description = "select 选项，value 为 factor 参数取值；与固定阈值共用同一套因子枚举，"
                    + "去掉动态阈值没有的 combined（组合环境条件只在固定阈值模块）")
    @GetMapping("/dynamic/factor/options")
    public ResponseDTO<SelectOptionVO[]> getDynamicFactorOptions() {
        SelectOptionVO[] optionVOS = Arrays.stream(Option.EnvironmentalFactorOption.values())
                .filter(e -> e != Option.EnvironmentalFactorOption.combined)
                .map(e -> new SelectOptionVO(e.getLabel(), e.name()))
                .toArray(SelectOptionVO[]::new);
        return ResponseDTO.success(optionVOS);
    }

    @Operation(summary = "动态阈值·年度候选与当前固定阈值 【LINE CHART + ARTICLE GRID】",
            description = """
                    返回所选环境因子 2019—2025 年的逐年候选阈值与当前的固定阈值，数值已从交接包提取为项目常量，\
                    接口不读 CSV、不重新训练：\
                    年度阈值折线用 candidates 的 year/threshold，折线竖线区间用 ciLow/ciHigh\
                    （按 7 天时间块重抽样 500 次的分位，区间宽表示候选不稳定，不是当天因子最小最大值）；\
                    「固定基线 → 年度候选」与阈值差值用每条候选的 baseline/delta（delta 保留正负号）；\
                    训练口径用 availableDays/expectedDays/missingDays/positive，trainStart/trainEnd 为拟合年份区间；\
                    operator 是当年候选的方向（可能与基线不同，如风速 2019、2020 年为 ge）；\
                    fixedThreshold 即当前固定阈值规则（与固定阈值模块同一条线，含 2024 检验结果）；\
                    复核建议看 adoptionStatus/adoptionLabel：2019—2023 为历史回看、\
                    2024 为维持基线或有改善迹象仍需复核、2025 缺当年检验暂不采用；\
                    动态窗口是累积历史（expanding），历史回放不是预报；\
                    动态阈值没有 combined（组合环境条件），传了返回 400""")
    @GetMapping("/dynamic/threshold")
    public ResponseDTO<DynamicThresholdVO> getDynamicThreshold(
            @Parameter(description = DYNAMIC_FACTOR_DESC, example = "rain")
            @RequestParam(value = "factor", required = false, defaultValue = "lightning_count")
            Option.EnvironmentalFactorOption factor) {
        requireDynamicFactor(factor);
        return ResponseDTO.success(dynamicThresholdService.annualCandidates(factor));
    }

    @Operation(summary = "动态阈值·各县符合新条件与判定改变百分比 【MAP + BAR CHART】",
            description = """
                    返回所选区域内所有区县在所选日期的动态阈值结果：hitPercent 为符合当年候选条件的格网占比、\
                    changedPercent 为与旧固定基线条件判定不同的格网占比（×100 后直接显示 %，不是准确率提高，\
                    也不是「符合旧条件」的同义词；两者一般不互补）；\
                    该日使用 assessmentYear 那一年的候选线（日期 2024-08-20 用 year=2024），逐日表已存应用结果，\
                    接口不重新逐格判定；counties 按 hitPercent 降序（无数据的县两个百分比为 null、排在最后），\
                    「无数据」与「0%」可区分；\
                    区县范围：选择地市返回所辖县区，选择县市区返回该县区本身，选择 230000 返回全省全部县级单元；\
                    regionHitPercent/regionChangedPercent 为区域合计，按可判断格网数加权\
                    （Σ(县百分比 × 可判断格网数) ÷ Σ可判断格网数，等价于「符合数（变化数）之和 ÷ 可判断数之和」，\
                    不是各县百分比简单平均），区域内无有效数据时为 null，不返回 404；""" + DYNAMIC_RANGE_DESC
                    + "；动态阈值没有 combined（组合环境条件），传了返回 400；regionCode 不是有效区划码时返回 400")
    @GetMapping("/dynamic/counties")
    public ResponseDTO<DynamicThresholdCountyDistributionVO> getDynamicCounties(
            @Parameter(description = DATE_DESC, example = "2024-08-20")
            @RequestParam("date") LocalDate date,
            @Parameter(description = RegionCodeService.CODE_DESC, example = "232700")
            @RequestParam("regionCode") Integer regionCode,
            @Parameter(description = DYNAMIC_FACTOR_DESC, example = "rain")
            @RequestParam(value = "factor", required = false, defaultValue = "lightning_count")
            Option.EnvironmentalFactorOption factor) {
        requireDynamicFactor(factor);
        RegionCode region = regionCodeService.requireCode(regionCode);
        return ResponseDTO.success(dynamicThresholdService.countyDistribution(date, region, factor));
    }

    /**
     * 动态阈值只有九个单因子：{@code combined}（组合环境条件）只在固定阈值模块有，
     * 传了记 400 而不是静默返回空图。
     */
    private void requireDynamicFactor(Option.EnvironmentalFactorOption factor) {
        if (factor == Option.EnvironmentalFactorOption.combined) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "动态阈值没有 combined（组合环境条件），"
                    + "组合条件请用 /environmental-impact-threshold/threshold?factor=combined");
        }
    }
}
