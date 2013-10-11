package net.exacode.logback.ext.recording;

import static org.fest.assertions.Assertions.assertThat;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.read.ListAppender;

/**
 * @author Tomasz Nurkiewicz
 * @author mendlik
 */
public class RecordingAppenderTest {

	static String RECORDING_CONF_FOLDER_PREFIX = "joran/recording/";

	@Rule
	public TestName testName = new TestName();

	private final LoggerContext lc = new LoggerContext();
	private final Logger log = lc.getLogger(RecordingAppenderTest.class);
	private final JoranConfigurator jc = new JoranConfigurator();

	@Before
	public void setup() throws JoranException {
		jc.setContext(lc);
	}

	@Test
	public void shouldNotLogAnythingWhenNoLogsCreated() throws Exception {
		// given
		configureFrom("all-params.xml");

		// when
		// nothing

		// then
		assertThat(logMsgs()).isEmpty();
	}

	@Test
	public void shouldNotLogAnythingWhenSingleLogBelowError() throws Exception {
		// given
		configureFrom("all-params.xml");

		// when
		log.warn("Test");

		// then
		assertThat(logMsgs()).isEmpty();
	}

	@Test
	public void shouldNotLogAnythingWhenMultipleLogsBelowError()
			throws Exception {
		// given
		configureFrom("all-params.xml");

		// when
		log.debug("Test 1");
		log.info("Test 2");
		log.trace("Test 3");
		log.warn("Test 4");

		// then
		assertThat(logMsgs()).isEmpty();
	}

	@Test
	public void shouldLogOnlyErrorLogWhenNoPreviousLogs() throws Exception {
		// given
		configureFrom("all-params.xml");

		// when
		log.error("Test");

		// then
		assertThat(logMsgs()).containsExactly("Test");
	}

	@Test
	public void shouldLogLastFewDebugLogsBeforeError() throws Exception {
		// given
		configureFrom("all-params.xml");

		// when
		log.debug("Test 1");
		log.debug("Test 2");
		log.warn("Test 3");
		log.info("Test 4");
		log.error("Test 5");

		// then
		assertThat(logMsgs()).containsExactly("Test 2", "Test 3", "Test 4",
				"Test 5");
	}

	@Test
	public void shouldLimitOutputToLastThreeDetailedLogs() throws Exception {
		// given
		configureFrom("all-params.xml");

		// when
		log.debug("Test 1");
		log.warn("Test 2");
		log.info("Test 3");
		log.debug("Test 4");
		log.warn("Test 5");
		log.error("Test 6");

		// then
		assertThat(logMsgs()).containsExactly("Test 3", "Test 4", "Test 5",
				"Test 6");
	}

	@Test
	public void shouldLimitOutputToRecentDetailedMessages() throws Exception {
		// given
		configureFrom("all-params.xml");

		// when
		log.debug("Test 1");
		log.info("Test 2");
		TimeUnit.MILLISECONDS.sleep(150);
		log.debug("Test 3");
		log.info("Test 4");
		log.error("Test 5");

		// then
		assertThat(logMsgs()).containsExactly("Test 3", "Test 4", "Test 5");
	}

	@Test
	public void shouldCleanTheHistoryAfterDumping() throws Exception {
		// given
		configureFrom("all-params.xml");

		// when
		log.debug("Test 1");
		log.info("Test 2");
		log.error("Test 3");

		log.error("Test 4");

		// then
		assertThat(logMsgs()).containsExactly("Test 1", "Test 2", "Test 3",
				"Test 4");
	}

	@Test
	public void shouldRecordEventsAfterDumpProperly() throws Exception {
		// given
		configureFrom("all-params.xml");

		// when
		log.info("Test 1");
		log.error("Test 2");

		log.info("Test 3");
		log.error("Test 4");

		// then
		assertThat(logMsgs()).containsExactly("Test 1", "Test 2", "Test 3",
				"Test 4");
	}

	@Test
	public void shouldLimitRecordedEventsAfterFirstDumpBothAccordingToExpiry()
			throws Exception {
		// given
		configureFrom("all-params.xml");

		// when
		log.info("Test 1");
		log.error("Test 2");

		log.info("Test 3");
		TimeUnit.MILLISECONDS.sleep(150);
		log.info("Test 4");
		log.info("Test 5");
		log.error("Test 6");

		// then
		assertThat(logMsgs()).containsExactly("Test 1", "Test 2", "Test 4",
				"Test 5", "Test 6");
	}

	@Test
	public void shouldLimitRecordedEventsAfterFirstDumpBothAccordingToMaxSize()
			throws Exception {
		// given
		configureFrom("all-params.xml");

		// when
		log.info("Test 1");
		log.error("Test 2");

		log.info("Test 3");
		log.info("Test 4");
		log.info("Test 5");
		log.info("Test 6");
		log.info("Test 7");
		log.error("Test 8");

		// then
		assertThat(logMsgs()).containsExactly("Test 1", "Test 2", "Test 5",
				"Test 6", "Test 7", "Test 8");
	}

	@Test
	public void shouldAlsoDumpOnWarningIfConfiguredSo() throws Exception {
		// given
		configureFrom("warn.xml");

		// when
		log.warn("Test");

		// then
		assertThat(logMsgs()).containsExactly("Test");
	}

	@Test
	public void shouldAlsoDumpOnWarningIncludingHistory() throws Exception {
		// given
		configureFrom("warn.xml");

		// when
		log.info("Test 1");
		log.warn("Test 2");

		// then
		assertThat(logMsgs()).containsExactly("Test 1", "Test 2");
	}

	@Test
	public void shouldWorkOnDefaultsSmokeTest() throws Exception {
		// given
		configureFrom("defaults.xml");

		// when
		log.info("Test 1");
		log.warn("Test 2");
		log.error("Test 3");

		// then
		assertThat(logMsgs()).containsExactly("Test 1", "Test 2", "Test 3");
	}

	@Test
	public void shouldDumpOnlyRecentLogsFromTriggeringThread() throws Exception {
		// given
		configureFrom("defaults.xml");
		final ExecutorService executorService = Executors
				.newFixedThreadPool(10);

		// when
		log.debug("Test 1");
		logFewStatementsInDifferentThreads(executorService, 100);
		log.error("Test 3");

		// then
		assertThat(logMsgs()).containsExactly("Test 1", "Test 3");
	}

	private void logFewStatementsInDifferentThreads(
			ExecutorService executorService, final int count)
			throws InterruptedException {
		for (int i = 0; i < count; ++i)
			executorService.submit(new Runnable() {
				@Override
				public void run() {
					log.info("Test 2");
				}
			});
		executorService.shutdown();
		executorService.awaitTermination(5, TimeUnit.SECONDS);
	}

	private void configureFrom(final String configFile) throws JoranException {
		InputStream inputStream = this.getClass().getClassLoader()
				.getResourceAsStream(RECORDING_CONF_FOLDER_PREFIX + configFile);
		jc.doConfigure(inputStream);
	}

	private List<ILoggingEvent> logEvents() {
		return ((ListAppender<ILoggingEvent>) lc.getLogger("LIST_LOG")
				.getAppender("LIST")).list;
	}

	private List<String> logMsgs() {
		final List<ILoggingEvent> events = logEvents();
		final ArrayList<String> msgs = new ArrayList<String>(events.size());
		for (ILoggingEvent event : events) {
			msgs.add(event.getMessage());
		}
		return msgs;
	}

	@Test
	public void shouldDumpLogMessagesComingFromDifferentMethodsAndClasses()
			throws Exception {
		// given
		configureFrom("all-params.xml");

		// when
		someMethod("Testing");
		new RecordingAppenderHelper(log).otherMethod("Verifying");
		log.error("Oh, no!");

		// then
		assertThat(logMsgs())
				.containsExactly("Testing", "Verifying", "Oh, no!");

	}

	@Test
	public void shouldDumpCorrectCallerDataOfLogsComingFromDifferentMethodsAndClasses()
			throws Exception {
		configureFrom("callerdata.xml");

		// when
		someMethod("Testing");
		new RecordingAppenderHelper(log).otherMethod("Verifying");
		log.error("Oh, no!");

		// then
		final List<ILoggingEvent> events = logEvents();
		assertThat(events).hasSize(3);
		assertCallerData(events.get(0), this.getClass().getCanonicalName(),
				this.getClass().getSimpleName() + ".java", "someMethod");
		assertCallerData(events.get(1),
				RecordingAppenderHelper.class.getCanonicalName(),
				RecordingAppenderHelper.class.getSimpleName() + ".java",
				"otherMethod");
		assertCallerData(events.get(2), this.getClass().getCanonicalName(),
				this.getClass().getSimpleName() + ".java",
				testName.getMethodName());
	}

	@Test
	public void shouldDumpUnknownCallerDataWhenNotExplicitlyEnabled()
			throws Exception {
		configureFrom("no-callerdata.xml");

		// when
		someMethod("Testing");
		new RecordingAppenderHelper(log).otherMethod("Verifying");
		log.error("Oh, no!");

		// then
		final List<ILoggingEvent> events = logEvents();
		assertThat(events).hasSize(3);
		assertThat(events.get(0).getCallerData()).isNull();
		assertThat(events.get(1).getCallerData()).isNull();
		assertThat(events.get(2).getCallerData()).isNull();
	}

	private void assertCallerData(final ILoggingEvent event,
			final String expectedClassName, final String expectedFileName,
			final String expectedMethodName) {
		final StackTraceElement[] callerData = event.getCallerData();
		assertThat(callerData).isNotNull();
		assertThat(callerData.length).isGreaterThan(0);

		final StackTraceElement ste = callerData[0];
		assertThat(ste.getClassName()).isEqualTo(expectedClassName);
		assertThat(ste.getFileName()).isEqualTo(expectedFileName);
		assertThat(ste.getMethodName()).isEqualTo(expectedMethodName);
	}

	private void someMethod(String msg) {
		log.debug(msg);
	}

}
