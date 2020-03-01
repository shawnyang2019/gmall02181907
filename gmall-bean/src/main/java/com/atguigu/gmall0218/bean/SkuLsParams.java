package com.atguigu.gmall0218.bean;

import lombok.Data;

import java.io.Serializable;
/**
 * @Author ShawnYang
 * @Date 2020/2/13 0013 20:56
 * @Description es搜索输入对象
 * 修改人：
 * 修改时间：
 * 修改备注：
 * 实现注意：
 */

@Data
public class SkuLsParams implements Serializable {

    // keyword = skuName

    String  keyword;

    String catalog3Id;

    String[] valueId;

    int pageNo=1;

    int pageSize=20;

}
