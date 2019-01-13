package au.com.quaysystems.ssim;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

import org.joda.time.DateTime;

public class SSIMManager {

	String fileSSIM;
	String homeAirport;
	String subYear;
	ArrayList<SSIMEntry> entries = new ArrayList<SSIMEntry>();
	private static SSIMManager instance;
	static ArrayList<SSIMFlight> flights = new  ArrayList<SSIMFlight>();
	
	
	public static SSIMManager getFlightManager(String apt, Integer year) {
		if (instance == null) {
			instance = new SSIMManager();
			instance.homeAirport = apt;
			instance.subYear = year.toString();
		}
		
		instance.processSortedFile("PROCESSED_SSIM.txt", year, apt);
		
		return instance;
	}
	

	private SSIMManager() {	}

	public void setFile(String file) {
		fileSSIM = file;		
	}

	public void processSortedFile(String filename, int year, String homeApt) {
		
		InputStream in = getClass().getResourceAsStream(filename); 
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));

		flights.clear();
		try {
//			File file = new File(filename);
			Scanner input = new Scanner(reader);

			while (input.hasNextLine()) {
				flights.add(new SSIMFlight(input.nextLine(), year, homeApt));
			}

			input.close();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void processRawFile() {

		try {
			Scanner input = new Scanner(new File(fileSSIM));

			while (input.hasNextLine()) {
				String entry = input.nextLine();

				if (!entry.startsWith("H")) {
					continue;
				}

				if (entry.endsWith(" J") || entry.endsWith(" F") || entry.endsWith(" P")) {
					//Single flight
					processEntry(entry.substring(1).trim(), entry.startsWith("H "));
				}  else if (entry.endsWith("JJ") || entry.endsWith("PJ") || entry.endsWith("JP") || entry.endsWith("PF") || entry.endsWith("FP")|| entry.endsWith("FF")) {
					//Linked flight
					processDoubleEntry(entry.substring(1).trim());		
				}
			}

			input.close();


		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		sortFlights();
	}

	private void processDoubleEntry(String doubleEntry) {

		if (doubleEntry.length() != 64) {
			return;
		}

		String seDep = doubleEntry.substring(7, 40)+doubleEntry.substring(51, 62)+doubleEntry.substring(63, 64);
		String seArr = doubleEntry.substring(0, 7)+doubleEntry.substring(14, 51)+doubleEntry.substring(62, 63);

		SSIMEntry eDep = new SSIMEntry(seDep,true, subYear);
		SSIMEntry eArr = new SSIMEntry(seArr,false, subYear);

		if(eDep.origin.equals(eDep.dest) ) {
			eDep.origin = this.homeAirport;
		}
		if(eArr.origin.equals(eArr.dest)) {
			eArr.dest = this.homeAirport;
		}
		eDep.linkedEntry = eArr;
		eArr.linkedEntry = eDep;


		entries.add(eDep);
		entries.add(eArr);
		
		ArrayList<SSIMFlight> flts = eDep.rolloutFlights();
		flights.addAll(flts);
		ArrayList<SSIMFlight> flts2 = eArr.rolloutFlights();
		flights.addAll(flts2);

	}

	private SSIMEntry processEntry(String entry, boolean isDeparture) {
		SSIMEntry e1 = new SSIMEntry(entry,isDeparture,  subYear);

		if(e1.origin.equals(e1.dest) || e1.origin.equals(e1.dest)) {
			return null;
		}
		if (isDeparture) {
			e1.origin = this.homeAirport;
		} else {
			e1.dest = this.homeAirport;
		}
		
		ArrayList<SSIMFlight> flts = e1.rolloutFlights();
		flights.addAll(flts);


		entries.add(e1);

		return e1;
	}

	public void sortFlights() {

		//Remove Duplicates
		Set<SSIMFlight> hs = new HashSet<>();
		hs.addAll(flights);

		flights.clear();
		flights.addAll(hs);

		//Sort
		Collections.sort(flights);
		
	}
	
	public void report() {
		System.out.println("Total Flights = "+flights.size());
	}

	public ArrayList<SSIMFlight> getAllFlights(DateTime start, DateTime end){

		ArrayList<SSIMFlight> fls = new ArrayList<SSIMFlight>();
		for (SSIMFlight f : flights) {
			if (start == null || end == null) {
				fls.add(f);
				continue;
			}

			if(start.isBefore(f.optime) && end.isAfter(f.optime)) {
				fls.add(f);
			}
		}
		return fls;

	}

	public ArrayList<SSIMFlight> getAllFlights(){
		return flights;	
	}

	public SSIMFlight getRandomFlight() {
		Random rand = new Random();
		int idx = rand.nextInt(flights.size());
		return flights.get(idx);
	}
	public List<SSIMFlight> getRandomFlight(int numFlights) {
		Random rand = new Random();
		int idx = rand.nextInt(flights.size()-numFlights);
		return flights.subList(idx, Math.min(idx+numFlights,flights.size()));
	}	

	public void saveXMLToFile(String fileName, String xml) {

		BufferedWriter bw = null;
		FileWriter fw = null;

		try {
			File file = new File(fileName);

			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}

			// true = append file
			fw = new FileWriter(file.getAbsoluteFile(), false);
			bw = new BufferedWriter(fw);

			bw.write(xml);


		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {

			try {

				if (bw != null)
					bw.close();

				if (fw != null)
					fw.close();

			} catch (IOException ex) {

				ex.printStackTrace();

			}
		}
	}	
	public void saveToFile(String fileName) {

		BufferedWriter bw = null;
		FileWriter fw = null;

		try {
			File file = new File(fileName);

			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}

			// true = append file
			fw = new FileWriter(file.getAbsoluteFile(), false);
			bw = new BufferedWriter(fw);

			for (SSIMFlight fl : flights) {
				bw.write(fl.toString());
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {

			try {

				if (bw != null)
					bw.close();

				if (fw != null)
					fw.close();

			} catch (IOException ex) {

				ex.printStackTrace();

			}
		}
	}
}
