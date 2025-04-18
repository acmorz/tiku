package com.fsh.tiku.mapper;

import com.fsh.tiku.model.entity.Question;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Date;

/**
* @author fanshihao
* @description 针对表【question(题目)】的数据库操作Mapper
* @createDate 2025-04-02 22:08:02
* @Entity com.fsh.tiku.model.entity.Question
*/
public interface QuestionMapper extends BaseMapper<Question> {

    /**
     * 查询前五分钟内被更新的question
     * 其中存在逻辑删除字段，如果用Mybatis-Plus内部的查询接口会查不到 自带isDelete = 0
     * 所以手写一个SQL语句进行查询
     * @param minUpdateTime
     * @return
     */
    @Select("select * from question where updateTime >= #{minUpdateTime}")
    List<Question> listQuestionWithDelete(Date minUpdateTime);
}




