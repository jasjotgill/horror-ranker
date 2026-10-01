package io.github.jasjotgill.horror_ranker;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	// The same image as docker-compose.yml, so tests run against what runs for real.
	// Testcontainers only accepts images it knows speak the Postgres protocol, hence the substitute.
	private static final DockerImageName POSTGIS = DockerImageName.parse("postgis/postgis:16-3.4")
		.asCompatibleSubstituteFor("postgres");

	// @ServiceConnection points the datasource at this container, overriding application.yaml.
	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(POSTGIS);
	}

}
