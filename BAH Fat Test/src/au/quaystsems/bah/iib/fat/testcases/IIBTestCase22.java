/*
 *  Test Case 4:
 *  
 *  Consists of three related test cases
 * 
 *  4.A Sends a mixture of well formed and malformed AIDX
 *  messages to the input queue
 * 
 *  4.B Reads the messages from the output queues and makes sure
 *  the correct number of valid and invalid messages have been received.

 *  
 *  To pass, the configured number of messages must be successfully
 *  sent to the input queue and the correct number of messages must 
 *  be read from each of the output queue 
 *  
 *  The message flow has a single input queue connected to a validation
 *  node which check compliance to the AIDX standard and outputs valid 
 *  messages to one queue and invalid messages to another
 * 
 *  Message Flow:
 *   IIB_FAT_TC_4.msgflow
 *  
 *  Queues:
 *   BAH.IIB.FAT.TC4.QUEUE.IN
 *   BAH.IIB.FAT.TC4.QUEUE.OUT
 *   BAH.IIB.FAT.TC4.QUEUE.ERR
 *   
 *  The message flow includes a validation node to check that the 
 *  message received is a valid AIDX message. 
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

import com.ibm.mq.MQException;

import au.quaystsems.bah.iib.fat.IIBTestCaseBase;
import au.quaystsems.bah.iib.fat.MReceiver;
import au.quaystsems.bah.iib.fat.MSender;


class IIBTestCase22 extends IIBTestCaseBase{

	private static String inQueue, outQueue1, outQueue2, outQueue3, errQueue;
	private static MSender sender;
	private static MReceiver recv1,recv2,recv3,err;

	public IIBTestCase22() {
		super();
	}

	@BeforeAll
	public  static void setUpClass() throws Exception {

		setUpTest();

		try {
			messageInterval = Integer.parseInt(config.props.getProperty("TEST_CASE_22_MESSAGE_INTERVAL"));
			numMessages = Integer.parseInt(config.props.getProperty("TEST_CASE_22_NUM_GOOD_MESSAGES"));
			numBadMessages = Integer.parseInt(config.props.getProperty("TEST_CASE_22_NUM_BAD_MESSAGES"));
			messageWait = Integer.parseInt(config.props.getProperty("TEST_CASE_22_MESSAGE_WAIT_TIME"));
			inQueue = config.props.getProperty("TEST_CASE_22_QUEUE_IN");
			outQueue1 = config.props.getProperty("TEST_CASE_22_QUEUE_OUT_1");
			outQueue2 = config.props.getProperty("TEST_CASE_22_QUEUE_OUT_2");
			outQueue3 = config.props.getProperty("TEST_CASE_22_QUEUE_OUT_3");
			errQueue = config.props.getProperty("TEST_CASE_22_QUEUE_ERR");
		} catch (Exception e) {
			e.printStackTrace();
			fail("Could not parse test case parameters");
		}
		
		sender = new MSender(inQueue);   
		recv1 = new MReceiver(outQueue1);
		recv2 = new MReceiver(outQueue2);
		recv3 = new MReceiver(outQueue3);
		err = new MReceiver(errQueue);

		assertNotNull(sender);
		assertNotNull(recv1);
		assertNotNull(recv2);
		assertNotNull(recv3);
		assertNotNull(err);

		// Clear the queue of any existing messages
		assertTrue(sender.clearQueue());
		assertTrue(recv1.clearQueue());
		assertTrue(recv2.clearQueue());
		assertTrue(recv3.clearQueue());
		assertTrue(err.clearQueue());

		//Check the queues are empty
		assertEquals(0, sender.getDepth());
		assertEquals(0, recv1.getDepth());
		assertEquals(0, recv2.getDepth());
		assertEquals(0, recv3.getDepth());
		assertEquals(0, err.getDepth());
	}

	/**
	 * @throws Exception
	 */
	@Test
	@DisplayName("Test Case 22.A")
	void testCase_22A() throws Exception {

		// Setup the logging for the test case
		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName);
		logHeader(testCaseName, "Send the configured mix of good and bad messages to the input queue");


		//Get the configure mix of good and bad messages
		ArrayList<Element> messages = messMgr.getGoodBadRandomFlight("SITA", "AODB", numMessages, numBadMessages);

		logln("");
		logln(String.format("Configured to send %s good messages and %s bad messages to %s",numMessages,numBadMessages,inQueue));
		logln("");

		int i = 0;
		for (Element el : messages) {

			el = updateStats(el);

			// Interval between sending messages
			pause(messageInterval);

			//Get the XML Representation of the message
			String message = getXMLFromMessage(el);

			//Check that the message is not null
			assertNotNull(message);

			//Log the correlationID and whether the message is well formed AIDX or not
			String correlID = el.getAttributeValue("CorrelationID");
			if (message.contains("Intentionally_Bad")) {
				logln(String.format("Sending Malformed AIDX XML: CorrelationID - %s",correlID));
			} else {
				logln(String.format("Sending Valid AIDX XML: CorrelationID - %s",correlID));
			}

			// Log the details of the message if required
			logDetail(String.format("Message# %s\nMessage Content:\n%s\n",i, message));


			//Send the message
			try {
				assertTimeout(ofMillis(2000), () -> {
					assertTrue(sender.mqPut(message));
				});
			} catch (Exception e) {
				e.printStackTrace();
				fail("Sender Error");
			}

		}

		logln("");
		logln("Message Sending Complete");

		assertEquals(numMessages.intValue(), sender.getMessageSentCount());
		logln(String.format("Sent %s valid messages to %s", sender.getMessageSentCount(), inQueue));

		//Disconnect the sender from the queue manager
		assertTrue(sender.disconnect());

		//Log successful completion
		logSuccess(testCaseName);
	}

	@Test
	@DisplayName("Test Case 22.B")
	void testCase_22B() throws Exception {
		// Setup the logging for the test case
		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName);
		logHeader(testCaseName, "Read all the messages sent");

		pauseForInspection();


		logln("Startting thread the read the messages as they are sent");
		Thread readThread1 =  new Thread() {
			public void run() {
				while (recv1.mGet(messageWait) != null) {
					try {
						logln(String.format("Received valid message on %s. Correlation ID = %s", recv1.queue.getName().trim(), recv1.getCorrelationId()));
					} catch (MQException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};
		Thread readThread2 =  new Thread() {
			public void run() {
				while (recv2.mGet(messageWait) != null) {
					try {
						logln(String.format("Received valid message on %s. Correlation ID = %s", recv2.queue.getName().trim(), recv2.getCorrelationId()));
					} catch (MQException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};
		Thread readThread3 =  new Thread() {
			public void run() {
				while (recv3.mGet(messageWait) != null) {
					try {
						logln(String.format("Received valid message on %s. Correlation ID = %s", recv3.queue.getName().trim(), recv3.getCorrelationId()));
					} catch (MQException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};

		readThread1.start();
		readThread2.start();
		readThread3.start();

		readThread1.join();
		readThread2.join();
		readThread3.join();

		logln("");
		logln("Message Receiving complete");

		assertEquals(sender.getMessageSentCount(), recv1.getMessageRecvCount());
		logln(String.format("Sent %s message on %s. Received %s messages on %s", sender.getMessageSentCount(), sender.queue.getName().trim(), recv1.getMessageRecvCount(), recv1.queue.getName()));

		assertEquals(sender.getMessageSentCount(), recv2.getMessageRecvCount());
		logln(String.format("Sent %s message on %s. Received %s messages on %s", sender.getMessageSentCount(), sender.queue.getName().trim(), recv2.getMessageRecvCount(), recv2.queue.getName()));

		assertEquals(sender.getMessageSentCount(), recv3.getMessageRecvCount());
		logln(String.format("Sent %s message on %s. Received %s messages on %s", sender.getMessageSentCount(), sender.queue.getName().trim(), recv3.getMessageRecvCount(), recv3.queue.getName()));

		//Log successful completion
		logSuccess(testCaseName);

	}
}
