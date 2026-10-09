package com.smartticket.ai.client;

import com.smartticket.ai.dto.request.ClassifyRequest;
import com.smartticket.ai.dto.request.ReplySuggestRequest;
import com.smartticket.ai.dto.response.ClassifyResponse;
import com.smartticket.ai.dto.response.ReplySuggestResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class PythonAgentClient {

    private final RestClient restClient;

    public PythonAgentClient(@Qualifier("pythonAgentRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * 工单分类。失败直接抛异常，由 Service 层降级。
     */
    public ClassifyResponse classify(ClassifyRequest request) {
        long start = System.currentTimeMillis();
        ClassifyResponse response = restClient.post()
                .uri("/agent/classify")
                .body(request)
                .retrieve()
                .body(ClassifyResponse.class);
        log.debug("classify 调用成功，耗时 {} ms", System.currentTimeMillis() - start);
        return response;
    }

    /**
     * 回复建议。失败直接抛异常，由 Service 层降级。
     */
    public ReplySuggestResponse replySuggest(ReplySuggestRequest request) {
        long start = System.currentTimeMillis();
        ReplySuggestResponse response = restClient.post()
                .uri("/agent/reply-suggest")
                .body(request)
                .retrieve()
                .body(ReplySuggestResponse.class);
        log.debug("replySuggest 调用成功，耗时 {} ms", System.currentTimeMillis() - start);
        return response;
    }
}