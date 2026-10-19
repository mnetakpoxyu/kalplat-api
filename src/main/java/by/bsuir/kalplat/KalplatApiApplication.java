package by.bsuir.kalplat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Точка входа серверной части программного средства «КалПлат». */
@SpringBootApplication
public class KalplatApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(KalplatApiApplication.class, args);
    }
}
