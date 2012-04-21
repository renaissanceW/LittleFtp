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
	
	public boolean mIsMasterConnected=false;
	/*initial worker count*/
	public int mWorkerCount = 1;
	public TaskManager mManager = null;
	
	/*all timer work share 1 thread*/
//	public Timer mMasterTimer = new Timer();
	public Timer mUIUpdateTimer = new Timer();
	
}
