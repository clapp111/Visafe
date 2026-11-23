package com.visafe.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 정해진 시각에 도는 작업(계약 종료일 처리)을 켠다. */
@Configuration
@EnableScheduling
public class SchedulingConfig {}
