package com.smartticket.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void testSuccess() {
        Result<String> result = Result.success("hello");
        assertEquals(0, result.getCode());
        assertEquals("success", result.getMessage());
        assertEquals("hello", result.getData());
    }

    @Test
    void testFailWithErrorCode() {
        Result<Void> result = Result.fail(ErrorCode.TICKET_NOT_FOUND);
        assertEquals(2001, result.getCode());
        assertEquals("工单不存在", result.getMessage());
    }

    @Test
    void testFailWithCustomMessage() {
        Result<Void> result = Result.fail(400, "标题不能为空");
        assertEquals(400, result.getCode());
        assertEquals("标题不能为空", result.getMessage());
    }
}