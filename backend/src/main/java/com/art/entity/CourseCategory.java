package com.art.entity;

import lombok.Data;
import java.io.Serializable;

@Data
/**
 * @author liukang&&heyan
 */
public class CourseCategory implements Serializable {

    private Integer categoryId;
    private String categoryName;  // 乐器、舞蹈、美术、声乐
    private String description;
}