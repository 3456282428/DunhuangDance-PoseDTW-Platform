package com.art.service;

import com.art.vo.TeacherProfileVO;
import com.art.vo.ClassVO;
import java.util.List;

/**
 * @author liukang&&heyan
 */
public interface TeacherProfileService {
    TeacherProfileVO getMyProfile();
    void updateProfile(TeacherProfileVO profileVO);
    List<ClassVO> getMyClasses();
    Long getTotalHours();
}