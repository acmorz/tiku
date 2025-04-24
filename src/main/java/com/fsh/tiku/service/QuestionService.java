package com.fsh.tiku.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fsh.tiku.model.dto.question.QuestionQueryRequest;
import com.fsh.tiku.model.entity.Question;
import com.fsh.tiku.model.entity.User;
import com.fsh.tiku.model.vo.QuestionVO;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 题目服务
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
public interface QuestionService extends IService<Question> {

    /**
     * 校验数据
     *
     * @param question
     * @param add 对创建的数据进行校验 ??？校验什么
     */
    void validQuestion(Question question, boolean add);

    /**
     * 获取查询条件
     *
     * @param questionQueryRequest
     * @return
     */
    QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest);
    
    /**
     * 获取题目封装
     *
     * @param question
     * @param request
     * @return
     */
    QuestionVO getQuestionVO(Question question, HttpServletRequest request);

    /**
     * 分页获取题目封装
     *
     * @param questionPage
     * @param request
     * @return
     */
    Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage, HttpServletRequest request);

    /**
     * 分页获取题目封装
     *
     * @param questionQueryRequest
     * @return
     */
    Page<Question> listQuestionByPage(QuestionQueryRequest questionQueryRequest);

    /**
     * ES查询题目
     * @param questionQueryRequest
     * @return
     */
    Page<Question> searchFromEs(QuestionQueryRequest questionQueryRequest);

    /**
     * 批量删除题目，并查找在题库中的题目，删除关联表中的对应关系
     * @param questionIds
     */
    void batchDeleteQuestion(List<Long> questionIds);

    /**
     * AI自动生成题目
     * @param questionType
     * @param number
     * @param user
     * @return
     */
    boolean aiGenerateQuestion(String questionType, int number, User user);

    /**
     * AI自动生成题目对应题解
     * @param questionTitle
     * @return
     */
    String aiGenerateQuestionAnswer(String questionTitle);
}
