package bio.terra.profile.common;

import bio.terra.profile.app.Main;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/** Base class for unit tests requiring Spring functionality (autowiring, etc.) */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = Main.class)
@SpringBootTest
@ActiveProfiles({"human-readable-logging"})
public class BaseSpringUnitTest extends BaseUnitTest {}
