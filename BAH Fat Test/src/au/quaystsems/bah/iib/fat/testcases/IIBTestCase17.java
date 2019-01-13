/*
 * 
 *  Consists of three related test cases
 * 
 *  4.A Sends a mixture of well formed and malformed AIDX
 *  messages to the input queue
 * 
 *  4.B Reads the messages from the output queue which receives
 *  the good messages
 * 
 *  4.C Read from the queue of malformed messages
 *  
 *  To pass, the configured number of messages must be successfully
 *  sent to the input queue and the correct number of messages must 
 *  be read from each of the output queue 
 *  
 *  The message flow has a single input queue connected to a validation
 *  node which check compliance to the AIDX standard and outputs valid 
 *  messages to one queue and invalid messages to another
 * 
 */
package au.quaystsems.bah.iib.fat.testcases;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Random;

import org.jdom2.Element;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import au.quaystsems.bah.iib.fat.IIBTestCaseBase;
import au.quaystsems.bah.iib.fat.MReceiver;
import au.quaystsems.bah.iib.fat.MSender;


class IIBTestCase17 extends IIBTestCaseBase{

	private static Integer numAODB = 0;
	private static Integer numNonAODB = 0;

	public IIBTestCase17() {
		super();
	}

	@BeforeAll
	public  static void setUpClass() throws Exception {

		setUpTest();

		try {
			messageInterval = Integer.parseInt(config.props.getProperty("TEST_CASE_17_MESSAGE_INTERVAL"));
			messageWait = Integer.parseInt(config.props.getProperty("TEST_CASE_17_MESSAGE_WAIT_TIME"));
			pauseForInspection = Boolean.parseBoolean(config.props.getProperty("pause_for_inspection"));
			numMessages = Integer.parseInt(config.props.getProperty("TEST_CASE_17_NUMBER_MESSAGES"));
			inQueue = config.props.getProperty("TEST_CASE_17_INPUT_QUEUE");
			outQueue = config.props.getProperty("TEST_CASE_17_OUTPUT_QUEUE");
			errQueue = config.props.getProperty("TEST_CASE_17_OUTPUT_ERROR_QUEUE");
		} catch (Exception e) {
			e.printStackTrace();
			fail("Could not parse test case parameters");
		}

		sender = new MSender(inQueue);	
		assertNotNull(sender);

		recv = new MReceiver(outQueue);
		assertNotNull(recv);

		err = new MReceiver(errQueue);
		assertNotNull(err);

		/*
		 * Clear the queue of any existing messages
		 */
		assertTrue(sender.clearQueue());
		assertTrue(recv.clearQueue());
		assertTrue(err.clearQueue());

	}

	/**
	 * @throws Exception
	 */
	@Test
	@DisplayName("Test Case 17.A")
	void testCase_17A() throws Exception {

		// Setup the logging for the test case
		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName);
		logHeader(testCaseName);


		Random rand = new Random();

		logln(String.format("Send %s messages to the %s",numMessages,inQueue));
		for (int i = 1; i <= numMessages; i++) {

			// Interval between sending messages
			pause(messageInterval);

			//Get an AIDX formatted message. Every third message is from ATC 
			String message = null;


			/*
			 * Every third message sent will be from AODB, while
			 * the other messages will be from AFTN
			 * 
			 */
			if(rand.nextInt(3) == 1) {
				Element el = messMgr.getRandomFlight("SITA", "AODB");
				String correlID = el.getAttributeValue("CorrelationID")+i;
				el.setAttribute("CorrelationID", correlID);
				message = getXMLFromMessage(el);
				logln(String.format("Sending Message from SITA. CorrelationID = %s",correlID));
				numAODB++;

			} else {
				Element el = messMgr.getRandomFlight("ATC", "AFTN");
				String correlID = el.getAttributeValue("CorrelationID")+i;
				el.setAttribute("CorrelationID", correlID);
				message = getXMLFromMessage(el);
				logln(String.format("Sending Message from ATC. CorrelationID = %s",correlID));
				numNonAODB++;
			}

			final String mess = message;

			//Check that the message is not null and is of a minimum length
			assertNotNull(mess);
			assertTrue(mess.length() > 500);

			// Log the details of the message if required
			logDetail(String.format("Message# %s\nMessage Content:\n%s\n", i,mess));

			try {
				assertTimeout(ofMillis(2000), () -> {
					assertTrue(sender.mqPut(mess));
				});
			} catch (Exception e) {
				e.printStackTrace();
				fail("Sender Error");
			}

		}
		logln("");
		logln("Message sending complete");
		logln(String.format("Sent %s AODB Messages to %s", numAODB, inQueue));
		logln(String.format("Sent %s Non AODB Messages to %s", numNonAODB, inQueue));


		//Check the number sent was the number that was configured to be sent
		assertEquals(sender.getMessageSentCount(), numMessages.intValue());

		//Disconnect the sender from the queue manager
		assertTrue(sender.disconnect());

		//Log successful completion
		logSuccess(testCaseName);
	}

	/**
	 * 
	 * @throws Exception
	 */
	@Test
	@DisplayName("Test Case 17.B")
	void testCase_17B() throws Exception {

		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName);
		logHeader(testCaseName);
		logln("Read in from the filtered queue, should only contain messages from AODB");
		
		pauseForInspection();

		MReceiver recv = new MReceiver(outQueue);
		assertNotNull(recv);
		
		//
		//Read all the messages on the queue
		while (recv.mGet(messageWait) != null) {
			logln(String.format("Message from %s, Delivering System = %s, CorrelationID = %s", outQueue, recv.getDeliveringSystem(),recv.getCorrelationId()));
		}

		logln("");
		logln(String.format("Read %s Messages from %s", recv.getMessageRecvCount(), outQueue));
		assertEquals(numAODB.intValue(),  recv.getMessageRecvCount() );
		logln(String.format("Sent %s AODB Messages, Received %s AODB Messages", numAODB,recv.getMessageRecvCount()));

		//Disconnect the receiver from the queue manager
		assertTrue(recv.disconnect());

		//Log successful completion
		logSuccess(testCaseName);
	}
}
