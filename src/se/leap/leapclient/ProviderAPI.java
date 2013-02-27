package se.leap.leapclient;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.agreement.srp.SRP6Client;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.prng.RandomGenerator;
import org.bouncycastle.jcajce.provider.digest.Whirlpool.Digest;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import se.leap.leapclient.ProviderListContent.ProviderItem;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

public class ProviderAPI extends IntentService {
	
	public ProviderAPI() {
		super("ProviderAPI");
		Log.v("ClassName", "Provider API");
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void onHandleIntent(Intent task_for) {
		final ResultReceiver receiver = task_for.getParcelableExtra("receiver");
		Bundle task;
		System.out.println("onHandleIntent called");
		if((task = task_for.getBundleExtra(ConfigHelper.downloadJsonFilesBundleExtra)) != null) {
			String cert_url = (String) task.get(ConfigHelper.cert_key);
			String eip_service_json_url = (String) task.get(ConfigHelper.eip_service_key);
			try {
				String cert_string = getStringFromProvider(cert_url);
				JSONObject cert_json = new JSONObject("{ \"certificate\" : \"" + cert_string + "\"}");
				ConfigHelper.saveSharedPref(ConfigHelper.cert_key, cert_json);
				JSONObject eip_service_json = getJSONFromProvider(eip_service_json_url);
				ConfigHelper.saveSharedPref(ConfigHelper.eip_service_key, eip_service_json);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				ConfigHelper.rescueJSONException(e);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		else if ((task = task_for.getBundleExtra(ConfigHelper.downloadNewProviderDotJSON)) != null) {
			boolean custom = true;
			String provider_main_url = (String) task.get(ConfigHelper.provider_key_url);
			String provider_name = provider_main_url.replaceFirst("http[s]?://", "").replaceFirst("\\/", "_");
			String provider_json_url = guessURL(provider_main_url);
			try {
				JSONObject provider_json = getJSONFromProvider(provider_json_url);
				String filename = provider_name + "_provider.json".replaceFirst("__", "_");
				ConfigHelper.saveFile(filename, provider_json.toString());
        		ProviderListContent.addItem(new ProviderItem(provider_name, ConfigHelper.openFileInputStream(filename), custom));
        		receiver.send(ConfigHelper.CUSTOM_PROVIDER_ADDED, Bundle.EMPTY);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else if ((task = task_for.getBundleExtra(ConfigHelper.srpAuth)) != null) {
			String username = "username";//(String) task.get(ConfigHelper.username_key);
			String password = "password";//(String) task.get(ConfigHelper.password_key);
			String authentication_server = "localhost:3000";//(String) task.get(ConfigHelper.srp_server_url_key);
			SRP6Client srp_client = new SRP6Client();
			srp_client.init(new BigInteger(ConfigHelper.NG_1024, 16), ConfigHelper.g, new SHA256Digest(), new SecureRandom());
			// Receive salt from server
			Random random_number_generator = new Random();
			int salt = random_number_generator.nextInt(Integer.valueOf(ConfigHelper.NG_1024));
			byte[] salt_in_bytes = ByteBuffer.allocate(4).putInt(salt).array();
			BigInteger clientA = srp_client.generateClientCredentials(salt_in_bytes, username.getBytes(), password.getBytes());
			//Send A to the server. Doing a http response with cookies?
			//Receive server generated serverB
			try {
				BigInteger serverB = sendParameterToSRPServer(authentication_server, "A", clientA);
				BigInteger s = srp_client.calculateSecret(serverB);
				MessageDigest digest_of_S = MessageDigest.getInstance("SHA-256");
				digest_of_S.update(s.toByteArray(), 0, s.toByteArray().length);
				BigInteger k = new BigInteger(digest_of_S.digest());
				BigInteger m1 = generateM1(k, clientA, serverB, salt, username);
				BigInteger m2 = sendParameterToSRPServer(authentication_server, "M1", m1);
				sendM1(m2, k, clientA, serverB, salt, username);
				boolean verified = verifyM2(m2, k, clientA, serverB, salt, username);
				if(!verified) {
					
				}
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// From sendAToSRPServer or from sendM1ToSRPServer
				e.printStackTrace();
			} catch (CryptoException e) {
				// TODO Auto-generated catch block
				// From calculateSecret
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				// From MessageDigest.getInstance
				e.printStackTrace();
			}
		}
	}
	
	private void sendM1(BigInteger m2, BigInteger k, BigInteger clientA,
			BigInteger serverB, int salt, String username) throws NoSuchAlgorithmException {
		BigInteger M1 = generateM1(k, clientA, serverB, salt, username);
		
	}

	private BigInteger generateM1(BigInteger K, BigInteger clientA, BigInteger serverB, int salt, String username) throws NoSuchAlgorithmException {
		/* https://github.com/leapcode/srp_js/blob/master/src/srp_session.js
		var hashN = SHA256(hex2a(N.toString(16)))
			    var hashG = SHA256(hex2a(g.toString(16)))
			    var hexString = hexXor(hashN, hashG);
			    hexString += SHA256(I);
			    hexString += salt;
			    hexString += Astr;
			    hexString += Bstr;
			    hexString += K
			    M = SHA256(hex2a(hexString));
			    //M2 = H(A, M, K)
			    M2 = SHA256(hex2a(Astr + M + K));
		*/
		MessageDigest digest_of_N = MessageDigest.getInstance("SHA-256");
		digest_of_N.update(ConfigHelper.NG_1024.getBytes());
		String digest_of_N_as_string = new String(digest_of_N.digest());
		
		MessageDigest digest_of_G = MessageDigest.getInstance("SHA-256");
		digest_of_G.update(ConfigHelper.g.toByteArray());
		String digest_of_G_as_string = new String(digest_of_G.digest());
		
		String xor_n_and_g = hexXor(digest_of_N_as_string, digest_of_G_as_string);
		
		MessageDigest digest_of_username = MessageDigest.getInstance("SHA-256");
		digest_of_username.update(username.getBytes());
		String digest_of_username_as_string = new String(digest_of_username.digest());
		
		MessageDigest my_M1 = MessageDigest.getInstance("SHA-256");
		String m1_source_string = xor_n_and_g + digest_of_username_as_string + clientA.toString() + serverB.toString() + K.toString();
		
		my_M1.update(m1_source_string.getBytes());
		
		return new BigInteger(my_M1.digest());
	}

	private boolean verifyM2(BigInteger M2, BigInteger K, BigInteger clientA, BigInteger serverB, int salt, String username) throws NoSuchAlgorithmException {
		/* https://github.com/leapcode/srp_js/blob/master/src/srp_session.js
		var hashN = SHA256(hex2a(N.toString(16)))
			    var hashG = SHA256(hex2a(g.toString(16)))
			    var hexString = hexXor(hashN, hashG);
			    hexString += SHA256(I);
			    hexString += salt;
			    hexString += Astr;
			    hexString += Bstr;
			    hexString += K
			    M = SHA256(hex2a(hexString));
			    //M2 = H(A, M, K)
			    M2 = SHA256(hex2a(Astr + M + K));
		*/
		MessageDigest digest_of_N = MessageDigest.getInstance("SHA-256");
		digest_of_N.update(ConfigHelper.NG_1024.getBytes());
		String digest_of_N_as_string = new String(digest_of_N.digest());
		
		MessageDigest digest_of_G = MessageDigest.getInstance("SHA-256");
		digest_of_G.update(ConfigHelper.g.toByteArray());
		String digest_of_G_as_string = new String(digest_of_G.digest());
		
		String xor_n_and_g = hexXor(digest_of_N_as_string, digest_of_G_as_string);
		
		MessageDigest digest_of_username = MessageDigest.getInstance("SHA-256");
		digest_of_username.update(username.getBytes());
		String digest_of_username_as_string = new String(digest_of_username.digest());
		
		MessageDigest my_M1 = MessageDigest.getInstance("SHA-256");
		String m1_source_string = xor_n_and_g + digest_of_username_as_string + clientA.toString() + serverB.toString() + K.toString();
		my_M1.update(m1_source_string.getBytes());
		
		MessageDigest my_own_M2 = MessageDigest.getInstance("SHA-256");
		String m2_source_string = clientA.toString() + new String(my_M1.digest()) + K.toString();
		
		my_own_M2.update(m2_source_string.getBytes());
		
		return M2 == new BigInteger(my_own_M2.digest());
	}


	  private String hexXor(String a, String b) {
	    String str = "";
	    for (int i = 0; i < a.length(); i += 2) {
	      int xor = Integer.parseInt(a.substring(i, 2), 16) ^ Integer.parseInt(b.substring(i, 2), 16);
	      String xor_string = String.valueOf(Integer.valueOf(String.valueOf(xor), 16));
	      str += (xor_string.length() == 1) ? ("0" + xor) : xor_string;
	    }
	    return str;
	  }

	private BigInteger sendParameterToSRPServer(String server_url, String parameter_name, BigInteger parameter) throws ClientProtocolException, IOException {
		DefaultHttpClient client = new LeapHttpClient(getApplicationContext());
		String parameter_chain = parameter_name + "=" + parameter.toString();
		HttpPost post = new HttpPost(server_url + "?" + parameter_chain);
		// TODO Look for how our srp server sends the serverB (as a cookie?) and how to fetch it from response.
		HttpResponse getResponse = client.execute(post);
		HttpEntity responseEntity = getResponse.getEntity();
		List<Cookie> cookies = client.getCookieStore().getCookies();
		return BigInteger.valueOf((Long.valueOf(cookies.get(0).getValue())));
	}

	private String guessURL(String provider_main_url) {
		return provider_main_url + "/provider.json";
	}

	private String getStringFromProvider(String string_url) throws IOException {
		
		String json_file_content = "";
		
		DefaultHttpClient client = new LeapHttpClient(getApplicationContext());
		HttpGet get = new HttpGet(string_url);
		// Execute the GET call and obtain the response
		HttpResponse getResponse = client.execute(get);
		HttpEntity responseEntity = getResponse.getEntity();
		
		json_file_content = new Scanner(responseEntity.getContent()).useDelimiter("\\A").next();
		
		return json_file_content;
	}
	private JSONObject getJSONFromProvider(String json_url) throws IOException, JSONException {
		String json_file_content = getStringFromProvider(json_url);
		return new JSONObject(json_file_content);
	}
}
