/*
 *  Test Case 13:
 *  
 *  Consists of 2 related test cases
 * 
 *  13.A Sends and receives messages to two message flows
 *  
 *  The test case starts two threads of execution which write and read
 *  messages from two separate message flows. 
 *  
 *  Both Message flows are configured to accept correctly formatted AIDX
 *  messages. 
 *  
 *  At some stage during the test, IIB_FAT_TC_13B.msgflow is stopped, and
 *  reconfigured to accept a different input message format and re-deployed  
 *  The messages sent to it should then end up in the error queue. Meanwhile
 *  the messages sent to IIB_FAT_TC_13A.msgflow should continue uninterrupted
 *  
 *  
 *  Message Flow:
 *   IIB_FAT_TC_13A.msgflow
 *   IIB_FAT_TC_13B.msgflow
 *  
 *  Queues:
 *   BAH.IIB.FAT.TC13A.QUEUE.IN
 *   BAH.IIB.FAT.TC13A.QUEUE.OUT
 *   BAH.IIB.FAT.TC13A.QUEUE.ERR
 *   BAH.IIB.FAT.TC13B.QUEUE.IN
 *   BAH.IIB.FAT.TC13B.QUEUE.OUT
 *   BAH.IIB.FAT.TC13B.QUEUE.ERR
 *  
 */
package au.quaystsems.bah.iib.fat.testcases;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Scanner;

import org.jdom2.Element;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import au.quaystsems.bah.iib.fat.IIBTestCaseBase;
import au.quaystsems.bah.iib.fat.MReceiver;
import au.quaystsems.bah.iib.fat.MSender;


class IIBTestCase13 extends IIBTestCaseBase{

	private static String inQueueA,outQueueA,inQueueB,outQueueB,errQueueA,errQueueB;
	private static MSender senderA,senderB;   
	private static MReceiver recvA, recvB,errA,errB;

	public static boolean stopFlag = false;

	private static Integer messageInterval;  //Interval between sending messages


	public IIBTestCase13() {
		super();
	}

	@BeforeAll
	public  static void setUpClass() throws Exception {

		setUpTest();

		try {
			messageInterval = Integer.parseInt(config.props.getProperty("TEST_CASE_13_MESSAGE_INTERVAL"));
			messageWait = Integer.parseInt(config.props.getProperty("TEST_CASE_13_MESSAGE_WAIT_TIME"));
			inQueueA = config.props.getProperty("TEST_CASE_13A_QUEUE_IN");
			outQueueA = config.props.getProperty("TEST_CASE_13A_QUEUE_OUT");
			inQueueB = config.props.getProperty("TEST_CASE_13B_QUEUE_IN");
			outQueueB = config.props.getProperty("TEST_CASE_13B_QUEUE_OUT");
			errQueueA = config.props.getProperty("TEST_CASE_13A_QUEUE_ERR");
			errQueueB = config.props.getProperty("TEST_CASE_13B_QUEUE_ERR");
		} catch (Exception e) {
			e.printStackTrace();
			fail("Could not parse test case parameters");
		}
		
		senderA = new MSender(inQueueA);   
		recvA = new MReceiver(outQueueA);
		senderB = new MSender(inQueueB);   
		recvB = new MReceiver(outQueueB);

		errA = new MReceiver(errQueueA);
		errB = new MReceiver(errQueueB);

		assertNotNull(senderA);
		assertNotNull(recvA);
		assertNotNull(senderB);
		assertNotNull(recvB);
		assertNotNull(errA);
		assertNotNull(errB);

		// Clear the queue of any existing messages
		assertTrue(senderA.clearQueue());
		assertTrue(recvA.clearQueue());
		assertTrue(senderB.clearQueue());
		assertTrue(recvB.clearQueue());
		assertTrue(errA.clearQueue());
		assertTrue(errB.clearQueue());

		//Check the queues are empty
		assertEquals(0, senderA.getDepth());
		assertEquals(0, recvA.getDepth());
		assertEquals(0, senderB.getDepth());
		assertEquals(0, recvB.getDepth());
		assertEquals(0, errA.getDepth());
		assertEquals(0, errB.getDepth());
	}

	@Test
	@DisplayName("Test Case 13.A")
	void testCase_13A() throws Exception {

		// Setup the logging for the test case
		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName);
		logHeader(testCaseName, "Send messages to the input queue, and read them from the output queue");


		Thread threadA =  new Thread() {
			public void run() {

				int i = 0;
				while (!stopFlag) {

					pause(messageInterval);

					i++;

					Element el = messMgr.getRandomFlightWithResources("SITA", "AODB", 1, false).get(0);
					String correlID = el.getAttributeValue("CorrelationID")+i;
					el.setAttribute("CorrelationID", correlID);
					final String message = getXMLFromMessage(el);


					//Send the message
					try {
						assertTimeout(ofMillis(2000), () -> {
							assertTrue(senderA.mqPut(message));
						});
					} catch (Exception e) {
						e.printStackTrace();
						fail("Sender Error");
					}


					//Wait for the sent message to be read at the output node
					while (!stopFlag) {
						if(recvA.mGet(messageWait) != null) {
							logln(String.format("-Sent Message to flow 13A, Correlation ID = %s, Message flow 13A Received Message, CorrelationID = %s", correlID, recvA.getCorrelationId()));
							break;
						} else {
							logln("");
							logln(String.format("*** No Messages from flow 13A. Waiting on %s ***", correlID));	
							logln("");
						}
					}

				}
			}
		};

		Thread threadB =  new Thread() {
			public void run() {

				int i = 0;
				while (!stopFlag) {

					pause(messageInterval);
					i++;
					Element el = messMgr.getRandomFlightWithResources("SITA", "AODB", 1, false).get(0);
					String correlID = "4321"+i;
					el.setAttribute("CorrelationID", correlID);
					final String message = getXMLFromMessage(el);


					try {
						assertTimeout(ofMillis(2000), () -> {
							assertTrue(senderB.mqPut(message));
						});
					} catch (Exception e) {
						e.printStackTrace();
						fail("Sender Error");
					}


					while (!stopFlag) {
						if(recvB.mGet(messageWait) != null) {
							logln(String.format("+Sent Message to flow 13B, Correlation ID = %s, Message flow 13B Received Message, CorrelationID = %s", correlID, recvB.getCorrelationId()));
							//							logln(recvB.getMessageContent());
							break;
						} else {

							//If the message does'nt turn up, check the error queue for it.
							logln("");
							logln(String.format("*** No Messages from flow 13B. Waiting on %s ***", correlID));	
							if(errB.mGet(messageWait) != null) {
								logln(String.format("->->Sent Message to flow 13B, Correlation ID = %s, Received on error queue, CorrelationID = %s", correlID, errB.getCorrelationId()));
								logln("");
								break;
							}
						}
					}
				}
			}
		};

		// Wait for keyboard input. Once there is input, terminate the other threads
		Thread threadC =  new Thread() {
			public void run() {
				@SuppressWarnings("resource")
				Scanner scanner = new Scanner( System.in );
				System.out.println( "\nHit any key to terminate: \n" );
				scanner.nextLine();
				stopFlag = true;
			}
		};

		//Start the threads
		threadC.start();
		threadA.start();
		threadB.start();

		//Wait for the threads to complete
		threadA.join();
		threadB.join();

		//Log successful completion
		logSuccess(testCaseName);
	}
}
