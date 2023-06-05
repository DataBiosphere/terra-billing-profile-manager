package bio.terra.profile.common;

import bio.terra.profile.app.Main;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

/** Base class for unit tests requiring Spring functionality (autowiring, etc.) */
@ContextConfiguration(classes = Main.class)
@SpringBootTest
public class BaseSpringUnitTest extends BaseUnitTest {}
