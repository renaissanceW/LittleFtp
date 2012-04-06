package n.w;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TreeSet;

import android.os.Bundle;
import android.os.Handler;

/*
 * in charge of schedule work
 * the worker don't known the existence of manager
 */

public class TaskManager {

	private static final int WORKER_COUNT	= 3;
	
	private Handler mHandler;
	
	private FtpWorker[] mWorker;
	private TreeSet<Integer> mFreeWorkerPool;
	
	
	private LinkedList<Task> mWaitingQ;
	private LinkedList<Task> mWorkingQ;

	
	public TaskManager(Handler h){
		mHandler = h;
		mWorker = new FtpWorker[WORKER_COUNT];
		for(int i=0; i<WORKER_COUNT; i++){
			mWorker[i] = new FtpWorker(i, mHandler);
		}
		mWaitingQ = new LinkedList<Task>();
		mWorkingQ = new LinkedList<Task>();
		mFreeWorkerPool = new TreeSet<Integer>();

		for(int i = 0; i<WORKER_COUNT; i++){
			mFreeWorkerPool.add(new Integer(i));
		}
		
		String remain = "";
		for(int j: mFreeWorkerPool){
			remain +=" "+j;
		}
		MyLog.d("Manager", "INIT worker: "+remain);
	}
	
	public void sendAllWorkerRequest(int what, Object obj){
		for(FtpWorker worker : mWorker){
			C.sendMessage(worker.getHandler(), what, obj);
		}
	}
	
	public synchronized void addTask(Bundle data) {
		
		Task t = new Task(data, mWaitingQ);
		mWaitingQ.addLast(t);
		MyLog.d("Manager", "ADD "+data);
	}
	
	public synchronized void schedule() {

		int count = mWaitingQ.size() < mFreeWorkerPool.size() ? 
				mWaitingQ.size() : mFreeWorkerPool.size();
		
		if(count==0) return;
		
		for(int i=0; i<count; i++){
			Task task = mWaitingQ.pollFirst();
			int workerId = mFreeWorkerPool.pollFirst();
			FtpWorker worker = mWorker[workerId];
			
			task.mWorkerId = workerId;
			task.mQueue = mWorkingQ;
			mWorkingQ.add(task);		
			C.sendMessage(worker.getHandler(), C.MSG_WORKER_FILEOP, task);
			
			
			
			MyLog.d("Manager", "SCHEDULE "+task.mData+" to "+"Worker "+workerId);
		}
		
	
	}
	
	
	
	
	
	/*
	 * this is triggered by a worker reply
	 * a finish task can be done or canceled
	 */
	public synchronized void finishTask(Task task){
		task.mQueue.remove(task);
		mFreeWorkerPool.add(task.mWorkerId);
		
		if(task.mStatus==Task.STATUS_DONE){
			task.mQueue = null;			
		}
			
		MyLog.d("Manager", "FINISH "+task.mData);
	}
	
	
	/*
	 * this is triggered by the user selection
	 */
	public synchronized void cancelTask(Task task){
		
		if(task.mQueue == mWorkingQ){
			mWorker[task.mWorkerId].cancelTask();		
		}else if(task.mQueue == mWaitingQ){
			mWaitingQ.remove(task);
		}
		
		MyLog.d("Manager", "CANCEL "+task.mData);
	}
	
	
	
	
	public synchronized ArrayList<Task> getAllTask(){
		ArrayList<Task> rlt = new ArrayList<Task>();
		rlt.addAll(mWorkingQ);
		rlt.addAll(mWaitingQ);
		
		
		String logString = "";
		for(Task i : rlt){
			logString = logString + i.mData;
		}
		MyLog.d("Manager", "GETALL "+logString);
		
		return rlt.size()==0? null:rlt;
	}
	
	

}



class Task{
	public static final int STATUS_WAIT 	= 0;
	public static final int STATUS_ING 		= 1;
	public static final int STATUS_DONE 	= 2;
	public static final int STATUS_CANCEL 	= 3;
	public static final int STATUS_FAIL 	= 4;
	public Task(Bundle data, LinkedList<Task> q){
		mData = data;
		mQueue = q;
		mWorkerId = -1;
		mStatus = STATUS_WAIT;
	}
	Bundle mData;
	LinkedList<Task> mQueue;
	int mWorkerId;
	int mStatus;
}
