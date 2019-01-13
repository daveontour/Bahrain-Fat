/*
 * Test Case 2.
 * 
 *  To test the throughput of a message flow with 
 *  direct forwarding, no content defendant routing
 *  
 *  Test Case 2.A sends the configured number of AIDX messages
 *  to the configured input queue
 *  
 *  Test Case 2.B reads the valid AIDX messages from the output queue
 *  Test Case 2.C reads the invalid AIDX messages from the error queue
 *  
 *  Throughput is determined by using the monitoring tools in the 
 *  IIB Web User Interface
 *  
 *  Message Flow:
 *   IIB_FAT_TC_2.msgflow
 *  
 *  Queues:
 *   BAH.IIB.FAT.TC2.QUEUE.IN
 *   BAH.IIB.FAT.TC2.QUEUE.OUT
 *   BAH.IIB.FAT.TC2.QUEUE.ERR
 *   
 *  The message flow includes a validation node to check that the 
 *  message received is a valid AIDX message
 *  
 *  The test case uses JUnit "asserts" to check the execution of 
 *  the test case against expected results.
 *  The test case fails if any of the asserts fail
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

class IIBTestCase02 extends IIBTestCaseBase{

	public IIBTestCase02() {
		super();
	}

	@BeforeAll
	public  static void setUpClass() throws Exception {

		setUpTest();

		try {
			numMessages = Integer.parseInt(config.props.getProperty("TEST_CASE_2_NUMBER_MESSAGES"));
			numBadMessages = Integer.parseInt(config.props.getProperty("TEST_CASE_2_NUMBER_BAD_MESSAGES"));
			messageInterval = Integer.parseInt(config.props.getProperty("TEST_CASE_2_MESSAGE_INTERVAL"));
			messageWait = Integer.parseInt(config.props.getProperty("TEST_CASE_2_MESSAGE_WAIT_TIME"));
			inQueue = config.props.getProperty("TEST_CASE_2_QUEUE_IN");
			outQueue = config.props.getProperty("TEST_CASE_2_QUEUE_OUT");
			errQueue = config.props.getProperty("TEST_CASE_2_QUEUE_ERR");

		} catch (Exception e) {
			fail("Could not parse test case parameters");
		}

		//Check the message sender/receivers are OK
		
		try {
			sender  = new MSender(inQueue);   
			recv  = new MReceiver(outQueue); 
			err  = new MReceiver(errQueue);
		} catch (Exception e) {
			e.printStackTrace();
			loglnS("Could not connect to the defined queue");
			fail("Could not connect to the defined queue");
		} 
		
		//Check, validate and empty queues 
		validateAndEmptyQueues(new MBase[] {sender,recv, err});
	}

	/**
	 * @throws Exception
	 */
	@Test
	@DisplayName("Test Case 2.A")
	void testCase_2A() throws Exception {

		// Setup the logging for the test case
		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName,"Sends the specified number of good and bad messages to the configured input queue");

		ArrayList<Element> messages = messMgr.getGoodBadRandomFlight("SITA", "AODB", numMessages, numBadMessages);
		logln(String.format("Congigured to send %s Valid messages via %s", numMessages, inQueue));
		logln(String.format("Congigured to send %s Invalid messages via %s", numBadMessages, inQueue));
		logln("");
		
		sendMessages(messages, sender,false, true);

		//Check the number sent was the number that was configured to be sent
		assertEquals(sender.getMessageSentCount(), numMessages.intValue()+numBadMessages.intValue());
		logFinalSendResults(numMessages.intValue()+numBadMessages.intValue());

		//Disconnect the sender from the queue manager
		assertTrue(sender.disconnect());

		//Log successful completion
		logSuccess(testCaseName);
	}

	@Test
	@DisplayName("Test Case 2.B")
	void testCase_2B() throws Exception {

		pauseForInspection();

		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName,"Reads all the messages from the configured output queues");

		recv = new MReceiver(outQueue);
		recv.receiveAllMessages(this, true, false);

		assertEquals(numMessages.intValue(), recv.getMessageRecvCount());
		logln(String.format("Configured Number of Valid messages (%s) recieved at %s", numMessages, outQueue));
		
		err = new MReceiver(errQueue);
		err.receiveAllMessages(this, true, false);

		//Check all the messages were received at the output queue
		assertEquals(numBadMessages.intValue(), err.getMessageRecvCount());
		logln(String.format("Configured Number of Invalid messages (%s) recieved at %s", numBadMessages, errQueue));

		//Log successful completion
		logSuccess(testCaseName);
	}
}
