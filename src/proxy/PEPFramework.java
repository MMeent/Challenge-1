package proxy;

import java.net.*;
import java.io.*;


//////////////////////////////////////////////////////////////////////////
//
// NO CHANGES REQUIRED IN THIS FILE
//   Please go to PEPThread.java to do this challenge
//
//////////////////////////////////////////////////////////////////////////

public class PEPFramework {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        boolean listening = true;

        int port = 8080;
        Boolean autoFlush = true;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
                if (args.length == 2) {
                    if(args[1].equals("--no-flush")) {
                        autoFlush = false;
                    }
                }
            } catch (Exception e) {
                System.err.println("Arguments: [<port> [<--no-flush>]]");
                System.exit(1);
            }
        }

        System.out.println("Starting PEP ...");
        System.out.print("Auto flush: ");
        if(autoFlush) {
            System.out.println("enabled (Add --no-flush for per-thread output)");
        } else {
            System.out.println("disabled");
        }

        try {
            //serverSocket = new ServerSocket(port);
            //serverSocket = new ServerSocket(port, 0, InetAddress.getByName("127.0.0.1"));
            serverSocket = new ServerSocket(port, 0, InetAddress.getByName(null));
            System.out.println("Started on: " + port);
        } catch (IOException e) {
            System.err.println("Could not listen on port: " + port);
            System.exit(-1);
        }

        while (listening) {
            //new PrivacyProxy(serverSocket.accept(), autoFlush).start();
            new MyProxy(serverSocket.accept(), autoFlush).start();
        }
        serverSocket.close();
    }
}
