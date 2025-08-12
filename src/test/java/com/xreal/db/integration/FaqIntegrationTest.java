package com.xreal.db.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xreal.db.dto.FaqRequest;
import com.xreal.db.dto.FaqResponse;
import com.xreal.db.dto.TagRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:mysql://localhost:3306/xreal_tech_faq_test?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC&createDatabaseIfNotExist=true",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FaqIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static Long createdFaqId;

    @BeforeEach
    void setUp() throws Exception {
        // 清理数据
        mockMvc.perform(delete("/faqs/all"));
    }

    @Test
    @Order(1)
    void testCreateTagsForTesting() throws Exception {
        // 创建测试用的标签
        String[] tagNames = {"Xreal Air", "Xreal Air 2", "Beam", "Setup", "Connection", "iPhone", "Android"};
        
        for (String tagName : tagNames) {
            TagRequest tagRequest = new TagRequest();
            tagRequest.setName(tagName);
            tagRequest.setDescription("Test tag: " + tagName);
            tagRequest.setActive(true);

            try {
                mockMvc.perform(post("/tags")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(tagRequest)))
                        .andExpect(status().isCreated());
            } catch (Exception e) {
                // Tag might already exist, ignore
            }
        }
    }

    @Test
    @Order(2)
    void testCompleteScenario_CreateUpdateSearchDelete() throws Exception {
        // Step 1: 创建第一个FAQ
        FaqRequest faqRequest1 = new FaqRequest();
        faqRequest1.setQuestion("How to set up Xreal Air?");
        faqRequest1.setAnswer("Connect the USB-C cable to your compatible device. Ensure DP Alt Mode is supported.");
        faqRequest1.setInstruction("Check device compatibility first");
        faqRequest1.setUrl("https://xreal.com/setup-air");
        faqRequest1.setActive(true);
        faqRequest1.setComment("Basic setup guide");
        faqRequest1.setTags(new HashSet<>(Arrays.asList("Xreal Air", "Setup")));

        MvcResult result1 = mockMvc.perform(post("/faqs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(faqRequest1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.question").value("How to set up Xreal Air?"))
                .andReturn();

        FaqResponse response1 = objectMapper.readValue(result1.getResponse().getContentAsString(), FaqResponse.class);
        Long faqId1 = response1.getId();

        // Step 2: 创建第二个FAQ
        FaqRequest faqRequest2 = new FaqRequest();
        faqRequest2.setQuestion("How to connect Xreal Air 2 to iPhone?");
        faqRequest2.setAnswer("Use a Lightning to USB-C adapter or iPhone 15 with direct USB-C connection.");
        faqRequest2.setInstruction("Adapter required for older iPhone models");
        faqRequest2.setUrl("https://xreal.com/iphone-setup");
        faqRequest2.setActive(true);
        faqRequest2.setComment("iPhone connection guide");
        faqRequest2.setTags(new HashSet<>(Arrays.asList("Xreal Air 2", "iPhone", "Connection")));

        MvcResult result2 = mockMvc.perform(post("/faqs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(faqRequest2)))
                .andExpect(status().isCreated())
                .andReturn();

        FaqResponse response2 = objectMapper.readValue(result2.getResponse().getContentAsString(), FaqResponse.class);
        Long faqId2 = response2.getId();

        // Step 3: 创建第三个FAQ（inactive）
        FaqRequest faqRequest3 = new FaqRequest();
        faqRequest3.setQuestion("How to use Beam with Xreal glasses?");
        faqRequest3.setAnswer("Connect Beam between your device and Xreal glasses for wireless experience.");
        faqRequest3.setInstruction("Charge Beam before first use");
        faqRequest3.setUrl("https://xreal.com/beam");
        faqRequest3.setActive(false);
        faqRequest3.setComment("Beam accessory guide");
        faqRequest3.setTags(new HashSet<>(Arrays.asList("Beam", "Xreal Air", "Xreal Air 2")));

        mockMvc.perform(post("/faqs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(faqRequest3)))
                .andExpect(status().isCreated());

        // Step 4: 获取单个FAQ
        mockMvc.perform(get("/faqs/{id}", faqId1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(faqId1))
                .andExpect(jsonPath("$.question").value("How to set up Xreal Air?"));

        // Step 5: 获取所有FAQ（不过滤）
        mockMvc.perform(get("/faqs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].question").exists());

        // Step 6: 获取所有激活的FAQ
        mockMvc.perform(get("/faqs")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        // Step 7: 根据标签搜索
        mockMvc.perform(get("/faqs/search")
                        .param("tags", "Xreal Air"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2)); // FAQ1和FAQ3都有Xreal Air标签

        // Step 8: 根据多个标签搜索
        mockMvc.perform(get("/faqs/search")
                        .param("tags", "iPhone", "Connection"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].question").value("How to connect Xreal Air 2 to iPhone?"));

        // Step 9: 更新FAQ
        faqRequest1.setAnswer("Updated answer: Connect USB-C cable. Check compatibility at xreal.com/compatibility");
        faqRequest1.setTags(new HashSet<>(Arrays.asList("Xreal Air", "Setup", "Connection")));

        mockMvc.perform(put("/faqs/{id}", faqId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(faqRequest1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value(faqRequest1.getAnswer()))
                .andExpect(jsonPath("$.tags").isArray());

        // Step 10: 验证更新后的标签搜索
        mockMvc.perform(get("/faqs/search")
                        .param("tags", "Connection"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2)); // 现在FAQ1和FAQ2都有Connection标签

        // Step 11: 分页测试
        mockMvc.perform(get("/faqs")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));

        // Step 12: 排序测试
        mockMvc.perform(get("/faqs")
                        .param("sort", "timestamp,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(faqId1)); // 第一个创建的应该排在前面

        // Step 13: 删除单个FAQ
        mockMvc.perform(delete("/faqs/{id}", faqId2))
                .andExpect(status().isNoContent());

        // Step 14: 验证删除后的结果
        mockMvc.perform(get("/faqs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        // Step 15: 删除所有FAQ
        mockMvc.perform(delete("/faqs/all"))
                .andExpect(status().isNoContent());

        // Step 16: 验证所有FAQ已删除
        mockMvc.perform(get("/faqs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @Order(3)
    void testValidation_InvalidFaqRequest() throws Exception {
        // 测试缺少必要字段
        FaqRequest invalidRequest = new FaqRequest();
        // 只设置部分字段，缺少question
        invalidRequest.setAnswer("Some answer");

        mockMvc.perform(post("/faqs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(4)
    void testGetNonExistentFaq() throws Exception {
        mockMvc.perform(get("/faqs/{id}", 99999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(5)
    void testUpdateNonExistentFaq() throws Exception {
        FaqRequest faqRequest = new FaqRequest();
        faqRequest.setQuestion("Test question");
        faqRequest.setAnswer("Test answer");
        faqRequest.setActive(true);
        faqRequest.setTags(new HashSet<>(Arrays.asList("Xreal Air")));

        mockMvc.perform(put("/faqs/{id}", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(faqRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(6)
    void testDeleteNonExistentFaq() throws Exception {
        mockMvc.perform(delete("/faqs/{id}", 99999L))
                .andExpect(status().isNotFound());
    }
}