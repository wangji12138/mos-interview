/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2020 All Rights Reserved.
 */
package com.alibaba.mos.dao;

import com.alibaba.mos.data.ItemDO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  这里用map模拟一个数据库, 只提供按主键的get和set, 不要在本类做扩展
 * @author superchao
 * @version : ItemDAO.java, v 0.1 2020年09月07日 11:54 上午 superchao Exp $
 */
@Service
public class ItemDAO {

    private static Map<String, ItemDO> artItemDb = new ConcurrentHashMap<>();
    private static Map<String, ItemDO> spuItemDb = new ConcurrentHashMap<>();

    /**
     * 根据itemId获取item
     * @param artNo
     * @return
     */
    public ItemDO getItemByArtNo(String artNo) {
        return artItemDb.get(artNo);
    }

    public ItemDO getItemBySpuId(String spuId) {
        return spuItemDb.get(spuId);
    }

    /**
     * 将itemDO保存
     * 对于sku type为原始商品(ORIGIN)的, 按货号(artNo)聚合成ITEM
     * 对于sku type为数字化商品(DIGITAL)的, 按spuId聚合成ITEM
     * @param itemDO
     */
    public void replaceItem(ItemDO itemDO) {
        String artNo = itemDO.getArtNo();
        String spuId = itemDO.getSpuId();
        if (StringUtils.isNotBlank(artNo)) {
            artItemDb.put(artNo, itemDO);
        } else if (StringUtils.isNotBlank(spuId)) {
            spuItemDb.put(spuId, itemDO);
        }
    }
}