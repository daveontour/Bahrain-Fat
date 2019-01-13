package au.quaystsems.bah.iib.fat;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

import org.apache.xml.utils.XMLChar;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Seconds;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import au.com.quaysystems.aidx.MessageManager;


public class IIBTestCaseBase {

	protected static FatUtil config;
	protected static MessageManager messMgr;
	protected static int messageWait = 5000;
	protected static boolean verbose;
	protected static boolean logToFile = false;
	protected static String logDirectory;
	protected static boolean logDetailsToFile;
	protected static Namespace namespace = Namespace.getNamespace("http://www.iata.org/IATA/2007/00");

	protected int gulfFlights = 0;
	protected int gulfArrivals = 0;
	protected int gulfDepartures = 0;
	protected int gulfArrivalDepartures = 0;

	protected int allArrivals = 0;
	protected int allDepartures = 0;
	protected int allArrivalDepartures = 0;

	//Execution parameters
	protected static Integer numMessages = 0;
	protected static Integer numBadMessages = 0;
	protected static Integer messageInterval = 0;

	protected static boolean pauseForInspection = true;

	protected static ArrayList<String> goodSendSequence = new ArrayList<String>();
	protected static ArrayList<String> badSendSequence = new ArrayList<String>();
	protected static ArrayList<String> goodRecvSequence = new ArrayList<String>();
	protected static ArrayList<String> badRecvSequence = new ArrayList<String>();

	protected static ArrayList<String> goodSendSequenceMsg = new ArrayList<String>();
	protected static ArrayList<String> badSendSequenceMsg = new ArrayList<String>();
	protected static ArrayList<String> goodRecvSequenceMsg = new ArrayList<String>();
	protected static ArrayList<String> badRecvSequenceMsg = new ArrayList<String>();

	protected static String inQueue,outQueue,errQueue;

	protected static MSender sender = null;   
	protected static MReceiver recv = null;
	protected static MReceiver err = null;

	protected static XMLOutputter xmlOutput = new XMLOutputter();

	protected static int reportingFrequncy = 0;
	protected static Double cummulativeRate = 0.0; 

	private  Writer output;

	public IIBTestCaseBase() {	

	}

	public  static void setUpTest() throws Exception {

		config = FatUtil.getFatUtil();

		xmlOutput.setFormat(Format.getPrettyFormat());

		logDirectory = config.props.getProperty("log_file_directory");
		logDetailsToFile = Boolean.parseBoolean(config.props.getProperty("log_details_to_file"));
		logToFile = Boolean.parseBoolean(config.props.getProperty("log_to_file"));
		pauseForInspection = Boolean.parseBoolean(config.props.getProperty("pause_for_inspection"));
		verbose = Boolean.parseBoolean(config.props.getProperty("verbose"));

		String airportCode = config.props.getProperty("airport_code");
		Integer year = Integer.parseInt(config.props.getProperty("year"));

		prepareAIDXMessages(airportCode, year);
	}

	@BeforeEach
	public void setUp() throws Exception {
		cummulativeRate = 0.0; 
	}

	@AfterEach
	public void tearDown() throws Exception {
		try {
			output.flush();
			output.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@AfterAll
	public static void tearDownClass() throws Exception {
		// Code executed after the last test method 
	}

	protected static void prepareAIDXMessages(String airportCode, int year) {
		logS("Preparing AIDX formatted messages...");
		messMgr = MessageManager.getMessageManager(airportCode, year);
		logS("Preparation Complete\n");		
	}

	protected void setUpLogging(String testcaseName) {

		try {
			output = new BufferedWriter(new FileWriter(logDirectory+"/"+testcaseName+".log"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail("Could not setup log file");
		}  
	}

	protected void setUpLogging(String testcaseName, String subject) {

		try {
			output = new BufferedWriter(new FileWriter(logDirectory+"/"+testcaseName+".log"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail("Could not setup log file");
		}  

		logHeader(testcaseName, subject);

	}

	public static void logS(String s) {
		System.out.print(s);
	}
	public static void loglnS(String s) {
		System.out.printf("[%s] %s\n", new DateTime().withZone(DateTimeZone.UTC),s);
	}

	public  void log(String s) {
		System.out.print(s);
		try {
			if (logToFile && output != null) {
				output.append(s);
			}
		} catch (IOException e) {
			e.printStackTrace();
			fail("Logging to file failed");
		}
	}
	public void logln(String s) {
		System.out.printf("[%s] %s\n", new DateTime().withZone(DateTimeZone.UTC),s);

		try {
			if (logToFile && output != null) {
				output.append(String.format("[%s] %s\n", new DateTime().withZone(DateTimeZone.UTC),s));
			}
		} catch (IOException e) {
			e.printStackTrace();
			fail("Logging to file failed");
		}
	}
	public void logDetail(String s) {
		if (verbose) {
			log(s);
		}
		if (!verbose && logDetailsToFile && logToFile) {
			try {
				output.append(s);
			} catch (IOException e) {
				e.printStackTrace();
				fail("Logging to file failed");
			}
		}
	}
	public void logHeader(String s) {
		logln("");
		logln("****************************");
		logln("*  Executing "+s.toUpperCase().replaceAll("_", " "));
		logln("****************************");
		logln("");
	}

	public void logHeader(String s, String details) {

		String line = "*  ";
		logln("");
		logln("****************************");
		logln("*  Executing "+s.toUpperCase().replaceAll("_", " "));
		logln("*");
		for (String word: details.split(" ")) {
			line = line.concat(word).concat(" ");
			if (line.length() > 60) {
				logln(line);
				line = "*  ";
			}
		}
		logln(line);
		logln("****************************");
		logln("");
	}	

	public void logSuccess(String s) {
		logln("");
		logln("****************************");
		logln("* "+s.toUpperCase().replaceAll("_", " ")+" Succeeded");
		logln("****************************");
		logln("");
	}
	public  void logProgress(int counter, String msg) {
		log(".");
		if (counter%80 == 0) {
			log(String.format(" [%s %s]\n",counter, msg));
		}
	}
	public void logLast(int counter, String msg) {

		if (counter % 80 == 0) {
			logln("");
			return;
		}

		log(".");
		log(String.format(" [%s %s]\n\n",counter, msg));
	}
	public static void pause(int pause) {
		if (pause > 0) {
			try {
				Thread.sleep(pause);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private static String stripInvalidXmlCharacters(String input) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			if (XMLChar.isValid(c)) {
				sb.append(c);
			}
		}

		return sb.toString();
	}

	public String getXMLFromMessage(Element el) {
		StringBuilder sb = new StringBuilder();
		sb.append(xmlOutput.outputString(el));
		sb.append("\n\n");

		return stripInvalidXmlCharacters(sb.toString());
	}

	protected Element updateStats(Element el) {

		Element flID = el.getChild("FlightLeg",namespace).getChild("LegIdentifier", namespace);
		if (flID.getChild("ArrivalAirport", namespace).getText().contains("BAH")) {
			this.allArrivals++;
		}
		if (flID.getChild("DepartureAirport", namespace).getText().contains("BAH")) {
			this.allDepartures++;
		}
		if (flID.getChild("DepartureAirport", namespace).getText().contains( flID.getChild("ArrivalAirport", namespace).getText())) {
			this.allArrivalDepartures++;
		}

		Random rand = new Random();
		// Alter the LegIdentifier so around about a third of the messages are GF Flights
		if ((rand.nextInt(3)+1)%3 == 1) {
			Element airline = flID.getChild("Airline", namespace);
			airline.setText("GF");
			this.gulfFlights++;

			if (flID.getChild("ArrivalAirport", namespace).getText().contains("BAH")) {
				this.gulfArrivals++;
			}
			if (flID.getChild("DepartureAirport", namespace).getText().contains("BAH")) {
				this.gulfDepartures++;
			}
			if (flID.getChild("DepartureAirport", namespace).getText().contains( flID.getChild("ArrivalAirport", namespace).getText())) {
				this.gulfArrivalDepartures++;
			}
		}
		return el;
	}

	protected void logFinalSendResults(int numMessages) {
		logln("");
		logln(String.format("Sent %s total messages",numMessages));
		logln(String.format("Sent %s Arrivals messages",allArrivals));
		logln(String.format("Sent %s Departure messages",allDepartures));
		logln(String.format("Sent %s Arrival Departure messages  (Same airport for arr/dep)",allArrivalDepartures));
		logln("");
		logln(String.format("Sent %s Gulf Air messages",gulfFlights));
		logln(String.format("Sent %s Gulf Air Arrivals messages",gulfArrivals));
		logln(String.format("Sent %s Gulf Air Departure messages",gulfDepartures));
		logln(String.format("Sent %s Gulf Air Arrival Departure messages (Same airport for arr/dep)",gulfArrivalDepartures));
		logln("");
	}

	public void pauseForInspection() {
		if(pauseForInspection) {
			@SuppressWarnings("resource")
			Scanner scanner = new Scanner( System.in );
			System.out.println( "\nPaused to allow inspection of queue.  Hit any key to continue: \n" );
			scanner.nextLine();
		}
	}

	protected Thread readThreadFactory(final MReceiver recv, final ArrayList<String> sequence, final String header, final boolean logDetails) {
		Thread readThread =  new Thread() {
			public void run() {
				DateTime start = new DateTime();
				logln(String.format("Starting to read messages from %s. Timeout for message read = %sm", recv.queueName, messageWait));
				while (recv.mGet(messageWait) != null) {
					if(logDetails) {
						logln(String.format("%s Message from %s received from queue %s, CorrelationID = %s", header, recv.getDeliveringSystem(), recv.queueName, recv.getCorrelationId()));
					}
					if (sequence != null) {
						sequence.add(recv.getCorrelationId());
					}
					if (reportingFrequncy > 0) {
						DateTime now = new DateTime();
						Seconds seconds = Seconds.secondsBetween(start, now);
						if (seconds.getSeconds() > 1 && recv.getMessageRecvCount()%reportingFrequncy == 0) {
							cummulativeRate = new Double(recv.getMessageRecvCount())/ new Double(seconds.getSeconds());
							logln(String.format("Received %s messages in %s seconds ( Cumulative %s msg/sec )", recv.getMessageRecvCount(), seconds.getSeconds(), cummulativeRate));
						}
					}

				}
			}
		};

		return readThread;
	}

	protected void sendMessages(ArrayList<Element> messages, MSender sender, boolean logProgress, boolean logDetail) {

		int i=0;

		//Send the configured number of messages to the queue
		for (Element el : messages) {

			i++;

			// Keeps track of the different messages types sent
			el = updateStats(el);	

			//Get the correlationID
			String ID;
			
			try {
				ID = new Integer(Integer.parseInt(el.getAttributeValue("CorrelationID"))+i).toString();
				el.setAttribute("CorrelationID", ID);
			} catch (NumberFormatException e1) {
				ID = el.getAttributeValue("CorrelationID");
			}

			final String correlID = ID;

			// Interval between sending messages
			pause(messageInterval);

			//Get an AIDX formatted message
			String message = getXMLFromMessage(el);

			//Check that the message is not null
			assertNotNull(message);

			// Log the details of the message if required
			logDetail(String.format("Message# %s\nMessage Content:\n%s\n", i,message));

			final Element el1 = el;
			//Send the message
			try {
				assertTimeout(ofMillis(2000), () -> {
					assertTrue(sender.mqPut(message));
					if (logProgress) {
						logProgress(sender.getMessageSentCount(), "sent");
					}
					if (logDetail) {
						if (sender.isLastSendGood()) {
							logln(String.format("Sent Well Formed AIDX XML to %s: CorrelationID - %s (%s)",sender.qName, correlID, getDescriptor(el1)));
							goodSendSequence.add(correlID);
							goodSendSequenceMsg.add(message);
						} else {
							logln(String.format("Sent Malformed AIDX XML to %s: CorrelationID - %s",sender.qName, correlID));
							badSendSequence.add(correlID);
							badSendSequenceMsg.add(message);
						}
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
				fail("Sender Error");
			}
		}

		if (logProgress) {
			logLast(sender.getMessageSentCount(), "sent");
		}
		logln("Message Sending Complete");
		logln("");
		logln(String.format("MQ Input Queue %s. Sent %s messages", sender.qName,sender.getMessageSentCount()));
	}

	protected static void validateAndEmptyQueues(MBase[] mbases) {

		loglnS("Validating and clearing queues");
		for (MBase mbase: mbases) {
			assertNotNull(mbase);
			assertTrue(mbase.clearQueue());
			assertEquals(0, mbase.getDepth());
		}
		loglnS("Validating and clearing queues complete");
	}

	protected boolean checkSequencesEqual(ArrayList<String> seq1, ArrayList<String> seq2 ) {

		assertEquals(seq1.size(), seq2.size());
		logln(String.format("Number sent and number received are equal (%s)", seq1.size()));
		if (seq1.size() == 0) {
			logln("*** Warning *** Sequence Lists are empty");
			return false;
		}
		logln("");
		for (int i = 1; i <= seq1.size(); i++) {
			assertEquals(seq1.get(i-1), seq2.get(i-1));
			logln(String.format("Match. Sequence Number %s. Sent ID = %s, Received ID = %s ", i, seq1.get(i-1), seq2.get(i-1)));
		}
		logln("");
		logln("Messages received in same sequence as sent");

		return true;
	}
	
	protected String getDescriptor(Element root) {

		String mvtType = null;
		String operator = null;

		Element flID = root.getChild("FlightLeg",namespace).getChild("LegIdentifier", namespace);
		if (flID.getChild("ArrivalAirport", namespace).getText().contains("BAH")) {
			mvtType = "Arrival";
		}
		if (flID.getChild("DepartureAirport", namespace).getText().contains("BAH")) {
			mvtType = "Departure";
		}
		if (flID.getChild("DepartureAirport", namespace).getText().contains( flID.getChild("ArrivalAirport", namespace).getText())) {
			mvtType = "Arrival/Departure";
		}

		if(flID.getChild("Airline", namespace).getText().contains("GF")){
			operator = "Gulf Air ";
		} else {
			operator = "Other Airline ";
		}

		return operator.concat(mvtType);
	}

}
