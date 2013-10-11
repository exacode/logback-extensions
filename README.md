Extensions for logback - Java logger
====================================

RecordingAppender
-----------------
This recording appender comes from great blog post [Tomasz Nurkiewicz - MongoDB and recording appenders for Logback](http://nurkiewicz.blogspot.com/2011/04/mongodb-and-recording-appenders-for.html). It allows you to minimize log count by dumping only really important parts. 

Configuration consists of:
- target appender that will receive log dump
- log level that triggers log dump
- number of logs that will be kept in the memory (buffer) unless they are dumpped or removed
- when logs kept in the buffer should be removed (without dump)

See full [RecordingAppender Configuration](/src/test/resources/joran/recording/all-params.xml).

**Example configuration - logback.xml**

<?xml version="1.0" encoding="UTF-8" ?>
<configuration>

	<appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
		<encoder>
			<pattern>%-4relative [%thread] %-5level - %msg%n</pattern>
		</encoder>
	</appender>

	<appender name="REC" class="net.exacode.logback.ext.recording.RecordingAppender">
		<appender-ref ref="STDOUT" />

		<maxEvents>10</maxEvents>
		<dumpThreshold>ERROR</dumpThreshold>
		<expiryTimeMs>100</expiryTimeMs>
	</appender>

	<root level="DEBUG">
		<appender-ref ref="REC" />
	</root>

</configuration>

MongoDbAppender
---------------
MongoDbAppender allows you to dump logs to MongoDB collection. By default this appender uses [capped collections](http://docs.mongodb.org/manual/core/capped-collections/) (that works like circular buffer). Moreover this jar provides also a `MongoDbLogDao` that is an easy way of retrieving logs from MongoDB.

See full [MongoDbAppender Configuration](/src/test/resources/joran/mongodb/all-params.xml).

MongoDbAppender is again a simple modification of an idea and code available on great (really!) blog post [Tomasz Nurkiewicz - MongoDB and recording appenders for Logback](http://nurkiewicz.blogspot.com/2011/04/mongodb-and-recording-appenders-for.html). My changes:
* Changed deprecated API invocations
* Added marshaller and unmarshaller of `ILoggingEvent`s 
* Added support for capped collections
* Modified tests (parameterized mongo server address, etc...) 

**Example configuration - logback.xml**

		<?xml version="1.0" encoding="UTF-8" ?>
		<configuration debug="true">

			<appender name="MONGODB"
				class="net.exacode.logback.ext.mongodb.MongoDbAppender">
				<host>localhost</host>
				<port>27017</port>
				<dbName>logsdb</dbName>
				<collectionName>logs</collectionName>
			</appender>

			<root level="DEBUG">
				<appender-ref ref="MONGODB" />
			</root>

		</configuration>


Maven dependency
----------------

In order to use this library add [repository](http://github.com/exacode/mvn-repo) location into your `pom.xml` and add appropriate dependency.

		<dependency>
			<groupId>net.exacode.logback.ext</groupId>
			<artifactId>logback-extensions</artifactId>
			<version>${version.spring-logging}</version>
		</dependency>

<a href='http://www.pledgie.com/campaigns/22342'><img alt='Click here to lend your support to: Exacode open projects and make a donation at www.pledgie.com !' src='http://www.pledgie.com/campaigns/22342.png?skin_name=chrome' border='0' /></a>
