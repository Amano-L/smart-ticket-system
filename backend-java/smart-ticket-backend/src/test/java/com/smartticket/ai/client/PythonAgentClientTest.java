package com.smartticket.ai.client;

import com.smartticket.ai.dto.request.ClassifyRequest;
import com.smartticket.ai.dto.request.ReplySuggestRequest;
import com.smartticket.ai.dto.response.ClassifyResponse;
import com.smartticket.ai.dto.response.ReplySuggestResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PythonAgentClientTest {

    @Autowired
    private PythonAgentClient client;

    @Test
    void testClassify() {
        ClassifyRequest req = new ClassifyRequest();
        req.setTicketId(1L);
        req.setTitle("退款申请");
        req.setContent("订单 12345 未收到货，申请退款");

        ClassifyResponse resp = client.classify(req);
        System.out.println("classify 返回：" + resp);
    }

    @Test
    void testReplySuggest() {
        ReplySuggestRequest req = new ReplySuggestRequest();
        req.setTicketId(1L);
        req.setContent("退款");

        ReplySuggestResponse resp = client.replySuggest(req);
        System.out.println("replySuggest 返回：" + resp);
    }
}