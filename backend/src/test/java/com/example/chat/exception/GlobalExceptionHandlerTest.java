package com.example.chat.exception;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.assertj.core.api.Assertions.*;

class GlobalExceptionHandlerTest {
    @Test void neverLogsProviderMessageOrCauseAndAlwaysGeneratesErrorId() {
        var logger=(Logger)LoggerFactory.getLogger(GlobalExceptionHandler.class);
        var appender=new ListAppender<ILoggingEvent>();appender.start();logger.addAppender(appender);
        try {
            var req=new MockHttpServletRequest("POST","/api/v1/classifications");
            var response=new GlobalExceptionHandler().handleRuntime(new IllegalStateException("private-question answer rendered-prompt"),req);
            var body=(com.example.chat.dto.ClassificationResponse)response.getBody();
            assertThat(body.requestId()).isNotBlank();assertThat(body.classification()).isNull();
            assertThat(appender.list).allSatisfy(event->{assertThat(event.getFormattedMessage()).doesNotContain("private-question","rendered-prompt");assertThat(event.getThrowableProxy()).isNull();});
        } finally {logger.detachAppender(appender);appender.stop();}
    }
}
