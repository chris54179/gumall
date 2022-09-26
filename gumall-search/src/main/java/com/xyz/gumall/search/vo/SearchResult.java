package com.xyz.gumall.search.vo;

import com.xyz.common.to.es.SkuEsModel;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SearchResult {
    //查询到的所有商品信息
    private List<SkuEsModel> products;

    //当前页面
    private Integer pageNum;

    //总记录数
    private Long total;

    //总页码数
    private Integer totalPages;

    //导航页
    private List<Integer> pageNavs;

    //查询到的所有品牌信息
    private List<BrandVo> brands;

    //查询所涉及到的所有分类信息
    private List<CatalogVo> catalogs;

    //查询所涉及到的所有属性
    private List<AttrVo> attrs;

    //麵包屑
    private List<NavVo> navs = new ArrayList<>();
    private List<Long> attrIds = new ArrayList<>();

    @Data
    public static class NavVo{
        private String navName;
        private String navValue;
        private String link;
    }

    @Data
    public static class BrandVo{
        private Long brandId;
        private String brandName;
        private String brandImg;
    }

    @Data
    public static class CatalogVo{
        private Long catalogId;
        private String catalogName;
    }

    @Data
    public static class AttrVo{
        private Long attrId;
        private String attrName;
        private List<String> attrValue;
    }



}
