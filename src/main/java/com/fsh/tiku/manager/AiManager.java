package com.fsh.tiku.manager;

import cn.hutool.core.collection.CollUtil;
import com.fsh.tiku.common.ErrorCode;
import com.fsh.tiku.config.AiConfig;
import com.fsh.tiku.exception.BusinessException;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionChoice;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 调用类
 */
@Service
public class AiManager {

    @Resource
    private ArkService aiService;

    private final String DEFAULT_MODEL = "deepseek-v3-250324";

    /**
     * 调用AI接口，获取响应字符串
     * @return
     */
    public String doChat(String userPrompt){
        String result = doChat("", userPrompt);
        return result;
    }

    /**
     * 调用AI接口，获取响应字符串
     * @return
     */
    public String doChat(String systemPrompt, String userPrompt){
        final List<ChatMessage> messages = new ArrayList<>();
        final ChatMessage systemMessage = ChatMessage.builder().role(ChatMessageRole.SYSTEM).content(systemPrompt).build();
        final ChatMessage userMessage = ChatMessage.builder().role(ChatMessageRole.USER).content(userPrompt).build();
        messages.add(systemMessage);
        messages.add(userMessage);

        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                // 指定您创建的方舟推理接入点 ID，此处已帮您修改为您的推理接入点 ID
                .model(DEFAULT_MODEL)
                .messages(messages)
                .build();

        List<ChatCompletionChoice> choices = aiService.createChatCompletion(chatCompletionRequest).getChoices();
        if(CollUtil.isNotEmpty(choices)){
            return (String)choices.get(0).getMessage().getContent();
        }
        throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "AI 调用失败，没有返回结果");
        // aiService.shutdownExecutor();
    }
}
