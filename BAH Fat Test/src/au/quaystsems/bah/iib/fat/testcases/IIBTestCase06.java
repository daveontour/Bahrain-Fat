/*
 * Test Case 6.
 * 
 *  To test the gauranteed delivery of messages
 *  
 *  Test Case 6.A sends the configured number of AIDX messages
 *  to the configured input queue, and then reads the message from the
 *  output queue. However the number of messages read it limited
 *  
 *  Test Case 6.B reads the remaining messages from the output queue
 *  
 *  
 *  Message Flow:
 *   IIB_FAT_TC_6.msgflow
 *  
 *  Queues:
 *   BAH.IIB.FAT.TC6.QUEUE.IN
 *   BAH.IIB.FAT.TC6.QUEUE.OUT
 *   BAH.IIB.FAT.TC6.QUEUE.ERR
 *   
 *  The message flow includes a validation node to check that the 
 *  message received is a valid AIDX message
 *  
 */

package au.quaystsems.bah.iib.fat.testcases;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.jdom2.Element;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import au.quaystsems.bah.iib.fat.IIBTestCaseBase;
import au.quaystsems.bah.iib.fat.MBase;
import au.quaystsems.bah.iib.fat.MReceiver;
import au.quaystsems.bah.iib.fat.MSender;

class IIBTestCase06 extends IIBTestCaseBase{

//	private static String inQueue = "BAH.IIB.FAT.TC6.QUEUE.IN";
//	private static String outQueue = "BAH.IIB.FAT.TC6.QUEUE.OUT";
//
//	private static MSender sender = new MSender(inQueue);
//	private static MReceiver recv = new MReceiver(outQueue);
//	
//	private static Integer numMessages;
//	private static Integer messageInterval;
	private static Integer numSynchronous;
	
	public IIBTestCase06() {
		super();
	}

	@BeforeAll
	public  static void setUpClass() throws Exception {

		setUpTest();

		try {
			numMessages = Integer.parseInt(config.props.getProperty("TEST_CASE_6_NUMBER_MESSAGES"));
			numSynchronous = Integer.parseInt(config.props.getProperty("TEST_CASE_6_NUMBER_SYNCHRONOUS_READ"));
			messageInterval = Integer.parseInt(config.props.getProperty("TEST_CASE_6_MESSAGE_INTERVAL"));
			messageWait = Integer.parseInt(config.props.getProperty("TEST_CASE_6_MESSAGE_WAIT_TIME"));
			inQueue = config.props.getProperty("TEST_CASE_6_QUEUE_IN");
			outQueue = config.props.getProperty("TEST_CASE_6_QUEUE_OUT");

		} catch (Exception e) {
			fail("Could not parse test case parameters");
		}
		
		sender = new MSender(inQueue);   
		recv = new MReceiver(outQueue);

		//Check, validate and empty queues 
		validateAndEmptyQueues(new MBase[] {sender,recv});
	}

	/**
	 * @throws Exception
	 */
	@Test
	@DisplayName("Test Case 6.A")
	void testCase_6A() throws Exception {

		// Setup the logging for the test case
		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName);
		logHeader(testCaseName);


		
		logln(String.format("Send %s messages to the %s",numMessages,inQueue));
		for (int i = 1; i <= numMessages; i++) {

			// Interval between sending messages
			pause(messageInterval);

			Element el = messMgr.getRandomFlight("SITA","AODB");
			String correlID = new Integer(Integer.parseInt(el.getAttributeValue("CorrelationID"))+i).toString();
			el.setAttribute("CorrelationID", correlID);
			String message = getXMLFromMessage(el);

			assertNotNull(message);

			// Log the details of the message if required
			logDetail(String.format("Message# %s\nMessage Content:\n%s\n", i,message));

			/*
			 * Send the message
			 * Send must occur within 2 seconds 
			 */
			try {
				assertTimeout(ofMillis(2000), () -> {
					assertTrue(sender.mqPut(message));
					logln(String.format("Sending Message on %s. CorrelationID = %s", sender.qName,correlID));
				});
			} catch (Exception e) {
				e.printStackTrace();
				fail("Sender Error");
			}
			
			if (recv.getMessageRecvCount() < numSynchronous) {
				if(recv.mGet(messageWait) != null) {
					logln(String.format("Read Message from %s. Correlation ID = %s", recv.queueName,recv.getCorrelationId()));
					logln("");
					// Check that the message sent was the one received
					assertEquals(correlID, recv.getCorrelationId());
				}
			}
		}

		logln("Message Sending Complere");
		logln("");
		
		assertEquals(numMessages.intValue(), sender.getMessageSentCount());
		logln(String.format("%s Configured to send, %s sent", numMessages, sender.getMessageSentCount()));
		logln("");
		logln(String.format("Read %s messages synchronously with the sending", recv.getMessageRecvCount()));
		logln(String.format("Correlation ID of last message read was %s", recv.getCorrelationId()));
		logln("");
		logln("Observe the remaining messages on the queue "+recv.queueName);
		logln("");
		logln("Execute Test Case 6B to read remaining messages");
		

		//Disconnect the sender from the queue manager
		assertTrue(sender.disconnect());

		//Log successful completion
		logSuccess(testCaseName);
	}
}
