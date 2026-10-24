package example.com.ltw;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class   LtwckApplication {

	public static void main(String[] args) {
		SpringApplication.run(LtwckApplication.class, args);
	}

}
