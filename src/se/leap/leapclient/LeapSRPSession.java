package se.leap.leapclient;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.jboss.security.Util;
import org.jboss.security.srp.SRPClientSession;
import org.jboss.security.srp.SRPParameters;
import org.jboss.security.srp.SRPPermission;

public class LeapSRPSession {

	private SRPParameters params;
	private BigInteger N;
	private BigInteger g;
	private BigInteger x;
	private BigInteger v;
	private byte[] s;
	private BigInteger a;
	private BigInteger A;
	private byte[] K;
	/** The M1 = H(H(N) xor H(g) | H(U) | s | A | B | K) hash */
	private MessageDigest clientHash;
	/** The M2 = H(A | M | K) hash */
	private MessageDigest serverHash;

	private static int A_LEN;

	/** Creates a new SRP server session object from the username, password
	    verifier,
	    @param username, the user ID
	    @param password, the user clear text password
	    @param params, the SRP parameters for the session
	 */
	public LeapSRPSession(String username, char[] password, SRPParameters params)
	{
		this(username, password, params, null);
	}

	/** Creates a new SRP server session object from the username, password
	    verifier,
	    @param username, the user ID
	    @param password, the user clear text password
	    @param params, the SRP parameters for the session
	    @param abytes, the random exponent used in the A public key. This must be
	      8 bytes in length.
	 */
	public LeapSRPSession(String username, char[] password, SRPParameters params,
			byte[] abytes)
	{
		try
		{
			// Initialize the secure random number and message digests
			Util.init();
		}
		catch(NoSuchAlgorithmException e)
		{
		}
		this.params = params;
		this.g = new BigInteger(1, params.g);
		byte[] N_bytes = Util.trim(params.N);
		this.N = new BigInteger(1, N_bytes);
		if( abytes != null ) {
			A_LEN = 8*abytes.length;
			/* TODO Why did they put this condition?
	         if( 8*abytes.length != A_LEN )
	            throw new IllegalArgumentException("The abytes param must be "
	               +(A_LEN/8)+" in length, abytes.length="+abytes.length);
			 */
			this.a = new BigInteger(abytes);
		}

		// Calculate x = H(s | H(U | ':' | password))
		byte[] xb = calculatePasswordHash(username, password, params.s);
		//xb = Util.trim(xb);
		this.x = new BigInteger(1, xb);

		// Calculate v = kg^x mod N
		this.v = calculateV();

		serverHash = newDigest();
		clientHash = newDigest();

		// H(N)
		byte[] hn = newDigest().digest(N_bytes);
		// H(g)
		byte[] hg = newDigest().digest(params.g);
		// clientHash = H(N) xor H(g)
		byte[] hxg = xor(hn, hg, hg.length);
		//hxg = Util.trim(hxg);
		clientHash.update(hxg);
		// clientHash = H(N) xor H(g) | H(U)
		byte[] username_bytes = username.getBytes();
		byte[] username_digest = newDigest().digest(username_bytes);
		clientHash.update(username_digest);
		// clientHash = H(N) xor H(g) | H(U) | s
		byte[] salt_bytes = params.s;
		clientHash.update(salt_bytes);
		K = null;
	}

	private BigInteger calculateV() {
		BigInteger k = new BigInteger("bf66c44a428916cad64aa7c679f3fd897ad4c375e9bbb4cbf2f5de241d618ef0", 16);
		return k.multiply(g.modPow(x, N));  // g^x % N
	}

	/**
	 * @returns The exponential residue (parameter A) to be sent to the server.
	 */
	public byte[] exponential() {
		byte[] Abytes = null;
		if(A == null) {
			/* If the random component of A has not been specified use a random
	         number */
			if( a == null ) {
				BigInteger one = BigInteger.ONE;
				do {
					a = new BigInteger(A_LEN, Util.getPRNG());
				} while(a.compareTo(one) <= 0);
			}
			A = g.modPow(a, N);
			Abytes = A.toByteArray();
			//Abytes = Util.trim(Abytes);
			// clientHash = H(N) xor H(g) | H(U) | A
			clientHash.update(Abytes);
			// serverHash = A
			serverHash.update(Abytes);
		}
		return Abytes;
	}

	public byte[] response(byte[] Bbytes) throws NoSuchAlgorithmException {
		// clientHash = H(N) xor H(g) | H(U) | s | A | B
		//Bbytes = Util.trim(Bbytes);
		clientHash.update(Bbytes);
		// Calculate S = (B - kg^x) ^ (a + u * x) % N
		BigInteger S = calculateS(Bbytes);
		byte[] S_bytes = S.toByteArray();
		//S_bytes = Util.trim(S_bytes);
		// K = SessionHash(S)
		String hash_algorithm = params.hashAlgorithm;
		MessageDigest sessionDigest = MessageDigest.getInstance(hash_algorithm);
		K = sessionDigest.digest(S_bytes);
		// clientHash = H(N) xor H(g) | H(U) | A | B | K
		clientHash.update(K);
		byte[] M1 = clientHash.digest();
		return M1;
	}


	private BigInteger calculateS(byte[] Bbytes) {
		byte[] ub = getU(A.toByteArray(), Bbytes);
		BigInteger B = new BigInteger(1, Bbytes);
		BigInteger u = new BigInteger(1, ub);
		BigInteger B_v = B.subtract(v);
		BigInteger a_ux = a.add(u.multiply(x));
		BigInteger S = B_v.modPow(a_ux, N);

		return S;
	}

	public byte[] getU(byte[] Abytes, byte[] Bbytes) {
		MessageDigest u_digest = newDigest();
		u_digest.update(Util.trim(Abytes));
		u_digest.update(Util.trim(Bbytes));
		return new BigInteger(1, u_digest.digest()).toByteArray();
	}

	/**
	 * @param M2 The server's response to the client's challenge
	 * @returns True if and only if the server's response was correct.
	 */
	public boolean verify(byte[] M2)
	{
		// M2 = H(A | M1 | K)
		byte[] myM2 = serverHash.digest();
		boolean valid = Arrays.equals(M2, myM2);
		return valid;
	}

	/** Returns the negotiated session K, K = SHA_Interleave(S)
	    @return the private session K byte[]
	    @throws SecurityException - if the current thread does not have an
	    getSessionKey SRPPermission.
	 */
	public byte[] getSessionKey() throws SecurityException
	{
		SecurityManager sm = System.getSecurityManager();
		if( sm != null )
		{
			SRPPermission p = new SRPPermission("getSessionKey");
			sm.checkPermission(p);
		}
		return K;
	}

	public MessageDigest newDigest()
	{
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return md;
	}

	public byte[] calculatePasswordHash(String username, char[] password, byte[] salt)
	{
		// Calculate x = H(s | H(U | ':' | password))
		MessageDigest xd = newDigest();
		// Try to convert the username to a byte[] using UTF-8
		byte[] user = null;
		byte[] colon = {};
		try {
			user = username.getBytes("UTF-8");
			colon = ":".getBytes("UTF-8");
		}
		catch(UnsupportedEncodingException e) {
			// Use the default platform encoding
			user = username.getBytes();
			colon = ":".getBytes();
		}
		byte[] passBytes = new byte[2*password.length];
		int passBytesLength = 0;
		for(int p = 0; p < password.length; p ++) {
			int c = (password[p] & 0x00FFFF);
			// The low byte of the char
			byte b0 = (byte) (c & 0x0000FF);
			// The high byte of the char
			byte b1 = (byte) ((c & 0x00FF00) >> 8);
			passBytes[passBytesLength ++] = b0;
			// Only encode the high byte if c is a multi-byte char
			if( c > 255 )
				passBytes[passBytesLength ++] = b1;
		}

		// Build the hash
		xd.update(user);
		xd.update(colon);
		xd.update(passBytes, 0, passBytesLength);
		byte[] h = xd.digest();
		xd.reset();
		xd.update(salt);
		xd.update(h);
		byte[] xb = xd.digest();
		return xb;
	}

	public byte[] xor(byte[] b1, byte[] b2, int length)
	{
		//TODO Check if length matters in the order, when b2 is smaller than b1 or viceversa
		return new BigInteger(1, b1).xor(new BigInteger(1, b2)).toByteArray();
	}
}
