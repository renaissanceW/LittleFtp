package n.w;

import java.util.Timer;

public class Global {

	private static Global mInstance=null;
	
	public static Global getInstance(){
		if(mInstance==null){
			mInstance = new Global();
		}
		return mInstance;
	}
	
	
	public int mWorkerCount = 1;
	
	/*all timer work share 1 thread*/
	public Timer mTimer = new Timer();
	public Timer mTimer1 = new Timer();
	
}
