package com.atguigu.gmall0218.bean;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SkuLsResult implements Serializable {

    //属性商品列表
    List<SkuLsInfo> skuLsInfoList;

    //总条数
    long total;
    //总页数
    long totalPages;
    //三级分类id对应的属性valueId
    List<String> attrValueIdList;

}
