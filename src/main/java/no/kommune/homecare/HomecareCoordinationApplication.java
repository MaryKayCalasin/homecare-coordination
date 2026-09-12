package no.kommune.homecare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HomecareCoordinationApplication {

    public static void main(String[] args) {
        SpringApplication.run(HomecareCoordinationApplication.class, args);
    }
}
