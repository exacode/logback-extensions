package net.exacode.logback.ext.mongodb.converter;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

/**
 * Converts {@link StackTraceElement} to and from {@link BasicDBObject}.
 * 
 * @author mendlik
 * 
 */
class StackTraceElementConverter {
	public static final String CLASS_FIELD = "class";
	public static final String METHOD_FIELD = "method";
	public static final String FILE_FIELD = "file";
	public static final String LINE_NO_FIELD = "lineNumber";
	public static final String NATIVE_FIELD = "native";

	public StackTraceElement[] convertToStackTrace(BasicDBList callerDataDocs) {
		StackTraceElement[] callerData = new StackTraceElement[callerDataDocs
				.size()];
		for (int i = 0; i < callerDataDocs.size(); ++i) {
			BasicDBObject callerDataDoc = (BasicDBObject) callerDataDocs.get(i);
			callerData[i] = convertToStackTrace(callerDataDoc);
		}
		return callerData;
	}

	public StackTraceElement convertToStackTrace(BasicDBObject callerDataDoc) {
		return new StackTraceElement(callerDataDoc.getString(CLASS_FIELD),
				callerDataDoc.getString(METHOD_FIELD),
				callerDataDoc.getString(FILE_FIELD),
				callerDataDoc.getInt(LINE_NO_FIELD));
	}

	public BasicDBList convertToDocument(StackTraceElement[] callerData) {
		final BasicDBList dbList = new BasicDBList();
		for (final StackTraceElement ste : callerData) {
			dbList.add(convertToDocument(ste));
		}
		return dbList;
	}

	public BasicDBObject convertToDocument(StackTraceElement callerData) {
		return new BasicDBObject().append(FILE_FIELD, callerData.getFileName())
				.append(CLASS_FIELD, callerData.getClassName())
				.append(METHOD_FIELD, callerData.getMethodName())
				.append(LINE_NO_FIELD, callerData.getLineNumber())
				.append(NATIVE_FIELD, callerData.isNativeMethod());
	}
}
