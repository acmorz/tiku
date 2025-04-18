package com.fsh.tiku.job.once;

import cn.hutool.core.collection.CollUtil;
import com.fsh.tiku.esdao.QuestionEsDao;
import com.fsh.tiku.model.dto.question.QuestionEsDTO;
import com.fsh.tiku.model.entity.Question;
import com.fsh.tiku.service.QuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 全量同步数据库中的题目到ES中
 * 如何实现单次执行的任务？
 * 1单元测试（每次打包都要执行，不稳妥）
 * 2.只要实现CommandLineRunner接口的run方法，执行完单次任务后不想执行了就把component注释掉
 * 3.写个接口，只让管理员去调用触发
 */
@Component
@Slf4j
public class FullSyncQuestionToEs implements CommandLineRunner {

    @Resource
    private QuestionService questionService;

    @Resource
    private QuestionEsDao questionEsDao;

    @Override
    public void run(String... args) {
        // 全量获取题目（数据量不大的情况下使用） todo 目前全量更新为isDelete=0的数据，逻辑删除的没有更新
        List<Question> questionList = questionService.list();
        if (CollUtil.isEmpty(questionList)) {
            return;
        }
        // 转为 ES 实体类
        List<QuestionEsDTO> questionEsDTOList = questionList.stream()
                .map(QuestionEsDTO::objToDto)
                .collect(Collectors.toList());
        // 为了保证插入的稳定性，分页批量插入到 ES
        final int pageSize = 500;
        int total = questionEsDTOList.size();
        log.info("FullSyncQuestionToEs start, total {}", total);
        for (int i = 0; i < total; i += pageSize) {
            // 注意同步的数据下标不能超过总数据量
            int end = Math.min(i + pageSize, total);
            log.info("sync from {} to {}", i, end);
            questionEsDao.saveAll(questionEsDTOList.subList(i, end));
        }
        log.info("FullSyncQuestionToEs end, total {}", total);
    }
}
