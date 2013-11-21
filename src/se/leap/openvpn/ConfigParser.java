package se.leap.openvpn;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Vector;

//! Openvpn Config FIle Parser, probably not 100% accurate but close enough

// And rember, this is valid :)
// --<foo>
// bar
// </foo>
public class ConfigParser {


	private HashMap<String, Vector<Vector<String>>> options = new HashMap<String, Vector<Vector<String>>>();
	public void parseConfig(Reader reader) throws IOException, ConfigParseError {


		BufferedReader br =new BufferedReader(reader);

		@SuppressWarnings("unused")
		int lineno=0;

		while (true){
			String line = br.readLine();
			if(line==null)
				break;
			lineno++;
			Vector<String> args = parseline(line);
			if(args.size() ==0)
				continue;


			if(args.get(0).startsWith("--"))
				args.set(0, args.get(0).substring(2));

			checkinlinefile(args,br);

			String optionname = args.get(0);
			if(!options.containsKey(optionname)) {
				options.put(optionname, new Vector<Vector<String>>());
			}
			options.get(optionname).add(args);
		}
	}
	public void setDefinition(HashMap<String,Vector<Vector<String>>> args) {
		options = args;
	}

	private void checkinlinefile(Vector<String> args, BufferedReader br) throws IOException, ConfigParseError {
		String arg0 = args.get(0);
		// CHeck for <foo>
		if(arg0.startsWith("<") && arg0.endsWith(">")) {
			String argname = arg0.substring(1, arg0.length()-1);
			String inlinefile = VpnProfile.INLINE_TAG;

			String endtag = String.format("</%s>",argname);
			do {
				String line = br.readLine();
				if(line==null){
					throw new ConfigParseError(String.format("No endtag </%s> for starttag <%s> found",argname,argname));
				}
				if(line.equals(endtag))
					break;
				else {
					inlinefile+=line;
					inlinefile+= "\n";					
				}
			} while(true);

			args.clear();
			args.add(argname);
			args.add(inlinefile);
		}

	}

	enum linestate {
		initial,
		readin_single_quote
		, reading_quoted, reading_unquoted, done}

	private boolean space(char c) {
		// I really hope nobody is using zero bytes inside his/her config file
		// to sperate parameter but here we go:
		return Character.isWhitespace(c) || c == '\0';

	}

	public class ConfigParseError extends Exception {
		private static final long serialVersionUID = -60L;

		public ConfigParseError(String msg) {
			super(msg);
		}
	}


	// adapted openvpn's parse function to java
	private Vector<String> parseline(String line) throws ConfigParseError {
		Vector<String> parameters = new Vector<String>(); 

		if (line.length()==0)
			return parameters;


		linestate state = linestate.initial;
		boolean backslash = false;
		char out=0;

		int pos=0;
		String currentarg="";

		do { 
			// Emulate the c parsing ...
			char in;
			if(pos < line.length())
				in = line.charAt(pos);
			else 
				in = '\0';

			if (!backslash && in == '\\' && state != linestate.readin_single_quote)
			{
				backslash = true;
			}
			else
			{
				if (state == linestate.initial)
				{
					if (!space (in))
					{
						if (in == ';' || in == '#') /* comment */
							break;
						if (!backslash && in == '\"')
							state = linestate.reading_quoted;
						else if (!backslash && in == '\'')
							state = linestate.readin_single_quote;
						else
						{
							out = in;
							state = linestate.reading_unquoted;
						}
					}
				}
				else if (state == linestate.reading_unquoted)
				{
					if (!backslash && space (in))
						state = linestate.done;
					else
						out = in;
				}
				else if (state == linestate.reading_quoted)
				{
					if (!backslash && in == '\"')
						state = linestate.done;
					else
						out = in;
				}
				else if (state == linestate.readin_single_quote)
				{
					if (in == '\'')
						state = linestate.done;
					else
						out = in;
				}

				if (state == linestate.done)
				{
					/* ASSERT (parm_len > 0); */
					state = linestate.initial;
					parameters.add(currentarg);
					currentarg = "";
					out =0;
				}

				if (backslash && out!=0)
				{
					if (!(out == '\\' || out == '\"' || space (out)))
					{
						throw new ConfigParseError("Options warning: Bad backslash ('\\') usage");
					}
				}
				backslash = false;
			}

			/* store parameter character */
			if (out!=0)
			{
				currentarg+=out;
			}
		} while (pos++ < line.length());

		return parameters;
	}


	final String[] unsupportedOptions = { "config", 
			"connection", 
			"proto-force", 
			"remote-random",
			"tls-server"

	};

	// Ignore all scripts
	// in most cases these won't work and user who wish to execute scripts will
	// figure out themselves
	final String[] ignoreOptions = { "tls-client",
			"askpass",
			"auth-nocache",
			"up",
			"down",
			"route-up",
			"ipchange",
			"route-up",
			"route-pre-down",
			"auth-user-pass-verify",
			"dhcp-release",
			"dhcp-renew",
			"dh",
			"management-hold",
			"management",
			"management-query-passwords",
			"pause-exit",
			"persist-key",
			"register-dns",
			"route-delay",
			"route-gateway",
			"route-metric",
			"route-method",
			"status",
			"script-security",
			"show-net-up",
			"suppress-timestamps",
			"tmp-dir",
			"tun-ipv6",
			"topology",
			"win-sys",
	};
	
	
	// This method is far too long
	public VpnProfile convertProfile() throws ConfigParseError{
		boolean noauthtypeset=true;
		VpnProfile np = new VpnProfile("converted Profile");
		// Pull, client, tls-client
		np.clearDefaults();

		// XXX we are always client
		if(/*options.containsKey("client") || options.containsKey("pull")*/ true) {
			np.mUsePull=true;
			options.remove("pull");
			options.remove("client");
		}
		
		Vector<String> secret = getOption("secret", 1, 2);
		if(secret!=null) 
		{
			np.mAuthenticationType=VpnProfile.TYPE_STATICKEYS;
			noauthtypeset=false;
			np.mUseTLSAuth=true;
			np.mTLSAuthFilename=secret.get(1);
			if(secret.size()==3)
				np.mTLSAuthDirection=secret.get(2);
			
		}
		
		Vector<Vector<String>> routes = getAllOption("route", 1, 4);
		if(routes!=null) {
			String routeopt = "";
			for(Vector<String> route:routes){
				String netmask = "255.255.255.255";
				if(route.size() >= 3)
					netmask = route.get(2);
				String net = route.get(1);	
				try {
					CIDRIP cidr = new CIDRIP(net, netmask);
					routeopt+=cidr.toString() + " ";
				} catch (ArrayIndexOutOfBoundsException aioob) {
					throw new ConfigParseError("Could not parse netmask of route " + netmask);
				} catch (NumberFormatException ne) {
					throw new ConfigParseError("Could not parse netmask of route " + netmask);
				}
			
			}
			np.mCustomRoutes=routeopt;
		}

		// Also recognize tls-auth [inline] direction ... 
		Vector<Vector<String>> tlsauthoptions = getAllOption("tls-auth", 1, 2);
		if(tlsauthoptions!=null) {
			for(Vector<String> tlsauth:tlsauthoptions) {
				if(tlsauth!=null) 
				{
					if(!tlsauth.get(1).equals("[inline]")) {
						np.mTLSAuthFilename=tlsauth.get(1);
						np.mUseTLSAuth=true;
					}
					if(tlsauth.size()==3)
						np.mTLSAuthDirection=tlsauth.get(2);
				}
			}
		}
		
		Vector<String> direction = getOption("key-direction", 1, 1);
		if(direction!=null)
			np.mTLSAuthDirection=direction.get(1);


		if(getAllOption("redirect-gateway", 0, 5) != null)
			np.mUseDefaultRoute=true;

		Vector<String> dev =getOption("dev",1,1);
		Vector<String> devtype =getOption("dev-type",1,1);

		if( (devtype !=null && devtype.get(1).equals("tun")) ||  
				(dev!=null && dev.get(1).startsWith("tun")) || 
				(devtype ==null && dev == null) ) {
			//everything okay 
		} else {
			throw new ConfigParseError("Sorry. Only tun mode is supported. See the FAQ for more detail");
		}



		Vector<String> mode =getOption("mode",1,1);
		if (mode != null){
			if(!mode.get(1).equals("p2p"))
				throw new ConfigParseError("Invalid mode for --mode specified, need p2p");
		}
		
		Vector<String> port = getOption("port", 1,1);
		if(port!=null){
			np.mServerPort = port.get(1);
		}
		
		Vector<String> proto = getOption("proto", 1,1);
		if(proto!=null){
			np.mUseUdp=isUdpProto(proto.get(1));;
		}

		// Parse remote config
		Vector<String> remote = getOption("remote",1,3);
		if(remote != null){
			switch (remote.size()) {
			case 4:
				np.mUseUdp=isUdpProto(remote.get(3));
			case 3:
				np.mServerPort = remote.get(2);
			case 2:
				np.mServerName = remote.get(1);
			}
		}
		
		// Parse remote config
		Vector<String> location = getOption("location",1,2);
		if(location != null && location.size() == 2){
			np.mLocation = location.get(1).replace("__", ", ");
		}

		Vector<Vector<String>> dhcpoptions = getAllOption("dhcp-option", 2, 2);
		if(dhcpoptions!=null) {
			for(Vector<String> dhcpoption:dhcpoptions) {
				String type=dhcpoption.get(1);
				String arg = dhcpoption.get(2);
				if(type.equals("DOMAIN")) {
					np.mSearchDomain=dhcpoption.get(2);
				} else if(type.equals("DNS")) {
					np.mOverrideDNS=true;
					if(np.mDNS1.equals(VpnProfile.DEFAULT_DNS1))
						np.mDNS1=arg;
					else
						np.mDNS2=arg;
				}
			}
		}

		Vector<String> ifconfig = getOption("ifconfig", 2, 2);
		if(ifconfig!=null) {
			CIDRIP cidr = new CIDRIP(ifconfig.get(1), ifconfig.get(2));
			np.mIPv4Address=cidr.toString();
		}
		
		if(getOption("remote-random-hostname", 0, 0)!=null)
			np.mUseRandomHostname=true;

		if(getOption("float", 0, 0)!=null)
			np.mUseFloat=true;

		if(getOption("comp-lzo", 0, 1)!=null)
			np.mUseLzo=true;

		Vector<String> cipher = getOption("cipher", 1, 1);
		if(cipher!=null)
			np.mCipher= cipher.get(1);

		Vector<String> ca = getOption("ca",1,1);
		if(ca!=null){
			np.mCaFilename = ca.get(1);
		}

		Vector<String> cert = getOption("cert",1,1);
		if(cert!=null){
			np.mClientCertFilename = cert.get(1);
			np.mAuthenticationType = VpnProfile.TYPE_CERTIFICATES;
			noauthtypeset=false;
		}
		Vector<String> key= getOption("key",1,1);
		if(key!=null)
			np.mClientKeyFilename=key.get(1);

		Vector<String> pkcs12 = getOption("pkcs12",1,1);
		if(pkcs12!=null) {
			np.mPKCS12Filename = pkcs12.get(1);
			np.mAuthenticationType = VpnProfile.TYPE_KEYSTORE;
			noauthtypeset=false;
		}

		Vector<String> tlsremote = getOption("tls-remote",1,1);
		if(tlsremote!=null){
			np.mRemoteCN = tlsremote.get(1);
			np.mCheckRemoteCN=true;
		} 

		Vector<String> verb = getOption("verb",1,1);
		if(verb!=null){
			np.mVerb=verb.get(1);
		}

		
		if(getOption("nobind", 0, 0) != null)
			np.mNobind=true;
		
		if(getOption("persist-tun", 0,0) != null)
			np.mPersistTun=true;
		
		Vector<String> connectretry = getOption("connect-retry", 1, 1);
		if(connectretry!=null)
			np.mConnectRetry =connectretry.get(1);
		
		Vector<String> connectretrymax = getOption("connect-retry-max", 1, 1);
		if(connectretrymax!=null)
			np.mConnectRetryMax =connectretrymax.get(1);
		
		Vector<Vector<String>> remotetls = getAllOption("remote-cert-tls", 1, 1);
		if(remotetls!=null)
			if(remotetls.get(0).get(1).equals("server"))
				np.mExpectTLSCert=true;
			else
				options.put("remotetls",remotetls);
		
		Vector<String> authuser = getOption("auth-user-pass",0,1);
		if(authuser !=null){
			if(noauthtypeset) {
				np.mAuthenticationType=VpnProfile.TYPE_USERPASS;
			} else if(np.mAuthenticationType==VpnProfile.TYPE_CERTIFICATES) {
				np.mAuthenticationType=VpnProfile.TYPE_USERPASS_CERTIFICATES;
			} else if(np.mAuthenticationType==VpnProfile.TYPE_KEYSTORE) {
				np.mAuthenticationType=VpnProfile.TYPE_USERPASS_KEYSTORE;
			}
			if(authuser.size()>1) {
				// Set option value to password get to get canche to embed later.
				np.mUsername=null;
				np.mPassword=authuser.get(1);
				useEmbbedUserAuth(np,authuser.get(1));
			}
			
		}

		
		// Check the other options

		checkIgnoreAndInvalidOptions(np);
		fixup(np);

		return np;
	}

	private boolean isUdpProto(String proto) throws ConfigParseError {
		boolean isudp;
		if(proto.equals("udp") || proto.equals("udp6"))
			isudp=true;
		else if (proto.equals("tcp-client") ||
				proto.equals("tcp")  || 
				proto.equals("tcp6") ||
				proto.endsWith("tcp6-client"))
			isudp =false;
		else 
			throw new ConfigParseError("Unsupported option to --proto " + proto);
		return isudp;
	}
	
	static public void useEmbbedUserAuth(VpnProfile np,String inlinedata)
	{
		String data = inlinedata.replace(VpnProfile.INLINE_TAG, "");
		String[] parts = data.split("\n");
		if(parts.length >= 2) {
			np.mUsername=parts[0];
			np.mPassword=parts[1];
		}
	}

	private void checkIgnoreAndInvalidOptions(VpnProfile np) throws ConfigParseError {
		for(String option:unsupportedOptions)
			if(options.containsKey(option))
				throw new ConfigParseError(String.format("Unsupported Option %s encountered in config file. Aborting",option));

		for(String option:ignoreOptions)
			// removing an item which is not in the map is no error
			options.remove(option);

		if(options.size()> 0) {
			String custom = "# These Options were found in the config file do not map to config settings:\n";

			for(Vector<Vector<String>> option:options.values()) {
				for(Vector<String> optionsline: option) {
					for (String arg : optionsline)
						custom+= VpnProfile.openVpnEscape(arg) + " ";
				}
				custom+="\n";

			}
			np.mCustomConfigOptions = custom;
			np.mUseCustomConfig=true;

		}
	}


	private void fixup(VpnProfile np) {
		if(np.mRemoteCN.equals(np.mServerName)) {
			np.mRemoteCN="";
		}
	}

	private Vector<String> getOption(String option, int minarg, int maxarg) throws ConfigParseError {
		Vector<Vector<String>> alloptions = getAllOption(option, minarg, maxarg);
		if(alloptions==null)
			return null;
		else
			return alloptions.lastElement();
	}


	private Vector<Vector<String>> getAllOption(String option, int minarg, int maxarg) throws ConfigParseError {
		Vector<Vector<String>> args = options.get(option);
		if(args==null)
			return null;

		for(Vector<String> optionline:args)

			if(optionline.size()< (minarg+1) || optionline.size() > maxarg+1) {
				String err = String.format(Locale.getDefault(),"Option %s has %d parameters, expected between %d and %d",
						option,optionline.size()-1,minarg,maxarg );
				throw new ConfigParseError(err);
			}
		options.remove(option);
		return args;
	}

}




