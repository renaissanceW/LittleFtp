package n.w;

import java.util.Timer;

public class Global {
	
	/*The so-called fuck lazy-initialized thread-safe
	 * singleton
	 */
	private Global(){
	}

	private static class GlobalHolder{
		private static Global mInstance= new Global();	
	}
	
	public static Global getInstance(){
		return GlobalHolder.mInstance;
	}
	
	
	
	
	public boolean mIsMasterConnected=false;
	/*initial worker count*/
	public int mWorkerCount = 1;
	public TaskManager mManager = null;
	
	/*all timer work share 1 thread*/
//	public Timer mMasterTimer = new Timer();
	public Timer mUIUpdateTimer = new Timer();
	
}
