package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class DataGenerator {

    private static final Logger logger = LoggerFactory.getLogger(DataGenerator.class);

    private final Random random = new Random();

    @Autowired
    private SimpMessagingTemplate template;

    @Scheduled(fixedRate = 3000)
    public void newRecord() {
        DataRecord dataRecord = new DataRecord(System.currentTimeMillis(), 50.0 + random.nextInt(25));
        logger.info("New data {}", dataRecord);
        template.convertAndSend("/data_out/new_data", dataRecord);
    }
}
