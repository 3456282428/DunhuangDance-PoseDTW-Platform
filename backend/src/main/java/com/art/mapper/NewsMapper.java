package com.art.mapper;

import com.art.entity.News;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
/**
 * @author liukang&&heyan
 */
public interface NewsMapper {
    News selectById(@org.apache.ibatis.annotations.Param("newsId") Integer newsId);
    int insert(News news);
    int update(News news);
    int deleteById(@org.apache.ibatis.annotations.Param("newsId") Integer newsId);
    List<News> selectAll();
}
