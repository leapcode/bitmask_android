package se.leap.bitmaskclient.test;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.jboss.security.srp.SRPParameters;

import android.test.suitebuilder.annotation.SmallTest;

import se.leap.bitmaskclient.ConfigHelper;
import se.leap.bitmaskclient.LeapSRPSession;

import junit.framework.TestCase;

public class testLeapSRPSession extends TestCase {

	public testLeapSRPSession(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testExponential() {
		byte[] expected_A;
		byte[] a_byte;
		SRPParameters params;
		LeapSRPSession client;
		
		/* Test 1: abytes = 4 */
		expected_A = new BigInteger("44eba0239ddfcc5a488d208df32a89eb00e93e6576b22ba2e4410085a413cf64e9c2f08ebc36a788a0761391150ad4a0507ca43f9ca659e2734f0457a85358c0bb39fa87183c9d3f9f8a3b148dab6303a4e796294f3e956472ba0e2ea5697382acd93c8b8f1b3a7a9d8517eebffd6301bfc8de7f7b701f0878a71faae1e25ad4", 16).toByteArray();
		String username = "username",
				password = "password",
				salt = "64c3289d04a6ecad",
				a = "3565fdc2";
		a_byte = new BigInteger(a, 16).toByteArray();
		params = new SRPParameters(new BigInteger(ConfigHelper.NG_1024, 16).toByteArray(), new BigInteger("2").toByteArray(), new BigInteger(salt, 16).toByteArray(), "SHA-256");
		client = new LeapSRPSession(username, password, params, a_byte);
		
		byte[] A = client.exponential();
		
		assertTrue(Arrays.equals(A, expected_A));
		
		/* Test 1: abytes = 5 */
		a = "67c152a3";
		expected_A = new BigInteger("11acfacc08178d48f95c0e69adb11f6d144dd0980ee6e44b391347592e3bd5e9cb841d243b3d9ac2adb25b367a2558e8829b22dcef96c0934378412383ccf95141c3cb5f17ada20f53a0225f56a07f2b0c0469ed6bbad3646f7b71bdd4bedf5cc6fac244b26d3195d8f55877ff94a925b0c0c8f7273eca733c0355b38360442e", 16).toByteArray();

		a_byte = new BigInteger(a, 16).toByteArray();
		params = new SRPParameters(new BigInteger(ConfigHelper.NG_1024, 16).toByteArray(), new BigInteger("2").toByteArray(), new BigInteger(salt, 16).toByteArray(), "SHA-256");
		client = new LeapSRPSession(username, password, params, a_byte);
		
		A = client.exponential();
		
		assertTrue(Arrays.equals(A, expected_A));
	}

	public void testResponse() throws NoSuchAlgorithmException {
		/* Test 1: with intermediate checks */
		byte[] expected_A = trim(new BigInteger("884380f70a62193bbe3589c4e1dbdc4467b6b5a1b4486e4b779023506fc1f885ae26fa4a5d817b3f38a35f3487b147b82d4bd0069faa64fdc845f7494a78251709e212698e42ced44b0f3849adc73f467afcb26983bd13bdc38906b178003373ddd0ac1d38ce8a39ffa3a7795787207a129a784f4b65ce0b302eb1bcf4045883", 16).toByteArray());
		byte[] expected_x = new BigInteger("4cb937fd74ee3bb53b79a3174d0c07c14131de9c825897cbca52154e74200602", 16).toByteArray();
		byte[] expected_M1 = trim(new BigInteger("e6a8efca2c07ef24e0b69be2d4d4a7e74742a4db7a92228218fec0008f7cc94b", 16).toByteArray());
		String username = "username",
				password = "password",
				salt = "64c3289d04a6ecad",
				a = "8c911355";
		byte[] a_byte = new BigInteger(a, 16).toByteArray();
		SRPParameters params = new SRPParameters(new BigInteger(ConfigHelper.NG_1024, 16).toByteArray(), new BigInteger("2").toByteArray(), new BigInteger(salt, 16).toByteArray(), "SHA-256");
		LeapSRPSession client = new LeapSRPSession(username, password, params, a_byte);
		
		byte[] x = client.calculatePasswordHash(username, password, new BigInteger(salt, 16).toByteArray());
		assertTrue(Arrays.equals(x, expected_x));
		
		byte[] A = client.exponential();
		assertTrue(Arrays.equals(A, expected_A));

		String B = "bc745ba25564fc312f44ea09fb663aa6d95867772e412a6a23f1bc24183e54b32f134372c560f4b3fda19ba7a56b0f84fdcdecc22be6fd256639e918e019691c40a39aa5c9631820e42b28da61b8c75b45afae9d77d63ac8f4dda093762be4a890fbd86061dbd7e5e7c03c4dacde769e0f564df00403e449c0535537f1ba7263";	
		
		byte[] M1 = client.response(new BigInteger(salt, 16).toByteArray(), new BigInteger(B, 16).toByteArray());
		assertTrue(Arrays.equals(M1, expected_M1));

		/* Test 2: no intermediate checks */
		expected_A = trim(new BigInteger("9ffc407afd7e7ecd32a8ea68aa782b0254a7e2197a955b5aa646fc1fc43ff6ef2239f01b7d5b82f152c870d3e69f3321878ca2acda06dd8fb6ce02f41c7ed48061c78697b01cf353f4222311334c707358b6ec067e317527316bfa85b5ec74537e38b5b14c1100d14c96320f385e5b1dcccde07e728c7ef624353167a29ae461", 16).toByteArray());
		expected_M1 = trim(new BigInteger("c3203ec1dd55c96038276456c18c447fb4d2a2f896c73c31d56da1781cae79a8", 16).toByteArray());
		a = "38d5b211";
		
		a_byte = new BigInteger(a, 16).toByteArray();
		params = new SRPParameters(new BigInteger(ConfigHelper.NG_1024, 16).toByteArray(), new BigInteger("2").toByteArray(), new BigInteger(salt, 16).toByteArray(), "SHA-256");
		client = new LeapSRPSession(username, password, params, a_byte);
		x = client.calculatePasswordHash(username, password, new BigInteger(salt, 16).toByteArray());
		A = client.exponential();
		
		B = "b8ca7d93dbe2478966ffe025a9f2fb43b9995ce04af9523ea9a3fa4b132136076aa66ead1597c3da23f477ce1cfaf68b5dcc94e146db06cf8391d14a76ce53aab86067b13c93b135d7be6275669b3f51afec6cc41f19e0afca7c3ad5c4d6ee4c09d4b11bcd12e26c727ee56d173b92eea6926e72cc73deebe12dd6f30f44db8a";	
		M1 = client.response(new BigInteger(salt, 16).toByteArray(), new BigInteger(B, 16).toByteArray());
		
		assertTrue(Arrays.equals(M1, expected_M1));
		
		/* Test 3: With intermediate values */
		expected_M1 = new BigInteger("4c01f65a9bb00f95e435593083040ae1e59e59800c598b42de821c21f3a35223", 16).toByteArray();
		expected_A = new BigInteger("1bceab3047a6f84495fdd5b4dbe891f0b30f870d7d4e38eaef728f6a7d4e9342d8dae8502fdae4f16b718d2e916a38b16b6def45559a5ebae417a1b115ba6b6a0451c7ff174c3e2507d7d1f18ef646fd065bc9ba165a2a0ae4d6da54f060427178b95b0eff745f5c3f8c4f19ea35addc3ce0daf2aca3328c98bafcf98286d115", 16).toByteArray();
		B = "41a7b384f2f52312fa79b9dc650ae894f543aea49800cf9477fbcf63e39cbfe6d422f7126777d645cdf749276a3ae9eb6dfcfdb887f8f60ac4094a343013fcebbd40e95b3153f403ab7bb21ea1315aa018bab6ab84017fcb084b3870a8bf1cb717b39c9a28177c61ce7d1738379be9d192dd9793b50ebc3afabe5e78b0a4b017";
		a = "36ee80ec";
		
		a_byte = new BigInteger(a, 16).toByteArray();
		params = new SRPParameters(new BigInteger(ConfigHelper.NG_1024, 16).toByteArray(), new BigInteger("2").toByteArray(), new BigInteger(salt, 16).toByteArray(), "SHA-256");
		client = new LeapSRPSession(username, password, params, a_byte);
		x = client.calculatePasswordHash(username, password, new BigInteger(salt, 16).toByteArray());
		A = client.exponential();
		
		M1 = client.response(new BigInteger(salt, 16).toByteArray(), new BigInteger(B, 16).toByteArray());
		
		assertTrue(Arrays.equals(M1, expected_M1));
	}
	
	public void testCalculateV() throws NoSuchAlgorithmException {
		String expected_v = "502f3ffddc78b866330550c2c60ebd68427c1793237d770e6390d1f794abd47b6786fa5025728d1ca4ec24cfc7a89808278330099ad66456a7c9c88be570b928f9825ac2ecdee31792335f7fa5fc9a78b692c487aa400c7d5cc5c1f2a3a74634c4afa0159600bbf22bf6dfb1e0d85061e55ce8df6243758066503bcf51c83848cf7184209731f89a90d888934c75798828859babe73c17009bf827723fc1bcd0";
		
		BigInteger k = new BigInteger("bf66c44a428916cad64aa7c679f3fd897ad4c375e9bbb4cbf2f5de241d618ef0", 16);
		BigInteger g = new BigInteger("2", 16);
		BigInteger N = new BigInteger(ConfigHelper.NG_1024, 16);
		BigInteger x = new BigInteger("4cb937fd74ee3bb53b79a3174d0c07c14131de9c825897cbca52154e74200602", 16);
		
		BigInteger v = k.multiply(g.modPow(x, N));  // g^x % N
		
		assertEquals(v.toString(16), expected_v);
		assertTrue(Arrays.equals(v.toByteArray(), new BigInteger(expected_v, 16).toByteArray()));
	}
	
	public void testGetU() throws NoSuchAlgorithmException {
		/* Test 1 */
		String Astr = "46b1d1fe038a617966821bd5bb6af967be1bcd6f54c2db5a474cb80b625870e4616953271501a82198d0c14e72b95cdcfc9ec867027b0389aacb313319d4e81604ccf09ce7841dc333be2e03610ae46ec0c8e06b8e86975e0984cae4d0b61c51f1fe5499a4d4d42460261a3e134f841f2cef4d68a583130ee8d730e0b51a858f";
		String Bstr = "5e1a9ac84b1d9212a0d8f8fe444a34e7da4556a1ef5aebc043ae7276099ccdb305fd7e1c179729e24b484a35c0e33b6a898477590b93e9a4044fc1b8d6bc73db8ac7778f546b25ec3f22e92ab7144e5b974dc58e82a333262063062b6944a2e4393d2a087e4906e6a8cfa0fdfd8a5e5930b7cdb45435cbee7c49dfa1d1216881";
		String ustr = "759c3cfb6bfaccf07eeb8e46fe6ea290291d4d32faca0681830a372983ab0a61";
		
		byte[] Abytes = new BigInteger(Astr, 16).toByteArray();
		byte[] Bbytes = new BigInteger(Bstr, 16).toByteArray();
		byte[] expected_u = new BigInteger(ustr, 16).toByteArray();
		
		MessageDigest u_digest = MessageDigest.getInstance("SHA256");
		u_digest.update(trim(Abytes));
		u_digest.update(trim(Bbytes));
		byte[] u = new BigInteger(1, u_digest.digest()).toByteArray();
		
		assertTrue(Arrays.equals(expected_u, u));
		
		/* Test 2 */
		Astr = "c4013381bdb2fdd901944b9d823360f367c52635b576b9a50d2db77141d357ed391c3ac5fa452c2bbdc35f96bfed21df61627b40aed8f67f21ebf81e5621333f44049d6c9f6ad36464041438350e1f86000a8e3bfb63d4128c18322d2517b0d3ead63fd504a9c8f2156d46e64268110cec5f3ccab54a21559c7ab3ad67fedf90";
		Bstr = "e5d988752e8f265f01b98a1dcdecc4b685bd512e7cd9507f3c29f206c27dac91e027641eed1765c4603bbd7a9aa7fac300ef67dafe611ba2dbe29a32d83d486296f328d38b44c0c211d01d3fe422aac168b6850c87782338969c54594fc87804d4db34910ad4b5452a81027842ac8d8d8288fd44872e4c719ac8fb971d0a33e1";
		ustr = "6510328f913b81ba662e564ee5afb7c395ea27c3c0276fc5ca51f0edecd4baf1";
		
		Abytes = new BigInteger(Astr, 16).toByteArray();
		Bbytes = new BigInteger(Bstr, 16).toByteArray();
		expected_u = new BigInteger(ustr, 16).toByteArray();
		expected_u = trim(expected_u);
		
		u_digest = MessageDigest.getInstance("SHA-256");
		u_digest.update(trim(Abytes));
		u_digest.update(trim(Bbytes));
		u = new BigInteger(1, u_digest.digest()).toByteArray();
		u = trim(u);
		
		assertTrue(Arrays.equals(expected_u, u));
		
		/* Test 3 */
		Astr = "d13973fe4e0e13423cd036caf0912e23a1f9b0c23966f5a5897c8ff17c5cbac8bab7f07d9ac4ee47396a7c68e80ce854c84f243148521277900aaa132a7b93b61e54d742d7f36edb4cdef54bc78cca69ac72653a7ae0fc47ec1e9a84024ea9487a61357e28eddc185e4fe01388e64e6b8f688dd74471d56dd244204522e08483";
		Bstr = "a6701686d9d987a43f06e8497330c8add8febd191a7a975bced0d058eb03ccc6805263349363b2d54ac435b01155dc41c6067287d9b93e3637ab3b7e8bc7d9cf38d9fdbb2ca9ee8ba1946a46cb555cb7dafcc177fcf7a4b0eb1e5db2249949c1fd15e0b7c1b3616f9e2649bdf074ed841efbdc9f29ee8c8bfcedeaed3dc49378";
		ustr = "78414ec80cf44225e7ed386dcf2ceb89837327ccae11b761fc77d48c0307977";
		
		Abytes = new BigInteger(Astr, 16).toByteArray();
		Bbytes = new BigInteger(Bstr, 16).toByteArray();
		expected_u = new BigInteger(ustr, 16).toByteArray();
		expected_u = trim(expected_u);
		
		u_digest = MessageDigest.getInstance("SHA-256");
		u_digest.update(trim(Abytes));
		u_digest.update(trim(Bbytes));
		u = new BigInteger(1, u_digest.digest()).toByteArray();
		u = trim(u);
		
		assertTrue(Arrays.equals(expected_u, u));
		
		/* Test 4 */
		Astr = "ee8bc0cb97dd9c9937759658ff9d791df1dd57b48c5febc2e98af028d0e36eaddf1a3fc555f2bcd6456827e8c7b07ec02a1f365457843bda226bfc1a55c4776879f9df6c916810131ec65a3a4cf473c6a34299d64c91cf26542ea0fc059d24422fc783460c3fafe26bf6f7c24904ae1c5a6421e2f5315030ab007ce8f2c2fd97";
		Bstr = "95ecbd13b28c7f38318fd664ee97d9e853b0d6e9cbff9a3775a3cc5d5077ffc146aec70d9439e75c19a34b67368b8bd7035ba6254e0a260d99b1e253aae2e0d8f4a13e1ed472f3ef0e3086300cd15d059f6be7d7141ee09071b1c5e5d1c83b250a3c8f1a587f8fe59d49aaeb2cfc7e13a5a58bc76cc8baf7f6a647982c67ee49";
		ustr = "e28737c7307c84e4d0866b7cf882f22852a764b109634f77a5eb986a96ffcf9a";
		
		Abytes = new BigInteger(Astr, 16).toByteArray();
		Bbytes = new BigInteger(Bstr, 16).toByteArray();
		expected_u = new BigInteger(ustr, 16).toByteArray();
		expected_u = trim(expected_u);
		assertEquals(new BigInteger(1, expected_u).toString(16), ustr);
		
		u_digest = MessageDigest.getInstance("SHA-256");
		u_digest.update(trim(Abytes));
		u_digest.update(trim(Bbytes));
		u = new BigInteger(1, u_digest.digest()).toByteArray();
		u = trim(u);
		
		assertTrue(Arrays.equals(expected_u, u));
	}
	
	@SmallTest
	public void testCalculatePasswordHash() throws UnsupportedEncodingException, NoSuchAlgorithmException {
		String salt_str = "", username_str = "", password_str = "";
		String expected_inner = "cfb9ae3ec5433076889c4fe5663926e20bf570cc7950a51c889a314fab2f5ed716bde9c1cc91be14",
				expected_x = "9736a5e386a18f35bb08cac0f7c70bdbe120f2efe019874d0eb23b85b1955858";
		
		/* Test 1 */
		salt_str = "cfb9ae3ec5433076"; username_str = "nostradamus"; password_str = "$[[//jjiilajfewahug43a89y¿";
		password_str = password_str.replaceAll("\\\\", "\\\\\\\\");
		// Calculate x = H(s | H(U | ':' | password))
		MessageDigest x_digest = MessageDigest.getInstance("SHA-256");
		
		// Try to convert the username to a byte[] using UTF-8
		byte[] user = null;
		byte[] password_bytes = null;
		byte[] colon = {};

		String encoding = "UTF-8";
		encoding = "ISO-8859-1";
		user = ConfigHelper.trim(username_str.getBytes(encoding));
		colon = ConfigHelper.trim(":".getBytes(encoding));
		password_bytes = ConfigHelper.trim(password_str.getBytes(encoding));
		
		// Build the hash
		x_digest.update(user);
		x_digest.update(colon);
		x_digest.update(password_bytes);
		byte[] h = x_digest.digest();
		byte[] salt_bytes = ConfigHelper.trim(new BigInteger(salt_str, 16).toByteArray());

		x_digest.reset();
		x_digest.update(salt_bytes);
		x_digest.update(h);
		byte[] x_digest_bytes = x_digest.digest();
		assertTrue(new BigInteger(1, x_digest_bytes).toString(16).equalsIgnoreCase(expected_x));
	}
	
	public void testCalculateS() throws NoSuchAlgorithmException {
		String expected_S = "34d71467d0a30c5787e6b4384c7176a724df28f6c1d3b0b7738238621be19080942ca5d4454ab3e5664d42970ea2d42d1188ab246afb1475c481111f30886c79cc99ddd1d711034bc4ac57a0c134fef40470e1ac3386f39abcd1bf9d0cc758a1f38a87b2f7261018cdc6191cd941292c77c320b3c664a9bb1325dd7a2bf62db4";
		
		String B_string = "d390da0991b84e20918764aed9bccf36fc70863f08a1d3fa0aecd9610d8c40d1895afaf3aa9b4582fb5bc673037dfc1bf0ca7d3d190a807dec563fe57d596b3551ec8a6461e0621b77fabfaa29234187063e715e2706522c65f35d89f3914a7cdf40ddd240ef3d22b5534469cf6b3b31e158f817dcf5b6b44b0914d4787d027e";
		String A_string = "c6385e56b9cb8987df3c58ffe6c47bfcd9f9d52f02b68048d72135a8522a0919b41683e3c732a0874e0555b8f288fa067842b1c40a56f1a7d910ad125e71238cb14ba47838f881cabf735d5414f4fcdd6855943621c271c5f013753d439fb124674c79e30d849e9178c780f7c25aded12f053ae41be68289787caf7c3cbb07a4";
		String a_string = "fc1c2d8d";
		
		BigInteger A = new BigInteger(A_string, 16);
		byte[] Bbytes = new BigInteger(B_string, 16).toByteArray();
		BigInteger a = new BigInteger(a_string, 16);
		BigInteger g = ConfigHelper.G;
		BigInteger N = new BigInteger(ConfigHelper.NG_1024, 16);
		BigInteger k = new BigInteger("bf66c44a428916cad64aa7c679f3fd897ad4c375e9bbb4cbf2f5de241d618ef0", 16);
		BigInteger x = new BigInteger("4cb937fd74ee3bb53b79a3174d0c07c14131de9c825897cbca52154e74200602", 16);

		/* GetU */
		MessageDigest u_digest = MessageDigest.getInstance("SHA256");
		u_digest.update(trim(A.toByteArray()));
		u_digest.update(trim(Bbytes));
		byte[] ub = new BigInteger(1, u_digest.digest()).toByteArray();

		BigInteger v =  k.multiply(g.modPow(x, N));  // g^x % N
		String expected_v = "502f3ffddc78b866330550c2c60ebd68427c1793237d770e6390d1f794abd47b6786fa5025728d1ca4ec24cfc7a89808278330099ad66456a7c9c88be570b928f9825ac2ecdee31792335f7fa5fc9a78b692c487aa400c7d5cc5c1f2a3a74634c4afa0159600bbf22bf6dfb1e0d85061e55ce8df6243758066503bcf51c83848cf7184209731f89a90d888934c75798828859babe73c17009bf827723fc1bcd0";
		String v_string = v.toString(16);
		
		BigInteger B = new BigInteger(1, Bbytes);
		BigInteger u = new BigInteger(1, ub);
		String expected_u_string = "2d36f816df24da7b904c904a7e2a2500511df118ced26bda92a63aca792c93b";
		String u_string = u.toString(16);
		
		BigInteger B_v = B.subtract(v);
		BigInteger a_ux = a.add(u.multiply(x));
		byte[] a_ux_byte= a_ux.toByteArray();
		String expected_a_aux_string = "d8d0843a60d471ab46a761b775f8fd28bbbd8affbc704424c79b822ac36b1177925404db910072737df3c5083cf20ebd2f08e1381d80d91e4f6fc99f592203";
		byte[] expected_a_aux_byte = new BigInteger(expected_a_aux_string, 16).toByteArray();
		
		BigInteger S = B_v.modPow(a_ux, N);

		byte[] expected_S_bytes = new BigInteger(expected_S, 16).toByteArray();
		byte[] S_bytes = S.toByteArray();
		assertTrue(Arrays.equals(S_bytes, expected_S_bytes));
		assertEquals(S.toString(16), expected_S);
	}
	
	public void testXor() throws Exception {
		String expected_xor_string = "928ade491bc87bba9eb578701d44d30ed9080e60e542ba0d3b9c20ded9f592bf";
		byte[] expected_xor = new BigInteger(expected_xor_string, 16).toByteArray();
		
		byte[] nbytes = trim(new BigInteger(ConfigHelper.NG_1024, 16).toByteArray());
		byte[] gbytes = trim(ConfigHelper.G.toByteArray());
		
		byte[] ndigest = trim(MessageDigest.getInstance("SHA-256").digest(nbytes));
		byte[] gdigest = MessageDigest.getInstance("SHA-256").digest(gbytes);

		BigInteger ndigest_bigInteger = new BigInteger(1, ndigest);
		String expected_ndigest_string = "494b6a801b379f37c9ee25d5db7cd70ffcfe53d01b7c9e4470eaca46bda24b39";
		String ndigest_string = ndigest_bigInteger.toString(16);
		assertEquals(ndigest_string, expected_ndigest_string);
		
		BigInteger gdigest_bigInteger = new BigInteger(1, gdigest);
		String xor_string = ndigest_bigInteger.xor(gdigest_bigInteger).toString(16);

		byte[] xor = new BigInteger(xor_string, 16).toByteArray();

		assertTrue(Arrays.equals(expected_xor, xor));
	}

	public void testVerify() throws NoSuchAlgorithmException {
		byte[] expected_A = trim(new BigInteger("884380f70a62193bbe3589c4e1dbdc4467b6b5a1b4486e4b779023506fc1f885ae26fa4a5d817b3f38a35f3487b147b82d4bd0069faa64fdc845f7494a78251709e212698e42ced44b0f3849adc73f467afcb26983bd13bdc38906b178003373ddd0ac1d38ce8a39ffa3a7795787207a129a784f4b65ce0b302eb1bcf4045883", 16).toByteArray());
		byte[] expected_x = new BigInteger("4cb937fd74ee3bb53b79a3174d0c07c14131de9c825897cbca52154e74200602", 16).toByteArray();
		byte[] expected_M1 = trim(new BigInteger("e6a8efca2c07ef24e0b69be2d4d4a7e74742a4db7a92228218fec0008f7cc94b", 16).toByteArray());
		byte[] expected_M2 = trim(new BigInteger("6402e108415ab4a7cd223ec435570614c8aacc09fcf081ade2dc00275e90ceee", 16).toByteArray());
		String username = "username",
				password = "password",
				salt = "64c3289d04a6ecad",
				a = "8c911355";
		byte[] a_byte = new BigInteger(a, 16).toByteArray();
		SRPParameters params = new SRPParameters(new BigInteger(ConfigHelper.NG_1024, 16).toByteArray(), new BigInteger("2").toByteArray(), new BigInteger(salt, 16).toByteArray(), "SHA-256");
		LeapSRPSession client = new LeapSRPSession(username, password, params, a_byte);
		
		byte[] x = client.calculatePasswordHash(username, password, new BigInteger(salt, 16).toByteArray());
		assertTrue(Arrays.equals(x, expected_x));
		
		byte[] A = client.exponential();
		assertTrue(Arrays.equals(A, expected_A));

		String B = "bc745ba25564fc312f44ea09fb663aa6d95867772e412a6a23f1bc24183e54b32f134372c560f4b3fda19ba7a56b0f84fdcdecc22be6fd256639e918e019691c40a39aa5c9631820e42b28da61b8c75b45afae9d77d63ac8f4dda093762be4a890fbd86061dbd7e5e7c03c4dacde769e0f564df00403e449c0535537f1ba7263";	
		
		byte[] M1 = client.response(new BigInteger(salt, 16).toByteArray(), new BigInteger(B, 16).toByteArray());
		assertTrue(Arrays.equals(M1, expected_M1));
		
		boolean verified = client.verify(expected_M2);
		assertTrue(verified);
		
		/* Test 2 */
		expected_A = trim(new BigInteger("180a1caf84efe93610a56772edea7b2d20ef3e9f34e578147b5402a898982f33131708233f9ddd2946246703c5db705f0859cca9cfc5b72ad5a05ec0c748545aa083d5b7b1bf06efe6737e9e0fd81b832b5cba983f1b9717041df8114385b93c8c669db06d62c5773b8e8a8f07e98a840a33d04d3448d4bcd2c042387c316750", 16).toByteArray());
		expected_M1 = trim(new BigInteger("a47782f23057a7e06704ea94389589b3c70971a63268acef2aefd74e234dd3c2", 16).toByteArray());
		a = "d89f0e33";
		expected_M2 = trim(new BigInteger("517278a03a0320a52dcb391caf5264d76149d7d9b71ed2b65536233344c550cf", 16).toByteArray());
		
		a_byte = new BigInteger(a, 16).toByteArray();
		params = new SRPParameters(new BigInteger(ConfigHelper.NG_1024, 16).toByteArray(), new BigInteger("2").toByteArray(), new BigInteger(salt, 16).toByteArray(), "SHA-256");
		client = new LeapSRPSession(username, password, params, a_byte);
		x = client.calculatePasswordHash(username, password, new BigInteger(salt, 16).toByteArray());
		A = client.exponential();
		
		B = "5f86fe2f7b7455e877e1760db8d3da1fcd4df0d10ec2a40298f87287bdb2f22c0ea54ff9b1f660cc1666459a7e2fd5501970b317490c3dfd3ba2e18f7be7526b72ea4d01e8f064754b935b107ced0892ce86112cbe32282f929907985fcb29f42c5d4dc32adeb29d12a611cac49cca3fefd2227efadc3989c2e72dd64a003141";	
		M1 = client.response(new BigInteger(salt, 16).toByteArray(), new BigInteger(B, 16).toByteArray());
		
		assertTrue(Arrays.equals(M1, expected_M1));
		
		verified = client.verify(expected_M2);
		assertTrue(verified);
		
		/* Test 3 */
		expected_A = trim(new BigInteger("a9c556c30bf4c1b1fdc1bc9e672ab4751806acc8581042b3779faaf25f85f47dfc58828742e2d2a06c51acbbb9f3fae0e01f64df0775a269f5ee4a6e71bc37b8a368e04b9053d399bc5b809ffd6ecab775a577804f2a5ed2e829f15e6af13bf0b78b6b108cf591bc9960992904fd1433698a51e0d05ee954cf98cbfe7995621e", 16).toByteArray());
		expected_M1 = trim(new BigInteger("0afca3583c4146990ec7312f9f4b4d9cceebc43a19f96709bf3d0a17b11dcc1e", 16).toByteArray());
		a = "50e662d6";
		expected_M2 = trim(new BigInteger("3bfb91c7d04b6da6381fe3d2648d992cdc6bc67b8ee16d1cfa733f786d492261", 16).toByteArray());
		
		a_byte = new BigInteger(a, 16).toByteArray();
		params = new SRPParameters(new BigInteger(ConfigHelper.NG_1024, 16).toByteArray(), new BigInteger("2").toByteArray(), new BigInteger(salt, 16).toByteArray(), "SHA-256");
		client = new LeapSRPSession(username, password, params, a_byte);
		x = client.calculatePasswordHash(username, password, new BigInteger(salt, 16).toByteArray());
		A = client.exponential();
		
		B = "6fe41e8262f4f8bc4ed9f4e1b4802ae3adac9c348e6efc07f16c6f5704b95a1f12325097489372c3936584a37301ebab400a32ac6699f4556da84f076489060527bd50578a317a3ec8b814bf2f4dd9c4adad368610eb638aa81663a205ba26d8f0b9654bf3940357b867cd42725e8532b97a2410a557d291aa55c0b44f249361";	
		M1 = client.response(new BigInteger(salt, 16).toByteArray(), new BigInteger(B, 16).toByteArray());
		
		assertTrue(Arrays.equals(M1, expected_M1));
		
		verified = client.verify(expected_M2);
		assertTrue(verified);
		
		/* Test 4: user abc, password abcdefghi */
		username = "abc";
		password = "abcdefghi";
		salt = "be26aac449a093e5";
		expected_A = trim(new BigInteger("c4013381bdb2fdd901944b9d823360f367c52635b576b9a50d2db77141d357ed391c3ac5fa452c2bbdc35f96bfed21df61627b40aed8f67f21ebf81e5621333f44049d6c9f6ad36464041438350e1f86000a8e3bfb63d4128c18322d2517b0d3ead63fd504a9c8f2156d46e64268110cec5f3ccab54a21559c7ab3ad67fedf90", 16).toByteArray());
		expected_x = trim(new BigInteger("6325967f1a161efd4e2d6e6fabbfccc32be05139cf82b08fb59c0a0db3f34bcf", 16).toByteArray());
		a = "5d4cde29";
		B = "e5d988752e8f265f01b98a1dcdecc4b685bd512e7cd9507f3c29f206c27dac91e027641eed1765c4603bbd7a9aa7fac300ef67dafe611ba2dbe29a32d83d486296f328d38b44c0c211d01d3fe422aac168b6850c87782338969c54594fc87804d4db34910ad4b5452a81027842ac8d8d8288fd44872e4c719ac8fb971d0a33e1";	
		expected_M1 = trim(new BigInteger("e5972ddc53e6190735fc79cd823053a65ffb6041d69480adcba2f6a2dc2f2e86", 16).toByteArray());
		expected_M2 = trim(new BigInteger("8f4552b1021a4de621d8f50f0921c4d20651e702d9d71276f8f6c15b838de018", 16).toByteArray());

		a_byte = new BigInteger(a, 16).toByteArray();
		params = new SRPParameters(new BigInteger(ConfigHelper.NG_1024, 16).toByteArray(), new BigInteger("2").toByteArray(), new BigInteger(salt, 16).toByteArray(), "SHA-256");
		client = new LeapSRPSession(username, password, params, a_byte);
		
		x = client.calculatePasswordHash(username, password, trim(new BigInteger(salt, 16).toByteArray()));
		assertTrue(Arrays.equals(x, expected_x));
		
		A = client.exponential();
		assertTrue(Arrays.equals(A, expected_A));
		
		M1 = client.response(trim(new BigInteger(salt, 16).toByteArray()), new BigInteger(B, 16).toByteArray());
		
		assertTrue(Arrays.equals(M1, expected_M1));
		
		verified = client.verify(expected_M2);
		assertTrue(verified);
		
		/* Test 5: user abc, password abcdefghi */
		username = "abc";
		password = "abcdefghi";
		salt = "be26aac449a093e5";
		expected_A = trim(new BigInteger("d13973fe4e0e13423cd036caf0912e23a1f9b0c23966f5a5897c8ff17c5cbac8bab7f07d9ac4ee47396a7c68e80ce854c84f243148521277900aaa132a7b93b61e54d742d7f36edb4cdef54bc78cca69ac72653a7ae0fc47ec1e9a84024ea9487a61357e28eddc185e4fe01388e64e6b8f688dd74471d56dd244204522e08483", 16).toByteArray());
		expected_x = trim(new BigInteger("6325967f1a161efd4e2d6e6fabbfccc32be05139cf82b08fb59c0a0db3f34bcf", 16).toByteArray());
		a = "fc57e4b1";
		B = "a6701686d9d987a43f06e8497330c8add8febd191a7a975bced0d058eb03ccc6805263349363b2d54ac435b01155dc41c6067287d9b93e3637ab3b7e8bc7d9cf38d9fdbb2ca9ee8ba1946a46cb555cb7dafcc177fcf7a4b0eb1e5db2249949c1fd15e0b7c1b3616f9e2649bdf074ed841efbdc9f29ee8c8bfcedeaed3dc49378";	
		expected_M1 = trim(new BigInteger("0b590fde631566d0d3420a898a9b469656e64bfaff165c146b78964eee7920b8", 16).toByteArray());
		expected_M2 = trim(new BigInteger("04cf3ab3b75dbc4b116ca2fec949bf3deca1e360e016d7ab2b8a49904c534a27", 16).toByteArray());

		a_byte = new BigInteger(a, 16).toByteArray();
		params = new SRPParameters(new BigInteger(ConfigHelper.NG_1024, 16).toByteArray(), new BigInteger("2").toByteArray(), new BigInteger(salt, 16).toByteArray(), "SHA-256");
		client = new LeapSRPSession(username, password, params, a_byte);
		
		x = client.calculatePasswordHash(username, password, trim(new BigInteger(salt, 16).toByteArray()));
		assertTrue(Arrays.equals(x, expected_x));
		
		A = client.exponential();
		assertTrue(Arrays.equals(A, expected_A));
		
		M1 = client.response(trim(new BigInteger(salt, 16).toByteArray()), new BigInteger(B, 16).toByteArray());
		
		assertTrue(Arrays.equals(M1, expected_M1));
		
		verified = client.verify(expected_M2);
		assertTrue(verified);
		
		/* Test 6: user and password of real use */
		username = "parmegv";
		password = "wR\"P}x@_,:k$`Y<i7PH9\\\\zubHtn[-4MoL+$(?k>Yd*s T`-n.";
		salt = "40c3f47b99ce8dc9";
		expected_A = trim(new BigInteger("490b5de7a287c59cefe267441a186ec24f63210fbf28877305f5896eaec5a7245d304ecb2b09d91066e627d7b2c8bf9e5271d882361a435355d1c2d1ac9d3069877189a01d64b2dd73a569e9e96b9a99767dbc02e04c839b09444f48430b113c1827c20b684ae33f5018051169f5acf4913ebd76a205c6f1aa2cc75747687d56", 16).toByteArray());
		String x_string = "9665839759b4fb9684e7438daecbd6e7129b4ebd3e4a107916e9a64bbbf399c9";
		expected_x = trim(new BigInteger(x_string, 16).toByteArray());
		assertEquals(new BigInteger(1, expected_x).toString(16), x_string);
		a = "a72111a2";
		B = "6574ddce3e33c44a77198fa8b3656627e4a24c8786948e79f0c2588febaa485c94b1deb5e420bd3b46f9a34c7862525452ca7a0542c52f939d9f277a013aeceef7353a7741440f6dd2f6c2f1dc07fa5ca003e305c89c876a3035bd04f546b711d44da06a3ba827bc8affbf9ed46de1bfbc670ef9ed7c0bb8cdc588285d13849e";	
		expected_M1 = trim(new BigInteger("03bbcf57aeaec89a3a254bb9650a924ea86aa0fdd83fd7274a75b7083f221cf0", 16).toByteArray());
		expected_M2 = trim(new BigInteger("082cf49ad5a34cc5ca571e3d063aec4bd96e7b96a6d951295180631650a84587", 16).toByteArray());

		a_byte = new BigInteger(a, 16).toByteArray();
		params = new SRPParameters(new BigInteger(ConfigHelper.NG_1024, 16).toByteArray(), new BigInteger("2").toByteArray(), new BigInteger(salt, 16).toByteArray(), "SHA-256");
		client = new LeapSRPSession(username, password, params, a_byte);
		
		x = client.calculatePasswordHash(username, password, trim(new BigInteger(salt, 16).toByteArray()));
		assertTrue(Arrays.equals(x, expected_x));
		assertEquals(new BigInteger(1, expected_x).toString(16), new BigInteger(1, x).toString(16));
		
		A = client.exponential();
		assertTrue(Arrays.equals(A, expected_A));
		
		M1 = client.response(new BigInteger(salt, 16).toByteArray(), new BigInteger(B, 16).toByteArray());
		
		assertTrue(Arrays.equals(M1, expected_M1));
		
		verified = client.verify(expected_M2);
		assertTrue(verified);
		
		/* Test 7: password with ! */
		username = "parmegvtest3";
		password = "holahola!";
		salt = "1bf48d42b9a7ed32";
		expected_A = trim(new BigInteger("4e2fbe8db5d07f33ff1f4303959b5396dcffc1460b6ce8866fd388415f27fe10f9042986ab8682cdcf9a033e651bca89173688989adad854c91bc1918f98d5c82525fb6f328a8cf74ce1436b23821cba5337aaa20a3e5631e4b957053d542f2b5fc456e888371c9d6b94360b37adb2793eca8db100c24887c459e36d729a98e1", 16).toByteArray());
		x_string = "363d1d62dda07b2d987a9739ddb5ec32fcad9c7322fb64e87937f2da86c45d9f";
		expected_x = trim(new BigInteger(x_string, 16).toByteArray());
		assertEquals(new BigInteger(1, expected_x).toString(16), x_string);
		a = "16dd0cf5";
		B = "dd5c9c5e13eb5daa6e7303928b3c826cec520ccef429c0dcb785be34c330d5bb89c99d7d94842b6b5c19cac600f884c50b26989b105f397115df7f3d13c5c7c2f6327cc547fc854ae40f09f1f6a104968bd510243feb104eb559e085fe1d720770be2887a1f424c534a3ab962d82e92458f652328bcf9878f95fdcf463d06193";	
		expected_M1 = trim(new BigInteger("a7ffbff753a547b877f8944339b707b3ce1998da27badf253d56bf39f35308a6", 16).toByteArray());
		expected_M2 = trim(new BigInteger("5cc3d7f0077e978c83acdef14a725af01488c1728f0cf32cd7013d24faf5d901", 16).toByteArray());

		a_byte = new BigInteger(a, 16).toByteArray();
		params = new SRPParameters(new BigInteger(ConfigHelper.NG_1024, 16).toByteArray(), new BigInteger("2").toByteArray(), new BigInteger(salt, 16).toByteArray(), "SHA-256");
		client = new LeapSRPSession(username, password, params, a_byte);
		
		x = client.calculatePasswordHash(username, password, trim(new BigInteger(salt, 16).toByteArray()));
		assertTrue(Arrays.equals(x, expected_x));
		assertEquals(new BigInteger(1, expected_x).toString(16), new BigInteger(1, x).toString(16));
		
		A = client.exponential();
		assertTrue(Arrays.equals(A, expected_A));
		
		M1 = client.response(new BigInteger(salt, 16).toByteArray(), new BigInteger(B, 16).toByteArray());
		
		assertTrue(Arrays.equals(M1, expected_M1));
		
		verified = client.verify(expected_M2);
		assertTrue(verified);
		
		/* Test 8: username and password *was* failing in localhost testProviderAPI*/
		username = "gg";
		password = "password";
		a = "bc925bfb";
		salt = "ff9ebb44e947cf59";
		expected_A = trim(new BigInteger("8f434633414faeaf035a0dea8c1cb7876bb1f8ee80d6fee8ea43ae60c4f9658550d825c25f1ed5c6a5543358bbcb559b76958c8047a2e7e5fe0072bc1f16401bcfa77b57651ff50dd665c6f28c302b37c98495eff397a56befead2e5ceffaace45f2ec200520258adb66df751e815e464656d869454e360d98cbc70f9c64fd4c", 16).toByteArray());
		x_string = "9cad2eca264380dd0b48e3b405e109c1be0615ee6ec92e7105eff5bc3a309fd9";
		expected_x = trim(new BigInteger(x_string, 16).toByteArray());
		assertEquals(new BigInteger(1, expected_x).toString(16), x_string);
		B = "9ca2cd50b4c41047e4aa9e4fac9078ae21175e51e04a23877d6c2044765e39959e9a6a3ede99d08a556c196f51a2be12117681b1ef9d0b0498fb2fa4e88649ab9403e743504e3aaefbce8c5cb474eef8f4724ccd076fd33857de510d8509b67f166d986443bc262d776ec20985f617a7aa86e490290ce5d66332c8b45742a527";	
		expected_M1 = trim(new BigInteger("7a2f768791abaeb954eb7f001bb60d91e6f61e959c8fcdefb58de857af9edaac", 16).toByteArray());
		expected_M2 = trim(new BigInteger("d78da7e0a23c9b87a2f09cdee05c510c105b4a8d471b47402c38f4cdfa49fe6d", 16).toByteArray());

		a_byte = new BigInteger(a, 16).toByteArray();
		params = new SRPParameters(new BigInteger(ConfigHelper.NG_1024, 16).toByteArray(), new BigInteger("2").toByteArray(), new BigInteger(salt, 16).toByteArray(), "SHA-256");
		client = new LeapSRPSession(username, password, params, a_byte);
		
		x = client.calculatePasswordHash(username, password, trim(new BigInteger(salt, 16).toByteArray()));
		assertTrue(Arrays.equals(x, expected_x));
		assertEquals(new BigInteger(1, expected_x).toString(16), new BigInteger(1, x).toString(16));
		
		A = client.exponential();
		assertTrue(Arrays.equals(A, expected_A));
		
		M1 = client.response(new BigInteger(salt, 16).toByteArray(), new BigInteger(B, 16).toByteArray());
		
		assertTrue(Arrays.equals(M1, expected_M1));
		
		verified = client.verify(expected_M2);
		assertTrue(verified);
		
		/* Test 9: username and password *was* failing in localhost testProviderAPI*/
		username = "oo";
		password = "password";
		a = "1322ec50";
		salt = "a93c74934dcadd90";
		expected_A = trim(new BigInteger("c8e9f30a2f67977ee7e61e9ca5af8bd854b6cc98fe01dbe1b1a4cf002c1e2523b7e49f373a600ff85a84867817ec60fec532857812a07f5c6189f6172f133023af75ca4cb98b758bb84620d0aa3cfc74dc69e0507114c0aeab5a75c3ae3f07a919c5729420f03266c26ed41d1846e07de023ec68dd6830e9ebf129cf51abb571", 16).toByteArray());
		x_string = "20470538560c4beb4908e6bfe5b0e00da94223e361302a25c898cbdd3724020";
		expected_x = trim(new BigInteger(x_string, 16).toByteArray());
		assertEquals(new BigInteger(1, expected_x).toString(16), x_string);
		B = "24f98ede155212bea8b1d8bacf8153735ee8114faa824c57c84df55f8d6072ab87f5ae885ce1062939dbaa68ca6e63147c1d2dc1f751e8be20d8a6f87287a2a83fcb1dc9b85dd406d438aeee5ccbc873603cb399627e26e6444e94b3d5d26764e866776c8960fe206bd33febeca9f55f6291dd2cb832eab69e5373f548adeefb";	
		expected_M1 = trim(new BigInteger("1b35c705e563bd5239cdccc6627aa877c3023286f49b4b7c21341d2949ca2d15", 16).toByteArray());
		expected_M2 = trim(new BigInteger("a382025452bad8a6ccd0f703253fda90e7ea7bd0c2d466a389455080a4bd015d", 16).toByteArray());

		a_byte = new BigInteger(a, 16).toByteArray();
		params = new SRPParameters(new BigInteger(ConfigHelper.NG_1024, 16).toByteArray(), new BigInteger("2").toByteArray(), new BigInteger(salt, 16).toByteArray(), "SHA-256");
		client = new LeapSRPSession(username, password, params, a_byte);
		
		x = client.calculatePasswordHash(username, password, trim(new BigInteger(salt, 16).toByteArray()));
		assertTrue(Arrays.equals(x, expected_x));
		assertEquals(new BigInteger(1, expected_x).toString(16), new BigInteger(1, x).toString(16));
		
		A = client.exponential();
		assertTrue(Arrays.equals(A, expected_A));
		
		M1 = client.response(new BigInteger(salt, 16).toByteArray(), new BigInteger(B, 16).toByteArray());
		
		assertTrue(Arrays.equals(M1, expected_M1));
		
		verified = client.verify(expected_M2);
		assertTrue(verified);
		
		/* Test 10: derived from test 11, discovered that password bytes should be in ISO-8859-1*/
		username = "nostradamus";
		password = "$[[//jjiilajfewahug43a89y¿";
		a = "800f0819";
		salt = "cfb9ae3ec5433076";
		expected_A = trim(new BigInteger("2ab09ee2fa01058f2f72fd2142b129f2ec26313801052889bcc4af57ee2e4d5b92c90cdfd6ecd660e82c635b2a091ba1b164e5b371c911ce0c4e69686baa120c58e2e0af84b2adc10da6cdfb0b579a1685032c57fd6ed1306d9713a562eddf5c833725042e825fa1abc7017f74760cb53d8c755ffe628c510022c296d1cd3584", 16).toByteArray());
		x_string = "9736a5e386a18f35bb08cac0f7c70bdbe120f2efe019874d0eb23b85b1955858";
		expected_x = trim(new BigInteger(x_string, 16).toByteArray());
		assertEquals(new BigInteger(1, expected_x).toString(16), x_string);
		B = "2d19fe17dca1bda01044a0f406547895c32a10df2b0e69676de911273a8685d294763c4d16b3663f722b8980126e2c659efd33ffc6435a9594a2539e726c48e365893b3374670bd1958c13f55c2defa8ea9c0f9ba1345a5dca0e78debba434c8b755353d066d42bc5dfe0403fdcacfe5efd25c685f883ee6766c710b775c50f2";	
		expected_M1 = trim(new BigInteger("a33feada1771c6f53e9343f5b9e69d51d4f15043c95fb663b6dd5b1c7af6f66b", 16).toByteArray());
		expected_M2 = trim(new BigInteger("9e99f9adfbfaa7add3626ed6e6aea94c9fa60dab6b8d56ad0cc950548f577d32", 16).toByteArray());

		a_byte = new BigInteger(a, 16).toByteArray();
		params = new SRPParameters(new BigInteger(ConfigHelper.NG_1024, 16).toByteArray(), new BigInteger("2").toByteArray(), new BigInteger(salt, 16).toByteArray(), "SHA-256");
		client = new LeapSRPSession(username, password, params, a_byte);
		
		x = client.calculatePasswordHash(username, password, trim(new BigInteger(salt, 16).toByteArray()));
		assertTrue(Arrays.equals(x, expected_x));
		assertEquals(new BigInteger(1, expected_x).toString(16), new BigInteger(1, x).toString(16));
		
		A = client.exponential();
		assertTrue(Arrays.equals(A, expected_A));
		
		M1 = client.response(new BigInteger(salt, 16).toByteArray(), new BigInteger(B, 16).toByteArray());
		
		assertTrue(Arrays.equals(M1, expected_M1));
		
		verified = client.verify(expected_M2);
		assertTrue(verified);
		
		/* Test 11: username and password failing in localhost testProviderAPI*/
		username = "nostradamus";
		password = "$[['//\"jjiilajfewahug43a89y¿";
		a = "5bfbc968";
		salt = "1bcae1065951bbf5";
		expected_A = trim(new BigInteger("7a74c254d46dd6010a7090e574817a03f32ba13f98ed3c695d96f09c9d334e591771541400e68b6d27a19e734baccf3965ca79c0294ffbf553716b41fbca627c7cd3ea4a0d1c640c22411881696f59ad7ed8ce6ef7010e43f57fb3858aa4c3479dd41e4073afadb6a516c41f649b8cf30dea6366efa711c5106c83ea71b00da4", 16).toByteArray());
		x_string = "9834210874c883db35785ee6648079e13d22450c472d6469192ea775ff50c646";
		expected_x = trim(new BigInteger(x_string, 16).toByteArray());
		assertEquals(new BigInteger(1, expected_x).toString(16), x_string);
		B = "285b00c034da5676dd8938ce6a7b717968fef2e5f479ecca6d95828a6ce809dd37893752c956245b5d13315987c50e57cc68aa4f770ff9ce977ddfd65052f278b90545286cf32b3d18307140514e0fe2269fc0437fb16104358f6fa127dc97281a017582759644862d736f48025f2b35cb1662067c11f2fcf0753e2f72c9e028";	
		expected_M1 = trim(new BigInteger("fedbaff9d9a19efc4eea949b045297a6a3121cf371e2acdda85a2a1ca61c929d", 16).toByteArray());
		expected_M2 = trim(new BigInteger("ffccafa0febc1771a428082b30b7ce409856de4581c7d7d986f5b80015aba0d3", 16).toByteArray());

		a_byte = new BigInteger(a, 16).toByteArray();
		params = new SRPParameters(new BigInteger(ConfigHelper.NG_1024, 16).toByteArray(), new BigInteger("2").toByteArray(), new BigInteger(salt, 16).toByteArray(), "SHA-256");
		client = new LeapSRPSession(username, password, params, a_byte);
		
		x = client.calculatePasswordHash(username, password, trim(new BigInteger(salt, 16).toByteArray()));
		assertTrue(Arrays.equals(x, expected_x));
		assertEquals(new BigInteger(1, expected_x).toString(16), new BigInteger(1, x).toString(16));
		
		A = client.exponential();
		assertTrue(Arrays.equals(A, expected_A));
		
		M1 = client.response(new BigInteger(salt, 16).toByteArray(), new BigInteger(B, 16).toByteArray());
		
		assertTrue(Arrays.equals(M1, expected_M1));
		
		verified = client.verify(expected_M2);
		assertTrue(verified);
	}

	public byte[] trim(byte[] in) {
		if(in.length == 0 || in[0] != 0)
			return in;

		int len = in.length;
		int i = 1;
		while(in[i] == 0 && i < len)
			++i;
		byte[] ret = new byte[len - i];
		System.arraycopy(in, i, ret, 0, len - i);
		return ret;
	}

}
