package proxy;

import java.util.*;

/**
 * Created by Matthias on 05/02/2015.
 */
public class Configs {
    public static List<String> BLOCKED_ADRESSES = new ArrayList<String>();
    
    public static List<String> BLOCKED_HEADERS = new ArrayList<String>();
    
    public static HashMap<String, String> ALTER_HEADERS = new HashMap<String, String>();
    
    public static List<String> REMOVE_CONTENT_REGEXES = new ArrayList<String>();
    
    public static void registerValues(){
        BLOCKED_ADRESSES.add("www.googletagmanager.com");
        
        BLOCKED_HEADERS.add("Referer");
        BLOCKED_HEADERS.add("Proxy-Authorization");
        BLOCKED_HEADERS.add("Pragma");
        BLOCKED_HEADERS.add("Origin");
    }
}
