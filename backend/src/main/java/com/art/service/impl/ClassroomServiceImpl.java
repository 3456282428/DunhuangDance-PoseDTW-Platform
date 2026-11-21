package com.art.service.impl;

import com.art.entity.Classroom;
import com.art.mapper.ClassroomMapper;
import com.art.service.ClassroomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * @author liukang&&heyan
 */
public class ClassroomServiceImpl implements ClassroomService {

    private final ClassroomMapper classroomMapper;

    @Override
    public List<Classroom> listAll() {
        return classroomMapper.selectAll();
    }

    @Override
    public Classroom getById(Integer id) {
        return classroomMapper.selectById(id);
    }

    @Override
    public void add(Classroom classroom) {
        classroom.setStatus(1);
        classroomMapper.insert(classroom);
    }

    @Override
    public void update(Classroom classroom) {
        classroomMapper.update(classroom);
    }

    @Override
    public void delete(Integer id) {
        classroomMapper.deleteById(id);
    }
}