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

        // print all the request headers 
        for (String header : requestHeaders.keySet()) {
            if(header.contains("User-Agent")) {
                requestHeaders.put(header, "None of your bussiness");
            } else if (header.contains("Accept-Datatime")) {
                requestHeaders.remove(header);
            } else if (header.equals("TE")) {
                requestHeaders.remove(header);
            //} else if (header.equals("Referer")) {
            //    requestHeaders.remove(header);
            } else if (header.contains("Proxy")) {
                requestHeaders.remove(header);
            //} else if (header.contains("Cache")) {
            //    requestHeaders.remove(header);
            } else if (header.contains("Origin")) {
                requestHeaders.remove(header);
            }
            
            
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
        byte[] alteredBytes = originalBytes;
        log("I received " + this.inOctets + " bytes");

        for (String header : responseHeaders.keySet()) {
            log("  RSP: " + header + ": " + responseHeaders.get(header));

            if (header.equals("Content-Type") && responseHeaders.get("Content-Type").startsWith("text/html")) {
               //String s = new String(originalBytes);
               //String s2 = s.replaceAll("Nieuws", "Nieuws!");
               //alteredBytes = s2.getBytes();
            }

        }

        // alter the original response and return it
        
        return alteredBytes;
    }

    
    // Constructor, no need to touch this
    public MyProxy(Socket socket, Boolean autoFlush) {
        super(socket, autoFlush);
    }
}
