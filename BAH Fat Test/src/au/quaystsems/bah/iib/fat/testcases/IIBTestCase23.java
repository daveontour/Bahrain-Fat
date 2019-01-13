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

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.UUID;

import org.jdom2.Element;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import au.quaystsems.bah.iib.fat.IIBTestCaseBase;
import au.quaystsems.bah.iib.fat.MReceiver;
import au.quaystsems.bah.iib.fat.MSender;


class IIBTestCase23 extends IIBTestCaseBase{

	private static String inQueue, inCSVQueue, outQueue, err;
	private static MSender sender, senderCSV; 
	private static MReceiver recv , recv2, recvErr;

	private static Integer goodSend = 0;

	private static ArrayList<String> sentSequence =  new ArrayList<String> ();
	private static ArrayList<String> recvSequence =  new ArrayList<String> ();

	public static String csvFileName;



	public IIBTestCase23() {
		super();
	}

	@BeforeAll
	public  static void setUpClass() throws Exception {

		setUpTest();

		try {
			messageInterval = Integer.parseInt(config.props.getProperty("TEST_CASE_23_MESSAGE_INTERVAL"));
			numMessages = Integer.parseInt(config.props.getProperty("TEST_CASE_23_NUM_MESSAGES"));
			messageWait = Integer.parseInt(config.props.getProperty("TEST_CASE_23_MESSAGE_WAIT_TIME"));
			csvFileName = config.props.getProperty("TEST_CASE_23_CSV_FILENAME");
			pauseForInspection = Boolean.parseBoolean(config.props.getProperty("pause_for_inspection"));
			inQueue = config.props.getProperty("TEST_CASE_23_QUEUE_IN");
			inCSVQueue = config.props.getProperty("TEST_CASE_23_QUEUE_IN_CSV");
			outQueue = config.props.getProperty("TEST_CASE_23_QUEUE_OUT");
			err = config.props.getProperty("TEST_CASE_23_QUEUE_ERR");

		} catch (Exception e) {
			e.printStackTrace();
			fail("Could not parse test case parameters");
		}

		assertNotNull(sender);
		assertNotNull(senderCSV);
		assertNotNull(recv);
		assertNotNull(recv2);
		assertNotNull(recvErr);

		// Clear the queue of any existing messages
		assertTrue(sender.clearQueue());
		assertTrue(senderCSV.clearQueue());
		assertTrue(recv.clearQueue());
		assertTrue(recv2.clearQueue());
		assertTrue(recvErr.clearQueue());

		//Check the queues are empty
		assertEquals(0, sender.getDepth());
		assertEquals(0, senderCSV.getDepth());
		assertEquals(0, recv.getDepth());
		assertEquals(0, recv2.getDepth());
		assertEquals(0, recvErr.getDepth());
	}

	@Test
	@DisplayName("Test Case 23.A")
	void testCase_23A() throws Exception {

		// Setup the logging for the test case
		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName);
		logHeader(testCaseName, "Send messages to the input queue");


		//Get the configure mix of good and bad messages
		ArrayList<Element> messages = messMgr.getRandomFlightWithResources("SITA", "AODB", numMessages, true);

		logln("");
		logln(String.format("Configured to send %s messages to %s",numMessages,inQueue));
		logln("");

		int i = 0;
		for (Element el : messages) {

			el = updateStats(el);

			// Interval between sending messages
			pause(messageInterval);


			//Create random string for the correlation ID of the message
			final String uuid = UUID.randomUUID().toString().replace("-", "");
			el.getAttribute("CorrelationID").setValue(uuid);

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
					sentSequence.add(message);
				});
			} catch (Exception e) {
				e.printStackTrace();
				fail("Sender Error");
			}
		}

		logln("");
		logFinalSendResults(goodSend.intValue());

		assertEquals(sender.getMessageSentCount(), numMessages.intValue());
		logln(String.format("Sent %s valid messages to %s", goodSend, inQueue));

		//Disconnect the sender from the queue manager
		assertTrue(sender.disconnect());

		//Log successful completion
		logSuccess(testCaseName);
	}

	@Test
	@DisplayName("Test Case 23.B")
	void testCase_23B() throws Exception {

		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName);
		logHeader(testCaseName);
		logln("Read in from the queue");
		
		pauseForInspection();

		MReceiver recv = new MReceiver(outQueue);
		assertNotNull(recv);

		MReceiver recvIn = new MReceiver(inQueue);
		assertNotNull(recvIn);


		while (recv.mGet(messageWait) != null) {
			logln(String.format("Message from %s", outQueue));
			logln(recv.getMessageContent());
			recvSequence.add(recv.getMessageContent());
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

	@Test
	@DisplayName("Test Case 23.C")
	void testCase_23C() throws Exception {

		// Setup the logging for the test case
		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName);
		logHeader(testCaseName, "Check that the sent and received messages are equivalent");

		assertEquals(sentSequence.size(), recvSequence.size());
		logln("Number sent and number received are equal");
		logln("");

		for (int i = 1; i <= sentSequence.size(); i++) {
			logln(String.format("Sequence Number %s. Sent Message: \n\n%s \n\nReceived Message:\n\n%s \n", i, sentSequence.get(i-1), recvSequence.get(i-1)));
			logln("");
		}
		logln("");
		logln("Manually check that the above messages are eqivalent for the selected fields");


		logSuccess(testCaseName);

	}

	@Test
	@DisplayName("Test Case 23.D")
	void testCase_23D() throws Exception {
		
		// Setup the logging for the test case
		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName);
		logHeader(testCaseName, "Read a CSV file and send record to message flow");

		
		String line;

		logln(String.format("Reading CSV file %s", csvFileName));
		logln("");
		FileReader fr = new FileReader(csvFileName);
		BufferedReader br = new BufferedReader(fr);
		while((line = br.readLine()) != null) {
			senderCSV.mqPut(line);
			logln(String.format("Sending record '%s' to %s", line, inCSVQueue));
		}
		logln("");
		logln("Sending records complete");
		
		 recvErr = new MReceiver(err);
		 
		while (recv2.mGet(messageWait) != null) {
			logln(String.format("++ Read Correctly Formatted Record from %s", outQueue));
			logln(recv2.getMessageContent());
			logln("");
		}
		while (recvErr.mGet(messageWait) != null) {
			logln(String.format("-- Read Incorrectly Formatted Record from %s", err));
			logln(recvErr.getMessageContent());
			logln("");
		}		
		logln("Reading CSV Records Completed");
		logln("");
		logln(String.format("Read %s correctly formatted records", recv2.getMessageRecvCount()));
		logln(String.format("Read %s incorrectly formatted records", recvErr.getMessageRecvCount()));
		
		br.close();
		logSuccess(testCaseName);
	}

}
