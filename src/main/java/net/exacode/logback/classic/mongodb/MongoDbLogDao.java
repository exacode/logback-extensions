package net.exacode.logback.classic.mongodb;

import java.util.ArrayList;
import java.util.List;

import net.exacode.logback.classic.mongodb.converter.LoggingEventConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class MongoDbLogDao {

	private final LoggingEventConverter loggingEventConverter;

	private final DBCollection logCollection;

	public MongoDbLogDao(DBCollection logCollection) {
		this(logCollection, new LoggingEventConverter());
	}

	public MongoDbLogDao(DBCollection logCollection,
			LoggingEventConverter loggingEventConverter) {
		this.loggingEventConverter = loggingEventConverter;
		this.logCollection = logCollection;
	}

	public void append(ILoggingEvent logEvent) {
		logCollection.insert(convert(logEvent));
	}

	public List<ILoggingEvent> find() {
		DBCursor cursor = logCollection.find();
		return convert(cursor);
	}

	public List<ILoggingEvent> find(boolean naturalOrder) {
		DBCursor cursor = logCollection.find().sort(
				new BasicDBObject().append("$natural", naturalOrder ? 1 : -1));
		return convert(cursor);
	}

	public List<ILoggingEvent> find(int size, boolean naturalOrder) {
		DBCursor cursor = logCollection
				.find()
				.sort(new BasicDBObject().append("$natural", naturalOrder ? 1
						: -1)).limit(size);
		return convert(cursor);
	}

	public List<ILoggingEvent> find(int offset, int size, boolean naturalOrder) {
		DBCursor cursor = logCollection
				.find()
				.sort(new BasicDBObject().append("$natural", naturalOrder ? 1
						: -1)).skip(offset).limit(size);
		return convert(cursor);
	}

	public List<ILoggingEvent> findByTimestamp(long timestamp,
			boolean naturalOrder) {
		DBCursor cursor = logCollection.find(new BasicDBObject().append(
				LoggingEventConverter.TIME_STAMP_FIELD, timestamp));
		return convert(cursor);
	}

	/**
	 * Removes all documents from the collection.
	 * <p>
	 * If the collection is defined as capped then new collection is created
	 * with the same capped size. You cannot easily clear capped collection.
	 */
	public void clear() {
		if (logCollection.isCapped()) {
			int cappedSize = getCappedSize();
			String collectionName = logCollection.getName();
			DB db = logCollection.getDB();
			logCollection.drop();
			db.getCollection(collectionName);
			ensureCapped(cappedSize);
		} else {
			logCollection.remove(new BasicDBObject());
		}
	}

	/**
	 * Ensures that current collection is capped and has appropriate capped
	 * size.
	 * 
	 * @param size
	 */
	public void ensureCapped(int size) {
		if (getCappedSize() != size) {
			logCollection.getDB().command(
					new BasicDBObject("convertToCapped", logCollection
							.getName()).append("size", size));
		}
	}

	/**
	 * 
	 * @return cappedSize - size of the capped collection or -1 if the current
	 *         collection is not capped.
	 */
	public int getCappedSize() {
		DB db = logCollection.getDB();
		DBCollection collection = db.getCollection("system.namespaces");
		DBObject opt = collection.findOne(
				new BasicDBObject("name", "logdb.log"), new BasicDBObject(
						"options.size", "1"));
		if (opt == null || opt.keySet().size() == 0) {
			return -1;
		}
		return ((BasicDBObject) opt.get("options")).getInt("size");
	}

	private BasicDBObject convert(ILoggingEvent logEvent) {
		return loggingEventConverter.convertToDocument(logEvent);
	}

	private ILoggingEvent convert(DBObject document) {
		return loggingEventConverter
				.convertToLoggingEvent((BasicDBObject) document);
	}

	private List<ILoggingEvent> convert(DBCursor cursor) {
		List<ILoggingEvent> logEvents = new ArrayList<ILoggingEvent>();
		while (cursor.hasNext()) {
			logEvents.add(convert(cursor.next()));
		}
		return logEvents;
	}

}
