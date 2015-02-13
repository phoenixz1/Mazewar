import java.io.Serializable;
import java.awt.event.KeyEvent;

public class MazewarPacket implements Serializable {

	/* define packet formats */
	public static final int MW_NULL        = 0;
	public static final int MW_JOIN        = 100;
	public static final int MW_REQUEST     = 200;
	public static final int MW_REPLY       = 300;
	public static final int MW_BYE         = 400;
	public static final int MW_START       = 500;
	public static final int MW_TICK	       = 600;
	public static final int MW_INIT	       = 700;
	/* the packet payload */
	
	/* initialized to be a null packet */
	public int type = MW_NULL;
	
	// Client name
	public String cID;
	
	// Key event
	public KeyEvent event = null;	

	// Client's starting position & orientation
	public Point StartPoint = null;
	public String dir = null;

};
