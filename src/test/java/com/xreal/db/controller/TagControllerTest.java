package com.xreal.db.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xreal.db.dto.TagRequest;
import com.xreal.db.dto.TagResponse;
import com.xreal.db.service.TagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.context.ActiveProfiles;

@WebMvcTest(controllers = TagController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class
})
@ActiveProfiles("test")
@Import(com.xreal.db.MockJpaConfiguration.class)
class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TagService tagService;

    private TagRequest tagRequest;
    private TagResponse tagResponse;

    @BeforeEach
    void setUp() {
        tagRequest = new TagRequest();
        tagRequest.setName("Xreal Air");
        tagRequest.setDescription("Xreal Air product series");
        tagRequest.setActive(true);

        tagResponse = new TagResponse();
        tagResponse.setName("Xreal Air");
        tagResponse.setDescription("Xreal Air product series");
        tagResponse.setActive(true);
    }

    @Test
    void createTag_Success() throws Exception {
        when(tagService.createTag(any(TagRequest.class))).thenReturn(tagResponse);

        mockMvc.perform(post("/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tagRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Xreal Air"))
                .andExpect(jsonPath("$.description").value("Xreal Air product series"))
                .andExpect(jsonPath("$.active").value(true));

        verify(tagService, times(1)).createTag(any(TagRequest.class));
    }

    @Test
    void updateTag_Success() throws Exception {
        String tagName = "Xreal Air";
        when(tagService.updateTag(eq(tagName), any(TagRequest.class))).thenReturn(tagResponse);

        mockMvc.perform(put("/tags/{name}", tagName)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tagRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Xreal Air"))
                .andExpect(jsonPath("$.description").value("Xreal Air product series"));

        verify(tagService, times(1)).updateTag(eq(tagName), any(TagRequest.class));
    }

    @Test
    void deleteTag_Success() throws Exception {
        String tagName = "Xreal Air";
        doNothing().when(tagService).deleteTag(tagName);

        mockMvc.perform(delete("/tags/{name}", tagName))
                .andExpect(status().isNoContent());

        verify(tagService, times(1)).deleteTag(tagName);
    }

    @Test
    void getAllTags_Success() throws Exception {
        List<TagResponse> tags = Arrays.asList(
                createTagResponse("Xreal Air", "Xreal Air product", true),
                createTagResponse("Xreal Air 2", "Xreal Air 2 product", true),
                createTagResponse("Beam", "Beam accessory", false)
        );

        when(tagService.getAllTags()).thenReturn(tags);

        mockMvc.perform(get("/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Xreal Air"))
                .andExpect(jsonPath("$[1].name").value("Xreal Air 2"))
                .andExpect(jsonPath("$[2].name").value("Beam"))
                .andExpect(jsonPath("$[2].active").value(false));

        verify(tagService, times(1)).getAllTags();
    }

    @Test
    void getActiveTags_Success() throws Exception {
        List<TagResponse> activeTags = Arrays.asList(
                createTagResponse("Xreal Air", "Xreal Air product", true),
                createTagResponse("Xreal Air 2", "Xreal Air 2 product", true)
        );

        when(tagService.getActiveTags()).thenReturn(activeTags);

        mockMvc.perform(get("/tags/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Xreal Air"))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[1].name").value("Xreal Air 2"))
                .andExpect(jsonPath("$[1].active").value(true));

        verify(tagService, times(1)).getActiveTags();
    }

    private TagResponse createTagResponse(String name, String description, boolean active) {
        TagResponse response = new TagResponse();
        response.setName(name);
        response.setDescription(description);
        response.setActive(active);
        return response;
    }
}