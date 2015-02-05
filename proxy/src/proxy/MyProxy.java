package proxy;

import java.net.*;
import java.io.*;
import java.util.*;

public class MyProxy extends PrivacyProxy {

    //////////////////////////////////////////////////////////////////////////
    //
    // Enhance your proxy by implementing the following three methods:
    //   - manipulateRequestHeaders
    //   - onRequest
    //   - onResponse
    //
    //////////////////////////////////////////////////////////////////////////

    protected HashMap<String, String> onRequest(HashMap<String, String> requestHeaders){
        for (String s: Configs.BLOCKED_HEADERS) {
            requestHeaders.remove(s);
        }
        
        for (String s: Configs.ALTER_HEADERS.keySet()) {
            requestHeaders.put(s, Configs.ALTER_HEADERS.get(s));
        }

        if(Configs.BLOCKED_ADRESSES.contains(requestHeaders.get("Host"))) return null;
        
        // print all the request headers 
        for (String header : requestHeaders.keySet()) {
            log("  REQ: " + header + ": " + requestHeaders.get(header));
        }
        
        return requestHeaders;

        // return the (manipulated) headers, or
        // alternatively, drop this request by returning null
        // return null;
    }


    // The number of valid bytes in the buffer is expressed by the inOctets instance variable
    // e.g. log("I received " + this.inOctets + " bytes");
    protected byte[] onResponse(byte[] originalBytes){
        log("I received " + this.inOctets + " bytes");

        for (String header : responseHeaders.keySet()) {
            log("  RSP: " + header + ": " + responseHeaders.get(header));
            
            if (header.equals("Content-Type") && responseHeaders.get("Content-Type").startsWith("text/html")) {
               //String s = new String(originalBytes);
               //String s2 = s.replaceAll("Nieuws", "Nieuws!");
               //alteredBytes = s2.getBytes();
            }
        }
        
        String content = new String(originalBytes);
        
        for (String s: Configs.CONTENT_REGEXES.keySet()) {
            log(s);
            content = content.replaceAll(s, Configs.CONTENT_REGEXES.get(s));
        }
        
        byte[] alteredBytes = content.getBytes();

        // alter the original response and return it
        
        return alteredBytes;
    }

    
    // Constructor, no need to touch this
    public MyProxy(Socket socket, Boolean autoFlush) {
        super(socket, autoFlush);
    }
}
