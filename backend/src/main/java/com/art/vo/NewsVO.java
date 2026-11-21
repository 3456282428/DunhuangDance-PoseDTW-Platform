package com.art.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
/**
 * @author liukang&&heyan
 */
public class NewsVO {

    private Integer newsId;
    private String title;
    private String content;
    private String publisher;
    private LocalDateTime publishTime;
    private Integer viewCount;
}
