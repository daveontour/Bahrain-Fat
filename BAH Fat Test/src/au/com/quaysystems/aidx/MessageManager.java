package au.com.quaysystems.aidx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.quaysystems.ssim.SSIMFlight;
import au.com.quaysystems.ssim.SSIMManager;

public class MessageManager {

	private static MessageManager instance;
	private static SSIMManager flightMgr;
	private static XMLOutputter xmlOutput;
	private Random rand = new Random();
	private String[] gates = {"A20","A21","A22","A23","A24","A25","A26","A27","A28","A29","A30","B1","B2","B3","B4","B5","B6","B7","B8","B9","B10"};
	private String[] runways = {"29R","29L","11L","11R","09","27"};
	private String[] bagMakeup = {"1W","2W","3W","4W","5W","1E","2E","3E","4E","5E"};
	private String[] bagClaim = {"1","2","3","4","5","6","7","8","9"};
	

	private MessageManager() {}

	public static MessageManager getMessageManager(String apt, Integer year) {
		if (instance == null) {
			instance = new MessageManager();
			flightMgr = SSIMManager.getFlightManager(apt, year);
			xmlOutput = new XMLOutputter();
			xmlOutput.setFormat(Format.getPrettyFormat());
		}

		return instance;
	}

	private AIDXFlightLeg getFlightLeg(SSIMFlight fl) {
		AIDXFlightLeg fltLeg = new AIDXFlightLeg(fl);
		if (fl.mvtType.equals("DEP")) {
			fltLeg.addOperationTime(fl.optime.withZone(DateTimeZone.UTC).toString(), "9750", "SCH", "OFB");
		}
		if (fl.mvtType.equals("ARR")) {
			fltLeg.addOperationTime(fl.optime.withZone(DateTimeZone.UTC).toString(), "9750", "SCH", "ONB");
		}

		fltLeg.addOperationalStatus("SCH", "1245");
		fltLeg.setOwner(fl.airline);
		fltLeg.setAcInfo(fl.acType, "A6-1233");

		return fltLeg;
	}

	private AIDXFlightLegNotifRQ getFlightLegMessage(AIDXFlightLeg fltLeg, String originator, String delsystem) {
		AIDXFlightLegNotifRQ msg = new AIDXFlightLegNotifRQ();
		msg.setOrignator(originator, delsystem);

		msg.addFlightLeg(fltLeg);
		msg.getAIDXDoc().getRootElement().setAttribute("TimeStamp", new DateTime().withZone(DateTimeZone.UTC).toString());

		return msg;

	}

	private ArrayList<AIDXFlightLegNotifRQ> getRandomFlightMessages(String originator, String delsystem, int num) {

		ArrayList<AIDXFlightLegNotifRQ> flightMessages = new ArrayList<AIDXFlightLegNotifRQ>();

		Integer correlID = 1000;

		for (SSIMFlight flt : flightMgr.getRandomFlight(num)) {

			AIDXFlightLeg fltLeg = getFlightLeg(flt);			
			AIDXFlightLegNotifRQ msg = this.getFlightLegMessage(fltLeg, originator, delsystem);
			msg.getAIDXDoc().getRootElement().setAttribute("CorrelationID", correlID.toString());

			correlID++;
			flightMessages.add(msg);
		}
		return flightMessages;
	}

	public ArrayList<Element> getRandomFlightWithResources(String originator, String delsystem, int num, boolean actuals) {

		ArrayList<Element> flightMessages = new ArrayList<Element>();

		Integer correlID = 1000;

		for (AIDXFlightLegNotifRQ msg : getRandomFlightMessages(originator, delsystem, num)) {

			DateTime optime = msg.getFlightLeg().flight.optime;
			optime = optime.minusDays(2);

			msg.getFlightLeg().clearResources(true, true, true, true);

			if(msg.getFlightLeg().isDep) {
				msg.getFlightLeg().setReourceDeparture(true, gates[rand.nextInt(gates.length)], gates[rand.nextInt(gates.length)], bagMakeup[rand.nextInt(bagMakeup.length)], "B1", "B8", runways[rand.nextInt(runways.length)]);
			} else {
				msg.getFlightLeg().setReourceArrival(true, gates[rand.nextInt(gates.length)], gates[rand.nextInt(gates.length)], bagClaim[rand.nextInt(bagClaim.length)],  runways[rand.nextInt(runways.length)]);

			}

			if (actuals) {
				if(msg.getFlightLeg().isDep) {
					msg.getFlightLeg().setReourceDeparture(false, gates[rand.nextInt(gates.length)], gates[rand.nextInt(gates.length)], bagMakeup[rand.nextInt(bagMakeup.length)],"B1", "B8",  runways[rand.nextInt(runways.length)]);
				} else {
					msg.getFlightLeg().setReourceArrival(false, gates[rand.nextInt(gates.length)], gates[rand.nextInt(gates.length)], bagClaim[rand.nextInt(bagClaim.length)],  runways[rand.nextInt(runways.length)]);

				}				
			}
			msg.updateMessage(originator, delsystem);
			msg.getAIDXDoc().getRootElement().setAttribute("TimeStamp", optime.withZone(DateTimeZone.UTC).toString());
			msg.getAIDXDoc().getRootElement().setAttribute("CorrelationID", correlID.toString());
			correlID++;
			flightMessages.add(msg.getAIDXDoc().getRootElement());
		}
		return flightMessages;
	}

	public String getRandomFlightXML(String originator, String delSystem, int num) {

		StringBuilder sb = new StringBuilder();
		for (Element el : getRandomFlight( originator,  delSystem,  num)) {
			sb.append(xmlOutput.outputString(el));
			sb.append("\n\n");
		}
		return sb.toString();
	}

	public String getBadRandomFlightXML(String originator, String delSystem, int num) {

		StringBuilder sb = new StringBuilder();
		for (Element el : getRandomFlight( originator,  delSystem,  num)) {
			el.addContent(new Element("Intentionally_Bad").setText("THIS DOES NOT BELONG - INTENTIONALY HERE"));
			sb.append(xmlOutput.outputString(el));
			sb.append("\n\n");
		}
		return sb.toString();
	}
	
	public ArrayList<Element> getGoodBadRandomFlight(String originator, String delSystem, int numGood, int numBad) {
		return getGoodBadRandomFlight(originator, delSystem, numGood, numBad, 12000);
	}
	

	public ArrayList<Element> getGoodBadRandomFlight(String originator, String delSystem, int numGood, int numBad, int startID) {

		ArrayList<Element> good = getRandomFlight( originator,  delSystem,  numGood);
		ArrayList<Element> bad = getRandomFlight( originator,  delSystem,  numBad);

		for (Element el : bad) {
			el.addContent(new Element("Intentionally_Bad").setText("THIS DOES NOT BELONG - INTENTIONALY HERE"));
		}

		good.addAll(bad);

		long seed = System.nanoTime();
		Collections.shuffle(good, new Random(seed));

		Integer correlID = startID;
		for (Element el: good) {
			el.setAttribute("CorrelationID", correlID.toString());
			correlID++;
		}

		return good;

	}

	public ArrayList<Element> getRandomFlight(String originator, String delsystem, int num) {

		ArrayList<Element> flightMessages = new ArrayList<Element>();

		Integer correlID = 1000;

		for (AIDXFlightLegNotifRQ msg : getRandomFlightMessages(originator, delsystem, num)) {

			DateTime optime = msg.getFlightLeg().flight.optime;
			optime = optime.minusDays(2);
			msg.getAIDXDoc().getRootElement().setAttribute("TimeStamp", optime.withZone(DateTimeZone.UTC).toString());
			msg.getAIDXDoc().getRootElement().setAttribute("CorrelationID", correlID.toString());
			correlID++;
			flightMessages.add(msg.getAIDXDoc().getRootElement());
		}
		return flightMessages;
	}

	public Element getRandomFlight(String originator, String delsystem) {

		AIDXFlightLegNotifRQ msg = getRandomFlightMessages(originator, delsystem, 1).get(0);

		DateTime optime = msg.getFlightLeg().flight.optime;
		optime = optime.minusDays(2);
		msg.getAIDXDoc().getRootElement().setAttribute("TimeStamp", optime.withZone(DateTimeZone.UTC).toString());
		msg.getAIDXDoc().getRootElement().setAttribute("CorrelationID", "1000");
		return msg.getAIDXDoc().getRootElement();
	}


	public String getRandomFlightWithResourcesXML(String originator, String delSystem, int num, boolean actual) {

		StringBuilder sb = new StringBuilder();
		for (Element el : getRandomFlightWithResources( originator,  delSystem,  num, actual)) {
			sb.append(xmlOutput.outputString(el));
			sb.append("\n\n");
		}
		return sb.toString();
	}	

	public ArrayList<Element> getVDSGMessages(int num){

		ArrayList<Element> flightMessages = new ArrayList<Element>();

		Integer correlID = 1000;


		for (AIDXFlightLegNotifRQ msg : getRandomFlightMessages("FMT", "VDGS", num)) {

			DateTime optime = msg.getFlightLeg().flight.optime;
			int offset = 30 - rand.nextInt(60);
			optime = optime.plusMinutes(offset);

			msg.getAIDXDoc().getRootElement().setAttribute("CorrelationID", correlID.toString());
			msg.getAIDXDoc().getRootElement().setAttribute("TimeStamp", optime.plusSeconds(45).withZone(DateTimeZone.UTC).toString());

			msg.getFlightLeg().clearResources(true, true, true, true);
			msg.getFlightLeg().setOwner(null);
			msg.getFlightLeg().setAcInfo(null, null);
			msg.getFlightLeg().clearAssociateFlightLeg();
			msg.getFlightLeg().addOperationalStatus(null, null);


			if (msg.getFlightLeg().isDep) {
				msg.getFlightLeg().addOperationTime(optime.withZone(DateTimeZone.UTC).toString(), "9750", "ACT", "OFB");
			} else {
				msg.getFlightLeg().addOperationTime(optime.withZone(DateTimeZone.UTC).toString(), "9750", "ACT", "ONB");
			}
			msg.updateMessage("FMT", "VDGS");
			correlID++;
			flightMessages.add(msg.getAIDXDoc().getRootElement());
		}
		return flightMessages;

	}

	public String getVDGSMessagesXML( int num) {

		StringBuilder sb = new StringBuilder();
		for (Element el : getVDSGMessages(num)) {
			sb.append(xmlOutput.outputString(el));
			sb.append("\n\n");
		}
		return sb.toString();
	}
}
