package com.art.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
/**
 * @author liukang&&heyan
 */
public class StudentProfileDTO {

    private Integer studentId;

    private String studentNo;

    private String name;

    private String gender;

    private LocalDate birthDate;

    private String parentPhone;

    private String email;
}