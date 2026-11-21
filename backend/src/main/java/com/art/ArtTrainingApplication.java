package com.art;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication
@MapperScan("com.art.mapper")
@EnableScheduling
/**
 * @author liukang&&heyan
 */
public class ArtTrainingApplication {
    public static void main(String[] args) {
        SpringApplication.run(ArtTrainingApplication.class, args);
    }
}
