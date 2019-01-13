package au.com.quaysystems.aidx;

public class FlightStory {
	
	public static long INITIAL = 1; 
	public static long STAND_ALLOC = 2;
	public static long CHECKIN_ALLOC = 4;
	public static long BHS_ALLOC = 8;
	public static long RANDOM_DIV = 16;
	public static long RANDOM_CANCEL = 32;
	public static long DEP_ORIGIN = 64;
	public static long ETO_UPDATES = 128;
	public static long PAX_NUM_UPDATES = 256;
	public static long VDGS_ACTUALS = 512;
	public static long TOWER_ACTUALS = 1024;

	public static void main(String[] args) {
		log (BHS_ALLOC | INITIAL);
	
	}

	public static void log(String s) {
		System.out.println(s);
	}
	public static void log(Long s) {
		System.out.println(s);
	}
}
