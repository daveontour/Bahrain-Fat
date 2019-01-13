/*
 *  Test Case 10:
 *  
 *  Demonstrated multiple inputs to a single output (Many to One)
 *  
 *  Consists of single sub test cases
 * 
 *  10.A Sends the configured number of messages to the input queue
 *    
 *  The message flow has a 4  input queue nodes and  2 HTTP input nodes
 *  connected to a validation node which check compliance to the AIDX standard. 
 *  Valid messages are routed to the output node
 *  A reply is waited on by all the sends
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

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Random;

import org.jdom2.Element;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import au.quaystsems.bah.iib.fat.HTTPSender;
import au.quaystsems.bah.iib.fat.IIBTestCaseBase;
import au.quaystsems.bah.iib.fat.MReceiver;
import au.quaystsems.bah.iib.fat.MSender;

class IIBTestCase10 extends IIBTestCaseBase{

	//MQ Queue Names
	private static String inQueue1, inQueue2,inQueue3,inQueue4;
	private static String errQueue,outQueue;

	//The URL of the HTTP input nodes (Configurable)
	private static String ws1, ws2; 

	// MQ input node senders
	private static MSender sender1,sender2,sender3, sender4;
	private static MSender[] senders;

	//HTTP Input node senders
	private static HTTPSender httpSender1;
	private static HTTPSender httpSender2;
	private static HTTPSender[] httpSenders;

//	//MQ Output node readers
//	private static MReceiver err = new MReceiver(errQueue);
//	private static MReceiver recv = new MReceiver(outQueue);
	

	private static Integer messageInterval = 0;  //Interval between sending messages
	

	public IIBTestCase10() {
		super();
	}

	@BeforeAll
	public  static void setUpClass() throws Exception {

		// Set up the preconditions, includes preparing the AIDX messages
		setUpTest();

		// Read in the configurable parameters
		try {		
			messageInterval = Integer.parseInt(config.props.getProperty("TEST_CASE_10_MESSAGE_INTERVAL"));
			numMessages = Integer.parseInt(config.props.getProperty("TEST_CASE_10_NUM_MESSAGES"));
			numBadMessages = Integer.parseInt(config.props.getProperty("TEST_CASE_10_NUM_BAD_MESSAGES"));
			inQueue1 = config.props.getProperty("TEST_CASE_10_QUEUE_IN.1");
			inQueue2 = config.props.getProperty("TEST_CASE_10_QUEUE_IN.2");
			inQueue3 = config.props.getProperty("TEST_CASE_10_QUEUE_IN.3");
			inQueue4 = config.props.getProperty("TEST_CASE_10_QUEUE_IN.4");
			errQueue = config.props.getProperty("TEST_CASE_10_QUEUE_ERR");
			outQueue = config.props.getProperty("TEST_CASE_10_QUEUE_OUT");
			ws1 = config.props.getProperty("TEST_CASE_10_WS1_URL");
			ws2 = config.props.getProperty("TEST_CASE_10_WS2_URL");
		} catch (Exception e) {
			e.printStackTrace();
			fail("Could not parse test case parameters");
		}

		httpSender1 = new HTTPSender(ws1,null);
		httpSender2 = new HTTPSender(ws2,null);

		httpSenders = new HTTPSender[]{new HTTPSender(ws1,null), new HTTPSender(ws2,null)};

		sender1 = new MSender(inQueue1);   
		sender2 = new MSender(inQueue2);   
		sender3 = new MSender(inQueue3);   
		sender4 = new MSender(inQueue4);
		senders = new MSender[]{sender1, sender2,sender3, sender4};

		//HTTP Input node senders
		
		//MQ Output node readers
		err = new MReceiver(errQueue);
		recv = new MReceiver(outQueue);

		
		// Check all the sender and receivers for the input and output queues
		assertNotNull(sender1);
		assertNotNull(sender2);
		assertNotNull(sender3);
		assertNotNull(sender4);
		assertNotNull(err);
		assertNotNull(recv);

		assertNotNull(httpSender1);
		assertNotNull(httpSender2);

		//Clear the queue of any existing messages
		assertTrue(sender1.clearQueue());
		assertTrue(sender2.clearQueue());
		assertTrue(sender3.clearQueue());
		assertTrue(sender4.clearQueue());
		assertTrue(recv.clearQueue());
		assertTrue(err.clearQueue());
	}


	@Test
	@DisplayName("Test Case 10.A")
	void testCase_10A() throws Exception {

		// Setup the logging for the test case
		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName);
		logHeader(testCaseName);

		ArrayList<Element> messages = messMgr.getGoodBadRandomFlight("SITA", "AODB", numMessages, numBadMessages);
		logln(String.format("Congigured to send %s Valid messages", numMessages));
		logln(String.format("Congigured to send %s In Valid messages", numBadMessages));
		logln("");


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

			//Randomly select which input to send the message to
			if (new Random().nextInt(21)%3 > 0) {
				
				//Send the message by one of the MQ inputs
				//Send the message via one of the MQ Queues				
				final MSender sender = senders[new Random().nextInt(4)];
				
//				//Create the "ReplyTo" temporary queue
				MReceiver recv = new MReceiver();
				assertNotNull(recv);

				sender.setReplyToQueue(recv.queue.getName());

				// Send the message on the selected MQ input queue
				try {
					assertTimeout(ofMillis(2000), () -> {
						assertTrue(sender.mqPut(message));
					});
				} catch (Exception e) {
					e.printStackTrace();
					fail("Sender Error");
				}
				
				recv.mGet(messageWait,true); 
				logln(String.format("Message %s sent via %s -> Response Message on %s: %s", correlID, sender.qName,recv.queue.getName().trim(), recv.getMessageContent()));
				recv.queue.close();
			
			} else {
				
				//Send the message to one of the HTTP Inputs
				try {
					HTTPSender hs = httpSenders[new Random().nextInt(2)];
					hs.setMessage(message);
					String reply = hs.send();
					logln(String.format("Message %s sent via %s -> Response Message: %s", correlID, hs.getUrl(), reply));
					
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		logln("");
		logln("Message Sending Complete");
		logln("");
		
		int mq = 0, rest = 0;
		for (MSender s : senders) {
			logln(String.format("MQ Input Queue %s. Sent %s messages", s.qName,s.getMessageSentCount()));
			mq = mq + s.getMessageSentCount();
		}
		for (HTTPSender s : httpSenders) {
			logln(String.format("HTTP Input Service %s. Sent %s messages", s.getUrl(),s.getMessageSentCount()));
			rest = rest + s.getMessageSentCount();
		}
		
		logln("");
		logln(String.format("MQ messages = %s, HTTP messages = %s", mq,rest));
		logln("");

	
		assertTimeout(ofMillis(2000), () -> {
			while(numMessages.intValue() != recv.getDepth()) {
				pause(5);
			}
		});

		//Check all the messages were received at the output queue
		assertEquals(numMessages.intValue(), recv.getDepth());
		logln(String.format("Congigured Number of Valid messages (%s) recieved at %s", numMessages, outQueue));

		//Make sure there number of error is as expected
		assertEquals(numBadMessages.intValue(), err.getDepth());
		logln(String.format("Congigured Number of In Valid messages (%s) recieved at %s", numBadMessages, errQueue));

		//Disconnect the sender from the queue manager
		assertTrue(sender1.disconnect());
		assertTrue(sender2.disconnect());
		assertTrue(sender3.disconnect());
		assertTrue(sender4.disconnect());
		assertTrue(recv.disconnect());
		assertTrue(err.disconnect());

		//Log successful completion
		logSuccess(testCaseName);
	}
}