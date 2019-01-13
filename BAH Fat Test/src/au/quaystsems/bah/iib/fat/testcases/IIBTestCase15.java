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

import au.quaystsems.bah.iib.fat.IIBTestCaseBase;
import au.quaystsems.bah.iib.fat.MReceiver;
import au.quaystsems.bah.iib.fat.MSender;


class IIBTestCase15 extends IIBTestCaseBase{

	private static Integer goodSend = 0;

	public IIBTestCase15() {
		super();
	}

	@BeforeAll
	public  static void setUpClass() throws Exception {

		setUpTest();

		try {
			messageInterval = Integer.parseInt(config.props.getProperty("TEST_CASE_15_MESSAGE_INTERVAL"));
			numMessages = Integer.parseInt(config.props.getProperty("TEST_CASE_15_NUM_MESSAGES"));
			messageWait = Integer.parseInt(config.props.getProperty("TEST_CASE_15_MESSAGE_WAIT_TIME"));
			inQueue = config.props.getProperty("TEST_CASE_15_QUEUE_IN");
			outQueue = config.props.getProperty("TEST_CASE_15_QUEUE_OUT");
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

	/**
	 * @throws Exception
	 */
	@Test
	@DisplayName("Test Case 15.A")
	void testCase_15A() throws Exception {

		// Setup the logging for the test case
		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName);
		logHeader(testCaseName, "Send the c messages to the input queue");


		//Get the configure mix of good and bad messages
		ArrayList<Element> messages = messMgr.getRandomFlightWithResources("SITA", "AODB", numMessages, false);

		logln("");
		logln(String.format("Configured to send %s messages to %s",numMessages,inQueue));
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

			String parkingPosition = el.getChild("FlightLeg", namespace)
					.getChild("LegData", namespace)
					.getChild("AirportResources",namespace)
					.getChild("Resource",namespace)
					.getChildText("AircraftParkingPosition",namespace);
			logln(String.format("Sending Valid AIDX XML: CorrelationID - %s, Parking Positon = %s",correlID, parkingPosition));
			goodSend++;

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

		logLast(i, "sent");
		logln("");
		logFinalSendResults(goodSend.intValue());

		assertEquals(goodSend.intValue(), numMessages.intValue());
		logln(String.format("Sent %s valid messages to %s", goodSend, inQueue));

		//Disconnect the sender from the queue manager
		assertTrue(sender.disconnect());

		//Log successful completion
		logSuccess(testCaseName);
	}
	
	@Test
	@DisplayName("Test Case 15.B")
	void testCase_15B() throws Exception {



		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName);
		logHeader(testCaseName);
		logln("Read in from the queue  and check the correlation IDs and Parking Position");

		MReceiver recv = new MReceiver(outQueue);
		assertNotNull(recv);
		
		//
		//Read all the messages on the queue
		int receiveCounter = 0;
		while (recv.mGet(messageWait) != null) {
			receiveCounter++;
			// Read the correlation ID and 
			Element el = recv.getRootElement();
			String id = el.getChildText("correlationID");
			String parkingPosition = el.getChildText("Gate");

			logln(String.format("Message from %s, CorrelationID = %s, Parking Positon = %s", outQueue, id, parkingPosition));
			
			String message = getXMLFromMessage(el);
			logln(message);

		}
		logln(String.format("%s Read %s Messages from %s", testCaseName, receiveCounter, outQueue));
		logln("");
		logln("Manually validate that the mappings of the parking positions are correct for the corresponding correlationID");
		logln("The mappings may be modified on the live system by editing the mapping.properties file");

		//Clean up connections and make sure expected number of messages were read

		//Disconnect the receiver from the queue manager
		assertTrue(recv.disconnect());


		//Log successful completion
		logSuccess(testCaseName);
	}

}
