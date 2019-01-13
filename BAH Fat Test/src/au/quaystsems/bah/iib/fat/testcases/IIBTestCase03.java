/*
 * Test Case 3.
 * 
 *  To test the throughput of a message flow with 
 *  content defendant routing
 *  
 *  Test Case 3.A sends the configured number of AIDX messages
 *  to the configured input queue. The messages are modified 
 *  so that the Delivering systems rotates through AODB,AFTN and VDGS
 *  
 *  Test Case 3.B reads the messages from each of the the output queues
 *  
 *  Throughput is determined by using the monitoring tools in the 
 *  IIB Web User Interface
 *  
 *  Message Flow:
 *   IIB_FAT_TC_3.msgflow
 *  
 *  Queues:
 *   BAH.IIB.FAT.TC3.QUEUE.IN
 *   BAH.IIB.FAT.TC3.QUEUE.AODB.OUT
 *   BAH.IIB.FAT.TC3.QUEUE.AFTN.OUT
 *   BAH.IIB.FAT.TC3.QUEUE.VDGS.OUT
 *   BAH.IIB.FAT.TC3.QUEUE.ERR
 *   
 *  The message flow includes a validation node to check that the 
 *  message received is a valid AIDX message. 
 *  The message flow includes a routing node to route the message to
 *  the corresponding queue based on the DeliveringSystem element
 *  of the AIDX message 
 *  
 */
package au.quaystsems.bah.iib.fat.testcases;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Random;

import org.jdom2.Element;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import au.quaystsems.bah.iib.fat.IIBTestCaseBase;
import au.quaystsems.bah.iib.fat.MBase;
import au.quaystsems.bah.iib.fat.MReceiver;
import au.quaystsems.bah.iib.fat.MSender;


class IIBTestCase03 extends IIBTestCaseBase{

	//Additional queues in use (in addition to in, out and err)
	private static String outAODBQueue;
	private static String outAFTNQueue;
	private static String outVDGSQueue;

	// Message sender and receivers (in addition to in, out and err)
	private static MReceiver recvAODB;
	private static MReceiver recvAFTN;
	private static MReceiver recvVDGS;

	//Counters for the number of messages sent to each system
	private static Integer countAODB = 0;
	private static Integer countVDGS = 0;
	private static Integer countAFTN = 0;

	public IIBTestCase03() {
		super();
	}

	@BeforeAll
	public static void setUpClass() throws Exception {

		setUpTest();

		try {
			numMessages = Integer.parseInt(config.props.getProperty("TEST_CASE_3_NUMBER_MESSAGES"));
			messageInterval = Integer.parseInt(config.props.getProperty("TEST_CASE_3_MESSAGE_INTERVAL"));
			messageWait = Integer.parseInt(config.props.getProperty("TEST_CASE_3_MESSAGE_WAIT_TIME"));
			inQueue = config.props.getProperty("TEST_CASE_3_QUEUE_IN");   
			errQueue = config.props.getProperty("TEST_CASE_3_QUEUE_ERR");  
			outAODBQueue = config.props.getProperty( "TEST_CASE_3_QUEUE_AODB_OUT");
			outAFTNQueue = config.props.getProperty("TEST_CASE_3_QUEUE_AFTN_OUT");
			outVDGSQueue = config.props.getProperty("TEST_CASE_3_QUEUE_VDGS_OUT");
		} catch (Exception e) {
			e.printStackTrace();
			fail("Could not parse test case parameters");
		}
		
		sender  = new MSender(inQueue);  
		err = new MReceiver(errQueue);
		recvAODB = new MReceiver(outAODBQueue);
		recvAFTN = new MReceiver(outAFTNQueue);
		recvVDGS= new MReceiver(outVDGSQueue);
		
		//Check, validate and empty queues 
		validateAndEmptyQueues(new MBase[] {sender,err,recvAODB,recvAFTN, recvVDGS});

	}

	@Test
	@DisplayName("Test Case 3.A")
	void testCase_3A() throws Exception {

		// Setup the logging for the test case
		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName);
		logHeader(testCaseName, "Sends messages to a single input queue. The content of the messagages are adjusted so model the different systems a message would come from");



		// Send the configured number of messages to the queue
		logln(String.format("Send %s messages to the %s",numMessages,inQueue));
		logln("");
		
		for (int i = 1; i <= numMessages; i++) {

			// Interval between sending messages
			pause(messageInterval);

			String org = null, system = null, message = null;

			//Randomly select a system to send the message from.
			switch (new Random().nextInt(3)) {
			case 0:
				org = "SITA";
				system = "AODB";
				countAODB++;
				break;
			case 1:
				org = "FMT";
				system = "VDGS";
				countVDGS++;
				break;
			case 2:
				org = "ATC";
				system = "AFTN";
				countAFTN++;
				break;
			}

			Element el = messMgr.getRandomFlight(org, system);
			//Get the correlationID
			String correlID = new Integer(Integer.parseInt(el.getAttributeValue("CorrelationID"))+i).toString();
			el.setAttribute("CorrelationID", correlID);
			message = getXMLFromMessage(el);

			final String mess = message;
			final String sys = system;

			//Check that the message is not null
			assertNotNull(mess);

			// Log the details of the message if required
			logDetail(String.format("Message# %s\nMessage Content:\n%s\n", i,mess));

			//Send the message
			try {
				assertTimeout(ofMillis(2000), () -> {
					assertTrue(sender.mqPut(mess));
					logln(String.format("Sending Message from %s put on queue %s. CorrelationID = %s",sys, sender.qName, correlID));
				});
			} catch (Exception e) {
				e.printStackTrace();
				fail("Sender Error");
			}
		}

		logLast(sender.getMessageSentCount(), "sent");
		logln(String.format("Sent %s Messages to %s", numMessages, inQueue));
		logln(String.format("Sent %s AODB Messages", countAODB));
		logln(String.format("Sent %s VDGS Messages", countVDGS));
		logln(String.format("Sent %s AFTN Messages", countAFTN));

		//Clean up connections and make sure expected number of messages were read

		//Disconnect the sender from the queue manager
		assertTrue(sender.disconnect());

		//Check the number sent was the number that was configured to be sent
		assertEquals(sender.getMessageSentCount(), numMessages.intValue());

		//Log successful completion
		logSuccess(testCaseName);
	}

	@Test
	@DisplayName("Test Case 3.B")
	void testCase_3B() throws Exception {
		
		pauseForInspection();

		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName);
		logHeader(testCaseName);

		logln("");
		logln("Read in from the output queues");
		logln("");

		MReceiver recvAODB = new MReceiver(outAODBQueue);
		MReceiver recvAFTN = new MReceiver(outAFTNQueue);
		MReceiver recvVDGS = new MReceiver(outVDGSQueue);

		assertNotNull(recvVDGS);
		assertNotNull(recvAODB);
		assertNotNull(recvAFTN);
		
		Thread aodb = this.readThreadFactory(recvAODB,null,"",true);
		Thread aftn = this.readThreadFactory(recvAFTN,null,"",true);
		Thread vdgs = this.readThreadFactory(recvVDGS,null,"",true);

		// Start receivers for each of the queues
		aodb.start();
		aftn.start();
		vdgs.start();
		
		//Wait for the receivers to complete
		aodb.join();
		aftn.join();
		vdgs.join();
		
		logln("");
		logln("Receiving messages complete");
		logln("");
		
		assertEquals(countAODB.intValue(), recvAODB.getMessageRecvCount());
		logln(String.format("%s AODB messages sent, %s AODB messages read", countAODB, recvAODB.getMessageRecvCount()));

		assertEquals(countAFTN.intValue(), recvAFTN.getMessageRecvCount());
		logln(String.format("%s AFTN messages sent, %s AFTN messages read", countAFTN, recvAFTN.getMessageRecvCount()));

		assertEquals(countVDGS.intValue(), recvVDGS.getMessageRecvCount());
		logln(String.format("%s VDGS messages sent, %s VDGS messages read", countVDGS, recvVDGS.getMessageRecvCount()));

		assertEquals(0, err.getDepth());
		logln(String.format("%s Invalid messages sent, %s Invalid messages read", 0 , err.getDepth()));

		//Disconnect the receiver from the queue manager
		assertTrue(recvAODB.disconnect());
		assertTrue(recvAFTN.disconnect());
		assertTrue(recvVDGS.disconnect());
		assertTrue(err.disconnect());
	
		//Log successful completion
		logSuccess(testCaseName);
	}
}
