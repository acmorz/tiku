package com.fsh.tiku.model.dto.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class QuestionBatchRemoveRequest implements Serializable {

    /**
     * 题目 id 列表
     */
    private List<Long> questionIdList;

    private static final long serialVersionUID = 1L;
}
