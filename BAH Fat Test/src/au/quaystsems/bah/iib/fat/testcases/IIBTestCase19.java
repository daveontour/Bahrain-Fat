/*
 *  Test Case 19:
 *  
 *  Consists of 2 related test cases
 * 
 *  19.A Sends a well formatted AIDX messages to the input queue
 *  
 *  19.B Wait to read the messages from the output queues. 
 *  
 *  The message flow has a trigger that fires every 30sec
 *  When the trigger fires, the current time is checked.
 *  If the current time is within 30 seconds of the configured time
 *  then the messages are read from the input queue and passed to 
 *  the output queue.
 *  
 *  The time for the message sending from the input to the output queue
 *  can be fonfigured on the live system by setting the User Define Parameter TransferMessageTime
 *  This is done in the Web UI.
 *  
 *  Message Flow:
 *   IIB_FAT_TC_19.msgflow
 *  
 *  Queues:
 *   BAH.IIB.FAT.TC19.QUEUE.IN
 *   BAH.IIB.FAT.TC19.QUEUE.OUT
 *  
 */
package au.quaystsems.bah.iib.fat.testcases;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;

import org.jdom2.Element;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import au.quaystsems.bah.iib.fat.IIBTestCaseBase;
import au.quaystsems.bah.iib.fat.MReceiver;
import au.quaystsems.bah.iib.fat.MSender;


class IIBTestCase19 extends IIBTestCaseBase{

	public IIBTestCase19() {
		super();
	}

	@BeforeAll
	public  static void setUpClass() throws Exception {

		setUpTest();

		try {
			messageInterval = Integer.parseInt(config.props.getProperty("TEST_CASE_19_MESSAGE_INTERVAL"));
			numMessages = Integer.parseInt(config.props.getProperty("TEST_CASE_19_NUM_MESSAGES"));
			messageWait = Integer.parseInt(config.props.getProperty("TEST_CASE_19_MESSAGE_WAIT_TIME"));
			pauseForInspection = Boolean.parseBoolean(config.props.getProperty("pause_for_inspection"));
			inQueue = config.props.getProperty("TEST_CASE_19_QUEUE_IN");
			outQueue = config.props.getProperty("TEST_CASE_19_QUEUE_OUT");
		} catch (Exception e) {
			e.printStackTrace();
			fail("Could not parse test case parameters");
		}
		
		sender = new MSender(inQueue);   
		recv = new MReceiver(outQueue);

		assertNotNull(sender);
		assertNotNull(recv);

		// Clear the queue of any existing messages
		assertTrue(sender.clearQueue());
		assertTrue(recv.clearQueue());

		//Check the queues are empty
		assertEquals(0, sender.getDepth());
		assertEquals(0, recv.getDepth());
	}

	@Test
	@DisplayName("Test Case 19.A")
	void testCase_19A() throws Exception {

		// Setup the logging for the test case
		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName);
		logHeader(testCaseName, "Send messages to the input queue");


		//Get the configure mix of good and bad messages
		ArrayList<Element> messages = messMgr.getRandomFlightWithResources("SITA", "AODB", numMessages, false);

		logln("");
		logln(String.format("Configured to send %s messages to %s",numMessages,inQueue));
		logln("");

		int i = 0;
		for (Element el : messages) {

			el = updateStats(el);
			final String correlID = el.getAttributeValue("CorrelationID");


			// Interval between sending messages
			pause(messageInterval);

			//Get the XML Representation of the message
			String message = getXMLFromMessage(el);

			//Check that the message is not null
			assertNotNull(message);

			// Log the details of the message if required
			logDetail(String.format("Message# %s\nMessage Content:\n%s\n",i, message));


			//Send the message
			try {
				assertTimeout(ofMillis(2000), () -> {
					assertTrue(sender.mqPut(message));
					logln(String.format("Sending Valid AIDX XML: CorrelationID - %s",correlID));
				});
			} catch (Exception e) {
				e.printStackTrace();
				fail("Sender Error");
			}
		}

		logLast(i, "sent");
		logln("");
		logFinalSendResults(sender.getMessageSentCount());

		assertEquals(sender.getMessageSentCount(), numMessages.intValue());
		logln(String.format("Sent %s valid messages to %s", sender.getMessageSentCount(), inQueue));

		//Disconnect the sender from the queue manager
		assertTrue(sender.disconnect());

		//Log successful completion
		logSuccess(testCaseName);
	}

	@Test
	@DisplayName("Test Case 19.B")
	void testCase_19B() throws Exception {

		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName);
		logHeader(testCaseName);
		logln("Read in from the queue");
		
		pauseForInspection();

		MReceiver recv = new MReceiver(outQueue);
		assertNotNull(recv);

		MReceiver recvIn = new MReceiver(inQueue);
		assertNotNull(recvIn);


		while (recv.getMessageRecvCount() < numMessages) {
			if (recv.mGet(messageWait) != null) {
				logln(String.format("Message from %s, CorrelationID = %s", outQueue, recv.getCorrelationId()));
			} else {
				logln(String.format("Input Queue Message Count = %s, Output Queue Message Count = %s", recvIn.getDepth(), recv.getDepth()));						
			}
		}
		logln("");
		logln("Message Receiving Complete");
		logln("");
		logln(String.format("Read %s Messages from %s", recv.getMessageRecvCount(), outQueue));
		logln("");

		//Clean up connections and make sure expected number of messages were read

		assertEquals(recv.getMessageRecvCount(), numMessages.intValue());
		//Disconnect the receiver from the queue manager
		assertTrue(recv.disconnect());

		//Log successful completion
		logSuccess(testCaseName);
	}
}
