/*
 * Test Case 7.
 * 
 *  To demonstrate the coordination of transaction within a message flow
 *  
 *   A failed transaction is induced by trying to write to an output
 *   node that is at it's maximum depth
 *  
 *  Test Case 7.A sends the configured number of AIDX messages
 *  to the configured input queue
 *  
 *  The message flow then takes the message and sends it to 2 output queues
 *  One of the output queues is configured with a small depth of only 4
 *  while the other output queue has a depth of 5000
 *  
 *  The messages a placed on the output queues in an transactional manner
 *  therefore when a 5th message is placed on the output queue the transaction
 *  fails, so the message is not committed to the larger queue, although
 *  there is still room for it. 
 *  
 *  The message flow places any messages that failed to be committed onto 
 *  the Error queue. 
 *  
 *  The test case checks that only 4 messages are on both the unlimited and 
 *  limited queues. It also check that the error queue has the messages in excess
 *  of the limite 
 *  
 *  Test Case 7.B uses a different input to demonstrate that messages
 *  can still be placed on the larger output queue when it is not involved
 *  with a rolled back transaction
 *  
 *  Message Flow:
 *   IIB_FAT_TC_7.msgflow
 *  
 *  Queues:
 *   BAH.IIB.FAT.TC7.QUEUE.IN
 *   BAH.IIB.FAT.TC7.QUEUE.OUT.UNLIMITED (limited to a depth of depthLimit)
 *   BAH.IIB.FAT.TC7.QUEUE.OUT.LIMITED (limited to a depth of 10000)
 *   BAH.IIB.FAT.TC7.QUEUE.ERR
 *   BAH.IIB.FAT.TC7.QUEUE.IN.2 A)
 *   
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

import com.ibm.mq.constants.MQConstants;

import au.quaystsems.bah.iib.fat.IIBTestCaseBase;
import au.quaystsems.bah.iib.fat.MBase;
import au.quaystsems.bah.iib.fat.MReceiver;
import au.quaystsems.bah.iib.fat.MSender;


class IIBTestCase07 extends IIBTestCaseBase{

	private static String inQueue;
	private static String inQueue2;
	private static String errorQueue;
	private static String outQueueUnLimited;
	private static String outQueueLimited;


	private static Integer numMessages2;
	private static Integer depthLimit;

	private static ArrayList<String> limitedSequence = new ArrayList<String> ();
	private static ArrayList<String> unlimitedSequence = new ArrayList<String> ();
	private static MSender sender;
	private static MSender sender2;
	private static MReceiver recvUnLimited;
	private static MReceiver recvLimited;
	private static MReceiver recvError;

	public IIBTestCase07() {
		super();
	}

	@BeforeAll
	public  static void setUpClass() throws Exception {

		setUpTest();

		try {
			numMessages = Integer.parseInt(config.props.getProperty("TEST_CASE_7_NUMBER_MESSAGES"));
			numMessages2 = Integer.parseInt(config.props.getProperty("TEST_CASE_7_NUMBER_MESSAGES_2"));
			messageInterval = Integer.parseInt(config.props.getProperty("TEST_CASE_7_MESSAGE_INTERVAL"));
			messageWait = Integer.parseInt(config.props.getProperty("TEST_CASE_7_MESSAGE_WAIT_TIME"));
			depthLimit = Integer.parseInt(config.props.getProperty("TEST_CASE_7_DEPTH_LIMIT"));

			inQueue = config.props.getProperty("TEST_CASE_7_QUEUE_IN");
			inQueue2 = config.props.getProperty("TEST_CASE_7_QUEUE_IN_2");
			errorQueue = config.props.getProperty("TEST_CASE_7_QUEUE_ERR");
			outQueueUnLimited = config.props.getProperty("TEST_CASE_7_QUEUE_OUT_UNLIMITED");
			outQueueLimited = config.props.getProperty("TEST_CASE_7_QUEUE_OUT_LIMITED");

		} catch (Exception e) {
			fail("Could not parse test case parameters");
		}

		sender = new MSender(inQueue);
		sender2 = new MSender(inQueue2);
		recvUnLimited = new MReceiver(outQueueUnLimited, MQConstants.MQOO_INQUIRE | MQConstants.MQOO_INPUT_AS_Q_DEF| MQConstants.MQOO_BROWSE);
		recvLimited = new MReceiver(outQueueLimited, MQConstants.MQOO_INQUIRE| MQConstants.MQOO_INPUT_AS_Q_DEF| MQConstants.MQOO_BROWSE);
		recvError = new MReceiver(errorQueue, MQConstants.MQOO_INQUIRE| MQConstants.MQOO_INPUT_AS_Q_DEF | MQConstants.MQOO_BROWSE);

		//Check, validate and empty queues 
		validateAndEmptyQueues(new MBase[] {sender,sender2,recvUnLimited, recvLimited, recvError});
	}

	@Test
	@DisplayName("Test Case 7.A")
	void testCase_7A() throws Exception {

		// Setup the logging for the test case
		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName,"");

		/*
		 * Send the configured number of messages to the queue
		 */

		logln("");
		logln(String.format("Configured to send %s messages", numMessages));
		logln(String.format("The queue BAH.IIB.FAT.TC7.QUEUE.OUT.LIMITED is configured with a limit of %s messages", depthLimit));
		logln(String.format("Therefore %s messages should appear in both the BAH.IIB.FAT.TC7.QUEUE.OUT.LIMITED and BAH.IIB.FAT.TC7.QUEUE.OUT.UNLIMITED queues",depthLimit));
		logln(String.format("Expecting %s messages in the error queue BAH.IIB.FAT.TC7.QUEUE.ERR", numMessages-depthLimit));
		logln("");


		//Get the configure mix of good and bad messages
		ArrayList<Element> messages = messMgr.getGoodBadRandomFlight("SITA", "AODB", numMessages, 0);

		logln("");
		logln(String.format("Configured to send %s good messages and %s bad messages to %s",numMessages,numBadMessages,inQueue));
		logln("");

		sendMessages(messages, sender, false, true);

		logln("");
		logln("Message sending complete");

		assertEquals(numMessages.intValue(), sender.getMessageSentCount());
		logln(String.format("%s Configured to send, %s sent", numMessages, sender.getMessageSentCount()));
		logln("");

		pauseForInspection();

		while(recvUnLimited.getDepth() != 4 && recvUnLimited.getDepth() != 4 && recvError.getDepth() != numMessages-4) {
			logln("Waiting for worflow to complete");
			pause(1000);
		}

		assertEquals(depthLimit.intValue(), recvUnLimited.getDepth());
		logln(String.format("%s message received on queue %s",  recvUnLimited.getDepth(), recvUnLimited.queueName));

		assertEquals(depthLimit.intValue(), recvLimited.getDepth());
		logln(String.format("%s message received on queue %s", recvLimited.getDepth(), recvLimited.queueName));

		assertEquals(sender.getMessageSentCount()-depthLimit.intValue(), recvError.getDepth());
		logln(String.format("%s message received on queue %s",  recvError.getDepth(), recvError.queueName));

		//Disconnect the sender from the queue manager
		assertTrue(sender.disconnect());

		//Log successful completion
		logSuccess(testCaseName);


		pauseForInspection();
	}

	@Test
	@DisplayName("Test Case 7.B")
	void testCase_7B() throws Exception {

		// Setup the logging for the test case
		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName, "Shows that when the limited queue is not part of the transaction, that messages can still be received by the unlimited queue");

		//Send the required number of messages
		logln("");
		logln(String.format("Configured to send %s messages to BAH.IIB.FAT.TC7.QUEUE.IN.2", numMessages2));
		logln(String.format("This input node does not connect to the Limited depth output node, so no transaction should fail"));
		logln("");
		
		ArrayList<Element> messages = messMgr.getGoodBadRandomFlight("SITA", "AODB", numMessages2, 0, 5432);
		sendMessages(messages, sender2, false, true);


		logln("");
		logln("Message sending complete");
		
		Thread readUnlimitedThread  = this.readThreadFactory(recvLimited,null,"-- Limited Queue",true);
		Thread readLimitedThread  = this.readThreadFactory(recvUnLimited,null,"++ Unlimited Queue",true);


		readUnlimitedThread.start();
		readLimitedThread.start();

		readUnlimitedThread.join();
		readLimitedThread.join();

		assertEquals(numMessages2.intValue(), sender2.getMessageSentCount());
		logln(String.format("%s Configured to send, %s sent", numMessages2, sender2.getMessageSentCount()));
		logln("");

		assertEquals(depthLimit+sender2.getMessageSentCount(), recvUnLimited.getDepth());
		logln(String.format("%s message received on queue BAH.IIB.FAT.TC7.QUEUE.OUT.UNLIMITED (the number from the previous testcase, plus the %s sent)",  recvUnLimited.getDepth(), numMessages2));

		assertEquals(depthLimit.intValue(), recvLimited.getDepth());
		logln(String.format("%s message on queue BAH.IIB.FAT.TC7.QUEUE.OUT.LIMITED (same as before)",  recvLimited.getDepth()));

		recvError = new MReceiver(errorQueue);
		assertEquals(sender.getMessageSentCount()-depthLimit, recvError.getDepth());
		logln(String.format("%s message on queue BAH.IIB.FAT.TC7.QUEUE.ERR (same as before)",  recvError.getDepth()));

		//Disconnect the sender from the queue manager
		assertTrue(sender.disconnect());

		//Log successful completion
		logSuccess(testCaseName);
	}

	@Test
	@DisplayName("Test Case 7.C")
	void testCase_7C() throws Exception {

		
		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName, "Check that the received sequence was the same as the sent sequence");
		

		logln("");
		logln("Check Messages are sent and received in same sequence");
		if (checkSequencesEqual(limitedSequence, unlimitedSequence)) {
			logln("Messages received in same sequence as sent");
		}

		logSuccess(testCaseName);
		// Setup the logging for the test case
	}
}
