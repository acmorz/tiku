package com.fsh.tiku.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fsh.tiku.annotation.AuthCheck;
import com.fsh.tiku.common.ErrorCode;
import com.fsh.tiku.constant.CommonConstant;
import com.fsh.tiku.exception.BusinessException;
import com.fsh.tiku.exception.ThrowUtils;
import com.fsh.tiku.mapper.QuestionBankQuestionMapper;
import com.fsh.tiku.model.dto.questionBankQuestion.QuestionBankQuestionQueryRequest;
import com.fsh.tiku.model.entity.Question;
import com.fsh.tiku.model.entity.QuestionBank;
import com.fsh.tiku.model.entity.QuestionBankQuestion;
import com.fsh.tiku.model.entity.User;
import com.fsh.tiku.model.vo.QuestionBankQuestionVO;
import com.fsh.tiku.model.vo.UserVO;
import com.fsh.tiku.service.QuestionBankQuestionService;
import com.fsh.tiku.service.QuestionBankService;
import com.fsh.tiku.service.QuestionService;
import com.fsh.tiku.service.UserService;
import com.fsh.tiku.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static co.elastic.clients.elasticsearch._types.query_dsl.Query.Kind.Wrapper;

/**
 * 题库题目关联表服务实现
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
@Service
@Slf4j
public class QuestionBankQuestionServiceImpl extends ServiceImpl<QuestionBankQuestionMapper, QuestionBankQuestion> implements QuestionBankQuestionService {

    @Resource
    private UserService userService;

    @Autowired(required = false)
    @Lazy
    private QuestionService questionService;

    @Resource
    private QuestionBankService questionBankService;
    /**
     * 校验数据
     *
     * @param questionBankQuestion
     * @param add      对创建的数据进行校验
     */
    @Override
    public void validQuestionBankQuestion(QuestionBankQuestion questionBankQuestion, boolean add) {
        ThrowUtils.throwIf(questionBankQuestion == null, ErrorCode.PARAMS_ERROR);
        // 题目和题库必须存在
        Long questionId = questionBankQuestion.getQuestionId();
        if (questionId != null) {
            Question question = questionService.getById(questionId);
            ThrowUtils.throwIf(question == null, ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }
        Long questionBankId = questionBankQuestion.getQuestionBankId();
        if (questionBankId != null) {
            QuestionBank questionBank = questionBankService.getById(questionBankId);
            ThrowUtils.throwIf(questionBank == null, ErrorCode.NOT_FOUND_ERROR, "题库不存在");
        }
    }


    /**
     * 获取查询条件
     *
     * @param questionBankQuestionQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<QuestionBankQuestion> getQueryWrapper(QuestionBankQuestionQueryRequest questionBankQuestionQueryRequest) {
        QueryWrapper<QuestionBankQuestion> queryWrapper = new QueryWrapper<>();
        if (questionBankQuestionQueryRequest == null) {
            return queryWrapper;
        }
        // todo 从对象中取值
        Long id = questionBankQuestionQueryRequest.getId();
        Long notId = questionBankQuestionQueryRequest.getNotId();
        String searchText = questionBankQuestionQueryRequest.getSearchText();
        String sortField = questionBankQuestionQueryRequest.getSortField();
        String sortOrder = questionBankQuestionQueryRequest.getSortOrder();
        Long userId = questionBankQuestionQueryRequest.getUserId();
        Long questionBankId = questionBankQuestionQueryRequest.getQuestionBankId();
        Long questionId = questionBankQuestionQueryRequest.getQuestionId();

        // todo 补充需要的查询条件

        // 精确查询
        queryWrapper.ne(ObjectUtils.isNotEmpty(notId), "id", notId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(questionId), "questionId", questionId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(questionBankId), "questionBankId", questionBankId);
        // 排序规则
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 获取题库题目关联表封装
     *
     * @param questionBankQuestion
     * @param request
     * @return
     */
    @Override
    public QuestionBankQuestionVO getQuestionBankQuestionVO(QuestionBankQuestion questionBankQuestion, HttpServletRequest request) {
        // 对象转封装类
        QuestionBankQuestionVO questionBankQuestionVO = QuestionBankQuestionVO.objToVo(questionBankQuestion);

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Long userId = questionBankQuestion.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        questionBankQuestionVO.setUser(userVO);
        // endregion

        return questionBankQuestionVO;
    }

    /**
     * 分页获取题库题目关联表封装
     *
     * @param questionBankQuestionPage
     * @param request
     * @return
     */
    @Override
    public Page<QuestionBankQuestionVO> getQuestionBankQuestionVOPage(Page<QuestionBankQuestion> questionBankQuestionPage, HttpServletRequest request) {
        List<QuestionBankQuestion> questionBankQuestionList = questionBankQuestionPage.getRecords();
        Page<QuestionBankQuestionVO> questionBankQuestionVOPage = new Page<>(questionBankQuestionPage.getCurrent(), questionBankQuestionPage.getSize(), questionBankQuestionPage.getTotal());
        if (CollUtil.isEmpty(questionBankQuestionList)) {
            return questionBankQuestionVOPage;
        }
        // 对象列表 => 封装对象列表
        List<QuestionBankQuestionVO> questionBankQuestionVOList = questionBankQuestionList.stream().map(questionBankQuestion -> {
            return QuestionBankQuestionVO.objToVo(questionBankQuestion);
        }).collect(Collectors.toList());

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Set<Long> userIdSet = questionBankQuestionList.stream().map(QuestionBankQuestion::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));

        // 填充信息
        questionBankQuestionVOList.forEach(questionBankQuestionVO -> {
            Long userId = questionBankQuestionVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            questionBankQuestionVO.setUser(userService.getUserVO(user));
        });
        // endregion

        questionBankQuestionVOPage.setRecords(questionBankQuestionVOList);
        return questionBankQuestionVOPage;
    }

    /**
     * 批量向题库中插入题目
     * @param questionIds
     * @param questionBankId
     * @param user
     */
    @Override
    public void batchAddQuestionToBank(List<Long> questionIds, Long questionBankId, Long userId) {
        ThrowUtils.throwIf(questionIds == null, ErrorCode.PARAMS_ERROR, "题目列表为空");
        ThrowUtils.throwIf(questionBankId <= 0, ErrorCode.PARAMS_ERROR, "题库id错误");
        ThrowUtils.throwIf(userId < 0, ErrorCode.NOT_LOGIN_ERROR);

        //判断题目id是否存在
        List<Question> questions = questionService.listByIds(questionIds);
        List<Long> validQuestionIdsList = questions.stream()
                .map(Question::getId)
                .collect(Collectors.toList());
        ThrowUtils.throwIf(validQuestionIdsList.isEmpty(), ErrorCode.NOT_FOUND_ERROR, "题库中题目id不存在");

        //判断题库id是否存在
        QuestionBank questionBank = questionBankService.getById(questionBankId);
        ThrowUtils.throwIf(questionBank == null, ErrorCode.NOT_FOUND_ERROR, "该题库不存在");

        //检查题目id是否存在与关联表中
        //查找已经存在于关联表中的题目id，并将其去除
        LambdaQueryWrapper<QuestionBankQuestion> questionLambdaQueryWrapper = Wrappers.lambdaQuery(QuestionBankQuestion.class)
                .select(QuestionBankQuestion::getQuestionId)
                .eq(QuestionBankQuestion::getQuestionBankId,questionBank.getId())
                .in(QuestionBankQuestion::getQuestionId, validQuestionIdsList);

        //存在于关联表中的题目existQuestionList
        List<Long> existQuestionList = this.listObjs(questionLambdaQueryWrapper, obj->(Long)obj);
        //filter是过滤，保留符合条件的元素，map是对其中的每一个元素进行变换处理
        List<Long> finalQuestionIds = validQuestionIdsList.stream()
                .filter(item -> !existQuestionList.contains(item))
                .collect(Collectors.toList());
        ThrowUtils.throwIf(finalQuestionIds.isEmpty(), ErrorCode.NOT_FOUND_ERROR, "所有题目已经添加到题库中");

        // 自定义线程池
        ThreadPoolExecutor customExecutor = new ThreadPoolExecutor(
                20,             // 核心线程数 同时处理20批
                50,                        // 最大线程数
                60L,                       // 线程空闲存活时间
                TimeUnit.SECONDS,           // 存活时间单位
                new LinkedBlockingQueue<>(10000),  // 阻塞队列容量
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：由调用线程处理任务，把异步变成同步
        );

        // 用于保存所有批次的任务 CompletableFuture
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        int batchSize = 10;
        for(int i = 0; i < finalQuestionIds.size(); i += batchSize){
            List<Long> subList = finalQuestionIds.subList(i, Math.min(i + batchSize, finalQuestionIds.size()));
            List<QuestionBankQuestion> questionBankQuestionList = subList.stream()
                    .map(questionId->{
                        QuestionBankQuestion questionBankQuestion = new QuestionBankQuestion();
                        questionBankQuestion.setQuestionId(questionId);
                        questionBankQuestion.setUserId(userId);
                        questionBankQuestion.setQuestionBankId(questionBankId);
                        return questionBankQuestion;
                    })
                    .collect(Collectors.toList());

            QuestionBankQuestionServiceImpl questionBankQuestionService = (QuestionBankQuestionServiceImpl) AopContext.currentProxy();
            //如果任务需要返回值用suplyASync，future是任务
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                questionBankQuestionService.batchAddQuestionToBankInner(questionBankQuestionList);
            }, customExecutor).exceptionally(ex -> {
                log.error("批处理任务执行失败", ex);
                return null;
            });

            futures.add(future);
        }
        // 等待所有批次操作完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 关闭线程池
        customExecutor.shutdown();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAddQuestionToBankInner(List<QuestionBankQuestion> questionBankQuestionList){
        log.info("项目线程" + Thread.currentThread().getName());
        for (QuestionBankQuestion questionBankQuestion : questionBankQuestionList) {
//                在关联表中插入对应的题目和题库
            Long questionBankId = questionBankQuestion.getQuestionBankId();
            Long questionId = questionBankQuestion.getQuestionId();
            try {
                boolean result = this.save(questionBankQuestion);
                ThrowUtils.throwIf(result == false, ErrorCode.OPERATION_ERROR, "批量上传题目到题库中失败");
            } catch (DataIntegrityViolationException e) {
                log.error("数据库唯一键冲突或违反其他完整性约束，题目 id: {}, 题库 id: {}, 错误信息: {}",
                        questionId, questionBankId, e.getMessage());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目已存在于该题库，无法重复添加");
            } catch (DataAccessException e) {
                log.error("数据库连接问题、事务问题等导致操作失败，题目 id: {}, 题库 id: {}, 错误信息: {}",
                        questionId, questionBankId, e.getMessage());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "数据库操作失败");
            } catch (Exception e) {
                // 捕获其他异常，做通用处理
                log.error("添加题目到题库时发生未知错误，题目 id: {}, 题库 id: {}, 错误信息: {}",
                        questionId, questionBankId, e.getMessage());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "向题库添加题目失败");
            }
        }
    }


    /**
     * 批量向题库中删除题目
     * @param questionIds
     * @param questionBankId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchRemoveQuestionToBank(List<Long> questionIds, Long questionBankId){
        ThrowUtils.throwIf(questionIds == null, ErrorCode.PARAMS_ERROR, "题目列表为空");
        ThrowUtils.throwIf(questionBankId <= 0, ErrorCode.PARAMS_ERROR, "题库id不存在");

        //根据题库表查询题目id是否存在
        List<Question> questions = questionService.listByIds(questionIds);
        List<Long> questionIdsList = questions.stream()
                .map(Question::getId)
                .collect(Collectors.toList());
        ThrowUtils.throwIf(questionIdsList == null, ErrorCode.NOT_FOUND_ERROR);
        //判断题库id是否存在
        QuestionBank questionBank = questionBankService.getById(questionBankId);
        ThrowUtils.throwIf(questionBank == null, ErrorCode.NOT_FOUND_ERROR, "该题库不存在");
        //检查题目id是否存在与关联表中
        for (Long questionId : questionIdsList) {
//            查找已经存在于关联表中的题目id，根据questionid进行查询，如果不存在则插入
            LambdaQueryWrapper<QuestionBankQuestion> questionLambdaQueryWrapper = Wrappers.lambdaQuery(QuestionBankQuestion.class)
                    .eq(QuestionBankQuestion::getQuestionId, questionId)
                    .eq(QuestionBankQuestion::getQuestionBankId,questionBank.getId());
            List<QuestionBankQuestion> exist = this.list(questionLambdaQueryWrapper);
            ThrowUtils.throwIf(exist.isEmpty(), ErrorCode.NOT_FOUND_ERROR);
            if (!exist.isEmpty()) {
//                在关联表中删除对应的题目和题库
                this.remove(questionLambdaQueryWrapper);
            }
        }
    }


}
