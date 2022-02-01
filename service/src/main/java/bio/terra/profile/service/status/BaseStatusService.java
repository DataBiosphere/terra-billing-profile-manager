package bio.terra.profile.service.status;

import bio.terra.profile.app.configuration.StatusCheckConfiguration;
import bio.terra.profile.generated.model.ApiSystemStatus;
import bio.terra.profile.generated.model.ApiSystemStatusSystems;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseStatusService {
  private static final Logger logger = LoggerFactory.getLogger(BaseStatusService.class);
  /** cached status */
  private final AtomicReference<ApiSystemStatus> cachedStatus;
  /** configuration parameters */
  private final StatusCheckConfiguration configuration;
  /** set of status methods to check */
  private final ConcurrentHashMap<String, Supplier<ApiSystemStatusSystems>> statusCheckMap;
  /** scheduler */
  private final ScheduledExecutorService scheduler;
  /** last time cache was updated */
  private final AtomicReference<Instant> lastStatusUpdate;

  public BaseStatusService(StatusCheckConfiguration configuration) {
    this.configuration = configuration;
    this.statusCheckMap = new ConcurrentHashMap<>();
    this.cachedStatus = new AtomicReference<>(new ApiSystemStatus().ok(false));
    this.lastStatusUpdate = new AtomicReference<>(Instant.now());
    this.scheduler = Executors.newScheduledThreadPool(1);
  }

  @PostConstruct
  public void startStatusChecking() {
    if (configuration.enabled()) {
      scheduler.scheduleAtFixedRate(
          this::checkStatus,
          configuration.startupWaitSeconds(),
          configuration.pollingIntervalSeconds(),
          TimeUnit.SECONDS);
    }
  }

  public void registerStatusCheck(String name, Supplier<ApiSystemStatusSystems> checkFn) {
    statusCheckMap.put(name, checkFn);
  }

  public void checkStatus() {
    if (configuration.enabled()) {
      var newStatus = new ApiSystemStatus();
      try {
        var systems =
            statusCheckMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
        newStatus.setOk(systems.values().stream().allMatch(ApiSystemStatusSystems::isOk));
        newStatus.setSystems(systems);
      } catch (Exception e) {
        logger.warn("Status check exception", e);
        newStatus.setOk(false);
      }
      cachedStatus.set(newStatus);
      lastStatusUpdate.set(Instant.now());
    }
  }

  public ApiSystemStatus getCurrentStatus() {
    if (configuration.enabled()) {
      // If staleness time (last update + stale threshold) is before the current time, then
      // we are officially not OK.
      if (lastStatusUpdate
          .get()
          .plusSeconds(configuration.stalenessThresholdSeconds())
          .isBefore(Instant.now())) {
        logger.warn("Status has not been updated since {}", lastStatusUpdate);
        cachedStatus.set(new ApiSystemStatus().ok(false));
      }
      return cachedStatus.get();
    }
    return new ApiSystemStatus().ok(true);
  }
}
