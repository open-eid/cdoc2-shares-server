package ee.cyber.cdoc2.server;

import jakarta.validation.ConstraintViolationException;

import java.security.KeyStore;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;

import ee.cyber.cdoc2.server.model.repository.KeyShareNonceRepository;
import ee.cyber.cdoc2.server.model.repository.KeyShareRepository;
import ee.cyber.cdoc2.server.model.repository.SessionNonceRepository;

import static org.junit.jupiter.api.Assertions.*;

// Starts server on https
// Starts PostgreSQL running on docker
@ExtendWith(SpringExtension.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"management.server.port=0"}
)
@ContextConfiguration(initializers = BaseInitializationTest.Initializer.class)
abstract class BaseInitializationTest {

    protected static final KeyStore CLIENT_TRUST_STORE = TestData.loadKeyStore(
        "JKS",
        TestData.getKeysDirectory().resolve("clienttruststore.jks"),
        "passwd"
    );

    private static PostgreSQLContainer<?> postgresContainer;

    @BeforeAll
    public static void startPostgresContainer() {
        //TODO This is a workaround for
        // https://github.com/testcontainers/testcontainers-java/issues/11212
        // proper solution would probably be upgrading to Testcontainers 2
        System.setProperty("api.version", "1.44");

        postgresContainer = new PostgreSQLContainer<>("postgres:14.17") //Jammy 22.04 default version
            .withDatabaseName("integration-tests-db")
            .withUsername("sa")
            .withPassword("sa")
            .withReuse(true); //prevent pulling images repeatedly
        //  Docker Hub imposes limits on anonymous users (100 pulls per 6 hours)
        postgresContainer.start();
    }

    static class Initializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
                "spring.datasource.url=" + postgresContainer.getJdbcUrl(),
                "spring.datasource.username=" + postgresContainer.getUsername(),
                "spring.datasource.password=" + postgresContainer.getPassword()
            ).applyTo(configurableApplicationContext.getEnvironment());
        }
    }

    @Value("https://localhost:${local.server.port}")
    protected String baseUrl;

    @Autowired
    protected KeyShareRepository shareRepository;

    @Autowired
    protected KeyShareNonceRepository shareNonceRepository;

    @Autowired
    protected SessionNonceRepository sessionNonceRepository;

    @Test
    void contextLoads() {
        // tests that server is configured properly (no exceptions means success)
        // In case configuration errors, spring fails run-time during initialization
        assertNotNull(shareRepository);
        assertNotNull(shareNonceRepository);
        assertNotNull(sessionNonceRepository);
        assertTrue(postgresContainer.isRunning());
    }

    void assertThrowsConstraintViolationException(Throwable cause) {
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        assertEquals(ConstraintViolationException.class, cause.getClass());
        assertNotNull(cause.getMessage());
    }

}
