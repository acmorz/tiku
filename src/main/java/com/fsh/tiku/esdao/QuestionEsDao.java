package com.fsh.tiku.esdao;

import com.fsh.tiku.model.dto.question.QuestionEsDTO;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

/**
 * 题目 ES 操作
 * QuestionEsDTO指定关联数据的类型
 * Long 索引id的类型
 */
public interface QuestionEsDao extends ElasticsearchRepository<QuestionEsDTO, Long> {
    /**
     * 根据用户 id 查询
     * ES自动根据方法名进行条件映射，自动根据UserId进行过滤
     * @param userId
     * @return
     */
    List<QuestionEsDTO> findByUserId(Long userId);

}
