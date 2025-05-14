package com.fsh.tiku.model.dto.question;

import com.fsh.tiku.model.entity.User;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;

@Data
public class QuestionAIGenerateRequest implements Serializable {

    /**
     * 题目类型
     */
    private String questionType;

    /**
     * 题目数量
     */
    private int number;

    /**
     * 用户ID
     */
    private Long userId;

    private static final long serialVersionUID = 1L;
}
