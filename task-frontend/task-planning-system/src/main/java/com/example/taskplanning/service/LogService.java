package com.example.taskplanning.service;

import com.example.taskplanning.entity.ActionLog;
import com.example.taskplanning.repository.ActionLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class LogService {
    private static final Logger logger = LoggerFactory.getLogger(LogService.class);

    private final ActionLogRepository actionLogRepository;

    @Autowired
    public LogService(ActionLogRepository actionLogRepository) {
        this.actionLogRepository = actionLogRepository;
    }

    @Async("logTaskExecutor")
    public void log(ActionLog actionLog) {
        try {
            actionLogRepository.save(actionLog);
            logger.debug("ActionLog saved: {}", actionLog);
        } catch (Exception e) {
            logger.error("Failed to save ActionLog", e);
        }
    }
}
