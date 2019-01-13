
package au.quaystsems.bah.iib.fat.testcases;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.Scanner;

import org.jdom2.Element;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import au.quaystsems.bah.iib.fat.IIBTestCaseBase;
import au.quaystsems.bah.iib.fat.MBase;
import au.quaystsems.bah.iib.fat.MReceiver;
import au.quaystsems.bah.iib.fat.MSender;


class IIBTestCase26 extends IIBTestCaseBase{

	public static boolean stopFlag = false;
	private static Integer messageInterval;  //Interval between sending messages

	public IIBTestCase26() {
		super();
	}

	@BeforeAll
	public  static void setUpClass() throws Exception {

		setUpTest();

		try {
			messageInterval = Integer.parseInt(config.props.getProperty("TEST_CASE_26_MESSAGE_INTERVAL"));
			messageWait = Integer.parseInt(config.props.getProperty("TEST_CASE_26_MESSAGE_WAIT_TIME"));
			inQueue = config.props.getProperty("TEST_CASE_26_QUEUE_IN");
			outQueue = config.props.getProperty("TEST_CASE_26_QUEUE_OUT");
		} catch (Exception e) {
			e.printStackTrace();
			fail("Could not parse test case parameters");
		}
		
		sender = new MSender(inQueue);   
		recv = new MReceiver(outQueue);

		//Check, validate and empty queues 
		validateAndEmptyQueues(new MBase[] {sender,recv});

	}

	@Test
	@DisplayName("Test Case 26.A")
	void testCase_26A() throws Exception {

		// Setup the logging for the test case
		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName);
		logHeader(testCaseName, "Send messages to the input queue, and read them from the output queue");



		Thread threadA =  new Thread() {
			public void run() {

				int i = 0;
				while (!stopFlag) {

					pause(messageInterval);

					try {
						sender = new MSender(inQueue, true);
					} catch (Exception e1) {
						logln(String.format(" **Sender** Connection not made to server. Retry in %s ms",messageInterval));
						continue;
					}


					i++;

					Element el = messMgr.getRandomFlightWithResources("SITA", "AODB", 1, false).get(0);
					String correlID = el.getAttributeValue("CorrelationID")+i;
					el.setAttribute("CorrelationID", correlID);
					final String message = getXMLFromMessage(el);

					boolean sentOK = sender.mqPut(message,true,true);
					if (sentOK) {
						logln(String.format("  **Sender** Message sent to %s. CorrelationID = %s", sender.qName, correlID));
					} else {
						i--;
						logln("Sending message failed, Try again in 5 seconds");
					}

					try {
						sender.disconnectFailover();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
					}

				}

			}
		};

		Thread threadB =  new Thread() {
			public void run() {

				while (!stopFlag) {

					pause(messageInterval);

					boolean messAvail = true;
					
					while (messAvail == true) {
						
						try {
							recv = new MReceiver(outQueue, false);
						} catch (Exception e1) {
							logln(String.format("**Receiver** Connection not made to server. Retry in %s ms",messageInterval));
							messAvail = false;
							continue;
						}
						
						try {
							if(recv.mGet(messageWait) != null) {
								logln(String.format(" **Receiver** Message Received On %s, CorrelationID = %s", recv.queueName, recv.getCorrelationId()));
								messAvail = true;
							} else {
								messAvail = false;
							}
							
							try {
								recv.disconnect();
							} catch (Exception e) {
								logln(" ** Receiver ** Server connection problem");
								messAvail = false;
							}
						} catch (Exception e) {
							logln(" ** Receiver ** Server connection problem");
							messAvail = false;
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
		threadB.start();
		threadA.start();

		//Wait for the threads to complete
		threadA.join();
		threadB.join();

		//Log successful completion
		logSuccess(testCaseName);
	}
}
