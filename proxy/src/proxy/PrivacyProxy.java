package proxy;

import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.System;

public abstract class PrivacyProxy extends Thread {
    private Socket socket = null;
    protected Boolean autoFlush = false;
    protected Boolean keepAlive = false;
    protected long threadId;
    protected StringBuilder logOutput = new StringBuilder();
    private static final int BUFFER_SIZE = 512 * 1024;
    protected int inOctets = 0;
    protected HashMap<String, String> requestHeaders = new HashMap<String, String>();
    protected HashMap<String, String> responseHeaders = new HashMap<String, String>();

    public PrivacyProxy(Socket socket, Boolean autoFlush) {
        super("ProxyThread");
        this.socket = socket;
        this.autoFlush = autoFlush;
        try {
            this.socket.setSoTimeout(0);
        } catch (Exception e) {
            log("Something went wrong while setting socket timeout: " + e.getMessage());
        }
        log("New connection from client: " + socket.getRemoteSocketAddress());
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Enhance your proxy by implementing the following three methods:
    //   - manipulateRequestHeaders
    //   - onRequest
    //   - onResponse
    //
    //////////////////////////////////////////////////////////////////////////

    protected abstract HashMap<String, String> onRequest(HashMap<String, String> headers);

    // The number of valid bytes in the buffer is expressed by the inOctets variable
    protected abstract byte[] onResponse(byte[] originalBytes);


    //////////////////////////////////////////////////////////////////////////
    //
    // Helper methods:
    //  - log:                  print debug output to stdout
    //  - printSafe:            print the contents of a byte array (in a safe manner)
    //
    //////////////////////////////////////////////////////////////////////////

    protected void log(String s) {
        if (autoFlush) {
            System.out.println(String.format("<%4d> %s", threadId, s));
        } else {
            logOutput.append(String.format("<%4d> %s%n", threadId, s));
        }
    }

    protected void printSafe(byte[] b) {
        log("[PS] " + new String(b).replaceAll("[^\\p{Graph}\\p{Space}]", "") + "\n");
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // run() loop of proxy.PrivacyProxy, no need to edit anything below this comment block
    //
    //////////////////////////////////////////////////////////////////////////

    public void run() {

        this.threadId = Thread.currentThread().getId();
        try {
            DataOutputStream toClient = new DataOutputStream(socket.getOutputStream());
            InputStream fromClient = socket.getInputStream();

            String inputLine, outputLine;
            int cnt = 0;
            String urlToCall = "";

            Socket connectionToServer;
            DataOutputStream toWeb = null;
            InputStream fromWeb = null;
            byte[] buffer = new byte[BUFFER_SIZE];
            inOctets = fromClient.read(buffer, 0, BUFFER_SIZE);
            //log("from client:");
            //printSafe(buffer);
            String request = "";
            if(inOctets != -1) {
                request = new String(buffer, 0, inOctets);
            }
            int totalInOctets = inOctets;
            boolean dropped = false;
            String method = "";
            String altered = "";
            String firstLine = "";
            //while (inOctets != -1) {
            //request = new String(buffer, 0, inOctets);
            //altered = onRequest(s);
            // Determine the host to connect to based on the HTTP request
            if (cnt == 0) {
                Scanner scanner = new Scanner(request);
                int lineCnt = 0;
                boolean headersDone = false;
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (lineCnt == 0) {
                        // first line, contains HTTP method
                        String[] tokens = line.split(" ");
                        method = tokens[0];
                        urlToCall = tokens[1];
                        firstLine = line;
                        if (!method.equals("GET") && !method.equals("POST") && !method.equals("OPTIONS")) { // && !method.equals("CONNECT")) {
                            log("Unsupported HTTP method '" + method + "', dropping request");
                            dropped = true;
                        }
                    } else if (!line.equals("")) {
                        if (!headersDone) {
                            try {
                                String[] tokens = line.split(":", 2);
                                requestHeaders.put(tokens[0], tokens[1].trim());
                            } catch (ArrayIndexOutOfBoundsException e) {
                                log("Error parsing line: " + line);
                            }
                        } else {
                            if (!method.equals("POST")) {
                                log("unsupported scenario ...");
                            }
                        }
                    } else {
                        // headers done, but there is still content, should be POST
                        headersDone = true;
                    }
                    lineCnt++;
                }


                log("Request for: " + urlToCall);

                requestHeaders = onRequest(requestHeaders);
                if (requestHeaders == null) {
                    log("Dropped request");
                    dropped = true;
                    //break;
                }

                //InetAddress ip = InetAddress.getByName(new URL(urlToCall).getHost());
                if (!dropped) {
                    try {
                        URL url = new URL(urlToCall);
                        String webserverHost = url.getHost();
                        String protocol = url.getProtocol();
                        int webserverPort = url.getPort();
                        if (webserverPort == -1) {
                            webserverPort = url.getDefaultPort();
                        }
                        //log("Connecting to " + webserverHost + " on port " + webserverPort);
                        connectionToServer = new Socket(webserverHost, webserverPort);
                        toWeb = new DataOutputStream(connectionToServer.getOutputStream());
                        fromWeb = connectionToServer.getInputStream();

                        String[] firstLineParts = firstLine.split(" ");
                        firstLineParts[1] = (new URL(urlToCall)).getPath();
                        firstLine = firstLineParts[0] + " " + firstLineParts[1] + " " + firstLineParts[2];
                        altered = firstLine + "\r\n";
                    } catch (MalformedURLException e) {
                        log("Malformed URL: " + urlToCall);
                        log("buffer:");
                        printSafe(buffer);
                        dropped = true;
                    }
                }
            }

            cnt++;

            //if(altered.substring(altered.length()-4).equals("\r\n\r\n")){
            //    requestHeaders = manipulateRequestHeaders(requestHeaders);
            //    altered = altered.split("\r\n")[0] + "\r\n";
            if (!dropped) {
                for (String h : requestHeaders.keySet()) {
                    altered = altered.concat(String.format("%s: %s\r\n", h, requestHeaders.get(h)));
                }
            }
            altered = altered.concat("\r\n");

            if (method.equals("POST")) {
                String originalPayload = request.split("\r\n\r\n")[1];
                altered = altered.concat(originalPayload);
            }
            //printSafe(buffer);
            //inOctets = fromClient.read(buffer, 0, BUFFER_SIZE);
            //totalInOctets += inOctets;
            //} //end of while

            if (!dropped) {
                toWeb.write(altered.getBytes());
                toWeb.flush();
                log("Proxy'd request to the real webserver, waiting for response");

                int cnt2 = 0;
                int contentLength = 0;
                int contentSent = 0;
                int sizeOfHeaders = 0;
                boolean cached = false;
                boolean moved = false;
                boolean chunked = false;
                boolean proxyDone = false;
                byte[] rsp_buffer = new byte[5 * 1024 * 1024];

                inOctets = fromWeb.read(buffer, 0, BUFFER_SIZE);
                responseHeaders = new HashMap<String, String>();
                while (inOctets != -1 && !proxyDone) { // TODO rewrite to do-while?
                    String s = new String(buffer, 0, inOctets);
                    // cnt2 is only used to determine whether we are processing headers, or actual data
                    if (cnt2 == 0) {
                        Scanner scanner = new Scanner(s);
                        boolean headersDone = false;
                        while (scanner.hasNextLine() && !headersDone) {
                            String line = scanner.nextLine();
                            if (line.startsWith("HTTP/")) {
                                log(line);
                                if (line.contains("200")) {
                                    //log("Request was succesful");
                                } else if (line.contains("301")) {
                                    log("Page has moved");
                                    moved = true;
                                } else if (line.contains("304")) {
                                    log("Page not modified");
                                    cached = true;
                                }
                            } else if (line.equals("")) {
                                //log("Reponse: end of headers");
                                headersDone = true;
                            } else {
                                try {
                                    String[] tokens = line.split(":", 2);
                                    responseHeaders.put(tokens[0], tokens[1].trim());
                                } catch (ArrayIndexOutOfBoundsException e) {
                                    log("Could not split line: " + line);
                                }
                            }
                        }

                        log("HTTP response from webserver complete, headers:");
                        if (responseHeaders.containsKey("Content-Length")) {
                            try {
                                contentLength = Integer.parseInt(responseHeaders.get("Content-Length"));
                            } catch (NumberFormatException e) {
                                log("Could not parse Content-Length header: " + responseHeaders.get("Content-Length"));
                            }
                        } else if (responseHeaders.containsKey("Transfer-Encoding")) {
                            chunked = responseHeaders.get("Transfer-Encoding").equals("chunked");
                        }

                        // Determine size of headers
                        String firstPart = new String(buffer);
                        String[] parts = firstPart.split("\r\n\r\n");
                        sizeOfHeaders = parts[0].length() + 4;
                    }

                    if (chunked) {
                        String tmp = new String(buffer);
                        //if(tmp.substring(tmp.length()-7).equals("\r\n0\r\n\r\n")) {
                        //TODO rewrite to regex, \r\n0\r\n.*\r\n$
                        if (tmp.contains("\r\n0\r\n")) {
                            //printSafe(buffer);
                            //log("chunking, got 0, done!");
                            //
                            proxyDone = true;
                            break;
                        }
                    }
                    //try {
                    //    byte[] alteredBytes = onResponse(buffer);
                    //    toClient.write(alteredBytes, 0, inOctets); //TODO double test me, was 'inOctets'
                    //    toClient.flush();
                    //}catch (IOException e) {
                    //    log("Connection to the client seems lost: " + e.getMessage());
                    //    break;
                    //}
                    System.arraycopy(buffer, 0, rsp_buffer, contentSent, inOctets);
                    contentSent += inOctets;
                    // public static void arraycopy(Object src, int srcPos, Object dest, int destPos, int length)
                    if (contentSent >= contentLength + sizeOfHeaders && !chunked || cached) {
                        //log("satisfied, calling break;");
                        //log("contentSent : " + Integer.toString(contentSent));
                        //log("contentLength + headerLength: " + Integer.toString(contentLength) + " + " + sizeOfHeaders);
                        proxyDone = true;
                        break;
                    }
                    if (moved) {
                        //log("page was moved, proxied 304, breaking this connection");
                        proxyDone = true;
                        break;
                    }

                    //toClient.flush();
                    try {
                        //buffer = new byte[BUFFER_SIZE]; //TODO is this really necessary?
                        inOctets = fromWeb.read(buffer, 0, BUFFER_SIZE);
                        if (inOctets == -1) {
                            proxyDone = true;
                            break;
                        }
                    } catch (SocketException e) {
                        log("Could not read anymore from the webserver, reason: " + e.getMessage());
                        proxyDone = true;
                        toClient.flush();
                        break;
                    }
                    cnt2++;
                }
                // now actually proxy the completed response to the client
                try {
                    //log("going to proxy this buffer:");
                    //log("is it the same as:");
                    //printSafe(buffer);
                    byte[] alteredBytes = onResponse(rsp_buffer);
                    //toClient.write(alteredBytes, 0, inOctets); //TODO double test me, was 'inOctets'
                    toClient.write(alteredBytes, 0, contentSent); //TODO double test me, was 'inOctets'
                    toClient.flush();
                } catch (IOException e) {
                    log("Connection to the client seems lost: " + e.getMessage());
                }
            } else {
                log("Dropped the request, not sending anything to a webserver");
            }


            // Clean up!

            if (toClient != null) {
                toClient.close();
            }
            if (fromClient != null) {
                fromClient.close();
            }
            if (socket != null) {
                socket.close();
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
        log("Request done");
        // Printing buffer in case of autoFlush==false
        if (!autoFlush) {
            System.out.println(String.format("Thread %d:%n%s", Thread.currentThread().getId(), logOutput.toString()));
        }
    }
}
