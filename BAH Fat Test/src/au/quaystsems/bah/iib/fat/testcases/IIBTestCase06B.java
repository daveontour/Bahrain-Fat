/*
 * Test Case 6.
 * 
 *  To test the throughput of a message flow with 
 *  direct forwarding, no content defendant routing
 *  
 *  Test Case 6.A sends the configured number of AIDX messages
 *  to the configured input queue
 *  
 *  Test Case 6.B reads the messages from the output queue
 *  
 *  Throughput is determined by using the monitoring tools in the 
 *  IIB Web User Interface
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.jdom2.output.Format;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import au.quaystsems.bah.iib.fat.FatUtil;
import au.quaystsems.bah.iib.fat.IIBTestCaseBase;
import au.quaystsems.bah.iib.fat.MBase;
import au.quaystsems.bah.iib.fat.MReceiver;


class IIBTestCase06B extends IIBTestCaseBase{

	private static Integer numSynchronous;
	
	public IIBTestCase06B() {
		super();
	}

	@BeforeAll
	public  static void setUpClass() throws Exception {

		config = FatUtil.getFatUtil();
		xmlOutput.setFormat(Format.getPrettyFormat());		
		logDirectory = config.props.getProperty("log_file_directory");
		logDetailsToFile = Boolean.parseBoolean(config.props.getProperty("log_details_to_file"));
		logToFile = Boolean.parseBoolean(config.props.getProperty("log_to_file"));

		try {
			numSynchronous = Integer.parseInt(config.props.getProperty("TEST_CASE_6_NUMBER_SYNCHRONOUS_READ"));
			messageWait = Integer.parseInt(config.props.getProperty("TEST_CASE_6_MESSAGE_WAIT_TIME"));
			outQueue = config.props.getProperty("TEST_CASE_6_QUEUE_OUT");
		} catch (Exception e) {
			fail("Could not parse test case parameters");
		}
		
		recv = new MReceiver(outQueue);

		//Check, validate and empty queues 
		validateAndEmptyQueues(new MBase[] {recv});
	}

	@Test
	@DisplayName("Test Case 6.B")
	void testCase_6B() throws Exception {

		String testCaseName = new Object(){}.getClass().getEnclosingMethod().getName();
		setUpLogging(testCaseName);
		logHeader(testCaseName);

		
		MReceiver recv = new MReceiver(outQueue);
		recv.receiveAllMessages(this, false, true);
		
		logln("");
		logln(String.format("Read %s messages", recv.getMessageRecvCount()));
		
		assertEquals(recv.getMessageRecvCount(), numMessages.intValue()-numSynchronous.intValue());
		logln("This is equal to the number of outstanding messages from previous testcase");
		
		
		//Log successful completion
		logSuccess(testCaseName);
	}
}
