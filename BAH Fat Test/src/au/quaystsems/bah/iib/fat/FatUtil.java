package au.quaystsems.bah.iib.fat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.ibm.mq.MQEnvironment;

public class FatUtil {

	private static FatUtil single;
	
	public Properties props;
	public String qmgr;
	public String channel;
	public String hostname;
	public String userID;
	public String password;
	public int port;
	public boolean use_client_connection;
	public static boolean use_non_aidx_messages;
	public static boolean verbose;

	private FatUtil() {

		props = new Properties();
		
		try (InputStream input = getClass().getResourceAsStream("IIBFAT.properties")){
			props.load(input);
			
			qmgr = props.getProperty("qmgr");
			hostname = props.getProperty("host");
			channel = props.getProperty("channel");
			port = Integer.parseInt(props.getProperty("port"));
			userID = props.getProperty("userID");
			password = props.getProperty("password");
			use_client_connection = Boolean.parseBoolean(props.getProperty("use_client_connection"));
			verbose = Boolean.parseBoolean(props.getProperty("verbose"));
			use_non_aidx_messages = Boolean.parseBoolean(props.getProperty("use_non_aidx_messages"));
			
			if (use_client_connection) {
				 setClientConnection();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} 
	}

	public static FatUtil getFatUtil() {
		if (single == null) {
			single = new FatUtil();
		}
		return single;
	}
	
	public void  setClientConnection() {
		MQEnvironment.hostname = hostname;
		MQEnvironment.channel = channel;
		MQEnvironment.port = port;
		MQEnvironment.userID = userID;
		MQEnvironment.password = password;
	}
}
