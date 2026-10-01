package com.example.chat.impl;

import com.example.chat.dto.*;
import com.example.chat.exception.ModelOperationException;
import com.example.chat.prompt.PromptTemplateRegistry;
import com.example.chat.memory.ConversationMemoryService;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.*;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class StructuredModelServiceTest {
    private ValidatorFactory factory;
    private ChatModel model;
    private SpringAiModelService service;
    @BeforeEach void setup() {
        factory=Validation.buildDefaultValidatorFactory();model=mock(ChatModel.class);
        when(model.getOptions()).thenReturn(OllamaChatOptions.builder().build());
        var memory=MessageWindowChatMemory.builder().chatMemoryRepository(new InMemoryChatMemoryRepository())
            .maxMessages(ConversationMemoryService.MAX_NON_SYSTEM_MESSAGES).build();
        service=new SpringAiModelService(ChatClient.builder(model),new PromptTemplateRegistry(),factory.getValidator(),
            new ConversationMemoryService(memory));
    }
    @AfterEach void close(){factory.close();}
    private org.springframework.ai.chat.model.ChatResponse response(String text) {
        return org.springframework.ai.chat.model.ChatResponse.builder().generations(List.of(new Generation(new AssistantMessage(text),ChatGenerationMetadata.builder().finishReason("stop").build())))
            .metadata(ChatResponseMetadata.builder().model("gemma3:4b").usage(new DefaultUsage(10,5,16)).build()).build();
    }
    @Test void realClientRendersSelectedTemplateAndPreservesLiteralUserContent() {
        when(model.call(any(Prompt.class))).thenReturn(response("answer"));
        for(String id:new String[]{"conciso","tutor","extractor"}) {
            var req=new ChatRequest();req.setQuestion("{x} <rol>");req.setTemplateId(id);req.setRol("custom-role");req.setTemperature(0.0);req.setTopK(0);
            assertThat(service.generateResponse(req).totalTokens()).isEqualTo(16);
        }
        var captor=org.mockito.ArgumentCaptor.forClass(Prompt.class);verify(model,times(3)).call(captor.capture());
        for(var prompt:captor.getAllValues()) {
            assertThat(prompt.getSystemMessage().getText()).contains("custom-role", "general");
            assertThat(prompt.getUserMessage().getText()).contains("{x} <rol>");
            assertThat(prompt.getOptions().getTemperature()).isZero();
        }
    }
    @Test void realClientProducesValidatedClassificationAndFinalMetadata() {
        when(model.call(any(Prompt.class))).thenReturn(response("{\"categoria\":\"soporte\",\"confianza\":90,\"justificacion\":\"motivo\"}"));
        var request=new ClassificationRequest();request.setText("private text");request.setTopK(0);request.setTemperature(0.0);
        var result=service.classify(request);
        assertThat(result.clasificacion().confianza()).isEqualTo(90);assertThat(result.totalTokens()).isEqualTo(16);
        assertThat(result.promptTokens()).isEqualTo(10);assertThat(result.completionTokens()).isEqualTo(5);
        var capture=org.mockito.ArgumentCaptor.forClass(Prompt.class);verify(model).call(capture.capture());
        assertThat(capture.getValue().getSystemMessage().getText()).contains("clasificador");
        assertThat(capture.getValue().getUserMessage().getText()).contains("private text");
        assertThat(capture.getValue().getOptions().getTemperature()).isZero();
        assertThat(((org.springframework.ai.model.tool.StructuredOutputChatOptions)capture.getValue().getOptions()).getOutputSchema())
            .contains("categoria", "confianza", "justificacion");
    }
    @Test void rejectsMalformedMissingNullBlankAndOutOfRangeStructuredOutput() {
        for(String json:new String[]{"not json", "{}", "{\"categoria\":\"cat\",\"confianza\":null,\"justificacion\":\"r\"}",
            "{\"categoria\":\"cat\",\"confianza\":101,\"justificacion\":\"r\"}",
            "{\"categoria\":\" \",\"confianza\":50,\"justificacion\":\"r\"}",
            "{\"categoria\":\"cat\",\"confianza\":1.5,\"justificacion\":\"r\"}"}) {
            when(model.call(any(Prompt.class))).thenReturn(response(json));
            var req=new ClassificationRequest();req.setText("x");
            assertThatThrownBy(()->service.classify(req), "Invalid output must fail: %s", json)
                .isInstanceOf(ModelOperationException.class).hasMessage("Model operation failed");
        }
    }
}
