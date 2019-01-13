/*
 *  Test Case 19:
 *  
 *  Consists of 2 related test cases
 * 
 *  19.A Sends a well formatted AIDX messages to the input queue
 *  
 *  19.B Wait to read the messages from the output queues. 
 *  
 *  The message flow has a trigger that fires every 30sec
 *  When the trigger fires, the current time is checked.
 *  If the current time is within 30 seconds of the configured time
 *  then the messages are read from the input queue and passed to 
 *  the output queue.
 *  
 *  The time for the message sending from the input to the output queue
 *  can be fonfigured on the live system by setting the User Define Parameter TransferMessageTime
 *  This is done in the Web UI.
 *  
 *  Message Flow:
 *   IIB_FAT_TC_19.msgflow
 *  
 *  Queues:
 *   BAH.IIB.FAT.TC19.QUEUE.IN
 *   BAH.IIB.FAT.TC19.QUEUE.OUT
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


class IIBTestCase25 extends IIBTestCaseBase{

	private static String inQueueA,outQueueA,inQueueB,outQueueB;

	private static MSender senderA, senderB;   
	private static MReceiver recvA, recvB;


	public static boolean stopFlag = false;

	private static Integer messageInterval;  //Interval between sending messages


	public IIBTestCase25() {
		super();
	}

	@BeforeAll
	public  static void setUpClass() throws Exception {

		setUpTest();

		try {
			messageInterval = Integer.parseInt(config.props.getProperty("TEST_CASE_25_MESSAGE_INTERVAL"));
			messageWait = Integer.parseInt(config.props.getProperty("TEST_CASE_25_MESSAGE_WAIT_TIME"));
			inQueueA = config.props.getProperty("TEST_CASE_25_QUEUE_IN_A");
			outQueueA = config.props.getProperty("TEST_CASE_25_QUEUE_OUT_A");
			inQueueB = config.props.getProperty("TEST_CASE_25_QUEUE_IN_B");
			outQueueB = config.props.getProperty("TEST_CASE_25_QUEUE_OUT_B");
		} catch (Exception e) {
			e.printStackTrace();
			fail("Could not parse test case parameters");
		}

		senderA = new MSender(inQueueA);   
		recvA = new MReceiver(outQueueA);
		senderB = new MSender(inQueueB);   
		recvB = new MReceiver(outQueueB);

		assertNotNull(senderA);
		assertNotNull(recvA);
		assertNotNull(senderB);
		assertNotNull(recvB);

		// Clear the queue of any existing messages
		assertTrue(senderA.clearQueue());
		assertTrue(recvA.clearQueue());
		assertTrue(senderB.clearQueue());
		assertTrue(recvB.clearQueue());

		//Check the queues are empty
		assertEquals(0, senderA.getDepth());
		assertEquals(0, recvA.getDepth());
		assertEquals(0, senderB.getDepth());
		assertEquals(0, recvB.getDepth());
	}

	@Test
	@DisplayName("Test Case 25.A")
	void testCase_25A() throws Exception {

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


					try {
						assertTimeout(ofMillis(2000), () -> {
							assertTrue(senderA.mqPut(message));
						});
					} catch (Exception e) {
						e.printStackTrace();
						fail("Sender Error");
					}


					while (!stopFlag) {
						if(recvA.mGet(messageWait) != null) {
							logln(String.format("-Sent Message to flow 25A, Correlation ID = %s, Message flow 25A Received Message, CorrelationID = %s", correlID, recvA.getCorrelationId()));
							break;
						} else {
							logln("");
							logln(String.format("*** No Messages from flow 25A. Waiting on %s ***", correlID));	
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
							logln(String.format("+Sent Message to flow 25B, Correlation ID = %s, Message flow 25B Received Message, CorrelationID = %s", correlID, recvB.getCorrelationId()));
							break;
						} else {
							logln("");
							logln(String.format("*** No Messages from flow 25B. Waiting on %s ***", correlID));	
							logln("");
						}
					}

				}
			}
		};

		Thread threadC =  new Thread() {
			public void run() {
				Scanner scanner = new Scanner( System.in );
				System.out.println( "\nHit any key to terminate: \n" );
				scanner.nextLine();

				scanner.close();

				stopFlag = true;

			}
		};

		threadC.start();
		threadA.start();
		pause(3000);
		threadB.start();

		threadA.join();
		threadB.join();




		//Log successful completion
		logSuccess(testCaseName);
	}

}
