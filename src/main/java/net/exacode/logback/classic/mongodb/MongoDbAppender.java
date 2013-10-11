package net.exacode.logback.classic.mongodb;

import java.net.UnknownHostException;

import net.exacode.logback.classic.mongodb.converter.LoggingEventConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;

/**
 * MongoDB appender.
 * 
 * @author mendlik
 */
public class MongoDbAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

	protected MongoClient mongo;
	protected LoggingEventConverter loggingEventConverter;
	protected MongoDbLogDao logDao;

	private String host = "localhost";
	private int port = 27017;
	private String dbName = "logsdb";
	private String collectionName = "logs";
	private String username;
	private String password;

	private int connectionsPerHost = 10;
	private int threadsAllowedToBlockForConnectionMultiplier = 5;
	private int maxWaitTime = 1000 * 60 * 2;
	private int connectTimeout;
	private int socketTimeout;
	private boolean autoConnectRetry;
	private int w = 1;
	private int wtimeout;
	private boolean j;
	private boolean fsync;
	private boolean capped = true;
	private int cappedSize = 1024 * 1024; // bytes
	private boolean includeCallerData = true;

	@Override
	public void start() {
		try {
			connectToMongoDB();
			super.start();
		} catch (UnknownHostException e) {
			addError(
					"Error connecting to MongoDB server: " + host + ":" + port,
					e);
		}
	}

	private void connectToMongoDB() throws UnknownHostException {
		mongo = new MongoClient(new ServerAddress(host, port), buildOptions());
		DB db = mongo.getDB(dbName);
		db.getStats(); // check DB connection, throws exception otherwise
		if (username != null && password != null) {
			db.authenticate(username, password.toCharArray());
		}
		DBCollection eventsCollection = db.getCollection(collectionName);
		logDao = new MongoDbLogDao(eventsCollection, new LoggingEventConverter(
				includeCallerData));
		if (capped) {
			// TODO: At the moment there is easy way to convert capped
			// collection back to normal
			logDao.ensureCapped(cappedSize);
		}
	}

	private MongoClientOptions buildOptions() {
		MongoClientOptions.Builder builder = MongoClientOptions
				.builder()
				.connectionsPerHost(connectionsPerHost)
				.threadsAllowedToBlockForConnectionMultiplier(
						threadsAllowedToBlockForConnectionMultiplier)
				.maxWaitTime(maxWaitTime).connectTimeout(connectTimeout)
				.socketTimeout(socketTimeout)
				.autoConnectRetry(autoConnectRetry)
				.writeConcern(new WriteConcern(w, wtimeout, fsync, j));
		return builder.build();
	}

	@Override
	protected void append(ILoggingEvent event) {
		logDao.append(event);
	}

	@Override
	public void stop() {
		if (mongo != null) {
			mongo.close();
		}
		super.stop();
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setConnectionsPerHost(int connectionsPerHost) {
		this.connectionsPerHost = connectionsPerHost;
	}

	public void setThreadsAllowedToBlockForConnectionMultiplier(
			int threadsAllowedToBlockForConnectionMultiplier) {
		this.threadsAllowedToBlockForConnectionMultiplier = threadsAllowedToBlockForConnectionMultiplier;
	}

	public void setMaxWaitTime(int maxWaitTime) {
		this.maxWaitTime = maxWaitTime;
	}

	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public void setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

	public void setAutoConnectRetry(boolean autoConnectRetry) {
		this.autoConnectRetry = autoConnectRetry;
	}

	public void setW(int w) {
		this.w = w;
	}

	public void setFsync(boolean fsync) {
		this.fsync = fsync;
	}

	public void setJ(boolean j) {
		this.j = j;
	}

	public void setWtimeout(int wtimeout) {
		this.wtimeout = wtimeout;
	}

	public void setCapped(boolean capped) {
		this.capped = capped;
	}

	public void setCappedSize(int cappedSize) {
		this.cappedSize = cappedSize;
	}

	public void setIncludeCallerData(boolean includeCallerData) {
		this.includeCallerData = includeCallerData;
	}

}
