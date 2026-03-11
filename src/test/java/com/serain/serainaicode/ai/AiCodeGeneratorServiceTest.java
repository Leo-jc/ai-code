package com.serain.serainaicode.ai;

import com.serain.serainaicode.ai.model.HtmlCodeResult;
import com.serain.serainaicode.ai.model.MultiFileCodeResult;
import com.serain.serainaicode.core.AiCodeGeneratorFacade;
import com.serain.serainaicode.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.util.List;

@SpringBootTest
class AiCodeGeneratorServiceTest {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Test
    void test() {
        String result = aiCodeGeneratorService.test("写一首古诗");
        Assertions.assertNotNull(result);
    }

    @Test
    void generateHtmlCode() {
        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode("做个程序员鱼皮的工作记录小工具");
        Assertions.assertNotNull(result);
    }

    @Test
    void generateMultiFileCode() {
        MultiFileCodeResult multiFileCode = aiCodeGeneratorService.generateMultiFileCode("做个程序员鱼皮的留言板");
        Assertions.assertNotNull(multiFileCode);
    }

    @Test
    void testChatMemory() {
        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(1, "做个程序员鱼皮的工具网站，总代码量不超过 20 行");
        Assertions.assertNotNull(result);
        result = aiCodeGeneratorService.generateHtmlCode(1, "不要生成网站，告诉我你刚刚做了什么？");
        Assertions.assertNotNull(result);
        result = aiCodeGeneratorService.generateHtmlCode(2, "做个程序员鱼皮的工具网站，总代码量不超过 20 行");
        Assertions.assertNotNull(result);
        result = aiCodeGeneratorService.generateHtmlCode(2, "不要生成网站，告诉我你刚刚做了什么？");
        Assertions.assertNotNull(result);
    }

    @Test
    void generateVueProjectCodeStream() {
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream(
                "简单的任务记录网站，总代码量不超过 200 行",
                CodeGenTypeEnum.VUE_PROJECT, 1L);
        // 阻塞等待所有数据收集完成
        List<String> result = codeStream.collectList().block();
        // 验证结果
        Assertions.assertNotNull(result);
        String completeContent = String.join("", result);
        Assertions.assertNotNull(completeContent);
    }

}
