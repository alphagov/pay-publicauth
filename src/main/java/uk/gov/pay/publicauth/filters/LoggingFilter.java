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

    static final String HEADER_REQUEST_ID = "X-Request-Id";
    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public void init(FilterConfig filterConfig) { }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;

        String requestURL = httpRequest.getRequestURI();
        String requestMethod = httpRequest.getMethod();
        String requestIdHeader = httpRequest.getHeader(HEADER_REQUEST_ID);
        String requestId = requestIdHeader == null ? "" : requestIdHeader;

        MDC.put(HEADER_REQUEST_ID, requestId);

        logger.info("[{}] - {} to {} began", requestId, requestMethod, requestURL);
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } catch (Throwable throwable) {
            logger.error("[{}] - Exception - {}", requestId, throwable.getMessage(), throwable);
        } finally {
            stopwatch.stop();
            logger.info("[{}] - {} to {} ended - total time {}ms", requestId, requestMethod, requestURL,
                    stopwatch.elapsed(TimeUnit.MILLISECONDS));
        }
    }

    @Override
    public void destroy() {}
}
