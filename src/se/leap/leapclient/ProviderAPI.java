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
import org.bouncycastle.crypto.agreement.srp.SRP6VerifierGenerator;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.prng.RandomGenerator;
import org.bouncycastle.jcajce.provider.digest.Whirlpool.Digest;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import se.leap.leapclient.ProviderListContent.ProviderItem;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

public class ProviderAPI extends IntentService {
	
	public ProviderAPI() {
		super("ProviderAPI");
		Log.v("ClassName", "Provider API");
	}

	@Override
	protected void onHandleIntent(Intent task_for) {
		final ResultReceiver receiver = task_for.getParcelableExtra("receiver");
		
		Bundle task;
		System.out.println("onHandleIntent called");
		if((task = task_for.getBundleExtra(ConfigHelper.downloadJsonFilesBundleExtra)) != null) {
			if(!downloadJsonFiles(task))
				receiver.send(ConfigHelper.INCORRECTLY_DOWNLOADED_JSON_FILES, Bundle.EMPTY);
			else
				receiver.send(ConfigHelper.CORRECTLY_DOWNLOADED_JSON_FILES, Bundle.EMPTY);
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
		else if ((task = task_for.getBundleExtra(ConfigHelper.srpRegister)) != null) {
			if(!registerWithSRP(task))
				receiver.send(ConfigHelper.SRP_REGISTRATION_FAILED, Bundle.EMPTY);
			else
				receiver.send(ConfigHelper.SRP_REGISTRATION_SUCCESSFUL, Bundle.EMPTY);
		}
		else if ((task = task_for.getBundleExtra(ConfigHelper.srpAuth)) != null) {
			if(!authenticateBySRP(task))
				receiver.send(ConfigHelper.SRP_AUTHENTICATION_FAILED, Bundle.EMPTY);
			else
				receiver.send(ConfigHelper.SRP_AUTHENTICATION_SUCCESSFUL, Bundle.EMPTY);
		}
	}

	private boolean downloadJsonFiles(Bundle task) {
		String cert_url = (String) task.get(ConfigHelper.cert_key);
		String eip_service_json_url = (String) task.get(ConfigHelper.eip_service_key);
		try {
			String cert_string = getStringFromProvider(cert_url);
			JSONObject cert_json = new JSONObject("{ \"certificate\" : \"" + cert_string + "\"}");
			ConfigHelper.saveSharedPref(ConfigHelper.cert_key, cert_json);
			JSONObject eip_service_json = getJSONFromProvider(eip_service_json_url);
			ConfigHelper.saveSharedPref(ConfigHelper.eip_service_key, eip_service_json);
			return true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (JSONException e) {
			ConfigHelper.rescueJSONException(e);
			return false;
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private boolean registerWithSRP(Bundle task) {
		String username = (String) task.get(ConfigHelper.username_key);
		String password = (String) task.get(ConfigHelper.password_key);
		String authentication_server = (String) task.get(ConfigHelper.srp_server_url_key);
		
		BigInteger ng_1024 = new BigInteger(ConfigHelper.NG_1024, 16);
		BigInteger salt = ng_1024.probablePrime(1024, null);
		byte[] salt_in_bytes = salt.toByteArray();
		
		SRP6VerifierGenerator verifier_generator = new SRP6VerifierGenerator();
		verifier_generator.init(ng_1024, ConfigHelper.g, new SHA256Digest());
		BigInteger verifier = verifier_generator.generateVerifier(salt_in_bytes, username.getBytes(), password.getBytes());
		
		return sendRegisterMessage(authentication_server, salt.toString(16), verifier.toString(16), username);
	}
	
	private boolean sendRegisterMessage(String server_url, String password_salt, String password_verifier, String login) {
		DefaultHttpClient client = new LeapHttpClient(getApplicationContext());
		String parameter_chain = "user[password_salt]" + "=" + password_salt + "&" + "user[password_verifier]" + "=" + password_verifier + "&" + "user[login]" + "=" + login;
		HttpPost post = new HttpPost(server_url + "/users.json" + "?" + parameter_chain);

		HttpResponse getResponse;
		try {
			getResponse = client.execute(post);
			HttpEntity responseEntity = getResponse.getEntity();
			String plain_response = new Scanner(responseEntity.getContent()).useDelimiter("\\A").next();
			JSONObject json_response = new JSONObject(plain_response);
			if(!json_response.isNull("errors") || json_response.has("errors")) {
				return false;
			}
			//else if(json_response.getString("password_salt").equalsIgnoreCase(password_salt) && json_response.getString("login").equalsIgnoreCase(login))
			else if(json_response.getBoolean("ok") && json_response.getString("login").equalsIgnoreCase(login))
				return true;
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		return false;
	}
	
	private boolean authenticateBySRP(Bundle task) {
		String username = (String) task.get(ConfigHelper.username_key);
		String password = (String) task.get(ConfigHelper.password_key);
		String authentication_server = (String) task.get(ConfigHelper.srp_server_url_key);
		
		SRP6Client srp_client = new SRP6Client();
		BigInteger n = new BigInteger(ConfigHelper.NG_1024, 16);
		srp_client.init(n, ConfigHelper.g, new SHA256Digest(), new SecureRandom());

		BigInteger salt = BigInteger.probablePrime(1024, null);

		BigInteger clientA = srp_client.generateClientCredentials(salt.toString(16).getBytes(), username.getBytes(), password.getBytes());

		try {
			BigInteger serverB = sendAToSRPServer(authentication_server, username, clientA);

			if(serverB == BigInteger.ZERO)
				return false; // TODO Show error: error trying to start authentication with provider
			
			BigInteger s = srp_client.calculateSecret(serverB);
			
			BigInteger k = new BigInteger(1, MessageDigest.getInstance("SHA-256").digest(s.toByteArray()));
			
			BigInteger m1 = generateM1(k, salt, clientA, serverB, username);
			
			BigInteger m2 = sendM1ToSRPServer(authentication_server, username, m1);
			
			if(m2 == BigInteger.ZERO)
				return false; // TODO Show error: error in M1
			
			boolean verified = verifyM2(m2, k, clientA, serverB, username);
			
			return verified; // TODO If false, Username or password are not correct -> Show a warning and get back to login fragment
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// From sendAToSRPServer or from sendM1ToSRPServer
			e.printStackTrace();
			return false;
		} catch (CryptoException e) {
			// TODO Auto-generated catch block
			// From calculateSecret
			e.printStackTrace();
			return false;
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			// From MessageDigest.getInstance
			e.printStackTrace();
			return false;
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	private BigInteger sendAToSRPServer(String server_url, String username, BigInteger clientA) throws ClientProtocolException, IOException, NumberFormatException, JSONException {
		DefaultHttpClient client = new LeapHttpClient(getApplicationContext());
		String parameter_chain = "A" + "=" + clientA.toString(16) + "&" + "login" + "=" + username;
		HttpPost post = new HttpPost(server_url + "/sessions.json" + "?" + parameter_chain);
	
		HttpResponse getResponse = client.execute(post);
		HttpEntity responseEntity = getResponse.getEntity();
		String plain_response = new Scanner(responseEntity.getContent()).useDelimiter("\\A").next();
		JSONObject json_response = new JSONObject(plain_response);
		if(!json_response.isNull("errors") || json_response.has("errors")) {
			return BigInteger.ZERO;
		}
		List<Cookie> cookies = client.getCookieStore().getCookies();
		if(!cookies.isEmpty()) {
			String session_id = cookies.get(0).getValue();
		}
		return new BigInteger(json_response.getString("B"), 16);
	}

	public BigInteger generateM1(BigInteger K, BigInteger salt, BigInteger clientA, BigInteger serverB, String username) throws NoSuchAlgorithmException {
		String digest_of_N_as_string = new BigInteger(1, MessageDigest.getInstance("SHA-256").digest(hex2ascii(ConfigHelper.NG_1024).getBytes())).toString(16);

		String digest_of_G_as_string = new BigInteger(1, MessageDigest.getInstance("SHA-256").digest(ConfigHelper.g.toString(16).getBytes())).toString(16);

		String xor_n_and_g = hexXor(digest_of_N_as_string, digest_of_G_as_string);

		String digest_of_username_as_string = new BigInteger(MessageDigest.getInstance("SHA-256").digest(username.getBytes())).toString(16);
		
		String m1_source_string = xor_n_and_g + digest_of_username_as_string + salt.toString(16) + clientA.toString(16) + serverB.toString(16) + K.toString(16);
		
		return new BigInteger(1, MessageDigest.getInstance("SHA-256").digest(m1_source_string.getBytes()));
	}

	private boolean verifyM2(BigInteger M2, BigInteger K, BigInteger clientA, BigInteger serverB, String username) throws NoSuchAlgorithmException {
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
		String digest_of_N_as_string = new BigInteger(digest_of_N.digest()).toString();
		
		MessageDigest digest_of_G = MessageDigest.getInstance("SHA-256");
		digest_of_G.update(ConfigHelper.g.toByteArray());
		String digest_of_G_as_string = new BigInteger(digest_of_G.digest()).toString();
		
		String xor_n_and_g = hexXor(digest_of_N_as_string, digest_of_G_as_string);
		
		MessageDigest digest_of_username = MessageDigest.getInstance("SHA-256");
		digest_of_username.update(username.getBytes());
		String digest_of_username_as_string = new BigInteger(digest_of_username.digest()).toString();
		
		MessageDigest my_M1 = MessageDigest.getInstance("SHA-256");
		String m1_source_string = xor_n_and_g + digest_of_username_as_string + clientA.toString() + serverB.toString() + K.toString();
		my_M1.update(m1_source_string.getBytes());
		
		MessageDigest my_own_M2 = MessageDigest.getInstance("SHA-256");
		String m2_source_string = clientA.toString() + new BigInteger(my_M1.digest()).toString() + K.toString();
		
		my_own_M2.update(m2_source_string.getBytes());
		
		return M2 == new BigInteger(my_own_M2.digest());
	}

	private String hexXor(String a, String b) {
	    String str = "";
	    for (int i = 0; i < a.length(); i += 2) {
	    	int xor = 0;
	    	if(a.length() > i + 2)
	    		xor = Integer.parseInt(a.substring(i, 2 + i), 16) ^ Integer.parseInt(b.substring(i, 2 + i), 16);
	    	else
	    		xor = Integer.parseInt(a.substring(i, 1 + i), 16) ^ Integer.parseInt(b.substring(i, 1 + i), 16);
	      String xor_string = String.valueOf(Integer.valueOf(String.valueOf(xor), 16));
	      str += (xor_string.length() == 1) ? ("0" + xor) : xor_string;
	    }
	    return stringToHex(str);
	  }
	
	private String stringToHex(String base)
    {
     StringBuffer buffer = new StringBuffer();
     int intValue;
     for(int x = 0; x < base.length(); x++)
         {
         int cursor = 0;
         intValue = base.charAt(x);
         String binaryChar = new String(Integer.toBinaryString(base.charAt(x)));
         for(int i = 0; i < binaryChar.length(); i++)
             {
             if(binaryChar.charAt(i) == '1')
                 {
                 cursor += 1;
             }
         }
         if((cursor % 2) > 0)
             {
             intValue += 128;
         }
         buffer.append(Integer.toHexString(intValue) + "");
     }
     return buffer.toString();
}
	
	private String hex2ascii(String hex) {
		StringBuilder output = new StringBuilder();
	    for (int i = 0; i < hex.length(); i+=2) {
	        String str = hex.substring(i, i+2);
	        output.append((char)Integer.parseInt(str, 16));
	    }
	    String debug = output.toString();
	    return output.toString();
	}

	private BigInteger sendM1ToSRPServer(String server_url, String username, BigInteger m1) throws ClientProtocolException, IOException, JSONException {
		DefaultHttpClient client = new LeapHttpClient(getApplicationContext());
		String parameter_chain = "client_auth" + "=" + m1.toString(16);
		HttpPut put = new HttpPut(server_url + "/sessions/" + username +".json" + "?" + parameter_chain);
		
		HttpResponse getResponse = client.execute(put);
		HttpEntity responseEntity = getResponse.getEntity();
		String plain_response = new Scanner(responseEntity.getContent()).useDelimiter("\\A").next();
		JSONObject json_response = new JSONObject(plain_response);
		if(!json_response.isNull("errors") || json_response.has("errors")) {
			return BigInteger.ZERO;
		}
		
		List<Cookie> cookies = client.getCookieStore().getCookies();
		String session_id = cookies.get(0).getValue();
		return new BigInteger(json_response.getString("M2"), 16);
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
