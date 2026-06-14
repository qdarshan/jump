package arc.astra.jump;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class JumpApplication {

    static void main(String[] args) {
        SpringApplication.run(JumpApplication.class, args);
    }

}
