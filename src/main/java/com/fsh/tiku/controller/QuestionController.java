package com.fsh.tiku.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fsh.tiku.annotation.AuthCheck;
import com.fsh.tiku.common.BaseResponse;
import com.fsh.tiku.common.DeleteRequest;
import com.fsh.tiku.common.ErrorCode;
import com.fsh.tiku.common.ResultUtils;
import com.fsh.tiku.constant.UserConstant;
import com.fsh.tiku.exception.BusinessException;
import com.fsh.tiku.exception.ThrowUtils;
import com.fsh.tiku.model.dto.question.*;
import com.fsh.tiku.model.dto.questionBank.QuestionBankAddRequest;
import com.fsh.tiku.model.entity.Question;
import com.fsh.tiku.model.entity.QuestionBankQuestion;
import com.fsh.tiku.model.entity.User;
import com.fsh.tiku.model.vo.QuestionVO;
import com.fsh.tiku.service.QuestionBankQuestionService;
import com.fsh.tiku.service.QuestionService;
import com.fsh.tiku.service.UserService;
import com.jd.platform.hotkey.client.callback.JdHotKeyStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.sql.Wrapper;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 题目接口
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
@RestController
@RequestMapping("/question")
@Slf4j
public class QuestionController {

    @Resource
    private QuestionService questionService;

    @Resource
    private QuestionBankQuestionService questionBankQuestionService;

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private RabbitTemplate rabbitTemplate;



    /**
     * 创建题目
     *
     * @param questionAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addQuestion(@RequestBody QuestionAddRequest questionAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(questionAddRequest == null, ErrorCode.PARAMS_ERROR);
        // todo 在此处将实体类和 DTO 进行转换
        Question question = new Question();
        BeanUtils.copyProperties(questionAddRequest, question);
        // 数据校验
        questionService.validQuestion(question, true);
        // todo 填充默认值
        User loginUser = userService.getLoginUser(request);
        question.setUserId(loginUser.getId());
        // 写入数据库
        boolean result = questionService.save(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回新写入的数据 id
        long newQuestionId = question.getId();
        return ResultUtils.success(newQuestionId);
    }

    /**
     * 删除题目
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteQuestion(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldQuestion.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = questionService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新题目（仅管理员可用）
     *
     * @param questionUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateQuestion(@RequestBody QuestionUpdateRequest questionUpdateRequest) {
        if (questionUpdateRequest == null || questionUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和 DTO 进行转换
        Question question = new Question();
        BeanUtils.copyProperties(questionUpdateRequest, question);
        // 数据校验 ??? 为什么要校验
        questionService.validQuestion(question, false);
        // 判断是否存在
        long id = questionUpdateRequest.getId();
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = questionService.updateById(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        // 删除缓存中对应的题目
        // 自动缓存热门题库
        String key = "detail_" + id;
        try{
            redisTemplate.delete(key);
        } catch (Exception e){
            log.error("删除缓存失败 Key: {}，错误信息: {}", key, e.getMessage());
        }

        return ResultUtils.success(true);
    }

//    /**
//     * 根据 题目 id 获取题目（封装类）
//     *
//     * @param id
//     * @return
//     */
//    @GetMapping("/get/vo")
//    public BaseResponse<QuestionVO> getQuestionVOById(long id, HttpServletRequest request) {
//        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
//
//        // 自动缓存热门题库
//        String key = "detail_" + id;
//        if(JdHotKeyStore.isHotKey(key)){
//            Object cachedQuestionVO = JdHotKeyStore.get(key);
//            if(cachedQuestionVO != null){
//                return ResultUtils.success((QuestionVO) cachedQuestionVO);
//            }
//        }
//
//        // 查询数据库
//        Question question = questionService.getById(id);
//        ThrowUtils.throwIf(question == null, ErrorCode.NOT_FOUND_ERROR);
//        // 获取封装类
//        QuestionVO questionVO = questionService.getQuestionVO(question, request);
//
//        //设置本地缓存
//        JdHotKeyStore.smartSet(key, questionVO);
//
//        return ResultUtils.success(questionVO);
//    }

    /**
     * 根据 题目 id 获取题目（封装类）
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<QuestionVO> getQuestionVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);

        // 自动缓存热门题库
        String key = "detail_" + id;
        QuestionVO valueQuestionVO = (QuestionVO)redisTemplate.opsForValue().get(key);

        if(ObjectUtil.isEmpty(valueQuestionVO)){
            synchronized (this) {
                // 再去判断一次缓存中是否有数据
                QuestionVO secondValuequestionVO = (QuestionVO)redisTemplate.opsForValue().get(key);

                if(ObjectUtil.isEmpty(secondValuequestionVO)){
                    log.info("从数据库中查询数据");
                    // 1.查询数据库
                    Question question = questionService.getById(id);
                    ThrowUtils.throwIf(question == null, ErrorCode.NOT_FOUND_ERROR);
                    // 获取封装类
                    QuestionVO questionVO = questionService.getQuestionVO(question, request);
                    // 2.设置本地缓存
                    redisTemplate.opsForValue().set(key, questionVO);
                    return ResultUtils.success(questionVO);
                }

                return ResultUtils.success(secondValuequestionVO);
            }
        }

        log.info("从缓存中获取数据成功");
        return ResultUtils.success(valueQuestionVO);
    }

    /**
     * 分页获取题目列表（仅管理员可用）
     *
     * @param questionQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Question>> listQuestionByPage(@RequestBody QuestionQueryRequest questionQueryRequest) {
        ThrowUtils.throwIf(questionQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Page<Question> questionPage = questionService.listQuestionByPage(questionQueryRequest);
        return ResultUtils.success(questionPage);
    }

    /**
     * 分页获取题目列表（封装类）
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                               HttpServletRequest request) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        // 获取封装类
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }

    /**
     * 分页获取当前登录用户创建的题目列表
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listMyQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(questionQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 补充查询条件，只查询当前登录用户的数据
        User loginUser = userService.getLoginUser(request);
        questionQueryRequest.setUserId(loginUser.getId());
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        // 获取封装类
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }

    /**
     * 编辑题目
     *
     * @param questionEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> editQuestion(@RequestBody QuestionEditRequest questionEditRequest, HttpServletRequest request) {
        if (questionEditRequest == null || questionEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和 DTO 进行转换
        Question question = new Question();
        BeanUtils.copyProperties(questionEditRequest, question);
        List<String> tags = questionEditRequest.getTags();
        if(tags != null) {
            question.setTags(JSONUtil.toJsonStr(tags));
        }
        // 数据校验
        questionService.validQuestion(question, false);
        User loginUser = userService.getLoginUser(request);
        // 判断是否存在
        long id = questionEditRequest.getId();
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldQuestion.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = questionService.updateById(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }


    @PostMapping("/search/page/vo")
    public BaseResponse<Page<QuestionVO>> searchQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                                 HttpServletRequest request) {
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 200, ErrorCode.PARAMS_ERROR);
        Page<Question> questionPage = questionService.searchFromEs(questionQueryRequest);
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }

    @PostMapping("/delete/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> batchDeleteQuestion(@RequestBody QuestionBatchRemoveRequest questionBatchRemoveRequest,
                                                     HttpServletRequest request) {
        ThrowUtils.throwIf(questionBatchRemoveRequest == null, ErrorCode.PARAMS_ERROR);

        List<Long> questionIdList = questionBatchRemoveRequest.getQuestionIdList();
        questionService.batchDeleteQuestion(questionIdList);

        return ResultUtils.success(true);
    }


    /**
     * Ai自动生成题目
     *
     * @return
     */
    @PostMapping("/add/ai")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> aiGenerateQuestion(@RequestBody QuestionAIGenerateRequest questionAIGenerateRequest, HttpServletRequest request){
        ThrowUtils.throwIf(questionAIGenerateRequest == null, ErrorCode.PARAMS_ERROR);


        int number = questionAIGenerateRequest.getNumber();
        User user = userService.getLoginUser(request);
        questionAIGenerateRequest.setUserId(user.getId());

        ThrowUtils.throwIf(number <= 0 || number > 30, ErrorCode.PARAMS_ERROR, "请求题目数量不合理");


        //往RabbitMQ中发送消息
        rabbitTemplate.convertAndSend("questionbank_exchange", "questionbank", questionAIGenerateRequest, new CorrelationData(UUID.randomUUID().toString()));

        return ResultUtils.success(true);
    }

    // endregion
}
