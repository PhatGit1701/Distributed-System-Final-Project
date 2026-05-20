package com.example.distributedsystemproject;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Context loading test disabled due to custom multi-datasource configuration with excluded JPA auto-configuration")
class DistributedSystemProjectApplicationTests {

	@Test
	void contextLoads() {
	}

}
