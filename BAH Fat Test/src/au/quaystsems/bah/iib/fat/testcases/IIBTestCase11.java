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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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


class IIBTestCase11 extends IIBTestCaseBase{

	
	public IIBTestCase11() {
		super();
	}

	@BeforeAll
	public  static void setUpClass() throws Exception {
		
		setUpTest();

		try {
			messageInterval = Integer.parseInt(config.props.getProperty("TEST_CASE_11_MESSAGE_INTERVAL"));
			numMessages = Integer.parseInt(config.props.getProperty("TEST_CASE_11_NUM_GOOD_MESSAGES"));
			numBadMessages = Integer.parseInt(config.props.getProperty("TEST_CASE_11_NUM_BAD_MESSAGES"));
			messageWait = Integer.parseInt(config.props.getProperty("TEST_CASE_11_MESSAGE_WAIT_TIME"));
			pauseForInspection = Boolean.parseBoolean(config.props.getProperty("pause_for_inspection"));
			inQueue = config.props.getProperty("TEST_CASE_11_QUEUE_IN");
			outQueue = config.props.getProperty("TEST_CASE_11_QUEUE_OUT");
			errQueue = config.props.getProperty("TEST_CASE_11_QUEUE_ERR");

		} catch (Exception e) {
			e.printStackTrace();
			fail("Could not parse test case parameters");
		}

		sender = new MSender(inQueue);   
		recv = new MReceiver(outQueue);
		err = new MReceiver(errQueue);


		assertNotNull(sender);
		assertNotNull(recv);
		assertNotNull(err);

		// Clear the queue of any existing messages
		assertTrue(sender.clearQueue());
		assertTrue(recv.clearQueue());
		assertTrue(err.clearQueue());
		
		//Check the queues are empty
		assertEquals(0, sender.getDepth());
		assertEquals(0, recv.getDepth());
		assertEquals(0, err.getDepth());
	}

	/**
	 * @throws Exception
	 */
	@Test
	@DisplayName("Test Case 11.A")
	void testCase_11A() throws Exception {

		// Setup the logging for the test case
		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName);
		logHeader(testCaseName, "Send the configured mix of good and bad messages to the input queue");


		//Get the configure mix of good and bad messages
		ArrayList<Element> messages = messMgr.getGoodBadRandomFlight("SITA", "AODB", numMessages, numBadMessages);
		
		logln("");
		logln(String.format("Configured to send %s good messages and %s bad messages to %s",numMessages,numBadMessages,inQueue));
		logln("");

		sendMessages(messages, sender, false, true);

		
		logln("");
		logFinalSendResults(sender.getMessageSentCount());
		
		assertEquals(sender.getGoodSentCount(), numMessages.intValue());
		logln(String.format("Sent %s valid messages to %s", sender.getGoodSentCount(), inQueue));
		
		assertEquals(sender.getBadSentCount(), numBadMessages.intValue());
		logln(String.format("Sent %s invalid messages to %s", sender.getBadSentCount(), inQueue));

		//Disconnect the sender from the queue manager
		assertTrue(sender.disconnect());

		//Log successful completion
		logSuccess(testCaseName);
	}
	
	@Test
	@DisplayName("Test Case 11.B")
	void testCase_11B() throws Exception {
		
		//Read in the messages
		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName);
		logHeader(testCaseName, "Reads in the send messages from the out put queue (valid messages) and the error queue (invalid messages)");

		
	   pauseForInspection();
	   
		Thread goodThread =  new Thread() {
			public void run() {
				while (recv.mGet(messageWait) != null) {
					logln(String.format("Good Message, Read from %s, CorrelationID = %s", recv.queueName, recv.getCorrelationId()));
				}
			}
		};
		
		err = new MReceiver(errQueue);
		
		Thread badThread =  new Thread() {
			public void run() {
				while (err.mGet(messageWait) != null) {
					logln(String.format("Bad Message, Read from %s, CorrelationID = %s", errQueue, err.getCorrelationId()));
				}
			}
		};
	
		goodThread.start();
		badThread.start();
		
		goodThread.join();
		badThread.join();


		//Clean up connections and make sure expected number of messages were read

		//Disconnect the receiver from the queue manager
		assertTrue(recv.disconnect());
		assertTrue(err.disconnect());

		logln("");
		logln("Receiving messages complete");
		logln("");
		
		assertEquals(numMessages.intValue(), recv.getMessageRecvCount());
		logln(String.format("%s Valid messages read", numMessages.intValue(),recv.getMessageRecvCount()));

		assertEquals(numBadMessages.intValue(), err.getMessageRecvCount());
		logln(String.format("%s Invalid messages read", numBadMessages.intValue(), err.getMessageRecvCount()));
		
		//Log successful completion
		logSuccess(testCaseName);
	}
}
