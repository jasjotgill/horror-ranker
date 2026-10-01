package io.github.jasjotgill.horror_ranker;

import org.springframework.boot.SpringApplication;

public class TestHorrorRankerApplication {

	public static void main(String[] args) {
		SpringApplication.from(HorrorRankerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
