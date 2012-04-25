package n.w.background;

import java.util.LinkedList;

import android.os.Bundle;

public class Task {
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
	
	LinkedList<Task> mQueue;
	int mWorkerId;
	
	public int mStatus;
	public Bundle mData;
}
