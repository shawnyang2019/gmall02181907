package com.atguigu.gmall0218.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0218.bean.*;
import com.atguigu.gmall0218.service.ListService;
import com.atguigu.gmall0218.service.ManageService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
public class ListController {

    @Reference
    private ListService listService;

    @Reference
    private ManageService manageService;


    // http://list.gmall.com/list.html?catalog3Id=61
    //调用此接口的四种参数情况
    //1 通过keyword进行
    //2 通过三级分类id进行调用
    //3 通过点击某个分类属性的值（在原来的搜索参数的基础上增加增加这个属性值作为新的搜索条件）进行调用（同时分类属性的值会跳到面包屑区域）
    //4 通过在面包屑点击某个分类属性的值（在原来搜索参数的基础上减少这个属性值作为新的搜索条件）进行调用（同时分裂属性的值会跳出面包屑区域）
    // 此方法的执行逻辑如下：
    //  1）通过keyword和三级分类id搜索：第一次进入list页面
    //          把查询到的skuLsInfo信息、属性名称、属性值id、属性值名称放入request返回到页面
    //          通过makeUrlParam(skuLsParams)拼接出当前的urlParam放入request并返回到页面，供接下来的从属性值分类区域点击添加搜索条件时的拼接使用
    //          把点击过的属性值区域的的属性值id和属性值名称判断后跳到面包屑区域（此时并不是通过点击属性值id而产生的请求，所以并不发生面包屑跳转）
    //          通过makeUrlParam(skuLsParams, valueId) 除开valueId拼接出新的urlParam包装成新的对象baseAttrValued 放入request返回给前端，供接下来从面包屑区域点击除去搜索条件
    //      产生的新的url来使用（此时并没有点击属性值进行搜索，skuLsParams并没有对应的valueId，面包屑区域并没有属性值id和属性值名称，所有两次产生的urlParam一样）
    //
    //  2) 接下来通过接下来添加属性值id，增加查询条件进行搜索：把1）中的urlParam拼接出新的urlParam 从list页面调用listData方法进行新的查询
    //          重新把新的skuLsInfo信息、属性名称、属性值id、属性值名称放入request返回到页面
    //          通过makeUrlParam(skuLsParams)拼接出当前的urlParam放入request并返回到页面，供接下来的从属性值分类区域点击添加搜索条件时的拼接使用
    //          把点击过的属性值区域的的属性值id和属性值名称包装成baseAttrValued并放入request返回前端，实现跳到面包屑区域功能（此时发生面包屑跳转）
    //          通过makeUrlParam(skuLsParams, valueId) 除开当前valueId拼接出新的urlParam包装成新的对象baseAttrValued 放入request返回给前端，供接下来从面包屑区域点击除去搜索条件
    //          产生的新的url来使用（每次请求到会对面包屑区域的每个属性值id产生当前urlParam除去各自的valueId对应的拼接参数产生新的）
    //
    //  3) 接下来通过点击面包屑的属性值名称进行，减少查询条件进行搜索： 把面包屑中的被点击的属性值对应的urlParam 从list页面调用listData方法进行新的查询
    //          重新把新的skuLsInfo信息、属性名称、属性值id、属性值名称放入request返回到页面
    //          通过makeUrlParam(skuLsParams)拼接出当前的urlParam放入request并返回到页面，供接下来的从属性值分类区域点击添加搜索条件时的拼接使用
    //          把点击过的面包屑区域的的属性值id和属性值名称判断后跳到面包屑区域（此时发生面包屑跳转）
    //          通过makeUrlParam(skuLsParams, valueId) 除开当前valueId拼接出新的urlParam包装成新的对象baseAttrValued 放入request返回给前端，供接下来从面包屑区域点击除去搜索条件
    //          产生的新的url来使用（每次请求到会对面包屑区域的每个属性值id产生当前urlParam除去各自的valueId对应的拼接参数产生新的）

    @RequestMapping("list.html")
//    @ResponseBody
    public String listData(SkuLsParams skuLsParams, HttpServletRequest request){

        skuLsParams.setPageSize(2) ;
        SkuLsResult skuLsResult = listService.search(skuLsParams);
        List<SkuLsInfo> skuLsInfoList = skuLsResult.getSkuLsInfoList();

        //返回平台属性值合集
        List<String> attrValueIdList = skuLsResult.getAttrValueIdList();

        List<BaseAttrInfo> baseAttrInfoList= manageService.getAttrList(attrValueIdList);

        //编写一个方法来拼接url后面的参数条件（用于返回调用listData接口后的最新的urlParam）(这里不包含点击面包屑减少搜索条件的情况），用于产生点击属性值时增加搜索条件的url的拼接
        String urlParam=makeUrlParam(skuLsParams);
        ArrayList<BaseAttrValue> crumbsList = new ArrayList<>();
        //移除已经选定好的属性value值
        for (Iterator<BaseAttrInfo> iterator = baseAttrInfoList.iterator(); iterator.hasNext(); ) {
            BaseAttrInfo baseAttrInfo = iterator.next();
            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
            for (BaseAttrValue baseAttrValue : attrValueList) {
                if (skuLsParams.getValueId() != null&& skuLsParams.getValueId().length>0) {
                    for (String valueId : skuLsParams.getValueId()) {
                        if (baseAttrValue.getId().equals(valueId)) {
                            iterator.remove();
                            BaseAttrValue baseAttrValued = new BaseAttrValue();
                            //巧妙的使用：new一个BaseAttrValue对象同时把属性值的名称更改为面包屑的名称进行面包屑区域的更新
                            baseAttrValued.setValueName(baseAttrInfo.getAttrName()+":"+baseAttrValue.getValueName());
                            // 如果是点击面包屑进行减少搜索条件，需重新调用makeUrlParam方法从里面减去当前已经存在的value所对应的拼接参数 并放入baseAttrValued对应的当前面包屑中的属性值名称
                            String urlParam1 = makeUrlParam(skuLsParams, valueId);
                            baseAttrValued.setUrlParam(urlParam1);
                            crumbsList.add(baseAttrValued);
                        }
                    }
                }
            }

        }

        // 商品显示数据
        request.setAttribute("skuLsInfoList", skuLsInfoList);
        //显示商品的属性信息（用于三级分类检索
        request.setAttribute("baseAttrInfoList", baseAttrInfoList);
        //面包屑功能的keyword信息
        request.setAttribute("keyword", skuLsParams.getKeyword());
        // 面包屑功能的属性信息
        request.setAttribute("crumbsList", crumbsList);
        // 当前的最新的urlParam
        request.setAttribute("urlParam", urlParam);

        // 当前页数
        request.setAttribute("pageNo", skuLsParams.getPageNo());
        // 总页数
        request.setAttribute("totalPages",skuLsResult.getTotalPages() );


        return "list";
    }

    /**
     * @Author ShawnYang
     * @Date 2020/2/14 0014 19:01
     * @Description 判断url后面有哪些参数
     * 修改人：
     * 修改时间：
     * 修改备注：
     * 实现注意：
     */
    private String makeUrlParam(SkuLsParams skuLsParams,String... excludeValueIds) {
        String urlParam="";
        // 根据keyword
        if (skuLsParams.getKeyword() != null&&skuLsParams.getKeyword().length()>0) {
            //http://list.gmall.com/list.html?keyword=手机
            urlParam+="keyword="+skuLsParams.getKeyword();
        }
        // http://list.gmall.com/list.html?keyword=手机&catalog3Id=61
        // 判断三级分类Id
        if (skuLsParams.getCatalog3Id() != null&&skuLsParams.getCatalog3Id().length()>0) {
            // 如果有多个参数则拼接&符号
            urlParam+=urlParam.length()>0?"&":"";
            urlParam+="catalog3Id="+skuLsParams.getCatalog3Id();
        }
        // 平台属性值id
        if (skuLsParams.getValueId() != null&&skuLsParams.getValueId().length>0) {
            // 循环遍历
            // http://list.gmall.com/list.html?keyword=手机&catalog3Id=61&valueId=13&valueId=83
            for (String valueId : skuLsParams.getValueId()) {
                if (excludeValueIds != null && excludeValueIds.length>0) {
                    //获取点击面包屑的平台值的id
                    String excludeValueId = excludeValueIds[0];
                    if (excludeValueId.equals(valueId)) {
                        //如果传过来的是点击面包屑的id 则这个id不参与urlParam的拼接
                        continue;
                    }

                }

                urlParam+=urlParam.length()>0?"&":"";
                urlParam+="valueId="+valueId;
            }
        }

        return urlParam;
    }
}
