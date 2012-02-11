package com.dtdsoftware.splunk.logging;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;

import com.splunk.Args;
import com.splunk.Receivers;
import com.splunk.Service;

/**
 * Common REST logic shared by all appenders/handlers
 * 
 * @author Damien Dallimore damien@dtdsoftware.com
 * 
 */

public class SplunkRestInput {


	// Java SDK objects
	private Service service;
	private Receivers receivers;
	private Args args;

	// streaming objects
	private Socket streamSocket = null;
	private OutputStream ostream;
	private Writer writerOut = null;

	/**
	 * Create a SplunkRestInput object to send events to Splunk via REST
	 * endpoints
	 * 
	 * @param user
	 *            Splunk user
	 * @param pass
	 *            Splunk user pass
	 * @param host
	 *            REST endppoint host
	 * @param port
	 *            REST endpoint port
	 * @param red
	 *            url parameter values
	 * @param stream
	 *            true=stream , false=do not stream (be aware, poor performance)
	 * @throws Exception
	 */
	public SplunkRestInput(String user, String pass, String host, int port,
			RestEventData red, boolean stream) throws Exception {

		this.service = new Service(host, port);
		this.service.login(user, pass);
		this.receivers = new Receivers(this.service);

		this.args = createArgs(red);

		if (stream) {
			this.streamSocket = this.receivers.attach(args);
			ostream = streamSocket.getOutputStream();
			writerOut = new OutputStreamWriter(ostream, "UTF8");
		}

	}

	/**
	 * Create a SDK Args object from a RestEventData object
	 * 
	 * @param red
	 * @return
	 */
	private Args createArgs(RestEventData red) {

		Args urlArgs = new Args();

		if (red != null) {
			if (red.getIndex().length() > 0)
				urlArgs.add(RestEventData.RECEIVERS_SIMPLE_ARG_INDEX, red.getIndex());
			if (red.getSource().length() > 0)
				urlArgs.add(RestEventData.RECEIVERS_SIMPLE_ARG_SOURCE, red.getSource());
			if (red.getSourcetype().length() > 0)
				urlArgs.add(RestEventData.RECEIVERS_SIMPLE_ARG_SOURCETYPE, red
						.getSourcetype());
			if (red.getHost().length() > 0)
				urlArgs.add(RestEventData.RECEIVERS_SIMPLE_ARG_HOST, red.getHost());
			if (red.getHostRegex().length() > 0)
				urlArgs.add(RestEventData.RECEIVERS_SIMPLE_ARG_HOSTREGEX, red.getHostRegex());

		}
		return urlArgs;
	}

	/**
	 * close the stream
	 */
	public void closeStream() {
		try {
			if (writerOut != null) {
				writerOut.flush();
				writerOut.close();
				if (streamSocket != null)
					streamSocket.close();
			}
		} catch (Exception e) {
		}
	}

	/**
	 * send a single event , not so good for performance, streaming is better
	 * 
	 * @param message
	 */
	public void sendEvent(String message) {

		if (streamSocket == null)
			this.receivers.submit(message, args);
	}

	/**
	 * send an event via stream
	 * 
	 * @param message
	 */
	public void streamEvent(String message) {

		try {
			if (writerOut != null) {
				writerOut.write(message + "\n");
				writerOut.flush();
			}
		} catch (IOException e) {
		}

	}
}
