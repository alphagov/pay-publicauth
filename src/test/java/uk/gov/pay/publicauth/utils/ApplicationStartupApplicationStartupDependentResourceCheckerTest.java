package uk.gov.pay.publicauth.utils;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;
import uk.gov.pay.publicauth.util.ApplicationStartupDependentResource;
import uk.gov.pay.publicauth.util.ApplicationStartupDependentResourceChecker;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationStartupApplicationStartupDependentResourceCheckerTest {

    @InjectMocks
    ApplicationStartupDependentResourceChecker applicationStartupDependentResourceChecker;

    @Mock
    ApplicationStartupDependentResource mockApplicationStartupDependentResource;
    @Mock
    Consumer<Duration> mockWaiter;

    private Appender<ILoggingEvent> mockAppender;

    @Captor
    ArgumentCaptor<LoggingEvent> loggingEventArgumentCaptor;

    @Before
    public void setup() {
        Logger root = (Logger) LoggerFactory.getLogger(ApplicationStartupDependentResourceChecker.class);
        mockAppender = mockAppender();
        root.addAppender(mockAppender);
    }

    @Test
    public void start_ShouldWaitAndLogUntilDatabaseIsAccessible() throws Exception {

        Connection mockConnection = mock(Connection.class);
        when(mockApplicationStartupDependentResource.getDatabaseConnection())
                .thenThrow(new SQLException("not there yet"))
                .thenReturn(mockConnection);

        applicationStartupDependentResourceChecker.checkAndWaitForResources();

        verify(mockApplicationStartupDependentResource, times(2)).getDatabaseConnection();
        verify(mockWaiter).accept(Duration.ofSeconds(5));

        verify(mockAppender, times(3)).doAppend(loggingEventArgumentCaptor.capture());
        List<LoggingEvent> allValues = loggingEventArgumentCaptor.getAllValues();

        assertThat(allValues.get(0).getFormattedMessage(), is("Unable to connect to database: not there yet"));
        assertThat(allValues.get(1).getFormattedMessage(), is("Waiting for 5 seconds till the database is available ..."));
        assertThat(allValues.get(2).getFormattedMessage(), is("Database available."));
    }

    @Test
    public void start_ShouldProgressivelyIncrementSleepingTimeBetweenChecksForDBAccessibility() throws Exception {
        Connection mockConnection = mock(Connection.class);
        when(mockApplicationStartupDependentResource.getDatabaseConnection())
                .thenThrow(new SQLException("not there"))
                .thenThrow(new SQLException("not there yet"))
                .thenThrow(new SQLException("still not there"))
                .thenReturn(mockConnection);

        applicationStartupDependentResourceChecker.checkAndWaitForResources();

        verify(mockApplicationStartupDependentResource, times(4)).getDatabaseConnection();
        verify(mockWaiter).accept(Duration.ofSeconds(5));
        verify(mockWaiter).accept(Duration.ofSeconds(10));
        verify(mockWaiter).accept(Duration.ofSeconds(15));
        verify(mockAppender, times(7)).doAppend(loggingEventArgumentCaptor.capture());

        List<LoggingEvent> logStatement = loggingEventArgumentCaptor.getAllValues();
        assertThat(logStatement.get(0).getFormattedMessage(), is("Unable to connect to database: not there"));
        assertThat(logStatement.get(1).getFormattedMessage(), is("Waiting for 5 seconds till the database is available ..."));
        assertThat(logStatement.get(2).getFormattedMessage(), is("Unable to connect to database: not there yet"));
        assertThat(logStatement.get(3).getFormattedMessage(), is("Waiting for 10 seconds till the database is available ..."));
        assertThat(logStatement.get(4).getFormattedMessage(), is("Unable to connect to database: still not there"));
        assertThat(logStatement.get(5).getFormattedMessage(), is("Waiting for 15 seconds till the database is available ..."));
        assertThat(logStatement.get(6).getFormattedMessage(), is("Database available."));
    }

    @Test
    public void start_ShouldCloseAnyAcquiredConnectionWhenTheCheckIsDone() throws Exception {
        Connection mockConnection = mock(Connection.class);
        when(mockApplicationStartupDependentResource.getDatabaseConnection()).thenReturn(mockConnection);

        applicationStartupDependentResourceChecker.checkAndWaitForResources();

        verify(mockConnection).close();
    }

    @SuppressWarnings("unchecked")
    private <T> Appender<T> mockAppender() {
        return mock(Appender.class);
    }
}
