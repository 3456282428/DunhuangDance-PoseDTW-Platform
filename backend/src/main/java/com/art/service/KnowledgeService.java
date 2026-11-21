package com.art.service;

import com.art.common.PageResult;
import com.art.entity.KbDoc;

import java.util.List;
import java.util.Map;

/**
 * 知识库统一服务（管理员/教师/学生三端同构隔离，scope 区分端别）
  * @author liukang&&heyan
 */
public interface KnowledgeService {

    String SCOPE_ADMIN = "ADMIN";
    String SCOPE_TEACHER = "TEACHER";
    String SCOPE_STUDENT = "STUDENT";

    /**
     * 分页查询知识条目
     * @param scope 端别 ADMIN/TEACHER/STUDENT
     * @param ownerId 归属人过滤（教师/学员端传本人ID：本人条目+机构共享；admin端传null：全部）
     */
    PageResult<KbDoc> pageDocs(String scope, Integer pageNum, Integer pageSize,
                               String keyword, String category, Integer status, Integer ownerId);

    KbDoc getDoc(String scope, Integer kbId, Integer ownerId);

    /**
     * 新增知识条目（自动记操作日志）
     * @param ownerId 教师/学员端强制归属本人；admin端可为null
     */
    void createDoc(String scope, KbDoc doc, Integer ownerId, String ownerName);

    /**
     * 更新知识条目（教师端校验归属，自动记操作日志）
     */
    void updateDoc(String scope, Integer kbId, KbDoc doc, Integer ownerId);

    /**
     * 删除知识条目（教师端校验归属，自动记操作日志）
     */
    void deleteDoc(String scope, Integer kbId, Integer ownerId);

    /** 浏览计数 */
    void increaseView(String scope, Integer kbId);

    /** 三端知识库统计：条目数/今日新增/来源分布 */
    Map<String, Object> stats();

    /**
     * 知识检索（AI模型调用依据约定：AI回答前优先检索知识库已有数据）
     * @param topK 最多返回条数
     */
    List<KbDoc> searchForAi(String scope, Integer ownerId, String keyword, String category, int topK);

    /** 校验scope合法性，非法抛BusinessException */
    void checkScope(String scope);

    /**
     * 跨知识库关联内容联动查询：列出与某条目存在业务关联的其他条目
     */
    List<Map<String, Object>> relatedOf(String scope, Integer kbId, Integer ownerId);

    /**
     * 跨知识库关联图谱：以某条目为中心的节点与关系边（可视化）
     */
    Map<String, Object> graph(String scope, Integer kbId, Integer ownerId);
}
