package bio.terra.profile.app.configuration;

import java.util.List;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfiguration {
  public static final String AZURE_SPEND_REPORT_CACHE_NAME = "azureSpendReport";

  @Bean
  public CacheManagerCustomizer<ConcurrentMapCacheManager> cacheManagerCustomizer() {
    return new CacheManagerCustomizer<ConcurrentMapCacheManager>() {
      @Override
      public void customize(ConcurrentMapCacheManager cacheManager) {
        cacheManager.setCacheNames(List.of(AZURE_SPEND_REPORT_CACHE_NAME));
      }
    };
  }
}
