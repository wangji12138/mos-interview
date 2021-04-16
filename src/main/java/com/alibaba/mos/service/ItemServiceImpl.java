/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package com.alibaba.mos.service;

import com.alibaba.mos.api.ItemService;
import com.alibaba.mos.api.SkuReadService;
import com.alibaba.mos.dao.ItemDAO;
import com.alibaba.mos.data.ItemDO;
import com.alibaba.mos.util.ThreadPoolUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author superchao
 * @version $Id: ItemServiceImpl.java, v 0.1 2019年11月20日 3:06 PM superchao Exp $
 */
@Slf4j
@Service
public class ItemServiceImpl implements ItemService<ItemDO> {

    @Autowired
    SkuReadService skuReadService;

    @Autowired
    private ItemDAO itemDAO;

    @Override
    public void aggregation() {
        // 聚合商品数据并通过com.alibaba.mos.dao.ItemDAO将其保存到虚拟数据库中
        //线程池
        //此方式可能对“数据库”压力过大
        skuReadService.loadSkus(skuDO -> {
            if (null == skuDO) {
                log.error("[Interview2Tests::appendAllPriceSkuIdMap] 当前sku为空，请核实");
                return null;
            }
            ThreadPoolUtils.getPool().submit(() -> {
                synchronized (skuDO.getArtNo().intern()) {
                    //1 getItemByArtNo
                    ItemDO artNoItem = itemDAO.getItemByArtNo(skuDO.getArtNo());
                    artNoItem = null == artNoItem ? new ItemDO(ItemDO.SkuType.ORIGIN, skuDO) : artNoItem.addSku(skuDO);
                    itemDAO.replaceItem(artNoItem);
                }
            });
            ThreadPoolUtils.getPool().submit(() -> {
                synchronized (skuDO.getSpuId().intern()) {
                    //2 getItemBySpuId
                    ItemDO spuIdItem = itemDAO.getItemBySpuId(skuDO.getSpuId());
                    spuIdItem = null == spuIdItem ? new ItemDO(ItemDO.SkuType.DIGITAL, skuDO) : spuIdItem.addSku(skuDO);
                    itemDAO.replaceItem(spuIdItem);
                }
            });
            return skuDO;
        });

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println(ItemDAO.artItemDb);
        System.out.println(ItemDAO.spuItemDb);
    }
}