/*
 *  Test Case 14 (TCP):
 *  
 *  Input via a TCP Node
 *  
 *  Consists of single sub test cases
 * 
 *  14.A Sends the configured number of messages to the input queue
 *  The message flow includes several input nodes and this 
 *  particular test case writes to the TCP node
 *    
 * 
 *  Message Flow:
 *   IIB_FAT_TC_14.msgflow
 *  
 *  Queues:
 *   BAH.IIB.FAT.TC14.QUEUE.OUT
 *   
 *  
 */
package au.quaystsems.bah.iib.fat.testcases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.jdom2.Element;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import au.quaystsems.bah.iib.fat.IIBTestCaseBase;
import au.quaystsems.bah.iib.fat.MReceiver;

class IIBTestCase14_TCP extends IIBTestCaseBase{

	private static String tcpHost;
	private static int tcpPort;


	private static Integer messageInterval = 0;  //Interval between sending messages


	public IIBTestCase14_TCP() {
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
			tcpHost = config.props.getProperty("TEST_CASE_14A_TCP_HOST");
			tcpPort = Integer.parseInt(config.props.getProperty("TEST_CASE_14A_TCP_PORT"));
			outQueue = config.props.getProperty("TEST_CASE_14_QUEUE_OUT");

		} catch (Exception e) {
			e.printStackTrace();
			fail("Could not parse test case parameters");
		}
		
		recv = new MReceiver(outQueue);
		
		assertNotNull(recv);

		//Clear the queue of any existing messages
		assertTrue(recv.clearQueue());
	}


	@Test
	@DisplayName("Test Case 14.TCP")
	void testCase_14TCP() throws Exception {

		// Setup the logging for the test case
		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName);
		logHeader(testCaseName);

		ArrayList<Element> messages = messMgr.getGoodBadRandomFlight("SITA", "AODB", numMessages, numBadMessages);
		logln(String.format("Congigured to send %s Valid messages", numMessages));
		logln(String.format("Congigured to send %s In Valid messages", numBadMessages));
		logln("");
		
		logln("Starting thread the read the messages as they are sent");
		logln("");
		
		Thread readThread =  new Thread() {
			public void run() {
				while (recv.mGet(65000, false) != null) {
						logln(String.format("Received Message on Queue %s. Correleation ID = %s", outQueue, recv.getCorrelationId()));
				}
			}
		};
		// Start the read thread
		readThread.start();
		
		//Message Counters
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

			//Send the message via a TCP server
			try {
				sendTCPMessage(message);
				logln(String.format("Message sent to TCP server at %s:%s, Correlation ID = %s", tcpHost, tcpPort,correlID));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		logln("");
		logln("Message Sending Complete");
		logln("");


		readThread.join();
		
		//Check all the messages were received at the output queue
		assertEquals(numMessages.intValue(), recv.getMessageRecvCount());
		logln("");
		logln(String.format("Configured Number of Valid messages (%s) recieved at %s", numMessages, outQueue));

		//Disconnect the sender from the queue manager
		assertTrue(recv.disconnect());

		//Log successful completion
		logSuccess(testCaseName);
	}

	/*
	 * Method to send the message via a simple socket connection
	 * (no retry or connection error correction)
	 */
	public static void sendTCPMessage(String message) throws UnknownHostException, IOException {
		Socket clientSocket = new Socket(tcpHost, tcpPort);
		DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
		outToServer.writeBytes(message);
		clientSocket.close();
	}
}