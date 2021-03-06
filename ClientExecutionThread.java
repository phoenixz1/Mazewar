import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.*;
import java.util.concurrent.*;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

public class ClientExecutionThread extends Thread {
	public boolean ispaused;
	public int ACKnum;
	public int defaultport = 8002;
	public int tID;	
	public LinkedBlockingQueue<MazewarPacket> inQueue;
	private LinkedBlockingQueue<MazewarPacket> outQueue;
	private Map<String, Client> players;
	public static String localID;

	// ***Lab3*** map containing connections to all clients
	public  Map<String, SocketInfo> clientsconn;
	
	public ClientExecutionThread (LinkedBlockingQueue<MazewarPacket> _inQueue, LinkedBlockingQueue<MazewarPacket> _outQueue, Map<String, Client> clients, String localID,  Map<String, SocketInfo> clientsconn,boolean ispaused, int ACKnum) 
	{
		super("ClientExecutionThread");
		this.inQueue = _inQueue;
		this.outQueue = _outQueue;
		this.players = clients;
		this.localID = localID;
		this.ACKnum = 0;
		this.clientsconn = clientsconn;
		this.ispaused = ispaused;
		this.ACKnum = ACKnum;

		LocalClient.isleader = false;
	}

	public void run() {
		
		// poll inQueue for packets, read packet, executePacket()
		//MazewarPacket a = new MazewarPacket();
		//a.type = 87;
		//LocalClient.inQueue.put(a);
		while(true){
			//while(LocalClient.inQueue.peek() == null);
			//synchronized(this) {
			if(LocalClient.inQueue.size() != 0) { // Something is in the queue
				//System.out.println("Inside run of executetion thread. localid = "+localID+"size "+LocalClient.inQueue.size());	
				try{
					MazewarPacket head = LocalClient.inQueue.take();
				
					//if(head.type != 805)
					//System.out.println("Inside run of executetion thread. localid = "+localID+"; packet type = "+head.type);
				
					if(head != null) {
						executePacket(head);
					}
				} catch(InterruptedException ex) {
					ex.printStackTrace();
				}
			}
		}
	}
	
	public void executePacket(MazewarPacket pkt) {
	
		String cID = pkt.cID;
		Client c = players.get(cID);
		assert(c != null);
		
		if(pkt.type == MazewarPacket.MW_REQUEST) { // Client event to process
			KeyEvent e = pkt.event;
			// c.execute_command(e);
			
                        // Up-arrow moves forward.
                        if(e.getKeyCode() == KeyEvent.VK_UP) {
                                c.forward();
                        // Down-arrow moves backward.
                        } else if(e.getKeyCode() == KeyEvent.VK_DOWN) {
                                c.backup();
                        // Left-arrow turns left.
                        } else if(e.getKeyCode() == KeyEvent.VK_LEFT) {
                                c.turnLeft();
                        // Right-arrow turns right.
                        } else if(e.getKeyCode() == KeyEvent.VK_RIGHT) {
                                c.turnRight();
                        // Spacebar fires.
                        } else if(e.getKeyCode() == KeyEvent.VK_SPACE) {
                                c.fire();
                        }
			sendack(cID);
		}
		else if(pkt.type == MazewarPacket.MW_BYE) { // Client wants to quit the game
			// Remove the client from the hash map of players active in the game and from the maze
			try {
				if(LocalClient.isleader) {
				// tell server a client has quit
					LocalClient.outStream.writeObject(pkt);
				}
				if(LocalClient.nextclient.equals(pkt.cID)) {
				// update the next player
					LocalClient.nextclient = pkt.nextclient;
				}
				if(localID.equals(pkt.nextclient)){
					MazewarPacket tok = new MazewarPacket();
					tok.type = MazewarPacket.RING_TOKEN;
					LocalClient.inQueue.add(tok);
				}

				LocalClient.clients.remove(pkt.cID);
				LocalClient.clientsconn.remove(pkt.cID);
				c.maze.removeClient(c);
				sendack(pkt.cID);
				LocalClient.p2pthreads.get(pkt.cID).terminate();
				LocalClient.p2pthreads.remove(pkt.cID);
				LocalClient.p2psockets.remove(pkt.cID);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		else if(pkt.type == MazewarPacket.MW_TICK){
			Client clnt = players.get(localID);
			clnt.maze.missiletick();
		}

		else if(pkt.type == MazewarPacket.JOIN_SERV) { //list of clients and leader from naming server
			try {
				clientsconn = pkt.cconns;
	       		     	// SocketInfo clientsocket = new SocketInfo(InetAddress.getLocalHost(), defaultport); //local hostname and desired port of self
				if(clientsconn.isEmpty()){
					LocalClient.isleader = true;
					LocalClient.ticker = new MazewarTickerThread(clientsconn,localID);
					LocalClient.ticker.start();
					pkt.type = MazewarPacket.RING_TOKEN;
					LocalClient.inQueue.put(pkt);
				}
				else{
		    			MazewarPacket leaderpkt = new MazewarPacket();
				    	leaderpkt.type = MazewarPacket.RING_PAUSE;
				    	leaderpkt.cID = localID;
		       		     	// leaderpkt.newsocket = clientsocket;
		       		     	c = players.get(localID);
		       		     	leaderpkt.StartPoint = c.getPoint();
		       		     	leaderpkt.dir = c.getOrientation().toString();
					leaderpkt.leader = pkt.leader;
			    
		       		     	// SocketInfo leaderinfo = clientsconn.get(pkt.leader); 
		       			Iterator i = clientsconn.keySet().iterator();
			
					while (i.hasNext()){
						Object o = i.next();
						assert (o instanceof String);
						String clname = (String) o;
						if(clname.equals(localID)) continue;
						SocketInfo s = clientsconn.get(clname);


						Socket socket = new Socket(s.getInetAddress(),defaultport);
						LocalClient.p2psockets.put(clname, socket);
						if(((String)o).equals(pkt.leader)){
							LocalClient.nextclient = pkt.leader;
							//LocalClient.nextclientSkt = socket;
						}
						//ObjectInputStream newinStream = new ObjectInputStream(socket.getInputStream());
						ClientReceiverThread receivethread = new ClientReceiverThread(socket, inQueue);
						LocalClient.p2pthreads.put(clname, receivethread);
						receivethread.start();
						receivethread.send(leaderpkt);
						//ObjectOutputStream newoutStream = new ObjectOutputStream(socket.getOutputStream());	
						//newoutStream.writeObject(leaderpkt);
						//newoutStream.close();
					}

				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch(InterruptedException ex) {
			ex.printStackTrace();
			}
		}

		else if(pkt.type == MazewarPacket.RING_INFO) { // only new client receives this
			
			//c = players.get(localID);
			c = LocalClient.clients.get(localID);
			RemoteClient newClient = new RemoteClient(pkt.cID, 50);

			//players.put(pkt.cID, newClient);
			LocalClient.clients.put(pkt.cID, newClient);
			c.maze.addRemoteClientScore(newClient, pkt.StartPoint, new Direction(pkt.dir), pkt.score);
			
		}
		else if(pkt.type == MazewarPacket.RING_PAUSE) { //non-leader clients get this
			
				LocalClient.ispaused = true;
			
				//add new client to all maps;
				c = LocalClient.clients.get(localID);
				RemoteClient newClient = new RemoteClient(pkt.cID, 50);
				LocalClient.clients.put(pkt.cID, newClient);
				//players.put(pkt.cID, newClient);
				c.maze.addRemoteClient(newClient, pkt.StartPoint, new Direction(pkt.dir));

				//add new client to ring
				
				if(LocalClient.nextclient == null || (LocalClient.nextclient).equals(pkt.leader)){
					LocalClient.nextclient = pkt.cID;

				}
			
				//ack to new
				MazewarPacket ackpkt = new MazewarPacket();
				ackpkt.type = MazewarPacket.RING_INFO;
				ackpkt.cID = localID;
				//c = players.get(localID);
				c = LocalClient.clients.get(localID);
				ackpkt.StartPoint = c.getPoint();
		       		ackpkt.dir = c.getOrientation().toString();
				ackpkt.leader = pkt.leader;
				ackpkt.score = LocalClient.scoreModel.getScore(c);

				System.out.println("sending ack to " + pkt.cID);
				LocalClient.p2pthreads.get(pkt.cID).send(ackpkt);

				while(LocalClient.ispaused);
			
		}
		else if(pkt.type == MazewarPacket.RING_UNPAUSE) { 
			;
		}
		else if(pkt.type == MazewarPacket.RING_TOKEN) {
			//System.out.println("token received. Calling sendmulticast"players.put);
			LocalClient.ACKnum = 0;
			sendmcast();
		}
		else if(pkt.type == MazewarPacket.MW_ELECTION) {
			// start a ticker thread
			LocalClient.ticker = new MazewarTickerThread(clientsconn, localID);
			LocalClient.ticker.start();
			LocalClient.isleader = true;
			
			MazewarPacket np = new MazewarPacket();
			np.cID = localID;
			np.type = MazewarPacket.MW_ELECTION;
			try{
				LocalClient.outStream.writeObject(np);
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		else if(pkt.type == MazewarPacket.MCAST_REQ) {
			pkt.type = MazewarPacket.RING_UNPAUSE;
			sendmcast(pkt);
		}
		else { // Other types have no actions
			return;
		}
	}

	public void sendmcast() {
		// Dequeue and multicast the head of outqueueplayers.put
		// Send RING_TOKEN to next client
	        // NOTE: Do not send the packet to the local client
	        //   - Process the packet immediately after receiving ACK's from all other clients

		try {				
			//System.out.println("trying sendmcast to others outqueue size: "+ LocalClient.outQueue.size());
			if (LocalClient.outQueue.size() > 0){
				MazewarPacket outPacket = LocalClient.outQueue.take();
				System.out.println("outpacket to send, type = " + outPacket.type);
				int ACKMax = LocalClient.clients.size() - 1;
				Client localClient = LocalClient.clients.get(localID);

				if (!LocalClient.p2pthreads.isEmpty()){
					Iterator i = LocalClient.p2pthreads.keySet().iterator();
		
					while (i.hasNext()){
						Object o = i.next();
						assert (o instanceof String);

						// If the socket is from the local client, skip it
						if (((String)o).equals(localID)) {
						    continue;
						}
		
						LocalClient.p2pthreads.get((String)o).send(outPacket);

					}
			       }
				
				// Wait until all ACK's are received
				// while(LocalClient.ACKnum < ACKMax) ;

				// All ACK's received; process the key event on the local client
				

				KeyEvent e = outPacket.event;
				if(e != null) {
				// Up-arrow moves forward.
				if(e.getKeyCode() == KeyEvent.VK_UP) {
				        localClient.forward();
				// Down-arrow moves backward.
				} else if(e.getKeyCode() == KeyEvent.VK_DOWN) {
				        localClient.backup();
				// Left-arrow turns left.
				} else if(e.getKeyCode() == KeyEvent.VK_LEFT) {
				        localClient.turnLeft();
				// Right-arrow turns right.
				} else if(e.getKeyCode() == KeyEvent.VK_RIGHT) {
				        localClient.turnRight();
				// Spacebar fires.
				} else if(e.getKeyCode() == KeyEvent.VK_SPACE) {
				        localClient.fire();
				} else if (outPacket.type == MazewarPacket.MW_BYE){			            
				    //Mazewar.quit();
				    System.exit(1);
				}
				}
			}

			if (LocalClient.clients.size() > 1) {
				// Event processed; send RING_TOKEN to next client in the ring
				MazewarPacket ringPacket = new MazewarPacket();
				ringPacket.type = MazewarPacket.RING_TOKEN;
				//System.out.println("sending token to " + LocalClient.nextclient);
				LocalClient.p2pthreads.get(LocalClient.nextclient).send(ringPacket);

			}
			else {
				MazewarPacket tok = new MazewarPacket();
				tok.type = MazewarPacket.RING_TOKEN;
				try{
					LocalClient.inQueue.put(tok);
					//System.out.println("sending token to self: " + localID);
				} catch(InterruptedException ex) {
					ex.printStackTrace();
				}
			}
		
		} catch(InterruptedException ex) {
			ex.printStackTrace();
		}
	}

	public static void passtok() {
		try {
			if (LocalClient.clients.size() > 1) {
	
				MazewarPacket ringPacket = new MazewarPacket();
				ringPacket.type = MazewarPacket.RING_TOKEN;
				LocalClient.p2pthreads.get(LocalClient.nextclient).send(ringPacket);

			}
			else {
				MazewarPacket tok = new MazewarPacket();
				tok.type = MazewarPacket.RING_TOKEN;
				
				LocalClient.inQueue.put(tok);
				
			}
		
		} catch(InterruptedException ex) {
			ex.printStackTrace();
		}

	}

	public static void sendmcast(MazewarPacket pkt) {
		// Special multicast to be used only by leader
		// send RING_STOP and RING_RESUME

		
			if (!LocalClient.p2pthreads.isEmpty()){
				Iterator i = LocalClient.p2pthreads.keySet().iterator();
	
				while (i.hasNext()){
					Object o = i.next();
					assert (o instanceof String);

					// If the socket is from the local client, skip it
					if (((String)o).equals(localID)) {
					    continue;
					}

					LocalClient.p2pthreads.get((String)o).send(pkt);

				}
		       }
	
	}

	public void sendack (String client) {
	    // Create an acknowledgement packet
	    MazewarPacket ackPkt = new MazewarPacket();
	    ackPkt.type = MazewarPacket.ACK;
	    LocalClient.p2pthreads.get(client).send(ackPkt);

	}

}
