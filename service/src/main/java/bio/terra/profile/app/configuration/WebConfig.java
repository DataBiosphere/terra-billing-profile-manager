package bio.terra.profile.app.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.TransformedResource;

@Component
public class WebConfig implements WebMvcConfigurer {
  private Map<String, String> cachedJs = new ConcurrentHashMap<>();

  @Autowired
  public WebConfig() {}

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // Make swagger UI respect the x-tokenName security scheme extension
    addJsFixers(
        registry,
        "/webjars/swagger-ui-dist/swagger-ui-bundle.js",
        "/webjars/swagger-ui-dist/4.3.0/swagger-ui-bundle.js",
        "\"response_type=token\"",
        "(t.name === \"b2c\" ? \"response_type=id_token&nonce=defaultNonce&prompt=login\" : \"response_type=token\")");
  }

  private void addJsFixers(
      ResourceHandlerRegistry registry,
      String jsPathMapping,
      String jsFile,
      String find,
      String replace) {
    String springUiJsPath = String.format("%s*", jsPathMapping);
    if (!registry.hasMappingForPattern(springUiJsPath)) {
      registry
          .addResourceHandler(springUiJsPath)
          .setCachePeriod(3600)
          .addResourceLocations(String.format("classpath:/META-INF/resources%s", jsFile))
          .resourceChain(true)
          .addTransformer(
              (request, resource, transformerChain) -> {
                String newJs =
                    cachedJs.computeIfAbsent(
                        jsFile,
                        key -> {
                          try (InputStream stream = resource.getInputStream()) {
                            return new String(stream.readAllBytes(), StandardCharsets.UTF_8)
                                .replace(find, replace);
                          } catch (IOException e) {
                            throw new RuntimeException(e);
                          }
                        });
                return new TransformedResource(resource, newJs.getBytes(StandardCharsets.UTF_8));
              });
    }
  }
}
