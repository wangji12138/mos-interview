package com.alibaba.mos.interview;

import com.alibaba.fastjson.JSON;
import com.alibaba.mos.api.ItemService;
import com.alibaba.mos.api.SkuReadService;
import com.alibaba.mos.data.ChannelInventoryDO;
import com.alibaba.mos.data.ItemDO;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 注意： 假设sku数据很多, 无法将sku列表完全加载到内存中
 */
@SpringBootTest
@Slf4j
class Interview2Tests {

    @Autowired
    SkuReadService skuReadService;

    @Autowired
    ItemService<ItemDO> itemService;

    /**
     * 需要生成排名数
     */
    private final static int RANK_NUM = 5;

    /**
     * 试题1:
     * 注意: 假设sku数据很多, 无法将sku列表完全加载到内存中
     *
     * 实现com.alibaba.mos.api.SkuReadService#loadSkus(com.alibaba.mos.api.SkuReadService.SkuHandler)
     * 从/resources/data/skus.txt读取数据并逐条打印数据，数据字段用'|'分隔
     */
    @Test
    void readDataFromExcelWithHandlerTest() {
        AtomicInteger count = new AtomicInteger();
        skuReadService.loadSkus(skuDO -> {
            log.info("读取SKU信息={}", JSON.toJSONString(skuDO));
            count.incrementAndGet();
            return skuDO;
        });
        Assert.isTrue(count.get() > 0, "未能读取商品列表");
    }

    /**
     * 试题2:
     * 注意: 假设sku数据很多, 无法将sku列表完全加载到内存中
     *
     * 计算以下统计值:
     * 1、假设所有sku的价格都是精确到1元且一定小于1万元,
     *  获取价格为中位数价格的任意一个skuId（比如价格为1、1、2、25、25、25、25，中位数价格是【2】）
     * 2、假设所有sku的价格都是精确到1元且一定小于1万元,
     *  获取按价格排序后在中间的价格（比如价格为1、1、2、25、25、25、25，按照题目要求，是第4个价格【25】）
     * 3、每个渠道库存量为前五的skuId列表, 例如: miao:[1,2,3,4,5],tmall:[3,4,5,6,7],intime:[7,8,4,3,1]
     * 4、所有sku的总价值
     */
    @Test
    void statisticsDataTest() {
        //定义总sku价格容器
        Map<Integer, SkuAggregation> allPriceSkuIdMap = new LinkedHashMap<>(10000);
        //定义sku种数
        AtomicLong skuNum = new AtomicLong(0L);
        //定义sku总价
        AtomicLong allSkuPrice = new AtomicLong(0L);
        //定义每个渠道前5容量
        Map<String, LinkedList<SkuDTO>> channelRankMap = new HashMap<>(128);

        //顺序加载所有sku
        skuReadService.loadSkus(skuDO -> {
            if (null == skuDO) {
                log.error("[Interview2Tests::appendAllPriceSkuIdMap] 当前sku为空，请核实");
                return null;
            }
            //获取当前sku价格（分）
            int price = skuDO.getPrice().intValue();

            //1. 添加价格
            appendAllPriceSkuIdMap(skuDO.getId(), price, allPriceSkuIdMap);

            //2.记录sku总计
            skuNum.getAndIncrement();

            //3.记录价格总计（数量*单价）
            allSkuPrice.getAndAdd(getSkuTotal(skuDO.getInventoryList()) * price);

            //4.记录当sku渠道排名
            appendChannelRankMap(skuDO.getId(), skuDO.getInventoryList(), channelRankMap);
            return skuDO;
        });

        List<Map.Entry<Integer, SkuAggregation>> mapList = new ArrayList<>(allPriceSkuIdMap.entrySet());
        //排序
        mapList.sort(Comparator.comparingInt(Map.Entry::getKey));
        //中位sku
        long[] skuMedianIndex = skuNum.get() % 2 != 0 ? new long[]{skuNum.get() / 2} : new long[]{skuNum.get() / 2, skuNum.get() / 2 + 1};
        //中位价格
        long[] skuPriceIndex = mapList.size() % 2 != 0 ? new long[]{mapList.size() / 2} : new long[]{mapList.size() / 2, mapList.size() / 2 + 1};

        String medianSkuStr = null;
        String medianPriceStr = null;
        //遍历统计容器，获取数据
        for (int i = 0, skuIndex = 0; i < mapList.size(); i++) {
            Map.Entry<Integer, SkuAggregation> entry = mapList.get(i);
            skuIndex += entry.getValue().size();
            //生成中位sku
            if (null == medianSkuStr && skuIndex >= skuMedianIndex[0]) {
                if (skuMedianIndex.length == 2 && skuIndex < skuMedianIndex[1]) {
                    String one = entry.getValue().getSkuId();
                    String two = mapList.get(i + 1).getValue().getSkuId();
                    medianSkuStr = String.format("当前中位sku为 %s %s", one, two);
                } else {
                    String one = entry.getValue().getSkuId();
                    medianSkuStr = String.format("当前中位sku为 %s", one);
                }
            }

            //生成中位价格
            if (null == medianPriceStr && i == skuPriceIndex[0]) {
                if (skuPriceIndex.length == 2) {
                    Integer one = entry.getKey();
                    Integer two = mapList.get(i + 1).getKey();
                    medianPriceStr = String.format("当前中位价格为 %s %s", one, two);
                } else {
                    medianPriceStr = String.format("当前中位价格为 %s", entry.getKey());
                }
            }
        }
        System.out.println(mapList.toString());
        System.out.println(medianSkuStr);
        System.out.println(medianPriceStr);
        System.out.println();

        //1.获取价格为中位数价格的任意一个skuId;
        //2.获取按价格排序后在中间的价格
        //3.每个渠道库存量为前五的skuId列表
        channelRankMap.forEach((channelId, list) -> {
            System.out.printf("当前渠道[%s]前5：%s%n", channelId, Arrays.toString(list.toArray()));
        });
        System.out.println();
        //4.所有sku的总价值
        System.out.printf("所有sku的总价值:%s ", allSkuPrice.get());

    }

    /**
     * 获取sku数量
     * @param inventoryList
     * @return
     */
    private long getSkuTotal(List<ChannelInventoryDO> inventoryList) {
        if (CollectionUtils.isEmpty(inventoryList)) {
            return 0L;
        }
        long total = 0;
        for (ChannelInventoryDO channelInventoryDO : inventoryList) {
            if (null != channelInventoryDO && null != channelInventoryDO.getInventory()) {
                total += channelInventoryDO.getInventory().longValue();
            }
        }
        return total;
    }

    /**
     * 累计库存量前RANK_NUM;
     * @param skuId
     * @param inventoryList
     * @param channelRankMap
     */
    private void appendChannelRankMap(String skuId, List<ChannelInventoryDO> inventoryList, Map<String, LinkedList<SkuDTO>> channelRankMap) {
        if (null == channelRankMap) {
            throw new RuntimeException("渠道排名map未初始化");
        }
        if (CollectionUtils.isEmpty(inventoryList)) {
            log.warn("[Interview2Tests::appendChannelRankMap] 当前sku[{}]渠道为空，已跳过", skuId);
            return;
        }
        for (ChannelInventoryDO o : inventoryList) {
            String channelCode = o.getChannelCode();
            int inventory = o.getInventory().intValue();
            //判断当前渠道是否已存在
            //如果存在
            LinkedList<SkuDTO> list = channelRankMap.get(o.getChannelCode());
            if (null != list) {
                //判断库存榜最小值比较1
                //如果小于库存榜最小值
                if (list.getLast().getInventory() >= inventory) {
                    //判断链表是否已满(容错)
                    if (list.size() < RANK_NUM) {
                        list.add(new SkuDTO(skuId, inventory));

                    }//丢弃
                } else {
                    //判断链表是否已满(容错)
                    if (list.size() >= RANK_NUM) {
                        //已满，需要删除最小值
                        list.removeLast();
                    }
                    for (int i = list.size() - 1; i >= 0; i--) {
                        //如果小于当前，则尾插
                        if (list.get(i).getInventory() > inventory) {
                            list.add(i + 1, new SkuDTO(skuId, inventory));
                            break;
                        }
                        //如果最后一个还小于，那么将头插
                        else if (i == 0) {
                            list.add(i, new SkuDTO(skuId, inventory));
                            break;
                        }
                    }

                }
            }
            //如果不存
            else {
                list = new LinkedList<>();
                list.add(new SkuDTO(skuId, inventory));
                channelRankMap.put(channelCode, list);
            }
        }
    }

    /**
     * 累加所有价格sku
     * @param skuId
     * @param skuPrice
     * @param allPriceSkuIdMap
     * @return
     */
    private int appendAllPriceSkuIdMap(String skuId, int skuPrice, Map<Integer, SkuAggregation> allPriceSkuIdMap) {
        if (null == allPriceSkuIdMap) {
            throw new RuntimeException("所有价格map未初始化");
        }
        //判断map中是否包含 当前价格
        //包含则累计
        SkuAggregation aggregation = allPriceSkuIdMap.get(skuPrice);
        if (null != aggregation) {
            //有序添加
            aggregation.incrNum();
            return 0;
        }
        //不包含，则新增
        else {
            allPriceSkuIdMap.put(skuPrice, new SkuAggregation(skuId));
            return 1;
        }
    }

    /**
     * 试题3:
     *
     * 基于试题1, 在com.alibaba.mos.service.ItemServiceImpl中实现一个生产者消费者,
     * 将sku列表聚合为商品并通过com.alibaba.mos.dao.ItemDAO保存到数据库中
     * 注意通过com.alibaba.mos.dao.ItemDAO进行数据操作无需考虑内存问题
     *
     * 聚合规则为:
     * 对于sku type为原始商品(ORIGIN)的, 按货号(artNo)聚合成ITEM
     * 对于sku type为数字化商品(DIGITAL)的, 按spuId聚合成ITEM
     * 聚合结果需要包含: item的最大价格、最小价格、sku列表及总库存
     */
    @Test
    void aggregationSkusWithConsumerProviderTest() {
        itemService.aggregation();
    }

    /**
     * 统计
     */
    @NoArgsConstructor
    private static class SkuAggregation {
        private String skuId;
        private int num;

        public SkuAggregation(String skuId) {
            this.skuId = skuId;
            this.num = 1;
        }

        public void incrNum() {
            this.num++;
        }

        public String getSkuId() {
            return skuId;
        }

        public int size() {
            return num;
        }

        @Override
        public String toString() {
            return "SkuAggregation{" +
                    "skuId='" + skuId + '\'' +
                    ", num=" + num +
                    '}';
        }
    }

    /**
     * 节点
     */
    @Data
    @NoArgsConstructor
    private static class SkuDTO {
        private String skuId;
        private int inventory;

        public SkuDTO(String skuId, int inventory) {
            this.skuId = skuId;
            this.inventory = inventory;
        }

        @Override
        public String toString() {
            return "Node{" +
                    "skuId='" + skuId + '\'' +
                    ", inventory=" + inventory +
                    '}';
        }

    }
}
