package bio.terra.profile.common;

import org.junit.jupiter.api.Tag;
import org.springframework.test.context.ActiveProfiles;

@Tag("unit")
@ActiveProfiles({"test", "unit"})
public class BaseUnitTest {}
