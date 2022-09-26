package com.xyz.gumall.product.vo;

import com.xyz.gumall.product.entity.SkuImagesEntity;
import com.xyz.gumall.product.entity.SkuInfoEntity;
import com.xyz.gumall.product.entity.SpuInfoDescEntity;
import lombok.Data;

import java.util.List;

@Data
public class SkuItemVo {
    // 1、sku基本获取 pms_sku_info
    SkuInfoEntity info;
    // 是否有库存
    boolean  hasStock = true;
    // 2、sku的图片信息 pms_sku_images
    List<SkuImagesEntity> images;
    // 3、获取spu的销售属性组
    List<SkuItemSaleAttrVo> saleAttr;
    // 4、获取spu的介绍
    SpuInfoDescEntity desp;
    // 5、获取spu的规格参数信息
    List<SpuItemAttrGroupVo> groupAttrs;

    SeckillInfoVo seckillInfo;//當前商品的秒殺優惠信息
}
