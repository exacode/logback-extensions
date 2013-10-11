package net.exacode.logback.classic.mongodb.converter;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

/**
 * Converts {@link ILoggingEvent} to and from {@link BasicDBObject}.
 * 
 * @author mendlik
 * 
 */
public class LoggingEventConverter {

	public static final String TIME_STAMP_FIELD = "timeStamp";
	public static final String LEVEL_FIELD = "level";
	public static final String THREAD_FIELD = "thread";
	public static final String LOGGER_FIELD = "logger";
	public static final String MESSAGE_FIELD = "message";
	public static final String MDC_FIELD = "mdc";
	public static final String CALLER_DATA_FIELD = "callerData";
	public static final String ARGUMENTS_FIELD = "arguments";
	public static final String THROWABLE_FIELD = "throwable";

	private final StackTraceElementConverter steConverter = new StackTraceElementConverter();

	private final ThrowableConverter throwableConverter = new ThrowableConverter();

	private boolean includeCallerData = true;

	public LoggingEventConverter() {
	}

	public LoggingEventConverter(boolean includeCallerData) {
		this.includeCallerData = includeCallerData;
	}

	public ILoggingEvent convertToLoggingEvent(BasicDBObject object) {
		final LoggingEvent event = new LoggingEvent();
		event.setTimeStamp(object.getDate(TIME_STAMP_FIELD).getTime());
		event.setLevel(Level.toLevel(object.getString(LEVEL_FIELD)));
		event.setThreadName(object.getString(THREAD_FIELD));
		event.setLoggerName(object.getString(LOGGER_FIELD));
		event.setMessage(object.getString(MESSAGE_FIELD));
		if (object.containsField(MDC_FIELD)) {
			BasicDBObject mdcMapDoc = (BasicDBObject) object.get(MDC_FIELD);
			Map<String, String> mdcMap = new HashMap<String, String>();
			for (String key : mdcMapDoc.keySet()) {
				mdcMap.put(key, mdcMapDoc.getString(key));
			}
		}
		if (object.containsField(CALLER_DATA_FIELD)) {
			event.setCallerData(steConverter
					.convertToStackTrace((BasicDBList) object
							.get(CALLER_DATA_FIELD)));
		}
		if (object.containsField(ARGUMENTS_FIELD)) {
			BasicDBList argListDoc = (BasicDBList) object.get(ARGUMENTS_FIELD);
			event.setArgumentArray(argListDoc.toArray());
		}
		if (object.containsField(THROWABLE_FIELD)) {
			event.setThrowableProxy(throwableConverter
					.convertToThrowableProxy((BasicDBObject) object
							.get(THROWABLE_FIELD)));
		}
		return event;
	}

	public BasicDBObject convertToDocument(ILoggingEvent event) {
		final BasicDBObject doc = new BasicDBObject();
		doc.append(TIME_STAMP_FIELD, new Date(event.getTimeStamp()));
		doc.append(LEVEL_FIELD, event.getLevel().levelStr);
		doc.append(THREAD_FIELD, event.getThreadName());
		doc.append(LOGGER_FIELD, event.getLoggerName());
		doc.append(MESSAGE_FIELD, event.getFormattedMessage());
		if (event.getMDCPropertyMap() != null
				&& !event.getMDCPropertyMap().isEmpty()) {
			doc.append(MDC_FIELD, event.getMDCPropertyMap());
		}
		if (includeCallerData) {
			doc.append(CALLER_DATA_FIELD,
					steConverter.convertToDocument(event.getCallerData()));
		}
		if (event.getArgumentArray() != null
				&& event.getArgumentArray().length > 0) {
			doc.append(ARGUMENTS_FIELD, event.getArgumentArray());
		}
		if (event.getThrowableProxy() != null) {
			final BasicDBObject val = throwableConverter
					.convertToDocument(event.getThrowableProxy());
			doc.append(THROWABLE_FIELD, val);
		}
		return doc;
	}
}
