import java.io.*;
import java.net.*;
import java.util.*;

public class MazewarServer {

    // Map to store the client names and port numbers in insertion order
    public static final Map<String, Socket> clients = new LinkedHashMap<String, Socket>();


    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        boolean listening = true;
        try {
        	if(args.length == 1) {
        		serverSocket = new ServerSocket(Integer.parseInt(args[0]));
        	} else {
        		System.err.println("ERROR: Invalid arguments!");
        		System.exit(-1);
        	}
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }

	int threadnum = 0;
        while (listening) { // listen and enqueue
		threadnum++;
        	MazewarServerHandlerThread client = new MazewarServerHandlerThread(threadnum, serverSocket.accept(), clients);
		client.start();
        }
        serverSocket.close();
    } 
}

