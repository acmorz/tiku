package com.fsh.tiku.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fsh.tiku.annotation.AuthCheck;
import com.fsh.tiku.common.ErrorCode;
import com.fsh.tiku.constant.CommonConstant;
import com.fsh.tiku.exception.ThrowUtils;
import com.fsh.tiku.manager.AiManager;
import com.fsh.tiku.mapper.QuestionBankQuestionMapper;
import com.fsh.tiku.mapper.QuestionMapper;
import com.fsh.tiku.model.dto.question.QuestionAIGenerateRequest;
import com.fsh.tiku.model.dto.question.QuestionEsDTO;
import com.fsh.tiku.model.dto.question.QuestionQueryRequest;
import com.fsh.tiku.model.entity.Question;
import com.fsh.tiku.model.entity.QuestionBank;
import com.fsh.tiku.model.entity.QuestionBankQuestion;
import com.fsh.tiku.model.entity.User;
import com.fsh.tiku.model.vo.QuestionVO;
import com.fsh.tiku.model.vo.UserVO;
import com.fsh.tiku.service.QuestionBankQuestionService;
import com.fsh.tiku.service.QuestionBankService;
import com.fsh.tiku.service.QuestionService;
import com.fsh.tiku.service.UserService;
import com.fsh.tiku.utils.SqlUtils;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.impl.AMQImpl;
import com.volcengine.ark.runtime.service.ArkService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
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

/**
 * 题目服务实现
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
@Service
@Slf4j
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {

    @Resource
    private AiManager aiManager;
    @Resource
    private QuestionBankQuestionMapper questionBankQuestionMapper;
    @Resource
    private UserService userService;
    @Resource
    private ElasticsearchRestTemplate elasticsearchRestTemplate;
    @Autowired
    private QuestionBankQuestionService questionBankQuestionService;
    @Resource
    private QuestionBankService questionBankService;


    /**
     * 校验数据
     * 比如检查题目长度，题目内容是否符合你定义的要求，就用这个函数检查
     *
     * @param question
     * @param add      对创建的数据进行校验
     */
    @Override
    public void validQuestion(Question question, boolean add) {
        ThrowUtils.throwIf(question == null, ErrorCode.PARAMS_ERROR);
        // todo 从对象中取值
        String title = question.getTitle();
        // 创建数据时，参数不能为空
        if (add) {
            // todo 补充校验规则
            ThrowUtils.throwIf(StringUtils.isBlank(title), ErrorCode.PARAMS_ERROR);
        }
        // 修改数据时，有参数则校验
        // todo 补充校验规则
        if (StringUtils.isNotBlank(title)) {
            ThrowUtils.throwIf(title.length() > 80, ErrorCode.PARAMS_ERROR, "标题过长");
        }
    }

    /**
     * 获取查询条件
     *
     * @param questionQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest) {
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        if (questionQueryRequest == null) {
            return queryWrapper;
        }
        // todo 从对象中取值
        Long id = questionQueryRequest.getId();
        Long notId = questionQueryRequest.getNotId();
        String title = questionQueryRequest.getTitle();
        String content = questionQueryRequest.getContent();
        String searchText = questionQueryRequest.getSearchText();
        String sortField = questionQueryRequest.getSortField();
        String sortOrder = questionQueryRequest.getSortOrder();
        List<String> tagList = questionQueryRequest.getTags();
        Long userId = questionQueryRequest.getUserId();
        String answer = questionQueryRequest.getAnswer();

        // todo 补充需要的查询条件
        // 从多字段中搜索
        if (StringUtils.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("title", searchText).or().like("content", searchText));
        }
        // 模糊查询
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.like(StringUtils.isNotBlank(content), "content", content);
        queryWrapper.like(StringUtils.isNotBlank(answer), "answer", answer);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tagList)) {
            for (String tag : tagList) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 精确查询
        queryWrapper.ne(ObjectUtils.isNotEmpty(notId), "id", notId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        // 排序规则
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 获取题目封装
     *
     * @param question
     * @param request
     * @return
     */
    @Override
    public QuestionVO getQuestionVO(Question question, HttpServletRequest request) {
        // 对象转封装类
        QuestionVO questionVO = QuestionVO.objToVo(question);

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Long userId = question.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        questionVO.setUser(userVO);
        // 2. 已登录，获取用户点赞、收藏状态 p2
        // endregion

        return questionVO;
    }

    /**
     * 分页获取题目封装
     * 把没有用户信息的集合封装成带有用户信息的类
     * @param questionPage
     * @param request
     * @return
     */
    @Override
    public Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage, HttpServletRequest request) {
        List<Question> questionList = questionPage.getRecords();
        Page<QuestionVO> questionVOPage = new Page<>(questionPage.getCurrent(), questionPage.getSize(), questionPage.getTotal());
        if (CollUtil.isEmpty(questionList)) {
            return questionVOPage;
        }
        // 对象列表 => 封装对象列表
        List<QuestionVO> questionVOList = questionList.stream().map(question -> {
            return QuestionVO.objToVo(question);
        }).collect(Collectors.toList());

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Set<Long> userIdSet = questionList.stream().map(Question::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 填充信息
        questionVOList.forEach(questionVO -> {
            Long userId = questionVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            questionVO.setUser(userService.getUserVO(user));
        });
        // endregion

        questionVOPage.setRecords(questionVOList);
        return questionVOPage;
    }

    /**
     * 分页获取题目封装
     * 把没有用户信息的集合封装成带有用户信息的类
     * @param questionQueryRequest
     * @return
     */
    public Page<Question> listQuestionByPage(QuestionQueryRequest questionQueryRequest){
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();

        QueryWrapper<Question> queryWrapper = this.getQueryWrapper(questionQueryRequest);

        //根据题库id查询题库对应的题目
        Long questionBankId = questionQueryRequest.getQuestionBankId();
        if(questionBankId != null) {
            //通过关联表去查询题库对应的题目id集合
            LambdaQueryWrapper<QuestionBankQuestion> lambdaQueryWrapper = Wrappers.lambdaQuery(QuestionBankQuestion.class)
                    .select(QuestionBankQuestion::getQuestionId) //只保留对应的question_id
                    .eq(QuestionBankQuestion::getQuestionBankId, questionBankId);
            //List<QuestionBankQuestion> list = questionBankQuestionService.list(lambdaQueryWrapper);
            List<Long> questionIdList = questionBankQuestionMapper.listByquestionBankId(questionBankId);
            if(CollUtil.isNotEmpty(questionIdList)) {
                /*List<Long> questionIdList = list.stream()
                        .map(item -> item.getQuestionId())
                        .collect(Collectors.toList());*/
                queryWrapper.in("id", questionIdList);
            }
        }

        // 查询数据库
        Page<Question> questionPage = this.page(new Page<>(current, size), queryWrapper);
        return questionPage;
    }

    /**
     * ES查询题目
     * @param questionQueryRequest
     * @return
     */
    @Override
    public Page<Question> searchFromEs(QuestionQueryRequest questionQueryRequest) {
        // 获取参数
        Long id = questionQueryRequest.getId();
        Long notId = questionQueryRequest.getNotId();
        String searchText = questionQueryRequest.getSearchText();
        List<String> tags = questionQueryRequest.getTags();
        Long questionBankId = questionQueryRequest.getQuestionBankId();
        Long userId = questionQueryRequest.getUserId();
        // 注意，ES 的起始页为 0
        int current = questionQueryRequest.getCurrent() - 1;
        int pageSize = questionQueryRequest.getPageSize();
        //继承父类非私有的方法和字段
        String sortField = questionQueryRequest.getSortField();
        String sortOrder = questionQueryRequest.getSortOrder();

        /**
         * 构造查询条件 mysql中用and连接，ES中用bool
         * fiter和must的区别：mustNot（排除符合条件的文档）和must计算到评分中，性能较低，filter直接过滤，过滤之后在计算索引和得分
         * termQuery用于精确查询
         * matchQuery用于
         * should 相当于数据库中的or
         */
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        // 过滤
        boolQueryBuilder.filter(QueryBuilders.termQuery("isDelete", 0));
        if (id != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("id", id));
        }
        if (notId != null) {
            //不存在这个Id才查
            boolQueryBuilder.mustNot(QueryBuilders.termQuery("id", notId));
        }
        if (userId != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("userId", userId));
        }
        if (questionBankId != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("questionBankId", questionBankId));
        }
        // 必须包含所有标签
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                boolQueryBuilder.filter(QueryBuilders.termQuery("tags", tag));
            }
        }
        // 按关键词检索
        if (StringUtils.isNotBlank(searchText)) {
            boolQueryBuilder.should(QueryBuilders.matchQuery("title", searchText));
            boolQueryBuilder.should(QueryBuilders.matchQuery("content", searchText));
            boolQueryBuilder.should(QueryBuilders.matchQuery("answer", searchText));
            //三个or中有一个满足就可以
            boolQueryBuilder.minimumShouldMatch(1);
        }
        // 排序
        SortBuilder<?> sortBuilder = SortBuilders.scoreSort();
        if (StringUtils.isNotBlank(sortField)) {
            sortBuilder = SortBuilders.fieldSort(sortField);
            sortBuilder.order(CommonConstant.SORT_ORDER_ASC.equals(sortOrder) ? SortOrder.ASC : SortOrder.DESC);
        }
        // 分页
        PageRequest pageRequest = PageRequest.of(current, pageSize);
        // 构造查询
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(boolQueryBuilder)
                .withPageable(pageRequest)
                .withSorts(sortBuilder)
                .build();
        SearchHits<QuestionEsDTO> searchHits = elasticsearchRestTemplate.search(searchQuery, QuestionEsDTO.class);
        // 复用 MySQL 的分页对象，封装返回结果
        Page<Question> page = new Page<>();
        page.setTotal(searchHits.getTotalHits());
        List<Question> resourceList = new ArrayList<>();
        if (searchHits.hasSearchHits()) {
            List<SearchHit<QuestionEsDTO>> searchHitList = searchHits.getSearchHits();
            for (SearchHit<QuestionEsDTO> questionEsDTOSearchHit : searchHitList) {
                resourceList.add(QuestionEsDTO.dtoToObj(questionEsDTOSearchHit.getContent()));
            }
        }
        page.setRecords(resourceList);
        return page;
    }


    /**
     * 批量删除题目，并查找在题库中的题目，删除关联表中的对应关系
     * @param questionIds
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteQuestion(List<Long> questionIds){
        ThrowUtils.throwIf(questionIds == null, ErrorCode.PARAMS_ERROR, "批量删除题目列表，但为空");

        //根据题目表查询题目id是否存在
        List<Question> questions = this.listByIds(questionIds);
        List<Long> questionIdsList = questions.stream()
                .map(Question::getId)
                .collect(Collectors.toList());
        //判断题目列表是否为空
        ThrowUtils.throwIf(questionIdsList == null, ErrorCode.NOT_FOUND_ERROR);
        //检查题目id是否存在与关联表中
        for (Long questionId : questionIdsList) {
            //删除题目表中的题目
            this.removeById(questionId);
//            查找已经存在于关联表中的题目id，根据questionid进行查询，如果不存在则插入
            LambdaQueryWrapper<QuestionBankQuestion> questionLambdaQueryWrapper = Wrappers.lambdaQuery(QuestionBankQuestion.class)
                    .eq(QuestionBankQuestion::getQuestionId, questionId);
            List<QuestionBankQuestion> exist = questionBankQuestionService.list(questionLambdaQueryWrapper);
            if (!exist.isEmpty()) {
//                在关联表中删除对应的题目和题库
                questionBankQuestionService.remove(questionLambdaQueryWrapper);
            }
        }
    }

    @Override
    public boolean aiGenerateQuestion(String questionType, int number, Long userId) {
        ThrowUtils.throwIf(ObjectUtil.hasEmpty(questionType, number), ErrorCode.PARAMS_ERROR, "参数空缺");
        // 1.定义系统Prompt
        String systemPrompt = "你是专业程序员面试官，你要帮我生成 {数量} 道 {方向} 热门常问的面试题，要求输出格式如下：\n" +
                "\n" +
                "1. 什么是 Java 中的反射？\n" +
                "2. Java 8 中的 Stream API 有什么作用？\n" +
                "3. xxxxxx\n" +
                "\n" +
                "请不要输出：多余的内容、开头，结尾。只输出题目。\n" +
                "\n" +
                "我会给你要生成的题目{数量}、以及题目{方向}\n";
        // 2.拼接用于Prompt
        String userPrompt = "题目数量" + number + "题目方向" + questionType;
        // 3.调用AI接口返回题目列表
        long startTime = System.currentTimeMillis(); // 开始时间戳
        String result;
        try {
            result = aiManager.doChat(systemPrompt, userPrompt);
        } finally {
            long cost = System.currentTimeMillis() - startTime; // 计算耗时
            log.info("AI接口调用耗时: {}ms", cost);
        }
        // 4.对题目列表进行后处理
        List<String> lines = Arrays.asList(result.split("\n"));

        List<String> answer = lines.stream()
                .map(line -> StrUtil.removePrefix(line, StrUtil.subBefore(line, " ", false)))
                .map(line -> line.replace("`", ""))
                .collect(Collectors.toList());

        // 自定义线程池
        ThreadPoolExecutor customExecutor = new ThreadPoolExecutor(
                30,             // 核心线程数 同时处理批数
                50,                        // 最大线程数
                60L,                       // 线程空闲存活时间
                TimeUnit.SECONDS,           // 存活时间单位
                new LinkedBlockingQueue<>(10000),  // 阻塞队列容量
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：由调用线程处理任务，把异步变成同步
        );


        //判断题库表中是否存在该题目
        QuestionBank questionBank = new QuestionBank();
        String tag = "[\"AI生成\",\"" + questionType + "\"]";
        //检测questionBank表中是否有该题库
        questionBank.setUserId(userId);
        questionBank.setTitle(questionType);
        //获取题库id
        Long questionBankId = questionBankService.searchByTitle(questionBank);

        List<Question> questionsList = new ArrayList<>();
        List<Long> questionsId = new ArrayList<>();
        // 用于保存所有批次的任务 CompletableFuture
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for(int i = 0; i < answer.size(); i ++ ){
            String title = answer.get(i);

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                Question question = new Question();
                question.setTitle(title);
                question.setUserId(userId);
                question.setTags(tag);
                question.setAnswer(this.aiGenerateQuestionAnswer(title));
                questionsList.add(question);
                log.info("当前线程名：" + Thread.currentThread().getName());
                this.save(question);
                //插入题目到题库中
                Long questionId = question.getId();
                questionsId.add(questionId);
//                QuestionBankQuestion questionBankQuestion = new QuestionBankQuestion();
//                questionBankQuestion.setQuestionBankId(questionBankId);
//                questionBankQuestion.setQuestionId(questionId);
//                questionBankQuestion.setUserId(userId);
//                questionBankQuestionService.save(questionBankQuestion);
            }, customExecutor).exceptionally(ex -> {
                log.error("批处理执行ai生成答案失败", ex);
                return null;
            });

            futures.add(future);
        }

        // 等待所有批次操作完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
//        this.saveBatch(questionsList);
        questionBankQuestionService.batchAddQuestionToBank(questionsId, questionBankId, userId);
        customExecutor.shutdown();
        return true;
    }

    /**
     * 从消息队列中取出消息
     * @param message
     * @param questionAIGenerateRequest
     * @param channel
     */
    @Override
    @RabbitListener(queues = {"questionbank_queue"})
    public void aiGenerateQuestionFromRabbitMQ(Message message, QuestionAIGenerateRequest questionAIGenerateRequest, Channel channel) {
        String questionType = questionAIGenerateRequest.getQuestionType();
        int number = questionAIGenerateRequest.getNumber();
        Long userId = questionAIGenerateRequest.getUserId();

        log.info("从RabbitMQ中获取消息，进行AI生成对应题目");
        aiGenerateQuestion(questionType, number, userId);

        //deliveryTag是channel中按顺序自增的
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            /**
             *  deliveryTag - 来自接收到的 AMQP.Basic.GetOk 或 AMQP.Basic.Deliver 的标签,deliveryTag是channel中按顺序自增的
             *  true 表示确认从第一个消息到包括所提供的 deliveryTag 在内的所有消息；false 表示仅确认所提供的 deliveryTag 对应的消息。
             */
            channel.basicAck(deliveryTag, false);
            //第二个true标识重新放回队列，false标识不放回队列
            //channel.basicNack(deliveryTag, false, true);
        }catch(Exception e){
            log.error("网络中断，消息确认失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String aiGenerateQuestionAnswer(String questionTitle) {
        ThrowUtils.throwIf(ObjectUtil.hasEmpty(questionTitle), ErrorCode.PARAMS_ERROR, "参数空缺");
        // 1.定义系统Prompt
        String systemPrompt = "你是专业的程序员面试官，我给你一道面试题，帮我生成详细题解。要求如下：\n" +
                "\n" +
                "1. 题解的语句要自然流畅\n" +
                "2. 题解先给出总结性的回答，再详细解释\n" +
                "3. 要使用 Markdown 语法输出\n" +
                "\n" +
                "请不要输出：多余的内容、开头，结尾。只输出题目答案。\n" +
                "\n" +
                "接下来我会给你要生成的面试题目\n";
        // 2.拼接用于Prompt
        String userPrompt = "题目标题：" + questionTitle;
        // 3.调用AI接口返回题目列表
        String answer = aiManager.doChat(systemPrompt, userPrompt);

        ThrowUtils.throwIf(StringUtils.isEmpty(answer), ErrorCode.OPERATION_ERROR, "ai生成答案为空");

        return answer;
    }
}
