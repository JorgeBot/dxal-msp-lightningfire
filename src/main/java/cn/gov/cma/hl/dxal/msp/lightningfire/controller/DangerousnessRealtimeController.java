package cn.gov.cma.hl.dxal.msp.lightningfire.controller;

import cn.gov.cma.hl.dxal.msp.lightningfire.constant.RegionCode;
import cn.gov.cma.hl.dxal.msp.lightningfire.dto.ResponseDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.service.DangerousnessRealtimeService;
import cn.gov.cma.hl.dxal.msp.lightningfire.service.RegionCodeService;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.DangerousnessRealtimeOverviewVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.DangerousnessRealtimeRankingVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 危险性实时分析页面
 *
 * <p>数据源为实时表 dangerousness_realtime_statistics（粒度：日期 × 区县，主键 date + county_code），
 * 一行一个区县一天的实时危险性结果：危险性评分、危险等级文案与四个要素的分数，
 * 取数与汇总口径见 {@link DangerousnessRealtimeService}。</p>
 *
 * <p>与综合风险评估与区划、环境影响阈值模块的区别：本模块只取<b>单日</b>的实时结果
 * （两个接口都只有 date 一个日期参数、不做区间展开），且评分、等级、四个要素都由源表直接给出，
 * 接口不重算等级、不做 AHP 合成；逐日历史评分的区间查询属于环境影响阈值模块的
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

    private static final String DATE_DESC = "统计日期（必填），格式 yyyy-MM-dd";

    /**
     * 实时表的覆盖范围，用于说明为什么日期参数要显式传入而不是取「今天」。
     */
    private static final String REALTIME_RANGE_DESC =
            "数据源只覆盖 2026-09-01—2026-09-30（每天 125 个县级单元，全省 13 个地市）";

    private final DangerousnessRealtimeService dangerousnessRealtimeService;

    private final RegionCodeService regionCodeService;

    /**
     * 危险性实时分析概览统计：风险等级、危险性分数与四个要素的分数。
     *
     * <p>输出所选区域范围内所有区县的风险分数数组（每县一行，含等级与四个要素的分数），
     * 区域内取哪个数值展示由前端按所选区域的行政级别决定：选区县一级直接取该县那一行，
     * 选市一级对该市下所有区县的分数取平均后展示。</p>
     */
    @Operation(summary = "危险性实时分析·概览统计 【ARTICLE GRID + MAP】",
            description = """
                    返回所选区域在 date 当天的逐区县危险性概览：危险性评分（0—100 百分制，越高越危险）、\
                    危险等级文案（III 中等 / IV 较高 / V 高 / 无数据）与四个要素的分数\
                    （顺序固定：气温 ℃、相对湿度 %、风速 m/s、降水 mm）；\
                    区县范围：选择地市返回所辖县区，选择县市区返回该县区本身，选择 230000 返回全省全部县级单元；\
                    本接口不做区域汇总：选地市 / 全省时展示用的数值由前端按行政区级别对各区县取平均\
                    （选区县直接取该县那一行）；\
                    等级取自源表、不按分数重算（等级由底层危险性 H 的原始分档给出，与评分的分段不逐点重合）；\
                    当天没有结果的区县照常返回、分数与四个要素为 null、等级为「无数据」（实测鹤岗市向阳区、\
                    工农区无结果），不补 0，因此 validCountyCount 才是结果有效的区县个数；\
                    counties 按分数降序（无结果的排最后）；当天一条结果都没有时返回空快照而不是 404——\
                    「这一天还没有结果」本身就是实时页面要展示的状态；""" + REALTIME_RANGE_DESC
                    + "，区间外的日期即返回空快照；regionCode 不是有效区划码时返回 400")
    @GetMapping("/overview")
    public ResponseDTO<DangerousnessRealtimeOverviewVO> getOverview(
            @Parameter(description = DATE_DESC, example = "2026-09-01")
            @RequestParam("date") LocalDate date,
            @Parameter(description = RegionCodeService.CODE_DESC, example = "232700")
            @RequestParam("regionCode") Integer regionCode) {
        RegionCode region = regionCodeService.requireCode(regionCode);
        return ResponseDTO.success(dangerousnessRealtimeService.overview(date, region));
    }

    /**
     * 风险分数折线图排行：当日危险性分数前八名的区县。
     *
     * <p>折线图的 X 轴是区县、按分数由高到低排列（一个日期一次排名、一条折线），Y 轴是危险性分数，
     * 前端取 items 的 name 作 categories、score 作 series 即可。</p>
     */
    @Operation(summary = "危险性实时分析·风险分数折线图排行 【LINE CHART】",
            description = """
                    返回所选区域在 date 当天危险性分数最高的 8 个区县，按分数降序排列\
                    （X 轴为区县、顺序即名次，Y 轴为危险性分数）：\
                    rank 为名次、name / cityName 为区县与所属地市、score 为评分、level 为危险等级文案；\
                    区县范围与概览统计一致：选择地市返回所辖县区，选择县市区返回该县区本身，\
                    选择 230000 返回全省全部县级单元；\
                    当天结果为空的区县不参与排名（缺测不是低风险，不占名次），\
                    范围内有效区县不足 8 个时按实际个数返回（选择县市区时只有一个点），\
                    一条有效结果都没有时 items 为空数组、接口返回 200 而不是 404；""" + REALTIME_RANGE_DESC
                    + "，区间外的日期即返回空排行；regionCode 不是有效区划码时返回 400")
    @GetMapping("/score-ranking")
    public ResponseDTO<DangerousnessRealtimeRankingVO> getScoreRanking(
            @Parameter(description = DATE_DESC, example = "2026-09-15")
            @RequestParam("date") LocalDate date,
            @Parameter(description = RegionCodeService.CODE_DESC, example = "230000")
            @RequestParam("regionCode") Integer regionCode) {
        RegionCode region = regionCodeService.requireCode(regionCode);
        return ResponseDTO.success(dangerousnessRealtimeService.scoreRanking(date, region));
    }
}
