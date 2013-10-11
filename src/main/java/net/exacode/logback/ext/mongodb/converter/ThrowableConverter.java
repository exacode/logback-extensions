package net.exacode.logback.ext.mongodb.converter;

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

/**
 * Converts {@link ThrowableProxy} to and from {@link BasicDBObject}.
 * <p>
 * This converter operates on {@link ThrowableProxy} instead of
 * {@link IThrowableProxy} because objects of first type are acceptable by
 * {@link ThrowableProxy}.
 * 
 * @author mendlik
 * 
 */
class ThrowableConverter {

	static class SimpleThrowableProxy extends ThrowableProxy {

		public SimpleThrowableProxy() {
			// TODO: So far there is no better solution.
			super(new Throwable());
		}

		private String message;
		private String className;
		private StackTraceElementProxy[] stackTraceElementProxyArray;
		private int commonFrames;
		private IThrowableProxy cause;
		private IThrowableProxy[] suppressed;

		@Override
		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		@Override
		public String getClassName() {
			return className;
		}

		public void setClassName(String className) {
			this.className = className;
		}

		@Override
		public StackTraceElementProxy[] getStackTraceElementProxyArray() {
			return stackTraceElementProxyArray;
		}

		public void setStackTraceElementProxyArray(
				StackTraceElementProxy[] stackTraceElementProxyArray) {
			this.stackTraceElementProxyArray = stackTraceElementProxyArray;
		}

		@Override
		public int getCommonFrames() {
			return commonFrames;
		}

		public void setCommonFrames(int commonFrames) {
			this.commonFrames = commonFrames;
		}

		@Override
		public IThrowableProxy getCause() {
			return cause;
		}

		public void setCause(IThrowableProxy cause) {
			this.cause = cause;
		}

		@Override
		public IThrowableProxy[] getSuppressed() {
			return suppressed;
		}

		public void setSuppressed(IThrowableProxy[] suppressed) {
			this.suppressed = suppressed;
		}

	}

	public static final String CLASS_FILED = "class";
	public static final String MESSAGE_FILED = "message";
	public static final String STACK_TRACE_FIELD = "stackTrace";
	public static final String CAUSE_TRACE_FIELD = "cause";

	private final StackTraceElementConverter steConverter = new StackTraceElementConverter();

	public ThrowableProxy convertToThrowableProxy(BasicDBObject object) {
		final SimpleThrowableProxy throwable = new SimpleThrowableProxy();
		throwable.setClassName(object.getString(CLASS_FILED));
		throwable.setMessage(object.getString(MESSAGE_FILED));
		if (object.containsField(STACK_TRACE_FIELD)) {
			throwable
					.setStackTraceElementProxyArray(toSteArray((BasicDBList) object
							.get(STACK_TRACE_FIELD)));
		}
		if (object.containsField(CAUSE_TRACE_FIELD)) {
			throwable.setCause(convertToThrowableProxy((BasicDBObject) object
					.get(CAUSE_TRACE_FIELD)));
		}
		return throwable;
	}

	public BasicDBObject convertToDocument(IThrowableProxy throwable) {
		final BasicDBObject throwableDoc = new BasicDBObject();
		throwableDoc.append(CLASS_FILED, throwable.getClassName());
		throwableDoc.append(MESSAGE_FILED, throwable.getMessage());
		throwableDoc.append(
				STACK_TRACE_FIELD,
				toSteArray(throwable.getStackTraceElementProxyArray(),
						throwable.getCommonFrames()));
		if (throwable.getCause() != null)
			throwableDoc.append(CAUSE_TRACE_FIELD,
					convertToDocument(throwable.getCause()));
		return throwableDoc;
	}

	private BasicDBList toSteArray(StackTraceElementProxy[] elementProxies,
			int commonFrames) {
		final int totalFrames = elementProxies.length - commonFrames;
		final BasicDBList stackTraceElements = new BasicDBList();
		for (int i = 0; i < totalFrames; ++i) {
			stackTraceElements
					.add(steConverter.convertToDocument(elementProxies[i]
							.getStackTraceElement()));
		}
		return stackTraceElements;
	}

	private StackTraceElementProxy[] toSteArray(BasicDBList object) {
		final StackTraceElementProxy[] stackTraceElements = new StackTraceElementProxy[object
				.size()];
		for (int i = 0; i < object.size(); ++i) {
			BasicDBObject steObject = (BasicDBObject) object.get(i);
			stackTraceElements[i] = new StackTraceElementProxy(
					steConverter.convertToStackTrace(steObject));
		}
		return stackTraceElements;
	}
}
