/*
 *  Test Case 5:
 *  
 *  Consists of a single test cases
 * 	
 *  The test case starts a thread to read from the output queue of the 
 *  associated message flow.
 *  The test case then sends the configured number of messages to the 
 *  input queue. 
 *  The thread reading the messages reports on the rate at which messages are received
 *  
 *  Message throttling is achieved by setting the maximum message rate on the 
 *  operational policy assigned to the message flow in the IB Web Tool
 *  
 *  Message Flow:
 *   IIB_FAT_TC_5.msgflow
 *  
 *  Queues:
 *   BAH.IIB.FAT.TC5.QUEUE.IN
 *   BAH.IIB.FAT.TC5.QUEUE.OUT
 *   BAH.IIB.FAT.TC5.QUEUE.ERR
 *   
 *  The message flow includes a validation node to check that the 
 *  message received is a valid AIDX message. 
 *  
 */
package au.quaystsems.bah.iib.fat.testcases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;

import org.jdom2.Element;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import au.quaystsems.bah.iib.fat.IIBTestCaseBase;
import au.quaystsems.bah.iib.fat.MBase;
import au.quaystsems.bah.iib.fat.MReceiver;
import au.quaystsems.bah.iib.fat.MSender;


class IIBTestCase05 extends IIBTestCaseBase{

	public IIBTestCase05() {
		super();
	}

	@BeforeAll
	public  static void setUpClass() throws Exception {

		setUpTest();

		try {
			messageInterval = Integer.parseInt(config.props.getProperty("TEST_CASE_5_MESSAGE_INTERVAL"));
			numMessages = Integer.parseInt(config.props.getProperty("TEST_CASE_5_NUM_GOOD_MESSAGES"));
			messageWait = Integer.parseInt(config.props.getProperty("TEST_CASE_5_MESSAGE_WAIT_TIME"));
			reportingFrequncy = Integer.parseInt(config.props.getProperty("TEST_CASE_5_REPORTING_FREQUENCY"));
			inQueue = config.props.getProperty("TEST_CASE_5_QUEUE_IN");
			outQueue = config.props.getProperty("TEST_CASE_5_QUEUE_OUT");
			errQueue = config.props.getProperty("TEST_CASE_5_QUEUE_ERR");

		} catch (Exception e) {
			e.printStackTrace();
			fail("Could not parse test case parameters");
		}
		
		sender = new MSender(inQueue);   
		recv = new MReceiver(outQueue);
		err = new MReceiver(errQueue);

		//Check, validate and empty queues 
		validateAndEmptyQueues(new MBase[] {sender,recv, err});
	}

	@Test
	@DisplayName("Test Case 5.A")
	void testCase_5A() throws Exception {

		// Setup the logging for the test case
		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName);
		logHeader(testCaseName, "Send the configured number of messages to the input queue");

		logln("Startting thread the read the messages as they are sent and to calculate message rate");
		
		Thread readThread = readThreadFactory(recv, null,"",false);
		readThread.start();

		//Get the configure mix of good and bad messages
		ArrayList<Element> messages = messMgr.getGoodBadRandomFlight("SITA", "AODB", numMessages, 0);

		logln("");
		logln(String.format("Configured to send %s good messages to %s",numMessages,inQueue));
		logln("");
		
		sendMessages(messages, sender, false, true);

		logFinalSendResults(sender.getMessageSentCount());

		assertEquals(sender.getMessageSentCount(), numMessages.intValue());
		logln(String.format("Sent %s valid messages to %s", sender.getMessageSentCount(), inQueue));

		//Disconnect the sender from the queue manager
		assertTrue(sender.disconnect());

		logln("");
		logln("Waiting for all of the messages to be read");
		readThread.join();
		logln("");
		logln("Message reading complete");
		logln("");
		logln(String.format("Final cummulative rate = %s messages per second", cummulativeRate));

		//Log successful completion
		logSuccess(testCaseName);
	}
}
