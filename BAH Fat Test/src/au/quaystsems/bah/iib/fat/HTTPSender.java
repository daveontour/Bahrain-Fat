package au.quaystsems.bah.iib.fat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class HTTPSender {

	public String url;
	public String message;
	private int messSentCount = 0;

	public HTTPSender(String url, String message) {
		super();
		this.url = url;
		this.message = message;
	}
	
	public void resetCount() {
		messSentCount  = 0;
	}
	
	public int getMessageSentCount() {
		return this.messSentCount;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String send() {
		
		String reply = ""; 
		
		try {

			URL url = new URL(this.url);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/text");

			OutputStream os = conn.getOutputStream();
			os.write(this.message.getBytes());
			os.flush();

			BufferedReader br = new BufferedReader(new InputStreamReader(
					(conn.getInputStream())));

			String output;
			while ((output = br.readLine()) != null) {
				reply = output;
			}
			
			messSentCount++;
			conn.disconnect();
			

		} catch (MalformedURLException e) {
			e.printStackTrace();
			reply = "Malformed URL";
		} catch (IOException e) {
			e.printStackTrace();
			reply = "IO Exception";
		} 
		
		return reply;
	}
}
