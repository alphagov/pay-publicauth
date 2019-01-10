package uk.gov.pay.publicauth.filters;

import com.google.common.base.Stopwatch;
import org.jboss.logging.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

public class LoggingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public void init(FilterConfig filterConfig) { }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;

        String requestURL = httpRequest.getRequestURI();
        String requestMethod = httpRequest.getMethod();
        String requestIdHeader = httpRequest.getHeader("X-Request-Id");

        // The key passed to MDC here should match the value in our logging configuration
        MDC.put("X-Request-Id", requestIdHeader == null ? "(null)" : requestIdHeader);

        logger.info("{} to {} began", requestMethod, requestURL);
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } catch (Throwable throwable) {
            logger.error("Exception - {}", throwable.getMessage(), throwable);
        } finally {
            stopwatch.stop();
            logger.info("{} to {} ended - total time {}ms", requestMethod, requestURL,
                    stopwatch.elapsed(TimeUnit.MILLISECONDS));
        }
    }

    @Override
    public void destroy() {}
}
