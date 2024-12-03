package se.leap.bitmaskclient.base.utils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;

import se.leap.bitmaskclient.base.models.Provider;

public class CredentialsParser {

    public static void parseXml(String xmlString, Provider provider) throws XmlPullParserException, IOException {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xmlString));

            String currentTag = null;
            String ca = null;
            String key = null;
            String cert = null;

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG -> currentTag = parser.getName();
                    case XmlPullParser.TEXT -> {
                        if (currentTag != null) {
                            switch (currentTag) {
                                case "ca" -> {
                                    ca = parser.getText();
                                    ca = ca.trim();
                                }
                                case "key" -> {
                                    key = parser.getText();
                                    key = key.trim();
                                }
                                case "cert" -> {
                                    cert = parser.getText();
                                    cert = cert.trim();
                                }
                            }
                        }
                    }
                    case XmlPullParser.END_TAG -> currentTag = null;
                }
                eventType = parser.next();
            }

            provider.setCaCert(ca);
            provider.setPrivateKeyString(key);
            provider.setVpnCertificate(cert);

   }
}


