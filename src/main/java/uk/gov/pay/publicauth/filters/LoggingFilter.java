package uk.gov.pay.publicauth.filters;

import com.google.common.base.Stopwatch;
import org.apache.commons.lang3.StringUtils;
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

import static java.lang.String.format;

public class LoggingFilter implements Filter {

    static final String HEADER_REQUEST_ID = "X-Request-Id";
    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public void init(FilterConfig filterConfig) { }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) {
        Stopwatch stopwatch = Stopwatch.createStarted();

        String requestURL = ((HttpServletRequest) servletRequest).getRequestURI();
        String requestMethod = ((HttpServletRequest) servletRequest).getMethod();
        String requestId = StringUtils.defaultString(((HttpServletRequest) servletRequest).getHeader(HEADER_REQUEST_ID));

        MDC.put(HEADER_REQUEST_ID, requestId);

        logger.info(format("[%s] - %s to %s began", requestId, requestMethod, requestURL));
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } catch (Throwable throwable) {
            logger.error("Exception - publicauth request - " + requestURL + " - exception - " + throwable.getMessage(), throwable);
        } finally {
            logger.info(format("[%s] - %s to %s ended - total time %dms", requestId, requestMethod, requestURL,
                    stopwatch.elapsed(TimeUnit.MILLISECONDS)));
            stopwatch.stop();
        }
    }

    @Override
    public void destroy() {}
}
