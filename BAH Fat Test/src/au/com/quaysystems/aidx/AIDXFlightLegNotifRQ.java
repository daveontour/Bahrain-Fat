package au.com.quaysystems.aidx;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;

public class AIDXFlightLegNotifRQ {

	private Element root;
	private Namespace xsi = Namespace.getNamespace("xsi","http://www.w3.org/2001/XMLSchema-instance");
	private Namespace namespace = Namespace.getNamespace("http://www.iata.org/IATA/2007/00");
	private Document doc;
	private AIDXFlightLeg aidxFlightLeg;

	public AIDXFlightLegNotifRQ() {

		doc = new Document(new Element("IATA_AIDX_FlightLegNotifRQ",namespace));
		root = doc.getRootElement();
		root.addNamespaceDeclaration(xsi);
		root.setAttribute("Version", "16.1");

	}


	public Document getAIDXDoc() {
		return this.doc;
	}

	public void addFlightLeg(AIDXFlightLeg x) {
		this.aidxFlightLeg = x;
		Element content = x.getFlightLegElement();
		this.root.addContent(content);
	}
	
	public AIDXFlightLeg getFlightLeg() {
		return this.aidxFlightLeg;
	}
	
	public void updateMessage(String originator, String delSystem) {
		this.root.removeContent();
		setOrignator(originator, delSystem);
		Element content = this.aidxFlightLeg.getFlightLegElement();
		this.root.addContent(content);
	}

	public void setDocument(Document doc) {

		try {
			XMLOutputter xmlOutput = new XMLOutputter();
			InputStream stream = new ByteArrayInputStream(xmlOutput.outputString(doc).getBytes("UTF-8"));
			SAXBuilder builder = new SAXBuilder();
			this.doc = builder.build(stream);
			this.root = this.doc.getRootElement();
		} catch(Exception e) {
			e.printStackTrace();
		} 

	}

	public void setOrignator(String originator, String delSystem) {

		root.removeChild("Originator",namespace);
		root.removeChild("DeliveringSystem",namespace);

		if (originator != null) {
			root.addContent(new Element("Originator",namespace).setAttribute("CompanyShortName", originator));
		}
		if (delSystem != null) {
			root.addContent(new Element("DeliveringSystem",namespace).setAttribute("CompanyShortName", delSystem));
		}
	}
}
