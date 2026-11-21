package com.art.service;

import com.art.entity.Classroom;
import java.util.List;

/**
 * @author liukang&&heyan
 */
public interface ClassroomService {
    List<Classroom> listAll();
    Classroom getById(Integer id);
    void add(Classroom classroom);
    void update(Classroom classroom);
    void delete(Integer id);
}