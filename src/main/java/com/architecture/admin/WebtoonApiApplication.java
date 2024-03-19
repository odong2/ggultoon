package com.architecture.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Locale;
import java.util.TimeZone;

@SpringBootApplication
public class WebtoonApiApplication {
	public static void main(String[] args) {
		// 타임존 셋팅
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
		Locale.setDefault(Locale.KOREA);

		SpringApplication.run(WebtoonApiApplication.class, args);
	}
}
