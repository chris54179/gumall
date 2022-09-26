package com.xyz.gumall.search.service;

import com.xyz.gumall.search.vo.SearchParam;
import com.xyz.gumall.search.vo.SearchResult;

public interface MallSearchService {
    SearchResult search(SearchParam param);
}
