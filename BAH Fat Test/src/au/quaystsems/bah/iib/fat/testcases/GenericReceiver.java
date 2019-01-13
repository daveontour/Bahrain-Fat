/*
 *  Generic Receiver
 * 
 *  Execute as a Java application. 
 *  
 *  Asks for input on the queue to connect to reads from that queue. 
 *  
 *  Extends TestCaseBase so it has access to all the properties and logging
 *  functions
 *   
 */

package au.quaystsems.bah.iib.fat.testcases;

import java.io.IOException;

import au.quaystsems.bah.iib.fat.IIBTestCaseBase;
import au.quaystsems.bah.iib.fat.MReceiver;

class GenericReceiver extends IIBTestCaseBase{

	//Queue names used in this test case
	private static String outQueue;
	private static MReceiver recv;

	//Execution parameters
	private static Integer messageWaitInterval;

	public GenericReceiver() {
		super();
	}

	public static void main(String[] args) {

		try {
			Runtime.getRuntime().exec("clear");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}

		System.out.println(" Bahrain IIB FAT  - Generic AIDX Message Receiver");
		System.out.println("\nSelect Options:\n");

		try {
			System.out.print(" 1) Output Queue: "); 
			java.io.BufferedReader r = new java.io.BufferedReader (new java.io.InputStreamReader (System.in));
			outQueue =  r.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		} 

		try {
			System.out.print(" 2) Message Wait Time(ms): "); 
			java.io.BufferedReader r = new java.io.BufferedReader (new java.io.InputStreamReader (System.in));
			String msStr =  r.readLine();
			try {
				messageWaitInterval = Integer.parseInt(msStr);
			} catch (Exception e) {
				messageWaitInterval = 5000;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} 
		
		GenericReceiver r = new GenericReceiver ();
		try {
			setUpTest();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		recv = new MReceiver(outQueue);
		
		r.receive();
		
	}

	private void receive() {
		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName);
		logHeader(testCaseName);

		logln("");
		logln("Read in from the output queue");


		Thread readThread =  new Thread() {
			public void run() {
				logln(String.format("Starting to read messages from %s. Timeout for message read = %sm", recv.queueName, messageWait));
				while (recv.mGet( messageWaitInterval) != null) {
					logln(String.format("Message from %s, CorrelationID = %s", recv.queueName, recv.getCorrelationId()));
				}
			}
		};

	
		// Start receivers for each of the queues
		readThread.start();
		
		//Wait for the receivers to complete
		try {
			readThread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		logln("");
		logln("Receiving messages complete");
		logln("");
		
		logln(String.format("%s messages read",  recv.getMessageRecvCount()));

		//Disconnect the receiver from the queue manager
		recv.disconnect();
		//Log successful completion
		logSuccess(testCaseName);
		
	}

}
