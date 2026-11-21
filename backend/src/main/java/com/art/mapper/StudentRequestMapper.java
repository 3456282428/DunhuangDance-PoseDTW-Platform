package com.art.mapper;

import com.art.entity.StudentRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
/**
 * @author liukang&&heyan
 */
public interface StudentRequestMapper {
    StudentRequest selectById(@Param("requestId") Integer requestId);
    int insert(StudentRequest request);
    int updateStatus(@Param("requestId") Integer requestId,
                     @Param("status") String status,
                     @Param("replyRemark") String replyRemark,
                     @Param("handleTime") java.time.LocalDateTime handleTime);
    List<StudentRequest> selectByStudentId(@Param("studentId") Integer studentId);
    List<StudentRequest> selectByTeacherId(@Param("teacherId") Integer teacherId);
    /** 全量遍历（知识库全量归集用） */
    List<StudentRequest> selectAll();
}
