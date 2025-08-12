package com.xreal.db.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xreal.db.dto.FaqRequest;
import com.xreal.db.dto.FaqResponse;
import com.xreal.db.service.FaqService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.context.ActiveProfiles;

@WebMvcTest(controllers = FaqController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class
})
@ActiveProfiles("test")
@Import(com.xreal.db.MockJpaConfiguration.class)
class FaqControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FaqService faqService;

    private FaqRequest faqRequest;
    private FaqResponse faqResponse;

    @BeforeEach
    void setUp() {
        Set<String> tags = new HashSet<>(Arrays.asList("Xreal Air", "Setup"));
        
        faqRequest = new FaqRequest();
        faqRequest.setQuestion("How to set up Xreal Air?");
        faqRequest.setAnswer("Connect the USB-C cable to your device...");
        faqRequest.setInstruction("Please ensure your device supports DP Alt Mode");
        faqRequest.setUrl("https://xreal.com/setup");
        faqRequest.setActive(true);
        faqRequest.setComment("Setup guide for new users");
        faqRequest.setTags(tags);

        faqResponse = new FaqResponse();
        faqResponse.setId(1L);
        faqResponse.setQuestion("How to set up Xreal Air?");
        faqResponse.setAnswer("Connect the USB-C cable to your device...");
        faqResponse.setInstruction("Please ensure your device supports DP Alt Mode");
        faqResponse.setUrl("https://xreal.com/setup");
        faqResponse.setActive(true);
        faqResponse.setComment("Setup guide for new users");
        faqResponse.setTimestamp(LocalDateTime.now());
        faqResponse.setTags(tags);
    }

    @Test
    void deleteAll_Success() throws Exception {
        doNothing().when(faqService).deleteAll();

        mockMvc.perform(delete("/faqs/all"))
                .andExpect(status().isNoContent());

        verify(faqService, times(1)).deleteAll();
    }

    @Test
    void createFaq_Success() throws Exception {
        when(faqService.createFaq(any(FaqRequest.class))).thenReturn(faqResponse);

        mockMvc.perform(post("/faqs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(faqRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.question").value("How to set up Xreal Air?"))
                .andExpect(jsonPath("$.answer").value("Connect the USB-C cable to your device..."))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.tags[0]").exists());

        verify(faqService, times(1)).createFaq(any(FaqRequest.class));
    }

    @Test
    void updateFaq_Success() throws Exception {
        Long faqId = 1L;
        when(faqService.updateFaq(eq(faqId), any(FaqRequest.class))).thenReturn(faqResponse);

        mockMvc.perform(put("/faqs/{id}", faqId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(faqRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.question").value("How to set up Xreal Air?"));

        verify(faqService, times(1)).updateFaq(eq(faqId), any(FaqRequest.class));
    }

    @Test
    void deleteFaq_Success() throws Exception {
        Long faqId = 1L;
        doNothing().when(faqService).deleteFaq(faqId);

        mockMvc.perform(delete("/faqs/{id}", faqId))
                .andExpect(status().isNoContent());

        verify(faqService, times(1)).deleteFaq(faqId);
    }

    @Test
    void getFaq_Success() throws Exception {
        Long faqId = 1L;
        when(faqService.getFaq(faqId)).thenReturn(faqResponse);

        mockMvc.perform(get("/faqs/{id}", faqId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.question").value("How to set up Xreal Air?"))
                .andExpect(jsonPath("$.answer").value("Connect the USB-C cable to your device..."))
                .andExpect(jsonPath("$.instruction").value("Please ensure your device supports DP Alt Mode"))
                .andExpect(jsonPath("$.url").value("https://xreal.com/setup"));

        verify(faqService, times(1)).getFaq(faqId);
    }

    @Test
    void getAllFaqs_WithoutFilter() throws Exception {
        List<FaqResponse> faqList = Arrays.asList(faqResponse, createAnotherFaqResponse());
        Page<FaqResponse> faqPage = new PageImpl<>(faqList, PageRequest.of(0, 10), 2);

        when(faqService.getAllFaqs(isNull(), any(Pageable.class))).thenReturn(faqPage);

        mockMvc.perform(get("/faqs")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(faqService, times(1)).getAllFaqs(isNull(), any(Pageable.class));
    }

    @Test
    void getAllFaqs_WithActiveFilter() throws Exception {
        List<FaqResponse> faqList = Arrays.asList(faqResponse);
        Page<FaqResponse> faqPage = new PageImpl<>(faqList, PageRequest.of(0, 10), 1);

        when(faqService.getAllFaqs(eq(true), any(Pageable.class))).thenReturn(faqPage);

        mockMvc.perform(get("/faqs")
                        .param("active", "true")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].active").value(true))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(faqService, times(1)).getAllFaqs(eq(true), any(Pageable.class));
    }

    @Test
    void searchByTags_Success() throws Exception {
        Set<String> searchTags = new HashSet<>(Arrays.asList("Xreal Air", "Setup"));
        List<FaqResponse> faqList = Arrays.asList(faqResponse);
        Page<FaqResponse> faqPage = new PageImpl<>(faqList, PageRequest.of(0, 10), 1);

        when(faqService.searchByTags(eq(searchTags), isNull(), any(Pageable.class))).thenReturn(faqPage);

        mockMvc.perform(get("/faqs/search")
                        .param("tags", "Xreal Air", "Setup")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].tags").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(faqService, times(1)).searchByTags(eq(searchTags), isNull(), any(Pageable.class));
    }

    @Test
    void searchByTags_WithActiveFilter() throws Exception {
        Set<String> searchTags = new HashSet<>(Arrays.asList("Xreal Air"));
        List<FaqResponse> faqList = Arrays.asList(faqResponse);
        Page<FaqResponse> faqPage = new PageImpl<>(faqList, PageRequest.of(0, 10), 1);

        when(faqService.searchByTags(eq(searchTags), eq(true), any(Pageable.class))).thenReturn(faqPage);

        mockMvc.perform(get("/faqs/search")
                        .param("tags", "Xreal Air")
                        .param("active", "true")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].active").value(true))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(faqService, times(1)).searchByTags(eq(searchTags), eq(true), any(Pageable.class));
    }

    @Test
    void getAllFaqs_WithCustomSort() throws Exception {
        List<FaqResponse> faqList = Arrays.asList(faqResponse);
        Page<FaqResponse> faqPage = new PageImpl<>(faqList, PageRequest.of(0, 10), 1);

        when(faqService.getAllFaqs(isNull(), any(Pageable.class))).thenReturn(faqPage);

        mockMvc.perform(get("/faqs")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "timestamp,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));

        verify(faqService, times(1)).getAllFaqs(isNull(), any(Pageable.class));
    }

    private FaqResponse createAnotherFaqResponse() {
        FaqResponse response = new FaqResponse();
        response.setId(2L);
        response.setQuestion("How to connect to iPhone?");
        response.setAnswer("Use the Lightning to USB-C adapter...");
        response.setInstruction("Adapter sold separately");
        response.setUrl("https://xreal.com/iphone");
        response.setActive(false);
        response.setComment("iPhone connectivity guide");
        response.setTimestamp(LocalDateTime.now());
        response.setTags(new HashSet<>(Arrays.asList("iPhone", "Connection")));
        return response;
    }
}