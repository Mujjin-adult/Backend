package com.incheon.notice.controller;

import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TestController {

    @GetMapping("/hello")
    public Map<String, String> getHelloMessage() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "안녕하세요! 스프링 부트에서 리액트 네이티브로 성공적으로 연결되었습니다!");
        response.put("status", "success");
        return response;
    }
}