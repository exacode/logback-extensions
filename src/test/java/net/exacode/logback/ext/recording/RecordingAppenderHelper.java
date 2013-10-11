package net.exacode.logback.ext.recording;

import org.slf4j.Logger;

/**
 * @author Tomasz Nurkiewicz
 */
public class RecordingAppenderHelper {

	private final Logger log;

	public RecordingAppenderHelper(Logger log) {
		this.log = log;
	}

	public void otherMethod(String msg) {
		log.info(msg);
	}
}
