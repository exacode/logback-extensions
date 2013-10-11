package net.exacode.logback.classic.mongodb;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.MapAssert.entry;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import net.exacode.logback.classic.category.MongoDbTests;
import net.exacode.logback.classic.mongodb.MongoDbAppender;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestName;
import org.slf4j.MDC;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.joran.spi.JoranException;

import com.mongodb.MongoException;

/**
 * Requires MongoDB running on the same computer with default port (27017) and
 * default database logs. MongoDB port and host is parameterized in pom.xml.
 * 
 * @author Tomasz Nurkiewicz
 * @author mendlik
 */
@Category(MongoDbTests.class)
public class MongoDbAppenderTest {

	private static final String MONGODB_CONF_FOLDER_PREFIX = "joran/mongodb/";
	private static final String MONGO_APPENDER = "MONGODB";

	@Rule
	public TestName testName = new TestName();
	private final LoggerContext lc = new LoggerContext();
	private final Logger root = lc.getLogger(Logger.ROOT_LOGGER_NAME);
	private final Logger log = lc.getLogger(this.getClass().getName());

	@After
	public void tearDown() {
		MDC.clear();
		MongoDbAppender appender = mongoAppender();
		if (appender != null && appender.logDao != null) {
			try {
				appender.logDao.clear();
			} catch (MongoException me) {
				// deliberately empty
				// exception occurs when configured wrong connection information
			}
			appender.stop();
		}
		lc.stop();
	}

	private void configure(String file) throws JoranException {
		JoranConfigurator jc = new JoranConfigurator();
		jc.setContext(lc);
		InputStream inputStream = this.getClass().getClassLoader()
				.getResourceAsStream(MONGODB_CONF_FOLDER_PREFIX + file);
		jc.doConfigure(inputStream);
	}

	@Test
	public void shouldNotStartAppenderWhenMongoServerNotFound()
			throws Exception {
		// given

		// when
		configure("server-failure.xml");

		// then
		assertThat(mongoAppender().isStarted()).isFalse();
	}

	private MongoDbAppender mongoAppender() {
		return (MongoDbAppender) root.getAppender(MONGO_APPENDER);
	}

	@Test
	public void shouldFailWhenLoggingAndMongoDbNotAvailable() throws Exception {
		// given

		// when
		configure("connection-failure.xml");

		// then
		assertThat(mongoAppender().isStarted()).isFalse();
	}

	@Test
	public void shouldSaveInfoLogInMongoDB() throws Exception {
		// given
		configure("default.xml");

		// when
		log.info("Test: " + testName.getMethodName());

		// then
		final ILoggingEvent event = loadSingleEventFromMongo();
		assertThat(event.getLevel()).isEqualTo(Level.INFO);
		assertThat(event.getThreadName()).isEqualTo("main");
		assertThat(event.getLoggerName()).isEqualTo(
				this.getClass().getCanonicalName());
		assertThat(event.getMessage()).isEqualTo(
				"Test: " + testName.getMethodName());
	}

	@Test
	public void shouldSaveLogIncludingCallerData() throws Exception {
		// given
		configure("callerdata.xml");

		// when
		log.info("Test: " + testName.getMethodName());

		// then
		final ILoggingEvent event = loadSingleEventFromMongo();
		StackTraceElement[] callerData = event.getCallerData();
		assertThat(callerData.length).isGreaterThan(1);
		StackTraceElement ste = callerData[0];
		assertThat(ste.getFileName()).isEqualTo(
				this.getClass().getSimpleName() + ".java");
		assertThat(ste.getClassName()).isEqualTo(
				this.getClass().getCanonicalName());
		assertThat(ste.getMethodName()).isEqualTo(testName.getMethodName());
		assertThat(ste.isNativeMethod()).isEqualTo(false);
	}

	@Test
	public void shouldSaveProperDate() throws Exception {
		// given
		configure("default.xml");
		final Date before = new Date();

		// when
		log.info("Test: " + testName.getMethodName());

		// then
		final Date after = new Date();
		final ILoggingEvent event = loadSingleEventFromMongo();
		assertThat(event.getTimeStamp()).isGreaterThanOrEqualTo(
				before.getTime()).isLessThanOrEqualTo(after.getTime());
	}

	@Test
	public void shouldSaveExplicitLogArgument() throws Exception {
		configure("default.xml");

		// when
		log.warn("Test: {}", testName.getMethodName());

		// then
		final ILoggingEvent event = loadSingleEventFromMongo();
		assertThat(event.getLevel()).isEqualTo(Level.WARN);
		assertThat(event.getMessage()).isEqualTo(
				"Test: shouldSaveExplicitLogArgument");
	}

	@Test
	public void shouldSaveArgumentsOfDifferentTypes() throws Exception {
		configure("default.xml");
		final Date date = new Date();

		// when
		log.error("Test: {}, {} and {}", new Object[] { 42, "foo", date });

		// then
		final ILoggingEvent event = loadSingleEventFromMongo();
		assertThat(event.getLevel()).isEqualTo(Level.ERROR);
		assertThat(event.getArgumentArray()).containsOnly(42, "foo", date);
	}

	@Test
	public void shouldSaveMdcMap() throws Exception {
		// given
		configure("default.xml");
		MDC.put("sessionId", "XYZ");
		MDC.put("userId", "354");

		// when
		log.info("Test: " + testName.getMethodName());

		// then
		final ILoggingEvent event = loadSingleEventFromMongo();
		assertThat(event.getMDCPropertyMap()).hasSize(2)
				.includes(entry("sessionId", "XYZ"))
				.includes(entry("userId", "354"));
	}

	@Test
	public void shouldSaveThrowableDetails() throws Exception {
		// given
		configure("default.xml");

		// when

		try {
			throw new IllegalArgumentException("Something went wrong");
		} catch (IllegalArgumentException e) {
			log.info(":-(", e);
		}

		// then
		final ILoggingEvent event = loadSingleEventFromMongo();
		assertThat(event.getMessage()).isEqualTo(":-(");
		assertThrowable(event.getThrowableProxy(),
				"java.lang.IllegalArgumentException", "Something went wrong");
		assertThat(event.getThrowableProxy().getCause()).isNull();
	}

	private void assertThrowable(IThrowableProxy throwable,
			final String throwableClass, final String message) {
		assertThat(throwable).isNotNull();
		assertThat(throwable.getClassName()).isEqualTo(throwableClass);
		assertThat(throwable.getMessage()).isEqualTo(message);
	}

	@Test
	public void shouldSaveThrowableStackTrace() throws Exception {
		// given
		configure("default.xml");

		// when
		try {
			throw new IllegalArgumentException("Something went wrong");
		} catch (IllegalArgumentException e) {
			log.info(":-(", e);
		}

		// then
		final ILoggingEvent event = loadSingleEventFromMongo();
		StackTraceElementProxy[] steProxyArray = event.getThrowableProxy()
				.getStackTraceElementProxyArray();
		assertThat(steProxyArray.length).isGreaterThan(10); // long JUnit
															// stack...
		assertStackLineMethod(steProxyArray[0], testName.getMethodName());
	}

	@Test
	public void shouldSaveThrowableWithCauseStackTrace() throws Exception {
		// given
		configure("default.xml");
		final String FILE = "foo.db";

		// when
		try {
			try {
				throwSqlException(FILE);
			} catch (SQLException e) {
				throw new IllegalStateException("Persistence unavailable", e);
			}
		} catch (IllegalStateException e) {
			log.error(":-(", e);
		}

		// then
		final ILoggingEvent event = loadSingleEventFromMongo();
		assertThrowable(event.getThrowableProxy(),
				IllegalStateException.class.getCanonicalName(),
				"Persistence unavailable");
		StackTraceElementProxy[] steProxyArray = event.getThrowableProxy()
				.getStackTraceElementProxyArray();
		assertThat(steProxyArray.length).isGreaterThan(10); // long JUnit
															// stack...
		assertStackLineMethod(steProxyArray[0], testName.getMethodName());

		IThrowableProxy cause = event.getThrowableProxy().getCause();
		assertThrowable(cause, SQLException.class.getCanonicalName(),
				"Cannot open database: foo.db");
		assertThat(cause.getStackTraceElementProxyArray()).hasSize(2);
		assertStackLineMethod(cause.getStackTraceElementProxyArray()[0],
				"throwSqlException");
		assertStackLineMethod(cause.getStackTraceElementProxyArray()[1],
				testName.getMethodName());

		IThrowableProxy rootCause = cause.getCause();
		assertThrowable(rootCause,
				FileNotFoundException.class.getCanonicalName(), FILE);
		StackTraceElementProxy[] rootSteProxyArray = rootCause
				.getStackTraceElementProxyArray();
		assertThat(rootSteProxyArray).hasSize(3);
		assertStackLineMethod(rootSteProxyArray[0],
				"innerThrowFileNotFoundException");
		assertStackLineMethod(rootSteProxyArray[1],
				"throwFileNotFoundException");
		assertStackLineMethod(rootSteProxyArray[2], "throwSqlException");

		assertThat(rootCause.getCause()).isNull();
	}

	private void assertStackLineMethod(StackTraceElementProxy stackLine,
			final String method) {
		StackTraceElement ste = stackLine.getStackTraceElement();
		assertThat(ste.getClassName()).isEqualTo(
				this.getClass().getCanonicalName());
		assertThat(ste.getFileName()).isEqualTo(
				this.getClass().getSimpleName() + ".java");
		assertThat(ste.getMethodName()).isEqualTo(method);
		assertThat(ste.getLineNumber()).isGreaterThan(0);
	}

	private void throwSqlException(String file) throws SQLException {
		try {
			throwFileNotFoundException(file);
		} catch (FileNotFoundException e) {
			throw new SQLException("Cannot open database: " + file, e);
		}
	}

	private void throwFileNotFoundException(final String file)
			throws FileNotFoundException {
		innerThrowFileNotFoundException(file);
	}

	private void innerThrowFileNotFoundException(String file)
			throws FileNotFoundException {
		throw new FileNotFoundException(file);
	}

	private ILoggingEvent loadSingleEventFromMongo()
			throws InterruptedException {
		final List<ILoggingEvent> events = loadEventsFromMongo(1);
		return events.get(0);
	}

	private List<ILoggingEvent> loadEventsFromMongo(int expectedCount)
			throws InterruptedException {
		final List<ILoggingEvent> events = mongoAppender().logDao.find();
		assertThat(events.size()).isEqualTo(expectedCount);
		return events;
	}

	@Test
	public void shouldSaveMultipleEventsOnDifferentLevels() throws Exception {
		// given
		configure("default.xml");

		// when
		log.debug("A");
		log.info("B");
		log.warn("C");
		log.error("D");

		// then
		final List<ILoggingEvent> events = loadEventsFromMongo(4);
		assertLog(events.get(0), "A", Level.DEBUG);
		assertLog(events.get(1), "B", Level.INFO);
		assertLog(events.get(2), "C", Level.WARN);
		assertLog(events.get(3), "D", Level.ERROR);
	}

	private void assertLog(ILoggingEvent log, final String expectedMessage,
			final Level expectedLevel) {
		assertThat(log.getMessage()).isEqualTo(expectedMessage);
		assertThat(log.getLevel()).isEqualTo(expectedLevel);
	}

	@Test
	public void allAppenderParametersSetSmokeTest() throws Exception {
		// given

		// when
		configure("all-params.xml");

		// then
		assertThat(mongoAppender().isStarted()).isTrue();
	}

}
