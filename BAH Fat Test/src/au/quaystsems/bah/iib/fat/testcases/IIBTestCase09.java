/*
 *  Test Case 9:
 *  
 *  Demonstrated the Publish and Subscribe
 *  
 *  Consists of single sub test cases
 * 
 *  9.A Sends the configured number of messages to the input queue
 *    
 *  The message flow has a single input queue connected to a validation
 *  node which check compliance to the AIDX standard. Valid messages are
 *  filtered and routed to Publication nodes. The Topics of the node
 *  relate to the different classes of messages
 *  
 *  Clients connect to the Internal IIB MQTT server at the time the message
 *  is published will received the message if they are subscribed to the
 *  topic it is published on. 
 * 
 *  Message Flow:
 *   IIB_FAT_TC_9.msgflow
 *  
 *  Queues:
 *   BAH.IIB.FAT.TC9.QUEUE.IN
 *   
 *  Topics:
 *   /flightupdate_all
 *   /flightupdate_all_arrival
 *   /flightupdate_all_departure
 *   /flightupdate_all_gf
 *   /flightupdate_gf_arrival
 *   /flightupdate_gf_departure
 *   
 *  The message flow includes a validation node to check that the 
 *  message received is a valid AIDX message. 
 *  
 */
package au.quaystsems.bah.iib.fat.testcases;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;

import org.jdom2.Element;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import au.quaystsems.bah.iib.fat.IIBTestCaseBase;
import au.quaystsems.bah.iib.fat.MBase;
import au.quaystsems.bah.iib.fat.MSender;

class IIBTestCase09 extends IIBTestCaseBase{

	public IIBTestCase09() {
		super();
	}

	@BeforeAll
	public  static void setUpClass() throws Exception {

		setUpTest();

		try {
			messageInterval = Integer.parseInt(config.props.getProperty("TEST_CASE_9_MESSAGE_INTERVAL"));
			numMessages = Integer.parseInt(config.props.getProperty("TEST_CASE_9_NUM_MESSAGES"));
			inQueue = config.props.getProperty("TEST_CASE_9_QUEUE_IN");

		} catch (Exception e) {
			e.printStackTrace();
			fail("Could not parse test case parameters");
		}

		sender = new MSender(inQueue);
		//Check, validate and empty queues 
		validateAndEmptyQueues(new MBase[] {sender});
	}

	/**
	 * @throws Exception
	 */
	@Test
	@DisplayName("Test Case 9.A")
	void testCase_9A() throws Exception {

		// Setup the logging for the test case
		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName);
		logHeader(testCaseName);


		ArrayList<Element> messages = messMgr.getRandomFlight("SITA", "AODB", numMessages);
		logln(String.format("Send %s good messages to %s",numMessages,inQueue));
		
		sendMessages(messages, sender, false, true);

		logFinalSendResults(numMessages.intValue());

		assertTrue(sender.disconnect());
		logSuccess(testCaseName);
	}
}