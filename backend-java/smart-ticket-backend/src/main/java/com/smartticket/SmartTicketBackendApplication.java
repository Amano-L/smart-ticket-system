package com.smartticket;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({
        "com.smartticket.user.mapper",
        "com.smartticket.ticket.mapper",
        "com.smartticket.assign.mapper",
        "com.smartticket.audit.mapper",
        "com.smartticket.ai.mapper"
})public class SmartTicketBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartTicketBackendApplication.class, args);
    }

}
