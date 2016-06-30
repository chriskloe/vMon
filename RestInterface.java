package vMon;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

/***
The RestInterface is a helper class to send a Post request for a certain item to a certain
Rest Serverr
***/
public class RestInterface 
{
	
	static void sendPostRequest(String theUri, String theItem, String theValue)
	{
	    HttpClient mClient = HttpClientBuilder.create().build();
		StringEntity input = null;
		HttpPost post = new HttpPost(theUri+theItem);
		
		try {
			input = new StringEntity(theValue);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		post.setEntity(input);
		HttpResponse response = null;
		try {
			response = mClient.execute(post);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(ConnectException e){
			System.err.println(new String("Could not connetc to rest server " + theUri));
			return;
		}

		catch (IOException e) {
		    e.printStackTrace();
		}
		BufferedReader rd = null;
		try {
			rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
		} catch (UnsupportedOperationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String line = "";
		try {
			while ((line = rd.readLine()) != null) {
				System.out.println(line);

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
}
