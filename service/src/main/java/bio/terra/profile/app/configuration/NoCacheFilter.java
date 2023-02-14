package bio.terra.profile.app.configuration;

import java.io.IOException;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletResponse;

@WebFilter(urlPatterns = {"/api/*"})
public class NoCacheFilter implements Filter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    var servletResponse = (HttpServletResponse) response;
    servletResponse.setHeader("Cache-Control", "no-store");
    chain.doFilter(request, response);
  }
}
