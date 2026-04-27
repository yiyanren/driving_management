package com.example.demo.service;

import com.example.demo.model.OperationLog;
import com.example.demo.repository.OperationLogRepository;
import org.springframework.stereotype.Service;

@Service
public class OperationLogService {
    private final OperationLogRepository operationLogRepository;

    public OperationLogService(OperationLogRepository operationLogRepository) {
        this.operationLogRepository = operationLogRepository;
    }

    public void save(String module, String action, String operator, String result, String detail) {
        OperationLog log = new OperationLog();
        log.setModuleName(module);
        log.setActionName(action);
        log.setOperatorName(operator);
        log.setResult(result);
        log.setDetail(detail);
        operationLogRepository.save(log);
    }
}
