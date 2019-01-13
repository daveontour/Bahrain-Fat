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


class IIBTestCase20 extends IIBTestCaseBase{


	private static String inQueue2, inQueue5, inQueue8;
	private static MSender sender2 ,sender5,sender8 ; 
	private Integer p2Recv = 0, p5Recv = 0, p8Recv = 0;


	public IIBTestCase20() {
		super();
	}

	@BeforeAll
	public  static void setUpClass() throws Exception {

		setUpTest();

		try {
			numMessages = Integer.parseInt(config.props.getProperty("TEST_CASE_20_NUMBER_MESSAGES"));
			messageInterval = Integer.parseInt(config.props.getProperty("TEST_CASE_20_MESSAGE_INTERVAL"));
			messageWait = Integer.parseInt(config.props.getProperty("TEST_CASE_20_MESSAGE_WAIT_TIME"));
			pauseForInspection = Boolean.parseBoolean(config.props.getProperty("pause_for_inspection"));
			inQueue2 = config.props.getProperty("TEST_CASE_20_QUEUE_IN_P2");
			inQueue5 = config.props.getProperty("TEST_CASE_20_QUEUE_IN_P5");
			inQueue8 = config.props.getProperty("TEST_CASE_20_QUEUE_IN_P8");
			outQueue = config.props.getProperty("TEST_CASE_20_QUEUE_OUT");
			errQueue = config.props.getProperty("TEST_CASE_20_QUEUE_ERR");
		} catch (Exception e) {
			e.printStackTrace();
			fail("Could not parse test case parameters");
		}

		sender2 = new MSender(inQueue2);   
		sender5 = new MSender(inQueue5);    
		sender8 = new MSender(inQueue8);    
		recv = new MReceiver(outQueue);
		err = new MReceiver(errQueue);

		assertNotNull(sender2);
		assertNotNull(sender2);
		assertNotNull(sender2);
		assertNotNull(recv);
		assertNotNull(err);

		//Clear the queue of any existing messages
		assertTrue(sender2.clearQueue());
		assertTrue(sender5.clearQueue());
		assertTrue(sender8.clearQueue());
		assertTrue(recv.clearQueue());
		assertTrue(err.clearQueue());

		//Make sure queues are empty
		assertEquals(0, sender2.getDepth());
		assertEquals(0, sender5.getDepth());
		assertEquals(0, sender8.getDepth());
		assertEquals(0, recv.getDepth());
		assertEquals(0, err.getDepth());

	}

	@Test
	@DisplayName("Test Case 20.A")
	void testCase_20A() throws Exception {

		// Setup the logging for the test case
		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName);
		logHeader(testCaseName);


		logln(String.format("Send total of %s messages to the 3 different queues",numMessages));
		logln("");

		for (int i = 1; i <= numMessages; i++) {

			// Interval between sending messages
			pause(messageInterval);

			String message = null, priority = "";
			String org = null, system = null;

			MSender sender = null;
			//Randomly select a system to send the message from.
			switch (new Random().nextInt(3)) {
			case 0:
				org = "SITA";
				system = "AODB";
				sender = sender2;
				priority = "2";
				break;
			case 1:
				org = "FMT";
				system = "VDGS";
				sender = sender5;
				priority = "5";
				break;
			case 2:
				org = "ATC";
				system = "AFTN";
				sender = sender8;
				priority = "8";
				break;
			}

			Element el = messMgr.getRandomFlight(org, system);
			String correlID = el.getAttributeValue("CorrelationID")+i;
			el.setAttribute("CorrelationID", correlID);
			message = getXMLFromMessage(el);

			logln(String.format("Sending Message from %s. (Priority %s) CorrelationID = %s",system, priority, correlID));


			final String mess = message;

			//Check that the message is not null and is of a minimum length
			assertNotNull(mess);

			// Log the details of the message if required
			logDetail(String.format("Message# %s\nMessage Content:\n%s\n", i,mess));

			final MSender s = sender;
			//Send the message
			try {
				assertTimeout(ofMillis(2000), () -> {
					assertTrue(s.mqPut(mess));
				});
			} catch (Exception e) {
				e.printStackTrace();
				fail("Sender Error");
			}

		}

		logln("");
		logln(String.format("Sent %s messages to priority 2 queue", sender2.getMessageSentCount()));
		logln(String.format("Sent %s messages to priority 5 queue", sender5.getMessageSentCount()));
		logln(String.format("Sent %s messages to priority 8 queue", sender8.getMessageSentCount()));


		//Clean up connections and make sure expected number of messages were read

		//Disconnect the sender from the queue manager
		assertTrue(sender2.disconnect());

		//Log successful completion
		logSuccess(testCaseName);
	}

	/**
	 * 
	 * @throws Exception
	 */
	@Test
	@DisplayName("Test Case 20.B")
	void testCase_20B() throws Exception {

		/*
		 * Read the correctly formed AIDX messages
		 */

		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName);
		logHeader(testCaseName);

		pauseForInspection();

		MReceiver recv = new MReceiver(outQueue);
		assertNotNull(recv);

		//
		//Read all the messages on the queue
		int i = 0;
		while (recv.mGet(messageWait) != null) {
			i++;

			// Read the correlation ID and 
			Element el = recv.getRootElement();
			String from = el.getChild("Originator",namespace).getAttribute("CompanyShortName").getValue();
			String id = el.getAttribute("CorrelationID").getValue();

			if (from.contains("SITA")) p2Recv++;
			if (from.contains("FMT")) p5Recv++;
			if (from.contains("ATC")) p8Recv++;

			logln(String.format("Message %s from %s, CorrelationID = %s", i,from, id));

		}

		logln("");
		logln("Message receiving complete");
		logln("Check the above list to make sure the messages were recieved in priority order");
		logln("");


		assertEquals(sender2.getMessageSentCount(), p2Recv.intValue());
		logln(String.format("%s P2 messages sent, %s P2 messages read", sender2.getMessageSentCount(), p2Recv));

		assertEquals(sender5.getMessageSentCount(), p5Recv.intValue());
		logln(String.format("%s P5 messages sent, %s P5 messages read", sender5.getMessageSentCount(), p5Recv));

		assertEquals(sender8.getMessageSentCount(), p8Recv.intValue());
		logln(String.format("%s P8 messages sent, %s P8 messages read", sender8.getMessageSentCount(), p8Recv));

		//Clean up connections and make sure expected number of messages were read

		//Disconnect the receiver from the queue manager
		assertTrue(recv.disconnect());

		//Log successful completion
		logSuccess(testCaseName);
	}

}
