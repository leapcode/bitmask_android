package se.leap.leapclient;

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
	      this.N = new BigInteger(1, params.N);
	      if( abytes != null )
	      {
	    	  A_LEN = 8*abytes.length;
	    	  /* TODO Why did they put this condition?
	         if( 8*abytes.length != A_LEN )
	            throw new IllegalArgumentException("The abytes param must be "
	               +(A_LEN/8)+" in length, abytes.length="+abytes.length);
	               */
	         this.a = new BigInteger(abytes);
	      }

	      // Calculate x = H(s | H(U | ':' | password))
	      byte[] xb = Util.calculatePasswordHash(username, password, params.s);
	      this.x = new BigInteger(1, xb);
	      this.v = g.modPow(x, N);  // g^x % N
	      
	      serverHash = Util.newDigest();
	      clientHash = Util.newDigest();
	      // H(N)
	      byte[] hn = Util.newDigest().digest(params.N);
	      // H(g)
	      byte[] hg = Util.newDigest().digest(params.g);
	      // clientHash = H(N) xor H(g)
	      byte[] hxg = Util.xor(hn, hg, 20);
	      clientHash.update(hxg);
	      // clientHash = H(N) xor H(g) | H(U)
	      clientHash.update(Util.newDigest().digest(username.getBytes()));
	      // clientHash = H(N) xor H(g) | H(U) | s
	      clientHash.update(params.s);
	      K = null;
	   }
	   
	   /**
	    * @returns The exponential residue (parameter A) to be sent to the server.
	    */
	   public byte[] exponential()
	   {
	      byte[] Abytes = null;
	      if(A == null)
	      {
	         /* If the random component of A has not been specified use a random
	         number */
	         if( a == null )
	         {
	            BigInteger one = BigInteger.ONE;
	            do
	            {
	               a = new BigInteger(A_LEN, Util.getPRNG());
	            } while(a.compareTo(one) <= 0);
	         }
	         A = g.modPow(a, N);
	         Abytes = Util.trim(A.toByteArray());
	         // clientHash = H(N) xor H(g) | H(U) | A
	         clientHash.update(Abytes);
	         // serverHash = A
	         serverHash.update(Abytes);
	      }
	      return Abytes;
	   }
	   
		public byte[] response(byte[] Bbytes) throws NoSuchAlgorithmException {
			// clientHash = H(N) xor H(g) | H(U) | s | A | B
		      clientHash.update(Bbytes);
		      
		      /*
		      var B = new BigInteger(ephemeral, 16);
		      var Bstr = ephemeral;
		      // u = H(A,B)
		      var u = new BigInteger(SHA256(hex2a(Astr + Bstr)), 16);
		      // x = H(s, H(I:p))
		      var x = this.calcX(salt);
		      //S = (B - kg^x) ^ (a + ux)
		      var kgx = k.multiply(g.modPow(x, N));
		      var aux = a.add(u.multiply(x));
		      S = B.subtract(kgx).modPow(aux, N);
		      K = SHA256(hex2a(S.toString(16)));
		      */
		      byte[] hA = Util.newDigest().digest(A.toByteArray());
		      MessageDigest u_digest = Util.newDigest();
		      u_digest.update(A.toByteArray());
		      u_digest.update(Bbytes);
		      clientHash.update(u_digest.digest());
		      byte[] ub = new BigInteger(clientHash.digest()).toByteArray();
		      // Calculate S = (B - g^x) ^ (a + u * x) % N
		      BigInteger B = new BigInteger(1, Bbytes);
		      if( B.compareTo(v) < 0 )
		         B = B.add(N);
		      BigInteger u = new BigInteger(1, ub);
		      BigInteger B_v = B.subtract(v);
		      BigInteger a_ux = a.add(u.multiply(x));
		      BigInteger S = B_v.modPow(a_ux, N);
		      // K = SessionHash(S)
		      MessageDigest sessionDigest = MessageDigest.getInstance(params.hashAlgorithm);
		      K = sessionDigest.digest(S.toByteArray());
		      // clientHash = H(N) xor H(g) | H(U) | A | B | K
		      clientHash.update(K);
		      byte[] M1 = clientHash.digest();
		      return M1;
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

}
