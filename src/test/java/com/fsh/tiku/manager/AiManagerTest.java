package com.fsh.tiku.manager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiManagerTest {

    @Resource
    private AiManager aiManager;

    @Test
    void doChat() {
        String s = aiManager.doChat("你好");
        System.out.println(s);
    }

    @Test
    void testDoChat() {
        String s = aiManager.doChat("当我说你好的时候，你要回复：说谢谢了吗？", "你好");
        System.out.println(s);
    }
}