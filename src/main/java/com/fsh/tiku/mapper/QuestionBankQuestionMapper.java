package com.fsh.tiku.mapper;

import com.fsh.tiku.model.entity.QuestionBankQuestion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author fanshihao
* @description 针对表【question_bank_question(题库题目)】的数据库操作Mapper
* @createDate 2025-04-02 22:08:03
* @Entity com.fsh.tiku.model.entity.QuestionBankQuestion
*/
public interface QuestionBankQuestionMapper extends BaseMapper<QuestionBankQuestion> {

    @Select("select id from question_bank_question where questionBankId = #{questionBankId}")
    List<Long> listByquestionBankId(Long questionBankId);
}




