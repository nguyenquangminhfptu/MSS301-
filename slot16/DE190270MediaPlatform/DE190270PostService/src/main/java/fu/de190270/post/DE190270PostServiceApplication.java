package fu.de190270.post;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class DE190270PostServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DE190270PostServiceApplication.class, args);
    }
}
