package bio.terra.profile.common;

import bio.terra.profile.app.Main;
import org.springframework.boot.test.context.SpringBootTest;

/** Base class for unit tests requiring Spring functionality (autowiring, etc.) */
@SpringBootTest(classes = Main.class)
public class BaseSpringUnitTest extends BaseUnitTest {}
