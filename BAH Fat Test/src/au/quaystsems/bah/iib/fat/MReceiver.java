package au.quaystsems.bah.iib.fat;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;

import com.ibm.mq.MQException;
import com.ibm.mq.MQGetMessageOptions;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.MQConstants;

public class MReceiver extends MBase{

	private MQMessage theMessage;
	private int messRecvCount = 0;
	private Element root;
	private static Namespace namespace = Namespace.getNamespace("http://www.iata.org/IATA/2007/00");
	private String content;
	public String queueName;
	public long receivingTime = 0;
	private double elapsedMilliseconds;


	public void resetCount() {
		messRecvCount = 0;
	}

	public int getMessageRecvCount() {
		return this.messRecvCount;
	}
	
	public int receiveAllMessages(IIBTestCaseBase testcase, boolean logProgress, boolean logDetail) {
		
		boolean receivedFirst = false;
		DateTime firstRecieivedTime = null;
		
		testcase.logln("");
		testcase.logln(String.format("Starting to read messages from %s", queueName));
		testcase.logln(String.format("Timeout for message read =  %sms", IIBTestCaseBase.messageWait));
		
		while (mGet(IIBTestCaseBase.messageWait) != null) {
			if (!receivedFirst) {
				receivedFirst = true;
				firstRecieivedTime = new DateTime();
			}
			if(logProgress) {
				testcase.logProgress(getMessageRecvCount(), "read");
			}
			if (logDetail) {
				testcase.logln(String.format("Read Message from %s. Correlation ID = %s", queueName, getCorrelationId()));
			}
			testcase.logDetail(getMessageContent());
		}
		
		if (logProgress) {
			testcase.logLast(getMessageRecvCount(), "read");
		} else {
			testcase.logln("");
			testcase.logln("Receiving message complete");
		}
		assertTrue(disconnect());
		
	
		Period p = new Period( firstRecieivedTime, new DateTime(), PeriodType.millis());
		elapsedMilliseconds = p.getMillis() - IIBTestCaseBase.messageWait;
		
		elapsedMilliseconds = Math.abs(elapsedMilliseconds);
		
		
		testcase.logln("");
		testcase.logln(String.format("Received %s messages in %s ms (%s ms/msg)", this.getMessageRecvCount(), elapsedMilliseconds, elapsedMilliseconds/getMessageRecvCount()));
		return this.getMessageRecvCount();
	}

	public MReceiver() {
		try {
			qMgr = new MQQueueManager(config.qmgr);
			int openOptions =  MQConstants.MQOO_INQUIRE | MQConstants.MQOO_INPUT_AS_Q_DEF;
			queue = qMgr.accessQueue("BAH.IIB.FAT.MODEL.REPLY.QUEUE", openOptions, null, null, null);
			queue.setCloseOptions(MQConstants.MQCO_DELETE_PURGE);
		} catch (MQException e) {
			e.printStackTrace();
			fail("Reciever could not access the queue");
		}
	}

	public MReceiver(String q) {
		queueName = q;
		try {
			qMgr = new MQQueueManager(config.qmgr);
			int openOptions =  MQConstants.MQOO_INQUIRE | MQConstants.MQOO_INPUT_AS_Q_DEF;
			queue = qMgr.accessQueue(q, openOptions, null, null, null);
		} catch (MQException e) {
			e.printStackTrace();
			fail("Reciever could not access the queue");
		}
	}
	
	public MReceiver(String q, boolean noFail) throws MQException {
		queueName = q;
		try {
			qMgr = new MQQueueManager(config.qmgr);
			int openOptions =  MQConstants.MQOO_INQUIRE | MQConstants.MQOO_INPUT_AS_Q_DEF;
			queue = qMgr.accessQueue(q, openOptions, null, null, null);
		} catch (MQException e) {
			throw e;
		}
	}

	public MReceiver(String q, int openOptions ) {
		queueName = q;
		try {
			qMgr = new MQQueueManager(config.qmgr);
			queue = qMgr.accessQueue(q, openOptions, null, null, null);
		} catch (MQException e) {
			e.printStackTrace();
			fail("Reciever could not access the queue");
		}
	}	

	public String getMessageContent() {
		if (content == null) {


			try {
				content = theMessage.readStringOfByteLength(theMessage.getDataLength());
			} catch (IOException e) {
				e.printStackTrace();
				fail("Reciver could not read the message contents");
				return ("Error");
			}
		}

		return content;

	}

	public Element getRootElement() {
		String content = null;
		try {
			content = getMessageContent();
			content = content.substring(content.indexOf("<"));
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			System.out.println(content);
			}
		byte[] byteArray = null;
		try {
			byteArray = content.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
		}
		ByteArrayInputStream baos = new ByteArrayInputStream(byteArray);
		SAXBuilder builder = new SAXBuilder();
		Document doc = null;
		try {
			doc = builder.build(baos);
		} catch (JDOMException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Read the correlation ID and 
		Element el = doc.getRootElement();
		this.root = el;
		return this.root;
	}

	public String getCorrelationId() {
		if (this.root == null) {
			getRootElement();
		}

		return this.root.getAttribute("CorrelationID").getValue();
	}
	public String getDeliveringSystem() {
		if (this.root == null) {
			getRootElement();
		}
		return this.root.getChild("DeliveringSystem",namespace).getAttribute("CompanyShortName").getValue();
	}
	public MQMessage mGet(int waitTime) {		
		return mGet(waitTime, false);
	}	

	public MQMessage mGetReadOnly(int waitTime, boolean failOnTimeout) {

		this.root = null;
		this.content = null;

		try {

			theMessage = new MQMessage();
			MQGetMessageOptions gmo = new MQGetMessageOptions();

			//Define the time to wait for messages

			gmo.options = MQConstants.MQGMO_WAIT | MQConstants.MQGMO_BROWSE_NEXT;
			gmo.waitInterval = waitTime;
			queue.get(theMessage,gmo); 
			messRecvCount++;
			

			return theMessage;

		} catch (MQException ex) {
			if ( !failOnTimeout && ex.completionCode == 2 && ex.reasonCode == MQConstants.MQRC_NO_MSG_AVAILABLE) {
				return null;
			} else {
				try {
					System.out.println(String.format("Message read timeout on queue %s", this.queue.getName()));
				} catch (MQException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				fail("Timeout of receive");
			}
		}

		return null;
	}
	
	public MQMessage mGet(int waitTime, boolean failOnTimeout) {

		this.root = null;
		this.content = null;

		try {

			theMessage = new MQMessage();
			MQGetMessageOptions gmo = new MQGetMessageOptions();

			//Define the time to wait for messages

			gmo.options = MQConstants.MQGMO_WAIT;
			gmo.waitInterval = waitTime;
			queue.get(theMessage,gmo); 
			messRecvCount++;
			

			return theMessage;

		} catch (MQException ex) {
//			if ( !failOnTimeout && ex.completionCode == 2 && ex.reasonCode == MQConstants.MQRC_NO_MSG_AVAILABLE) {
			if ( !failOnTimeout) {
				return null;
			} else {
				try {
					System.out.println(String.format("Message read timeout on queue %s", this.queue.getName()));
				} catch (MQException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				fail("Timeout of receive");
			}
		}

		return null;
	}

}


