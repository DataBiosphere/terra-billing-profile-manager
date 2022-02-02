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

    addOauth2Redirect(
        registry, "oauth2-redirect.html", "/webjars/swagger-ui-dist/4.3.0/oauth2-redirect.html");
  }

  private void addOauth2Redirect(ResourceHandlerRegistry registry, String path, String file) {
    final String pathMapping = String.format("%s*", path);
    if (!registry.hasMappingForPattern(pathMapping)) {
      registry
          .addResourceHandler(pathMapping)
          .addResourceLocations("classpath:/META-INF/resources%s", file);
    }
  }

  private void addJsFixers(
      ResourceHandlerRegistry registry, String jsPath, String jsFile, String find, String replace) {
    String jsPathMapping = String.format("%s*", jsPath);
    if (!registry.hasMappingForPattern(jsPathMapping)) {
      registry
          .addResourceHandler(jsPathMapping)
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
