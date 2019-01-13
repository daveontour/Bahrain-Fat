package au.com.quaysystems.aidx;

import java.util.ArrayList;

import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.Namespace;

import au.com.quaysystems.ssim.SSIMFlight;

public class AIDXFlightLeg implements Cloneable{

	private Element flightLegID;
	private Element linkedFlightLegID;
	private Element flightLegData;
	private Element flightLeg;
	private Element serviceType;
	private Element owner;
	private Element acInfo;
	private Element resPlan;
	private Element resActual;
	private Element planDep;
	private Element planArr;
	private Element actDep;
	private Element actArr;

	private ArrayList<Element> opStatus = new ArrayList<Element>();
	private ArrayList<Element> opTimes = new ArrayList<Element>();
	
	public boolean isDep;
	public SSIMFlight flight;

	private Namespace xsi = Namespace.getNamespace("xsi","http://www.w3.org/2001/XMLSchema-instance");
	private Namespace namespace = Namespace.getNamespace("http://www.iata.org/IATA/2007/00");
	
	
	public AIDXFlightLeg() {}

	public AIDXFlightLeg clone() {


		AIDXFlightLeg leg = new AIDXFlightLeg();
		
		leg.opStatus = this.cloneList(this.opStatus);
		leg.opTimes = this.cloneList(this.opTimes);
		
		leg.flightLegID = (this.flightLegID != null)?this.flightLegID.clone():null;
		leg.flightLegData = null;
		leg.flightLeg = null;
		leg.serviceType = (this.serviceType != null)?this.serviceType.clone():null;
		leg.owner = (this.owner != null)?this.owner.clone():null;
		leg.acInfo = (this.acInfo != null)?this.acInfo.clone():null;
		leg.resPlan = (this.resPlan != null)?this.resPlan.clone():null;
		leg.resActual = (this.resActual != null)?this.resActual.clone():null;
		leg.planDep = (this.planDep != null)?this.planDep.clone():null;
		leg.planArr = (this.planArr != null)?this.planArr.clone():null;
		leg.actDep = (this.actDep != null)?this.actDep.clone():null;
		leg.actArr = (this.actArr != null)?this.actArr.clone():null;
		
		leg.flightLeg = new Element("FlightLeg",namespace);
		leg.flightLeg.addContent(leg.flightLegID.detach());
		leg.flightLeg.addContent(leg.createFlightLegData());
		
		leg.isDep = this.isDep;

		leg.getFlightLegElement();
			
		return leg;
	}
	
	public ArrayList<Element> cloneList(ArrayList<Element> list) {
	    ArrayList<Element> clone = new ArrayList<Element>(list.size());
	    for (Element item : list) clone.add(item.clone());
	    return clone;
	}

	public AIDXFlightLeg(SSIMFlight fl) {

		this.flight = fl;
		this.isDep = fl.mvtType.equals("DEP");
		this.flightLeg = new Element("FlightLeg",namespace);
		this.flightLeg.addContent(this.createFlightLegIndentifier(fl.airline,fl.flightNo,fl.origin,fl.dest,fl.originDate));
		this.flightLeg.addContent(this.createFlightLegData());
		
		//Include linked flight if exists
		if (fl.linkAirline != null) {
			this.linkedFlightLegID = new Element("AssociatedFlightLegAircraft", namespace).setAttribute("RepeatIndex","1");
			
			this.linkedFlightLegID.addContent(new Element("Airline",namespace).setAttribute("CodeContext","3").setText(fl.linkAirline));
			this.linkedFlightLegID.addContent(new Element("FlightNumber",namespace).setText(fl.linkFlightNo));
			this.linkedFlightLegID.addContent(new Element("DepartureAirport",namespace).setAttribute("CodeContext","3").setText(fl.linkOrigin));
			this.linkedFlightLegID.addContent(new Element("ArrivalAirport",namespace).setAttribute("CodeContext","3").setText(fl.linkDest));
			this.linkedFlightLegID.addContent(new Element("OriginDate",namespace).setText(fl.linkOriginDate));
			this.linkedFlightLegID.addContent(new Element("RepeatNumber",namespace).setText("1"));

		}

	}
	public void clearAssociateFlightLeg() {
		this.linkedFlightLegID = null;
	}
	public AIDXFlightLeg (Element el) {
		this.flightLeg = el.clone();
		this.flightLegID = (Element)this.flightLeg.getContent().get(0);
		this.flightLegData = (Element)this.flightLeg.getContent().get(1);
	}
	public Element getFlightLegID() {
		return this.flightLegID.clone();
	}
	public void setFlightLegID(Element el) {
		this.flightLegID = el.clone();
	}
	public Element getFlightLegElement() {
		
		this.flightLegData.removeContent();

		for(Element os : opStatus) {
			this.flightLegData.addContent(os);
		}

		add(this.serviceType);
		add(this.owner);
		if (this.linkedFlightLegID != null) {
			add(this.linkedFlightLegID);
		}
		prepareResources();
		add(this.resPlan);
		add(this.resActual);

		for(Element ot : opTimes) {
			this.flightLegData.addContent(ot);
		}

		add(this.acInfo);

		return this.flightLeg;
	}

	private void prepareResources() {
		
		if (this.resPlan != null) {
			this.resPlan.removeContent();
		}

		if (this.planDep != null || this.planArr != null) {
			this.resPlan = new Element("AirportResources",namespace).setAttribute("Usage", "Planned");
			if (this.planDep  != null) {
				this.resPlan.addContent(this.planDep);
			}
			if (this.planArr  != null) {
				this.resPlan.addContent(this.planArr);
			}
		} else {
			this.resPlan = null;
		}
		
		if (this.resActual != null) {
			this.resActual.removeContent();
		}


		if (this.actDep != null || this.actArr != null) {
			this.resActual = new Element("AirportResources",namespace).setAttribute("Usage", "Actual");
			if (this.actDep  != null) {
				this.resActual.addContent(this.actDep);
			}
			if (this.actArr  != null) {
				this.resActual.addContent(this.actArr);
			}
		} else {
			this.resActual = null;
		}
	}

	private void add(Element el) {
		if (el != null) {
			this.flightLegData.addContent(el);			
		}		
	}

	public Element createFlightLegIndentifier(String airline, String flNum, String depApt, String arrApt, String oDate) {

		Element flID = new Element("LegIdentifier",namespace);		
		flID.addContent(new Element("Airline",namespace).setAttribute("CodeContext","3").setText(airline));
		flID.addContent(new Element("FlightNumber",namespace).setText(flNum));
		flID.addContent(new Element("DepartureAirport",namespace).setAttribute("CodeContext","3").setText(depApt));
		flID.addContent(new Element("ArrivalAirport",namespace).setAttribute("CodeContext","3").setText(arrApt));
		flID.addContent(new Element("OriginDate",namespace).setText(oDate));
		flID.addContent(new Element("RepeatNumber",namespace).setText("1"));

		this.flightLegID = flID;

		return flID;
	}
	public Element createFlightLegData() {
		Element data = new Element("LegData",namespace);
		this.flightLegData = data;
		return data;
	}

	public void addOperationalStatus(String text, String codeContext) {

		if (text == null) {
			this.opStatus.clear();
		} else {

			Element el = new Element("OperationalStatus",namespace).setText(text).setAttribute("RepeatIndex",new Integer(opStatus.size()+1).toString());
			if (codeContext != null) {
				el.setAttribute("CodeContext", codeContext);
			}

			this.opStatus.add(el);
		}

	}
	public void addOperationTime(String text, String codeContext, String timeType, String opQualifier) {


		if (text == null) {
			this.opTimes.clear();
		} else {

			Element el = new Element("OperationTime",namespace).setText(text)
					.setAttribute("CodeContext",codeContext)
					.setAttribute("RepeatIndex",new Integer(opTimes.size()+1).toString())
					.setAttribute("TimeType",timeType)
					.setAttribute("OperationQualifier",opQualifier)
					;


			this.opTimes.add(el);
		}

	}

	public void setServiceType(String serviceType) {

		if (serviceType == null) {
			this.serviceType = null;
		} else {
			this.serviceType = new Element("ServiceType",namespace).setText(serviceType);
		}

	}
	public void setAcInfo(String type, String rego) {

		if (type == null) {
			this.acInfo = null;
		} else {
			Element el = new Element("AircraftInfo",namespace);
			el.addContent(new Element("AircraftType",namespace).setText(type));
			el.addContent(new Element("Registration",namespace).setText(rego));
			el.addContent(new Element("TailNumber",namespace));

			this.acInfo = el;
		}
	}
	public void setOwner(String owner) {

		if (owner == null) {
			this.owner = null;
		} else {

			Element el = new Element("OwnerAirline",namespace);
			el.addContent(new Element("Airline",namespace).setText(owner));

			this.owner = el;
		}
	}

	public void clearResources(boolean planDep, boolean actDep, boolean planArr, boolean actArr) {
		if (planDep) this.planDep = null;
		if (planArr) this.planArr = null;
		if (actDep) this.actDep = null;
		if (actArr) this.actArr = null;
	}
	public void setReourceDeparture(Boolean planned, String depPos, String paxGate, String bagMakeUp, String checkFirst, String checkLast, String runway) {

		Element dep = new Element("Resource",namespace).setAttribute("DepartureOrArrival","Departure");

		dep.addContent(new Element("AircraftParkingPosition",namespace).setAttribute("Qualifier","Remote").setText(depPos));
		dep.addContent(new Element("PassengerGate",namespace).setText(paxGate));
		dep.addContent(new Element("Runway",namespace).setText(runway));
		dep.addContent(new Element("BaggageMakeupBelt",namespace).setText(bagMakeUp));
		dep.addContent(new Element("CheckInInfo",namespace)
				.addContent(new Element("FirstPosition",namespace).addContent("Start").setText(checkFirst))
				.addContent(new Element("LastPosition",namespace).addContent("End").setText(checkLast))
				.setAttribute("RepeatIndex", "1")
				);

		setNulls(dep);

		if (planned) {
			this.planDep = dep;
		} else {
			this.actDep = dep;
		}
	}
	public void setReourceArrival(boolean planned, String depPos, String paxGate, String bagClaim, String runway) {

		Element arr = new Element("Resource",namespace).setAttribute("DepartureOrArrival","Arrival");

		arr.addContent(new Element("AircraftParkingPosition",namespace).setAttribute("Qualifier","Remote").setText(depPos));
		arr.addContent(new Element("PassengerGate",namespace).setText(paxGate));
		arr.addContent(new Element("Runway",namespace).setText(runway));
		arr.addContent(new Element("BaggageClaimUnit",namespace).setText(bagClaim));

		setNulls(arr);

		if (planned) {
			this.planArr = arr;
		} else {
			this.actArr = arr;
		}
	}

	private void setNulls(Element el) {		
		for (Content descendant : el.getDescendants()) {
			if (descendant.getCType().equals(Content.CType.Element)) {
				Element element = (Element) descendant;
				if (element.getText().isEmpty() && !element.getName().equals("Resource") && !element.getName().equals("CheckInInfo")&& !element.getName().equals("Runway")) {
					element.setAttribute("nil", "true",xsi);	
				} 
			}
		}
	}
}
