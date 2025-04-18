package com.fsh.tiku.job.cycle;

import cn.hutool.core.collection.CollUtil;
import com.fsh.tiku.esdao.QuestionEsDao;
import com.fsh.tiku.mapper.QuestionMapper;
import com.fsh.tiku.model.dto.question.QuestionEsDTO;
import com.fsh.tiku.model.entity.Question;
import com.fsh.tiku.service.QuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 增量把question写入到ES中
 */
@Component
@Slf4j
public class IncSyncQuestionToEs{

    @Resource
    private QuestionMapper questionMapper;

    @Resource
    private QuestionEsDao questionEsDao;

    /**
     * 每次只查询近五分钟内的数据，每一分钟调用一次
     */
    @Scheduled(fixedRate = 60 * 1000)
    public void run() {
        // 增量获取题目（数据量不大的情况下使用）
        long FIVE_MINUTES = 5 * 60 * 1000;
        Date fiveMintesAgoDate = new Date(new Date().getTime() - FIVE_MINUTES);
        List<Question> questionList = questionMapper.listQuestionWithDelete(fiveMintesAgoDate);
        if (CollUtil.isEmpty(questionList)) {
            log.info("no inc question");
            return;
        }
        // 转为 ES 实体类
        List<QuestionEsDTO> questionEsDTOList = questionList.stream()
                .map(QuestionEsDTO::objToDto)
                .collect(Collectors.toList());
        // 为了保证插入的稳定性，分页批量插入到 ES
        final int pageSize = 500;
        int total = questionEsDTOList.size();
        log.info("IncSyncQuestionToEs start, total {}", total);
        for (int i = 0; i < total; i += pageSize) {
            // 注意同步的数据下标不能超过总数据量
            int end = Math.min(i + pageSize, total);
            log.info("sync from {} to {}", i, end);
            questionEsDao.saveAll(questionEsDTOList.subList(i, end));
        }
        log.info("IncSyncQuestionToEs end, total {}", total);
    }
}
