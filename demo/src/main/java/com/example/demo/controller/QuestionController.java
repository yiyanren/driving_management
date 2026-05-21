package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    @Value("${app.juhe.key:}")
    private String juheKey;
    
    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping
    @PreAuthorize("hasAnyRole('学员', '管理员', '教练')")
    public ApiResponse<Object> getQuestions(@RequestParam int subject, 
                                            @RequestParam(defaultValue = "c1") String model, 
                                            @RequestParam(defaultValue = "rand") String testType) {
        if (juheKey == null || juheKey.trim().isEmpty()) {
            return ApiResponse.fail("请先在 application.properties 中配置 app.juhe.key");
        }
        
        String url = "https://v.juhe.cn/jztk/query?key=" + juheKey + "&subject=" + subject + "&model=" + model + "&testType=" + testType;
        
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map body = response.getBody();
            if (body != null && Integer.valueOf(0).equals(body.get("error_code"))) {
                return ApiResponse.ok(body.get("result"));
            }
            return ApiResponse.fail(body != null ? (String) body.get("reason") : "获取题库失败");
        } catch (Exception e) {
            return ApiResponse.fail("请求聚合API失败: " + e.getMessage());
        }
    }
}
