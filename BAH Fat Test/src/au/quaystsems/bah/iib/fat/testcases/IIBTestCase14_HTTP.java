/*
 *  Test Case 10:
 *  
 *  Demonstrated multiple inputs to a single output (Many to One)
 *  
 *  Consists of single sub test cases
 * 
 *  10.A Sends the configured number of messages to the input queue
 *    
 *  The message flow has a 4  input queue nodes and  4 HTTP input nodes
 *  connected to a validation node which check compliance to the AIDX standard. 
 *  Valid messages are routed to the output node
 * 
 *  Message Flow:
 *   IIB_FAT_TC_10.msgflow
 *  
 *  Queues:
 *   BAH.IIB.FAT.TC10.QUEUE.IN.1
 *   BAH.IIB.FAT.TC10.QUEUE.IN.2
 *   BAH.IIB.FAT.TC10.QUEUE.IN.3
 *   BAH.IIB.FAT.TC10.QUEUE.IN.4
 *   BAH.IIB.FAT.TC10.QUEUE.OUT
 *   BAH.IIB.FAT.TC10.QUEUE.ERR
 *   
 *   
 *  The message flow includes ReplyTo nodes to notify the send of the 
 *  result of the AIDX validation check
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

import au.quaystsems.bah.iib.fat.HTTPSender;
import au.quaystsems.bah.iib.fat.IIBTestCaseBase;
import au.quaystsems.bah.iib.fat.MReceiver;

class IIBTestCase14_HTTP extends IIBTestCaseBase{


	//The URL of the HTTP input nodes (Configurable)
	private static String ws1; 

	//HTTP Input node senders
	private static HTTPSender httpSender1;

	private static Integer messageInterval = 0;  //Interval between sending messages


	public IIBTestCase14_HTTP() {
		super();
	}

	@BeforeAll
	public  static void setUpClass() throws Exception {

		// Set up the preconditions, includes preparing the AIDX messages
		setUpTest();

		// Read in the configurable parameters
		try {		
			messageInterval = Integer.parseInt(config.props.getProperty("TEST_CASE_14A_MESSAGE_INTERVAL"));
			numMessages = Integer.parseInt(config.props.getProperty("TEST_CASE_14A_NUM_MESSAGES"));
			numBadMessages = Integer.parseInt(config.props.getProperty("TEST_CASE_14A_NUM_BAD_MESSAGES"));
			ws1 = config.props.getProperty("TEST_CASE_14A_WS1_URL");
			outQueue = config.props.getProperty("TEST_CASE_14_QUEUE_OUT");
		} catch (Exception e) {
			e.printStackTrace();
			fail("Could not parse test case parameters");
		}

		recv = new MReceiver(outQueue);
		httpSender1 = new HTTPSender(ws1,null);

		// Check all the sender and receivers for the input and output queues
		assertNotNull(recv);
		assertNotNull(httpSender1);

		//Clear the queue of any existing messages
		assertTrue(recv.clearQueue());
	}


	@Test
	@DisplayName("Test Case 14.HTTP")
	void testCase_14HTTP() throws Exception {

		// Setup the logging for the test case
		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName);
		logHeader(testCaseName);

		ArrayList<Element> messages = messMgr.getGoodBadRandomFlight("SITA", "AODB", numMessages, numBadMessages);
		logln(String.format("Congigured to send %s Valid messages", numMessages));
		logln(String.format("Congigured to send %s In Valid messages", numBadMessages));
		logln("");


		Thread readThread =  new Thread() {
			public void run() {
				while (recv.mGet(5000, false) != null) {
					logln(String.format("Received Message on Queue %s. Correleation ID = %s", outQueue, recv.getCorrelationId()));
				}
			}
		};
		// Start the read thread
		readThread.start();

		for (Element el : messages) {

			// Keeps track of the different messages types sent
			el = updateStats(el);			
			String correlID = el.getAttributeValue("CorrelationID");

			// Interval between sending messages
			pause(messageInterval);

			//Get the XML Representation of the message
			String message = getXMLFromMessage(el);

			//Check that the message is not null
			assertNotNull(message);

			// Log the details of the message if required
			logDetail(String.format("Message# %s\nMessage Content:\n%s\n",correlID, message));

			//Send the message to one of the HTTP Inputs
			try {
				HTTPSender hs = httpSender1;
				hs.setMessage(message);
				String reply = hs.send();
				logln(String.format("Message %s sent via %s -> Response Message: %s", correlID, hs.getUrl(), reply));

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		logln("");
		logln("Message Sending Complete");
		logln("");
		logln(String.format("HTTP Input Service %s. Sent %s messages", httpSender1.getUrl(),httpSender1.getMessageSentCount()));
		logln("");
		
		//Wait for the message reading to complete
		readThread.join();

		//Check all the messages were received at the output queue
		assertEquals(numMessages.intValue(), recv.getMessageRecvCount());
		logln(String.format("Configured Number of Valid messages (%s) recieved at %s", numMessages, outQueue));


		//Disconnect the sender from the queue manager
		assertTrue(recv.disconnect());

		//Log successful completion
		logSuccess(testCaseName);
	}
}