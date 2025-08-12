package com.xreal.db.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xreal.db.dto.TagRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:mysql://localhost:3306/xreal_tech_faq_test?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC&createDatabaseIfNotExist=true",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TagIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Order(1)
    void testCompleteTagLifecycle() throws Exception {
        // Step 1: 创建多个标签
        String[] tagData = {
            "Xreal Air|AR glasses first generation|true",
            "Xreal Air 2|AR glasses second generation|true",
            "Xreal Air 2 Pro|Pro version with electrochromic dimming|true",
            "Beam|Wireless adapter accessory|true",
            "iPhone|Apple iPhone compatibility|true",
            "Android|Android device compatibility|true",
            "Setup|Initial setup guides|true",
            "Troubleshooting|Problem solving guides|false"
        };

        for (String data : tagData) {
            String[] parts = data.split("\\|");
            TagRequest tagRequest = new TagRequest();
            tagRequest.setName(parts[0]);
            tagRequest.setDescription(parts[1]);
            tagRequest.setActive(Boolean.parseBoolean(parts[2]));

            mockMvc.perform(post("/tags")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(tagRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value(parts[0]))
                    .andExpect(jsonPath("$.description").value(parts[1]))
                    .andExpect(jsonPath("$.active").value(Boolean.parseBoolean(parts[2])));
        }

        // Step 2: 获取所有标签
        mockMvc.perform(get("/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)))
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[7].active").value(false));

        // Step 3: 获取激活的标签
        mockMvc.perform(get("/tags/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7))); // 只有7个激活的标签

        // Step 4: 更新标签
        TagRequest updateRequest = new TagRequest();
        updateRequest.setName("Beam");
        updateRequest.setDescription("Beam Pro - Enhanced wireless adapter with 3DoF");
        updateRequest.setActive(true);

        mockMvc.perform(put("/tags/{name}", "Beam")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Beam Pro - Enhanced wireless adapter with 3DoF"));

        // Step 5: 禁用一个标签
        TagRequest disableRequest = new TagRequest();
        disableRequest.setName("Android");
        disableRequest.setDescription("Android device compatibility");
        disableRequest.setActive(false);

        mockMvc.perform(put("/tags/{name}", "Android")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(disableRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // Step 6: 验证激活标签数量减少
        mockMvc.perform(get("/tags/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6))); // 现在只有6个激活的标签

        // Step 7: 删除标签
        mockMvc.perform(delete("/tags/{name}", "Troubleshooting"))
                .andExpect(status().isNoContent());

        // Step 8: 验证标签已删除
        mockMvc.perform(get("/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7))); // 现在只有7个标签
    }

    @Test
    @Order(2)
    void testDuplicateTagCreation() throws Exception {
        // 创建第一个标签
        TagRequest tagRequest = new TagRequest();
        tagRequest.setName("UniqueTag");
        tagRequest.setDescription("A unique tag");
        tagRequest.setActive(true);

        mockMvc.perform(post("/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tagRequest)))
                .andExpect(status().isCreated());

        // 尝试创建同名标签
        mockMvc.perform(post("/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tagRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(3)
    void testUpdateNonExistentTag() throws Exception {
        TagRequest tagRequest = new TagRequest();
        tagRequest.setName("NonExistent");
        tagRequest.setDescription("This tag doesn't exist");
        tagRequest.setActive(true);

        mockMvc.perform(put("/tags/{name}", "NonExistent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tagRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(4)
    void testDeleteNonExistentTag() throws Exception {
        mockMvc.perform(delete("/tags/{name}", "NonExistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(5)
    void testValidation_InvalidTagRequest() throws Exception {
        // 测试缺少名称
        TagRequest invalidRequest = new TagRequest();
        invalidRequest.setDescription("Description without name");
        invalidRequest.setActive(true);

        mockMvc.perform(post("/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}