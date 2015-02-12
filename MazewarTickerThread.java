import java.util.*;

public class MazewarTickerThread extends Thread{
	private Map<Integer, MazewarServerHandlerThread> clients;
	private int numthreads;
	
	public MazewarTickerThread(Map<Integer, MazewarServerHandlerThread> clients, int numthreads){
		super("MazewarTickerThread");
		this.clients = clients;
		this.numthreads = numthreads;
		System.out.println("Created MazewarTickerThread");
		
	}
	// might need a while loop in run()
	// TickerThread should only be created on receiving a 'fire' packet ?
	//added forever loop. 
	//ticker is like a server-side clock that broadcast the tick signal every 200ms
	public void run(){
		try{
			Thread.sleep(15000); // wait for 15 seconds
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		while(numthreads < 2);
		MazewarPacket startpkt =  new MazewarPacket();
		startpkt.type = MazewarPacket.MW_START;
		broadcast(startpkt);
		while(true){
		MazewarPacket tik =  new MazewarPacket();
		tik.type = MazewarPacket.MW_TICK;
		broadcast(tik); 
		try{
			Thread.sleep(200);
		}catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	}
	
	public void broadcast(MazewarPacket currPacket) {

		Set<Integer> s = clients.keySet();
		System.out.println("Broadcasting packet type: "+currPacket.type);
		for(int tid : s) {
			MazewarServerHandlerThread t = clients.get(tid);
       			t.send(currPacket);
 		}
	}
} 
