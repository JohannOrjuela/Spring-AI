package com.example.chat.filter;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.*;
import static org.assertj.core.api.Assertions.*;

class LogRedactionFilterTest {
    @Test void doesNotLogBodiesQueryStringsOrArbitraryPaths() throws Exception {
        var logger=(Logger)LoggerFactory.getLogger(LogRedactionFilter.class);
        var appender=new ListAppender<ILoggingEvent>();appender.start();logger.addAppender(appender);
        var previous=logger.getLevel();logger.setLevel(ch.qos.logback.classic.Level.DEBUG);
        try {
            for(String route:new String[]{"/api/v1/chat","/api/v1/classifications","/private-person@email.test"}) {
                var req=new MockHttpServletRequest("POST",route);req.setContent("private-question private-classification rendered-prompt".getBytes());
                req.setQueryString("token=secret-token");
                new LogRedactionFilter().doFilter(req,new MockHttpServletResponse(),(request,response)->{});
            }
            assertThat(appender.list).hasSize(3);
            assertThat(appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList().toString())
                .contains("/api/v1/chat","/api/v1/classifications").doesNotContain("private-", "rendered-prompt", "secret-token");
        } finally {logger.detachAppender(appender);logger.setLevel(previous);appender.stop();}
    }
}
