/**
 * 
 */
package se.leap.bitmaskclient.test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

import org.json.JSONException;
import org.json.JSONObject;

import se.leap.bitmaskclient.ConfigHelper;
import se.leap.bitmaskclient.ConfigurationWizard;
import se.leap.bitmaskclient.Dashboard;
import se.leap.bitmaskclient.LeapSRPSession;
import se.leap.bitmaskclient.LogInDialog;
import se.leap.bitmaskclient.Provider;
import se.leap.bitmaskclient.ProviderAPI;
import android.app.Application;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.test.ServiceTestCase;

/**
 * @author parmegv
 *
 */
public class testProviderAPI extends ServiceTestCase<ProviderAPI> {
	
	/**
	 * @param providerAPI_class
	 */
	public testProviderAPI() {
		super(ProviderAPI.class);
	}

	/* (non-Javadoc)
	 * @see android.test.ServiceTestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
        
        ConfigHelper.setSharedPreferences(getContext().getSharedPreferences(Dashboard.SHARED_PREFERENCES, Application.MODE_PRIVATE));
	}

	/* (non-Javadoc)
	 * @see android.test.ServiceTestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testGetHexString() throws InterruptedException {
		String expected_hex_representation = "44eba0239ddfcc5a488d208df32a89eb00e93e6576b22ba2e4410085a413cf64e9c2f08ebc36a788a0761391150ad4a0507ca43f9ca659e2734f0457a85358c0bb39fa87183c9d3f9f8a3b148dab6303a4e796294f3e956472ba0e2ea5697382acd93c8b8f1b3a7a9d8517eebffd6301bfc8de7f7b701f0878a71faae1e25ad4";
		byte[] byte_representation = new BigInteger(expected_hex_representation, 16).toByteArray();

		String hex_representation = new BigInteger(byte_representation).toString(16);
		assertEquals(expected_hex_representation, hex_representation);
	}
	
	public void testAuthenticateBySRP() throws InterruptedException, IOException, JSONException {
		int number_of_trials_per_login = 5;
		int trial = 0;
		int total_different_logins = 5;
		String username = "", password = "", api_url = "";
		AssetManager assets_manager = this.getContext().getAssets();
		String tests_string = new Scanner(new InputStreamReader(assets_manager.open("testAuthenticateBySRP_data"))).useDelimiter("\\A").next();
		JSONObject all_tests = new JSONObject(tests_string);
		JSONObject current_test;
		for(int current_login = 3; current_login < current_login + total_different_logins ; current_login++)
		{
			current_test = all_tests.getJSONObject(String.valueOf(current_login+1));
			username = current_test.getString("username");
			password = current_test.getString("password");
			api_url = current_test.getString("api-url");
			for(trial = 0; trial < number_of_trials_per_login; trial++)
			{
				System.out.println("Login " + current_login + ". Trial " + trial);
				
				CountDownLatch done_signal = new CountDownLatch(1);  
				JSONObject result = new JSONObject();
				new Thread(new RunAuthenticateBySRP(done_signal, result, username, password, api_url)).run();
				done_signal.await();
				assertEquals(ProviderAPI.SRP_AUTHENTICATION_SUCCESSFUL, result.getInt(ProviderAPI.RESULT_KEY));
				
				result = new JSONObject();
				done_signal = new CountDownLatch(1);  
				new Thread(new RunLogOut(done_signal, result, api_url)).run();
				done_signal.await();
				assertEquals(ProviderAPI.LOGOUT_SUCCESSFUL, result.getInt(ProviderAPI.RESULT_KEY));
			}
		}
	}
	
	public class RunGetHexString implements Runnable {
		
		private CountDownLatch done_signal;  
	    private Map<String, Integer> result;

	    public RunGetHexString(CountDownLatch cdl, Map<String, Integer> result, byte[] byte_representation) {
	    	this.done_signal = cdl;  
	    	this.result = result;
	    }

		@Override
		public void run() {
			Intent provider_API_command = new Intent();
			
			Bundle parameters = new Bundle();
			parameters.putString("cert", "https://bitmask.net/ca.crt");
			parameters.putString("eip", "https://api.bitmask.net/1/config/eip-service.json");

			provider_API_command.putExtra(ProviderAPI.DOWNLOAD_JSON_FILES_BUNDLE_EXTRA, parameters);
			provider_API_command.putExtra(ProviderAPI.PARAMETERS, parameters);
			ResultReceiver result_receiver = new ResultReceiver(null) {  
	            @Override  
	            protected void onReceiveResult(int result_code, Bundle resultData) {
	            	result.put("result", result_code);
	            	done_signal.countDown();
	            }
	       };
	       
			provider_API_command.putExtra("receiver", result_receiver);
			startService(provider_API_command);
		}
	}
	
	public class RunAuthenticateBySRP implements Runnable {
		
		private CountDownLatch done_signal;  
	    private JSONObject result;
		private String username;
		private String password;
		private String api_url;

	    public RunAuthenticateBySRP(CountDownLatch cdl, JSONObject result, String username, String password, String api_url) {
	    	this.done_signal = cdl;  
	    	this.result = result;
	    	this.username = username;
	    	this.password = password;
	    	this.api_url = api_url;
	    }

		@Override
		public void run() {
			Intent provider_API_command = new Intent();
			
			Bundle parameters = new Bundle();
			parameters.putString(LogInDialog.USERNAME, username);
			parameters.putString(LogInDialog.PASSWORD, password);
			parameters.putString(Provider.API_URL, api_url);

			provider_API_command.setAction(ProviderAPI.SRP_AUTH);
			provider_API_command.putExtra(ProviderAPI.PARAMETERS, parameters);
			ResultReceiver result_receiver = new ResultReceiver(null) {  
	            @Override  
	            protected void onReceiveResult(int result_code, Bundle resultData) {
	            	try {
						result.put("result", result_code);
						result.put("session_id_cookie_key", resultData.getString("session_id_cookie_key"));
						result.put("session_id", resultData.getString("session_id"));
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	            	done_signal.countDown();
	            }
	       };
	       
			provider_API_command.putExtra(ProviderAPI.RECEIVER_KEY, result_receiver);
			startService(provider_API_command);
		}
	}
	
	public class RunLogOut implements Runnable {
		
		private CountDownLatch done_signal;  
	    private JSONObject result;
		private String api_url;

	    public RunLogOut(CountDownLatch cdl, JSONObject result, String api_url) {
	    	this.done_signal = cdl;  
	    	this.result = result;
	    	this.api_url = api_url;
	    }

		@Override
		public void run() {
			Intent provider_API_command = new Intent();
			
			Bundle parameters = new Bundle();
			parameters.putString(Provider.API_URL, api_url);

			provider_API_command.setAction(ProviderAPI.LOG_OUT);
			provider_API_command.putExtra(ProviderAPI.PARAMETERS, parameters);
			ResultReceiver result_receiver = new ResultReceiver(null) {  
	            @Override  
	            protected void onReceiveResult(int result_code, Bundle resultData) {
	            	try {
						result.put("result", result_code);
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	            	done_signal.countDown();
	            }
	       };
			provider_API_command.putExtra(ProviderAPI.RECEIVER_KEY, result_receiver);
	       
			startService(provider_API_command);
		}
	}
}
