package net.exacode.logback.ext.recording;

import java.util.Date;
import java.util.Iterator;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEventVO;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.helpers.CyclicBuffer;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.spi.AppenderAttachableImpl;

/**
 * @see <a
 *      href="http://nurkiewicz.blogspot.com/2011/04/mongodb-and-recording-appenders-for.html">Tomasz
 *      Nurkiewicz - blog post</a>
 * @see <a
 *      href="https://github.com/nurkiewicz/logback/blob/recording-appender/logback-classic/src/main/java/ch/qos/logback/classic/RecordingAppender.java">Tomasz
 *      Nurkiewicz - source code</a>
 * @author Tomasz Nurkiewicz
 * @since 27.03.11, 21:25
 */
public class RecordingAppender extends
		UnsynchronizedAppenderBase<ILoggingEvent> implements
		AppenderAttachable<ILoggingEvent> {

	private int maxEvents = 100;
	private Level dumpThreshold = Level.ERROR;
	private long expiryTimeMs = 30000;
	private boolean callerData = true;

	private final ThreadLocal<CyclicBuffer<ILoggingEvent>> recordedEvents = new ThreadLocal<CyclicBuffer<ILoggingEvent>>() {
		@Override
		protected CyclicBuffer<ILoggingEvent> initialValue() {
			return new CyclicBuffer<ILoggingEvent>(maxEvents);
		}
	};

	@Override
	protected void append(ILoggingEvent eventObject) {
		if (callerData) {
			eventObject.getCallerData();
		} else {
			eventObject = LoggingEventVO.build(eventObject);
		}
		if (triggersDump(eventObject)) {
			dumpRecordedEvents();
			dump(eventObject);
		} else {
			recordedEvents.get().add(eventObject);
		}
	}

	private boolean triggersDump(ILoggingEvent eventObject) {
		return eventObject.getLevel().isGreaterOrEqual(dumpThreshold);
	}

	private void dumpRecordedEvents() {
		final CyclicBuffer<ILoggingEvent> events = recordedEvents.get();
		final long DUMP_AFTER_TIMESTAMP = new Date().getTime() - expiryTimeMs;
		for (int i = 0; i < events.length(); ++i) {
			final ILoggingEvent event = events.get(i);
			dumpIfRecent(event, DUMP_AFTER_TIMESTAMP);
		}
		events.clear();
	}

	private void dumpIfRecent(ILoggingEvent event, long recentThreshold) {
		if (event.getTimeStamp() > recentThreshold) {
			dump(event);
		}
	}

	private void dump(ILoggingEvent event) {
		final Iterator<Appender<ILoggingEvent>> iter = iteratorForAppenders();
		while (iter.hasNext()) {
			iter.next().doAppend(event);
		}
	}

	public void setMaxEvents(int maxEvents) {
		this.maxEvents = maxEvents;
	}

	public void setDumpThreshold(Level dumpThreshold) {
		this.dumpThreshold = dumpThreshold;
	}

	public void setExpiryTimeMs(long expiryTimeMs) {
		this.expiryTimeMs = expiryTimeMs;
	}

	public void setCallerData(boolean callerData) {
		this.callerData = callerData;
	}

	private transient AppenderAttachableImpl<ILoggingEvent> aai = new AppenderAttachableImpl<ILoggingEvent>();

	@Override
	public void addAppender(Appender<ILoggingEvent> newAppender) {
		aai.addAppender(newAppender);
	}

	@Override
	public Iterator<Appender<ILoggingEvent>> iteratorForAppenders() {
		return aai.iteratorForAppenders();
	}

	@Override
	public Appender<ILoggingEvent> getAppender(String name) {
		return aai.getAppender(name);
	}

	@Override
	public boolean isAttached(Appender<ILoggingEvent> appender) {
		return aai.isAttached(appender);
	}

	@Override
	public void detachAndStopAllAppenders() {
		aai.detachAndStopAllAppenders();
	}

	@Override
	public boolean detachAppender(Appender<ILoggingEvent> appender) {
		return aai.detachAppender(appender);
	}

	@Override
	public boolean detachAppender(String name) {
		return aai.detachAppender(name);
	}
}
