package com.fsh.tiku.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fsh.tiku.model.dto.questionBankQuestion.QuestionBankQuestionQueryRequest;
import com.fsh.tiku.model.entity.QuestionBankQuestion;
import com.fsh.tiku.model.entity.User;
import com.fsh.tiku.model.vo.QuestionBankQuestionVO;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 题库题目关联表服务
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
public interface QuestionBankQuestionService extends IService<QuestionBankQuestion> {

    /**
     * 校验数据
     *
     * @param questionBankQuestion
     * @param add 对创建的数据进行校验
     */
    void validQuestionBankQuestion(QuestionBankQuestion questionBankQuestion, boolean add);

    /**
     * 获取查询条件
     *
     * @param questionBankQuestionQueryRequest
     * @return
     */
    QueryWrapper<QuestionBankQuestion> getQueryWrapper(QuestionBankQuestionQueryRequest questionBankQuestionQueryRequest);

    /**
     * 获取题库题目关联表封装
     *
     * @param questionBankQuestion
     * @param request
     * @return
     */
    QuestionBankQuestionVO getQuestionBankQuestionVO(QuestionBankQuestion questionBankQuestion, HttpServletRequest request);

    /**
     * 分页获取题库题目关联表封装
     *
     * @param questionBankQuestionPage
     * @param request
     * @return
     */
    Page<QuestionBankQuestionVO> getQuestionBankQuestionVOPage(Page<QuestionBankQuestion> questionBankQuestionPage, HttpServletRequest request);

    /**
     * 批量向题库中插入题目
     * @param questionIds
     * @param questionBankId
     * @param user
     */
    void batchAddQuestionToBank(List<Long> questionIds, Long questionBankId, User user);

    @Transactional(rollbackFor = Exception.class)
    void batchAddQuestionToBankInner(List<QuestionBankQuestion> questionBankQuestionList);

    /**
     * 批量向题库中删除题目
     * @param questionIds
     * @param questionBankId
     */
    void batchRemoveQuestionToBank(List<Long> questionIds, Long questionBankId);
}
