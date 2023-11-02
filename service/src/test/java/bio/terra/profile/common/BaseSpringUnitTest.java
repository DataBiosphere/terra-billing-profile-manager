package bio.terra.profile.common;

import bio.terra.profile.app.Main;
import org.springframework.boot.test.context.SpringBootTest;

/** Base class for unit tests requiring Spring functionality (autowiring, etc.) */
@SpringBootTest(
    classes = Main.class,
    // Disable instrumentation for spring-webmvc because pact still uses javax libs which causes
    // opentelemetry to try to load the same bean name twice, once for javax and once for jakarta
    properties = "otel.instrumentation.spring-webmvc.enabled=false")
public class BaseSpringUnitTest extends BaseUnitTest {}
