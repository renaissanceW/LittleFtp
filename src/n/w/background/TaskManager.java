package n.w.background;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TreeSet;

import n.w.uitil.C;
import n.w.uitil.MyLog;

import android.os.Bundle;
import android.os.Handler;

/*
 * in charge of schedule work
 * the worker don't known the existence of manager
 */

/*
 * if i should ever rewrite this fucking class 
 * I will seperate the join and leave logic for workers, shit
 * but i'm too lasy to rewrite
 */

public class TaskManager {
	
	

	private int mCurrentWorkerCount;
	/*for dynamically adjust worker pool size*/
	private int mNewWorkerCount;
	
	private Handler mHandler;
	
	private FtpWorker[] mWorker;
	private TreeSet<Integer> mFreeWorkerPool;
	private TreeSet<Integer> mBusyWorkerPool;
	
	
	private LinkedList<Task> mWaitingQ;
	private LinkedList<Task> mWorkingQ;

	
	public TaskManager(Handler h, int c){
		MyLog.d("TaskManager", "workerCount:"+c);
		mCurrentWorkerCount = c;
		mNewWorkerCount = c;
		mHandler = h;
		mWorker = new FtpWorker[C.MAX_WORKER_COUNT];
		for(int i=0; i<mCurrentWorkerCount; i++){
			mWorker[i] = new FtpWorker(i, mHandler, this);
		}
		mWaitingQ = new LinkedList<Task>();
		mWorkingQ = new LinkedList<Task>();
		mFreeWorkerPool = new TreeSet<Integer>();
		mBusyWorkerPool = new TreeSet<Integer>();

		for(int i = 0; i<mCurrentWorkerCount; i++){
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
			if(worker!=null)
				C.sendMessage(worker.getHandler(), what, obj);
		}
	}
	
	public synchronized void killWorkerById(int id){
		C.sendMessage(mWorker[id].getHandler(), C.MSG_WORKER_KILL,null);
		mWorker[id] = null;
		mFreeWorkerPool.remove(id);
		mCurrentWorkerCount--;
		mNewWorkerCount--;
	}
	
	/*c should never exceed MAX_WORKER_COUNT*/
	public synchronized void adjustWorkerPoolSize(int c){
		mNewWorkerCount = c;
		
		if(mCurrentWorkerCount<mNewWorkerCount){/*expand*/
			for(int i=0,j=0;i<mNewWorkerCount-mCurrentWorkerCount;i++){
				while(mWorker[j]!=null)j++;
				mWorker[j] = new FtpWorker(j, mHandler, this);
				//FUCK, we really should wait here
				mFreeWorkerPool.add(j);
			}
			mCurrentWorkerCount = mNewWorkerCount;
		
		}else if(mCurrentWorkerCount>mNewWorkerCount){/*shrink*/
			int want = mCurrentWorkerCount - mNewWorkerCount;
			int count = want > mFreeWorkerPool.size()? mFreeWorkerPool.size():want;
			for(int i=0; i<count; i++){
				int workerId = mFreeWorkerPool.pollFirst();
				//kill the worker
				C.sendMessage(mWorker[workerId].getHandler(), C.MSG_WORKER_KILL,null);
				mWorker[workerId] = null;
			}
			mCurrentWorkerCount -= count;
		}
		
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		schedule();
	}
	
	public synchronized void addTask(Bundle data) {
		
		Task t = new Task(data, mWaitingQ);
		mWaitingQ.addLast(t);
		MyLog.d("Manager", "ADD "+data);
	}
	

	public synchronized void schedule() {

		//try to shrink
		if(mCurrentWorkerCount>mNewWorkerCount){
			int want = mCurrentWorkerCount - mNewWorkerCount;
			int count = want > mFreeWorkerPool.size()? mFreeWorkerPool.size():want;
			for(int i=0; i<count; i++){
				int workerId = mFreeWorkerPool.pollFirst();
				//kill the worker
				C.sendMessage(mWorker[workerId].getHandler(), C.MSG_WORKER_KILL,null);
				mWorker[workerId] = null;
			}
			mCurrentWorkerCount -= count;
		}
		
		//step2 schedule
		int count = mWaitingQ.size() < mFreeWorkerPool.size() ? 
				mWaitingQ.size() : mFreeWorkerPool.size();
		
		if(count==0) return;
		
		for(int i=0; i<count; i++){
			Task task = mWaitingQ.pollFirst();
			int workerId = mFreeWorkerPool.pollFirst();
			mBusyWorkerPool.add(workerId);
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
		mBusyWorkerPool.remove(task.mWorkerId);
		
		if(task.mStatus==Task.STATUS_DONE){
			task.mQueue = null;			
		}
			
		MyLog.d("Manager", "FINISH "+task.mData);
	}
	
	
	/*
	 * this is triggered by the user selection
	 */
	public synchronized void cancelTask(Task[] t){
		
		for (Task task : t) {
			if (task.mQueue == mWorkingQ) {
				mWorker[task.mWorkerId].cancelTask();
			} else if (task.mQueue == mWaitingQ) {
				mWaitingQ.remove(task);
			}
			MyLog.d("Manager", "CANCEL "+task.mData);
		}

	}
	
	
	
	
	public synchronized ArrayList<Task> getAllTask(){
		ArrayList<Task> rlt = new ArrayList<Task>();
		rlt.addAll(mWorkingQ);
		rlt.addAll(mWaitingQ);
		
		return rlt.size()==0? null:rlt;
	}
	
	

}



