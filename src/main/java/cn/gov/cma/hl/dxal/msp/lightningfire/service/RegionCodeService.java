package cn.gov.cma.hl.dxal.msp.lightningfire.service;

import cn.gov.cma.hl.dxal.msp.lightningfire.constant.RegionCode;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.SelectOptionVO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 区划码：全项目「6 位行政区划码 ↔ {@link RegionCode}」的唯一出入口。
 *
 * <p>接口层的 regionCode 参数、下拉选项的 value、各汇总表的 region_code 列统一都是 6 位行政区划码；
 * 业务代码则用枚举表达区域语义（取名称过滤地区名列、按范围判断可选区划、按码反查）。两类表示之间的
 * 转换规则收拢在本类，新增区划相关需求（下拉、校验、取名）都从这里进，不要在控制器或别的服务里
 * 再各写一段 {@code values() + filter} 或「码 → 枚举」的循环——否则规则一改就要全项目搜一遍。</p>
 */
@Service
public class RegionCodeService {

    /**
     * regionCode / code 参数的取值说明（OpenAPI 文档用），全项目共用一份。
     *
     * <p>区划码的位数与层级含义只有这一个定义；控制器只补充自己的参数是必填还是可空、默认值是多少，
     * 不再各写一版说明——此前的两版就分别多写／漏写了「原 4 位地市码后补 00」与「留空表示全省」。</p>
     */
    public static final String CODE_DESC = """
            行政区划代码（6 位，GB/T 2260）：230000=全省、\
            230100、230200…232700=地市（原 4 位地市码后补 00）、230102、230103…=县市区""";

    /**
     * 区划下拉选项，供 select 直接渲染。
     *
     * <p>value 统一为 6 位行政区划码字符串（如 232701），label 为区划名称；顺序沿用
     * {@link RegionCode#selectable(Integer)} 的枚举声明顺序。</p>
     *
     * @param scope 限定范围：null 或 230000（全省）= 全省全部县区；地市码（如 232700）= 该地市所辖县区；
     *              县区码（如 232764）= 该县区本身，便于下拉回显当前选中项
     * @return 下拉选项，无匹配时为空数组
     */
    public SelectOptionVO[] options(Integer scope) {
        return RegionCode.selectable(scope).stream()
                .map(region -> new SelectOptionVO(region.getLabel(), String.valueOf(region.getCode())))
                .toArray(SelectOptionVO[]::new);
    }

    /**
     * 校验并归一化接口入参 regionCode：6 位行政区划码 → 区域枚举。
     *
     * <p>未知区划码记 400，而不是按 null 放行：否则非法取值会被下游静默当成「不限区划」，
     * 返回一份看似正常的全量数据，比报错更难排查。大兴安岭地区以外的省内区划码仍放行（数据自然为空），
     * 「当前页面只覆盖哪些区划」由前端下拉的范围参数控制，不在本方法里收紧。</p>
     *
     * @param regionCode 6 位行政区划码，如 232701
     * @return 对应区域枚举
     * @throws ResponseStatusException 400：区划码不在 GB/T 2260 黑龙江区划之内
     */
    public RegionCode requireCode(Integer regionCode) {
        RegionCode region = RegionCode.fromCode(regionCode);
        if (region == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未知行政区划码：" + regionCode);
        }
        return region;
    }
}
