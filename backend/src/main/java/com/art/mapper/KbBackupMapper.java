package com.art.mapper;

import com.art.entity.KbBackup;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 知识库备份记录 Mapper
  * @author liukang&&heyan
 */
@Mapper
public interface KbBackupMapper {
    int insert(KbBackup backup);
    List<KbBackup> selectAll();
    KbBackup selectById(Integer backupId);
}
