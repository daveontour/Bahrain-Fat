package au.com.quaysystems.ssim;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class SSIMFlight implements Comparable<SSIMFlight> {
	public String flightNoCompound;
	public String origin;
	public String dest;
	public DateTime optime;
	public String seats;
	public String acType;
	public String mvtType;
	public String flightType;
	public String airline;
	public String flightNo;
	public String originDate;
	
	public String linkFlightNo;
	public String linkOrigin;
	public String linkDest;
	public String linkOriginDate;
	public String linkAirline;


	int hash = 1;

	boolean goodRecord = true;

	public SSIMFlight(String rec, int year, String homeApt) {

		String[] v = rec.split(" ");

		originDate = v[0];
		flightNoCompound = v[1];
		airline = v[2];
		flightNo = v[3]; 
		flightType = v[4]; 
		mvtType = v[5];
		seats = v[6];
		acType = v[7];
		origin = v[8];
		dest = v[9];
		

		if (mvtType.equals("DEP")) {
			origin = homeApt;
		}

		if (mvtType.equals("ARR")) {
			dest = homeApt;
		}		

		String optTimeString = v[10];
		optime = new DateTime(optTimeString);
		optime = optime.withYear(year);
		
		originDate = new Integer(optime.getYear()).toString().concat(originDate.substring(4));

		
		if (!v[11].equals("null")) {
			this.linkAirline = v[11];
			this.linkOrigin = v[12];
			this.linkDest = v[13];
			this.linkFlightNo = v[14];
			this.linkOriginDate = v[15];

			this.linkOriginDate = new Integer(optime.getYear()).toString().concat(this.linkOriginDate.substring(4));

			if (mvtType.equals("DEP")) {
				this.linkDest = homeApt;
			}

			if (mvtType.equals("ARR")) {
				this.linkOrigin = homeApt;
			}
			
			
		}
	}


	public SSIMFlight(SSIMEntry ssimEntry, DateTime opDate) {
		
		if (ssimEntry == null) {
			this.goodRecord = false;
			return;
		}
		flightNoCompound = ssimEntry.flightNo;
		origin = ssimEntry.origin;
		dest = ssimEntry.dest;
		seats = ssimEntry.seats;
		acType = ssimEntry.acType;
		mvtType = ssimEntry.mvtType;
		flightType = ssimEntry.flightType;

		airline = flightNoCompound.substring(0,2);
		flightNo = flightNoCompound.substring(2);

		DateTimeFormatter dtfOut = DateTimeFormat.forPattern("yyyy-MM-dd");


		try {
			optime = opDate
					.withHourOfDay(Integer.parseInt(ssimEntry.optime.substring(0,2)))
					.withMinuteOfHour(Integer.parseInt(ssimEntry.optime.substring(2)));

			if (mvtType.equals("DEP")){
				originDate = dtfOut.print(optime);
			} else {
				if (optime.hourOfDay().get() > 3) {
					originDate = dtfOut.print(optime);					
				} else {
					DateTime temp = optime.minusDays(1);
					originDate = dtfOut.print(temp);	
				}
			}


		} catch (Exception e) {
			goodRecord = false;
		}

		// Can happen coz of homeAirport Param
		if (origin.equals(dest)) {
			goodRecord = false;
		}
	}

	public String toString() {
		return String.format("%s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s\n", originDate, flightNoCompound,airline, flightNo, flightType, mvtType,seats, acType, origin, dest, optime.withZone(DateTimeZone.UTC), this.linkAirline, this.linkOrigin,this.linkDest, this.linkFlightNo, this.linkOriginDate);
	}

	@Override
	public int hashCode(){

		return flightNoCompound.hashCode()
				+origin.hashCode()
				+dest.hashCode()
				+optime.hashCode()
				+acType.hashCode()
				+mvtType.hashCode()
				+flightType.hashCode();

	}
	@Override
	public boolean equals(Object x) {

		if ( x == this) return true;
		if(!(x instanceof SSIMFlight)) return false;

		SSIMFlight fl = (SSIMFlight)x;

		return flightNoCompound == fl.flightNoCompound && optime.equals(fl.optime); 
	}

	@Override
	public int compareTo(SSIMFlight fl) {		
		try {
			DateTime x = fl.optime;
			return optime.compareTo(x);
		} catch (Exception e) {
			return 0;
		}
	}

}
