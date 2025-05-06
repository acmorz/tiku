package com.fsh.tiku.mapper;

import com.fsh.tiku.model.entity.QuestionBank;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author fanshihao
* @description 针对表【question_bank(题库)】的数据库操作Mapper
* @createDate 2025-04-02 22:08:02
* @Entity com.fsh.tiku.model.entity.QuestionBank
*/
public interface QuestionBankMapper extends BaseMapper<QuestionBank> {

    @Select("select id from question_bank where LOWER(title) like LOWER(CONCAT(#{title}, '%'))")
    List<Long> searchByTitle(String title);
}




