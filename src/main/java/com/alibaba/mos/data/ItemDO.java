/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package com.alibaba.mos.data;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author superchao
 * @version $Id: ItemDO.java, v 0.1 2019年10月28日 11:02 AM superchao Exp $
 */
@Data
@NoArgsConstructor
public class ItemDO implements Serializable {

    /**
     * 选择构造
     * @param skuType
     * @param skuDO
     */
    public ItemDO(String skuType, SkuDO skuDO) {
        if (Objects.equals(skuType, SkuType.ORIGIN)) {
            this.artNo = skuDO.getArtNo();
            this.spuId = null;
        } else if (Objects.equals(skuType, SkuType.DIGITAL)) {
            this.artNo = null;
            this.spuId = skuDO.getSpuId();
        } else {
            throw new RuntimeException("当前类型不存在！");
        }
        this.name = "name";
        this.inventory = BigDecimal.valueOf(0L);
        this.skuIds = new ArrayList<>(256);
        this.maxPrice = BigDecimal.valueOf(0L);
        this.minPrice = BigDecimal.valueOf(0L);
        doStatisticsInventory(skuDO.getInventoryList());
        cumulative(skuDO.getId(), skuDO.getPrice());
    }

    /**
     * 商品名称
     */
    private String name;

    /**
     * 货号
     */
    private String artNo;

    /**
     * itemid
     */
    private String spuId;

    /**
     * 库存数量, 保留小数点后2位
     */
    private BigDecimal inventory;

    /**
     * 最大价格, 保留小数点后2位
     */
    private BigDecimal maxPrice;

    /**
     * 最小价格, 保留小数点后2位
     */
    private BigDecimal minPrice;

    /**
     * 该item下的sku id列表
     */
    private List<String> skuIds;

    /**
     * 累计
     * @param skuDO
     * @return
     */
    public ItemDO addSku(SkuDO skuDO) {
        doStatisticsInventory(skuDO.getInventoryList());
        cumulative(skuDO.getId(), skuDO.getPrice());
        return this;
    }

    /**
     * 累计
     * @param skuId
     * @param price
     */
    private void cumulative(String skuId, BigDecimal price) {
        this.maxPrice = this.maxPrice.compareTo(price) < 0 ? price : this.maxPrice;
        this.minPrice = this.minPrice.compareTo(price) > 0 ? price : this.minPrice;
        this.skuIds.add(skuId);
    }

    /**
     * 统计库存量
     * @param inventoryList
     */
    private void doStatisticsInventory(List<ChannelInventoryDO> inventoryList) {
        if (CollectionUtils.isEmpty(inventoryList)) {
            return;
        }
        for (ChannelInventoryDO channelInventoryDO : inventoryList) {
            this.inventory.add(channelInventoryDO.getInventory());
        }
    }

    /**
     * Item类型
     */
    public interface SkuType {
        String ORIGIN = "ORIGIN";
        String DIGITAL = "DIGITAL";
    }
}