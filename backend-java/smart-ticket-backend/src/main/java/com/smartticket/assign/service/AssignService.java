package com.smartticket.assign.service;

public interface AssignService {
    // 自动派单，返回分配到的客服 id
    Long autoAssign(Long ticketId);

    // 手动改派
    void manualAssign(Long ticketId, Long assigneeId, String remark);
}
