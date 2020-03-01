package com.atguigu.gmall0218.service;

import com.atguigu.gmall0218.bean.SkuLsInfo;
import com.atguigu.gmall0218.bean.SkuLsParams;
import com.atguigu.gmall0218.bean.SkuLsResult;

public interface ListService {

    /**
     * 保存数据到es 中！
     * @param skuLsInfo
     */
    void saveSkuLsInfo(SkuLsInfo skuLsInfo);

    /**
     * 检索数据
     * @param skuLsParams
     * @return
     */
    SkuLsResult search(SkuLsParams skuLsParams);

    public void incrHotScore(String skuId);
}
