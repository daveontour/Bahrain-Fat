/*
 *  Test Case 8:
 *  
 *  Demonstrated the Request/Reply Semantics
 *  
 *  Consists of single sub test cases
 * 
 *  8.A Sends the configured number of messages to the input queue
 *  A dynamic temporary queue is created for the client to listen for the reply 
 *  The message is send and then the response is listened for on
 *  the configured reply queue
 *    
 *  The message flow has a single input queue connected to a validation
 *  node which check compliance to the AIDX standard and outputs valid 
 *  messages to an MQ "ReplyTo" node which sends the message to the 
 *  queue configured in the ReplyToQueue parameter of the message
 * 
 *  Message Flow:
 *   IIB_FAT_TC_8.msgflow
 *  
 *  Queues:
 *   BAH.IIB.FAT.TC8.QUEUE.IN
 *   BAH.IIB.FAT.TC8.QUEUE.OUT
 *   BAH.IIB.FAT.TC8.QUEUE.ERR
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

import java.util.ArrayList;

import org.jdom2.Element;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import au.quaystsems.bah.iib.fat.IIBTestCaseBase;
import au.quaystsems.bah.iib.fat.MBase;
import au.quaystsems.bah.iib.fat.MReceiver;
import au.quaystsems.bah.iib.fat.MSender;

class IIBTestCase08 extends IIBTestCaseBase{

	public IIBTestCase08() {
		super();
	}

	@BeforeAll
	public  static void setUpClass() throws Exception {

		setUpTest();

		try {
			messageInterval = Integer.parseInt(config.props.getProperty("TEST_CASE_8_MESSAGE_INTERVAL"));
			numMessages = Integer.parseInt(config.props.getProperty("TEST_CASE_8_NUM_MESSAGES"));
			numBadMessages = Integer.parseInt(config.props.getProperty("TEST_CASE_8_NUM_BAD_MESSAGES"));
			messageWait = Integer.parseInt(config.props.getProperty("TEST_CASE_8_MESSAGE_WAIT_TIME"));
			inQueue = config.props.getProperty("TEST_CASE_8_QUEUE_IN");
			outQueue = config.props.getProperty("TEST_CASE_8_QUEUE_OUT");
			errQueue = config.props.getProperty("TEST_CASE_8_QUEUE_ERR");
		} catch (Exception e) {
			e.printStackTrace();
			fail("Could not parse test case parameters");
		}

		sender = new MSender(inQueue);   
		recv = new MReceiver(outQueue);
		err = new MReceiver(errQueue);

		//Check, validate and empty queues 
		validateAndEmptyQueues(new MBase[] {sender,recv,err});
	}

	/**
	 * @throws Exception
	 */
	@Test
	@DisplayName("Test Case 8.A")
	void testCase_8A() throws Exception {

		// Setup the logging for the test case
		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName);
		logHeader(testCaseName);


		ArrayList<Element> messages = messMgr.getGoodBadRandomFlight("SITA", "AODB", numMessages, numBadMessages);
		logln(String.format("Congigured to send %s Valid messages", numMessages));
		logln(String.format("Congigured to send %s In Valid messages", numBadMessages));
		logln("");

		int i=0;

		for (Element el : messages) {
			
			i++;

			// Keeps track of the different messages types sent
			el = updateStats(el);			
			String correlID = el.getAttributeValue("CorrelationID");

			try {
				sender.queue.close();
				sender.disconnect();
			} catch (Exception e2) {
				// TODO Auto-generated catch block
				//e2.printStackTrace();
			}

			sender = new MSender(inQueue);  

			// Interval between sending messages
			pause(messageInterval);

			//Get the XML Representation of the message
			String message = getXMLFromMessage(el);

			//Check that the message is not null
			assertNotNull(message);
			// Log the details of the message if required
			logDetail(String.format("Message# %s\nMessage Content:\n%s\n",i, message));

			//Create the "ReplyTo" temporary queue
			MReceiver recv = new MReceiver();
			assertNotNull(recv);
			logln(String .format("Created 'ReplyTo' queue: %s", recv.queue.getName()));


			sender = new MSender(inQueue);   
			sender.setReplyToQueue(recv.queue.getName());

			// Send the request on the input queue
			try {
				assertTimeout(ofMillis(2000), () -> {
					assertTrue(sender.mqPut(message));
				});
			} catch (Exception e) {
				e.printStackTrace();
				fail("Sender Error");
			}

			//Read the reply
			recv.mGet(messageWait,true); 
			logln(String.format("Message %s sent via %s -> Response Message on %s: %s\n", correlID, sender.qName,recv.queue.getName().trim(), recv.getMessageContent()));			

			recv.queue.close();
			
			recv.disconnect();
			sender.disconnect();
		}
		logln("");
		logln("Message Sending Complete");
		logln("");

		logln(String.format("MQ Input Queue %s. Sent %s messages", sender.qName,sender.getMessageSentCount()));

		//Check the number sent was the number that was configured to be sent
		assertEquals(i, numMessages.intValue()+numBadMessages.intValue());
		logFinalSendResults(numMessages.intValue()+numBadMessages.intValue());

		//Check all the messages were received at the output queue
		assertEquals(numMessages.intValue(), recv.getDepth());
		logln(String.format("Congigured Number of Valid messages (%s) recieved at %s", numMessages, outQueue));

		//Make sure there number of error is as expected
		err = new MReceiver(errQueue);
		assertEquals(numBadMessages.intValue(), err.getDepth());
		logln(String.format("Congigured Number of In Valid messages (%s) recieved at %s", numBadMessages, errQueue));

		//Clean up connections and make sure expected number of messages were read
		//Disconnect the sender from the queue manager
		assertTrue(sender.disconnect());

		//Log successful completion
		logSuccess(testCaseName);
	}
}
