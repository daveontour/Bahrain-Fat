/*
 *  Generic Sender
 * 
 *  Execute as a Java application. 
 *  
 *  Asks for input on the queue to connect to  and messages to be send
 *  and then does so as configured. 
 *  
 *  Extends TestCaseBase so it has access to all the properties and logging
 *  functions
 *   
 */
package au.quaystsems.bah.iib.fat.testcases;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import org.jdom2.Element;

import au.quaystsems.bah.iib.fat.IIBTestCaseBase;
import au.quaystsems.bah.iib.fat.MSender;

class GenericSender extends IIBTestCaseBase{


	private int badSend = 0;
	private int goodSend = 0;
	private boolean keepGoing = true;

	public GenericSender() {
		super();
	}

	public static void main(String[] args) {

		try {
			Runtime.getRuntime().exec("clear");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}

		System.out.println(" Bahrain IIB FAT  - Generic AIDX Message Sender");
		System.out.println("\nSelect Options:\n");

		try {
			System.out.print(" 1) Input Queue: "); 
			java.io.BufferedReader r = new java.io.BufferedReader (new java.io.InputStreamReader (System.in));
			inQueue =  r.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		} 

		try {
			System.out.print(" 2) Number of good messages (-1 for continuous): "); 
			java.io.BufferedReader r = new java.io.BufferedReader (new java.io.InputStreamReader (System.in));
			String numGoodMessStr =  r.readLine();
			try {
				numMessages = Integer.parseInt(numGoodMessStr);
			} catch (Exception e) {
				numMessages = -1;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} 

		if (numMessages != -1) {
			try {
				System.out.print(" 3) Number of bad messages: "); 
				java.io.BufferedReader r = new java.io.BufferedReader (new java.io.InputStreamReader (System.in));
				String numBadMessStr =  r.readLine();
				try {
					numBadMessages = Integer.parseInt(numBadMessStr);
				} catch (Exception e) {
					numMessages = -1;
				}
			} catch (IOException e) {
				e.printStackTrace();
			} 
		}

		try {
			System.out.print(" 4) Time interval between messages (ms): "); 
			java.io.BufferedReader r = new java.io.BufferedReader (new java.io.InputStreamReader (System.in));
			String msStr =  r.readLine();
			try {
				messageInterval = Integer.parseInt(msStr);
			} catch (Exception e) {
				messageInterval = 0;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} 

		GenericSender send = new GenericSender ();
		try {
			setUpTest();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		sender = new MSender(inQueue);

		if (numMessages == -1) {
			send.continuousSend();
		} else {
			send.specifiedSend();
		}

	}

	private void specifiedSend() {
		// Setup the logging for the test case
		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName);
		logHeader(testCaseName, "Sends the specified number of good and bad messages to the configured input queue");

		ArrayList<Element> messages = messMgr.getGoodBadRandomFlight("SITA", "AODB", numMessages, numBadMessages);
		logln(String.format("Congigured to send %s Valid messages via %s", numMessages, inQueue));
		logln(String.format("Congigured to send %s Invalid messages via %s", numBadMessages, inQueue));
		logln("");

		//Message Counters
		int i=0;

		//Send the configured number of messages to the queue
		for (Element el : messages) {

			// Interval between sending messages
			pause(messageInterval);

			//Get an AIDX formatted message
			String message = getXMLFromMessage(el);

			// Log the details of the message if required
			logDetail(String.format("Message# %s\nMessage Content:\n%s\n", i,message));

			//Send the message
			try {
				sender.mqPut(message);
				String correlID = el.getAttributeValue("CorrelationID");
				if (message.contains("Intentionally_Bad")) {
					logln(String.format("xx Sending Malformed AIDX XML: CorrelationID - %s",correlID));
					badSend++;
				} else {
					logln(String.format("++ Sending Valid AIDX XML: CorrelationID - %s",correlID));
					goodSend++;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		logln("Message Sending Complete");
		logln("");

		logln(String.format("Sent %s valid messages to %s", goodSend, inQueue));
		logln(String.format("Sent %s invalid messages to %s", badSend, inQueue));
		//Disconnect the sender from the queue manager

		//Log successful completion
		logSuccess(testCaseName);

	}

	private void continuousSend() {
		// Setup the logging for the test case
		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName);
		logHeader(testCaseName, "Sends the continuous messages to the configured input queue");


		Thread threadC =  new Thread() {
			public void run() {
				Scanner scanner = new Scanner( System.in );
				System.out.println( "\nHit any key to terminate: \n" );
				scanner.nextLine();

				scanner.close();

				keepGoing = false;

			}
		};

		threadC.start();

		//Message Counters
		int i=0;

		//Send the configured number of messages to the queue
		while (keepGoing ) {

			i++;

			Element el = messMgr.getGoodBadRandomFlight("SITA", "AODB", 1, 0).get(0);
			String correlID = el.getAttributeValue("CorrelationID")+i;
			el.setAttribute("CorrelationID", correlID);
			String message = getXMLFromMessage(el);

			// Interval between sending messages
			pause(messageInterval);

			// Log the details of the message if required
			logDetail(String.format("Message# %s\nMessage Content:\n%s\n", i,message));

			//Send the message
			try {

				sender.mqPut(message);
				if (message.contains("Intentionally_Bad")) {
					logln(String.format("xx Sending Malformed AIDX XML: CorrelationID - %s",correlID));
					badSend++;
				} else {
					logln(String.format("++ Sending Valid AIDX XML: CorrelationID - %s",correlID));
					goodSend++;
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		logln("Message Sending Complete");
		logln("");

		logln(String.format("Sent %s valid messages to %s", goodSend, inQueue));

		//Log successful completion
		logSuccess(testCaseName);

	}

}
