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

import java.io.IOException;
import java.util.ArrayList;

import org.jdom2.Element;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ibm.mq.MQEnvironment;

import au.quaystsems.bah.iib.fat.IIBTestCaseBase;
import au.quaystsems.bah.iib.fat.MReceiver;
import au.quaystsems.bah.iib.fat.MSender;


class IIBTestCase18 extends IIBTestCaseBase{

	private static Integer goodSend = 0;
	private static String badPassword;


	public IIBTestCase18() {
		super();
	}

	@BeforeAll
	public  static void setUpClass() throws Exception {

		setUpTest();

		try {
			messageInterval = Integer.parseInt(config.props.getProperty("TEST_CASE_18_MESSAGE_INTERVAL"));
			numMessages = Integer.parseInt(config.props.getProperty("TEST_CASE_18_NUM_MESSAGES"));
			messageWait = Integer.parseInt(config.props.getProperty("TEST_CASE_18_MESSAGE_WAIT_TIME"));
			badPassword = config.props.getProperty("bad_password");
			inQueue = config.props.getProperty("TEST_CASE_18_QUEUE_IN");
			outQueue = config.props.getProperty("TEST_CASE_18_QUEUE_OUT");
		} catch (Exception e) {
			e.printStackTrace();
			fail("Could not parse test case parameters");
		}
	}


	@Test
	@DisplayName("Test Case 18.A")
	void testCase_18A() throws Exception {

		// Setup the logging for the test case
		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName);
		logHeader(testCaseName, "Send the messages to the input queue, correct password set");


		sender = new MSender(inQueue);   
		recv = new MReceiver(outQueue);

		// Send and receive the messages
		sendAndreceive(testCaseName);
		
		//Log successful completion
		logSuccess(testCaseName);


	}

	@Test
	@DisplayName("Test Case 18.B")
	void testCase_18B() throws Exception {

		// Setup the logging for the test case
		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName);
		logHeader(testCaseName, "Send the messages to the input queue, bad password set");
		
		pauseForInspection();
		
		try {
			System.out.println(" Bahrain IIB FAT Test Case 18");
			System.out.print("Enter an incorrect password for the user: "); 
			java.io.BufferedReader r = new java.io.BufferedReader (new java.io.InputStreamReader (System.in));
			badPassword =  r.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}

//		assertThrows(org.opentest4j.AssertionFailedError.class,() -> 
//		{
			MQEnvironment.password = badPassword;
			logln(String.format("Setting connection password to '%s' ", MQEnvironment.password));
			logln("");
			
			logln("Trying to connect with the bad password, an exception should be thrown");
			logln("");
			
			// Next line should throw an exception from underlying connector because of the bad connection credentials
			
			try {
				sender = new MSender(inQueue,true);   
				recv = new MReceiver(outQueue,true);
			} catch (Exception e) {
				logln("");
				logln("Sender was not able to connect with invalid credentials");
				MQEnvironment.password = config.props.getProperty("password");
				logSuccess(testCaseName);
				return;
			}
			
			MQEnvironment.password = config.props.getProperty("password");
			
			logln(String.format("Connected to MQ with password '%s' set.", badPassword));
			logln(String.format("This testcase should use a bad password to show that the client cant connect."));
			logln(String.format("Did you set the correct password? "));
			fail(String.format("Connected to MQ with password '%s' set.", badPassword));
	}

	void sendAndreceive(String testCaseName) throws Exception {

		assertNotNull(sender);
		assertNotNull(recv);

		// Clear the queue of any existing messages
		assertTrue(sender.clearQueue());
		assertTrue(recv.clearQueue());

		//Check the queues are empty
		assertEquals(0, sender.getDepth());
		assertEquals(0, recv.getDepth());



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

			logln(String.format("Sending Valid AIDX XML: CorrelationID - %s",correlID));
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
		logln("");
		logFinalSendResults(goodSend.intValue());

		assertEquals(goodSend.intValue(), numMessages.intValue());
		logln(String.format("Sent %s valid messages to %s", goodSend, inQueue));

		//Disconnect the sender from the queue manager
		assertTrue(sender.disconnect());

	}
}
