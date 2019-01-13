package au.com.quaysystems.ssim;

import java.util.ArrayList;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class SSIMEntry {
	public String flightNo;
	public String startService;
	public String endService;
	public String origin;
	public String dest;
	public String optime;
	public String seats;
	public String acType;
	public String mvtType;
	public String flightType;

	public SSIMEntry linkedEntry;

	//	public String linkFlightNo;
	//	public String linkOrigin;
	//	public String linkDest;
	//	public String linkOptime;

	public DateTime startTime;
	public DateTime endTime;

	boolean opMon;
	boolean opTue;
	boolean opWed;
	boolean opThu;
	boolean opFri;
	boolean opSat;
	boolean opSun;

	public SSIMEntry(String entry, Boolean isDep, String subYear) {

		try {
			mvtType = isDep?"DEP":"ARR";

			String[] parts = entry.split(" ");

			flightNo = parts[0];
			startService = parts[1].substring(0, 5);
			endService = parts[1].substring(5);

			opMon = parts[2].charAt(0)=='1';
			opTue = parts[2].charAt(1)=='2';
			opWed = parts[2].charAt(2)=='3';
			opThu = parts[2].charAt(3)=='4';
			opFri = parts[2].charAt(4)=='5';
			opSat = parts[2].charAt(5)=='6';
			opSun = parts[2].charAt(6)=='7';

			seats = parts[3].substring(0,3);
			acType = parts[3].substring(3);

			if (isDep) {
				optime = parts[4].substring(0,4);
				origin = parts[4].substring(4,7);
				dest = parts[4].substring(7);
			} else {
				optime = parts[4].substring(6);
				origin = parts[4].substring(0,3);
				dest = parts[4].substring(3,6);
			}

			flightType = parts[5];

			DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyyddMMMZ");

			startTime = fmt.parseDateTime(subYear+startService+"+0000");

			endTime = fmt.parseDateTime(subYear+endService+"+0000").withHourOfDay(23).withMinuteOfHour(59);

			//Winter Schedule Crosses Year

			if (startTime.isAfter(endTime)) {
				startTime = startTime.minusYears(1);
			}

			//				rolloutFlights();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ArrayList<SSIMFlight> rolloutFlights() {

		ArrayList<SSIMFlight> flights = new  ArrayList<SSIMFlight>();

		for(DateTime currentdate = startTime; currentdate.isBefore(endTime) || currentdate.isEqual(endTime); 
				currentdate= currentdate.plusDays(1)){


			if (
					(currentdate.dayOfWeek().get() == 1 && opMon) ||
					(currentdate.dayOfWeek().get() == 2 && opTue) ||
					(currentdate.dayOfWeek().get() == 3 && opWed) ||
					(currentdate.dayOfWeek().get() == 4 && opThu) ||
					(currentdate.dayOfWeek().get() == 5 && opFri) ||
					(currentdate.dayOfWeek().get() == 6 && opSat) ||
					(currentdate.dayOfWeek().get() == 7 && opSun)
					) {

				SSIMFlight f1 = new SSIMFlight(this, currentdate);

				if (f1.goodRecord) {
					
					f1.originDate = new Integer(f1.optime.getYear()).toString().concat(f1.originDate.substring(4));
					
					
					flights.add(f1);

					if (this.linkedEntry != null) {
						SSIMFlight f2 = new SSIMFlight(this.linkedEntry, currentdate);
						if (f2.goodRecord) {

							f1.linkAirline = f2.airline;
							f1.linkOrigin = f2.origin;
							f1.linkDest = f2.dest;
							f1.linkFlightNo = f2.flightNo;
							f1.linkOriginDate = f2.originDate;

							f2.linkAirline = f1.airline;
							f2.linkOrigin = f1.origin;
							f2.linkDest = f1.dest;
							f2.linkFlightNo = f1.flightNo;
							f2.linkOriginDate = f1.originDate;

							flights.add(f2);
						}
					}
				}
			}
		}

		return flights;
	}

}