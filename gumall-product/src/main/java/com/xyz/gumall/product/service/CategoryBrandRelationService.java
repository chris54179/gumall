package com.xyz.gumall.product.service;

import com.xyz.gumall.product.entity.BrandEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xyz.common.utils.PageUtils;
import com.xyz.gumall.product.entity.CategoryBrandRelationEntity;

import java.util.List;
import java.util.Map;

/**
 * 品牌分类关联
 *
 *
 *
 *
 */
public interface CategoryBrandRelationService extends IService<CategoryBrandRelationEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveDetail(CategoryBrandRelationEntity categoryBrandRelation);

    void updateBrand(Long brandId, String name);

    void updateCategory(Long catId, String name);

    List<BrandEntity> getBrandsByCatId(Long catId);
}

