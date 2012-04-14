package n.w;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class FtpWorker extends Thread {
	
	private String mHost = null;
	private int mPort = 21;
	private String mUser = null;
	private String mPassword = null;
	private FTPClient mFtp = null;
	
	private Handler mCallerHandler;
	private Handler mHandler;
	private int mId;
	private Timer mTimer;
	
	private TaskManager mManager;
	
	private Task mTask;
	private boolean mCanceled = false;
	
	static final int BUFFER_LEN = 64*1024;
	static final int STATISTIC_COUNT = 25;
	byte[] mBuffer = new byte[BUFFER_LEN];
	
	
	private static DecimalFormat mDf = new DecimalFormat("0.0");
	
	public void cancelTask(){
		mCanceled = true;
	}

	public FtpWorker(int id, Handler h, TaskManager m){
		mId = id;
		mCallerHandler = h;		
		mManager = m;
		start();
	}
	


	public void run() {
		setName("Worker" + mId);
		MyLog.d("Worker", "worker started!");
		Looper.prepare();
		mHandler = new FtpInstanceHandler();
		Looper.loop();
	}
	
	
	class FtpInstanceHandler extends Handler{
		public void handleMessage(Message msg){
			switch(msg.what){
			case C.MSG_WORKER_FILEOP:
				mTask = (Task)msg.obj;
				if(!Connect()){
					sendReply(C.MSG_WORKER_FILEOP_REPLY, C.FTP_OP_FAIL, mTask);
				}else if(mTask.mData.getBoolean("isDir")){
					dirOp();
				}else{
					fileOp();
				}
				break;
			default:
				MyLog.d("FtpInstanceHandler", "unhandled msg"+msg.what);
			}
		}
	}
	
	
	/*there must be only one caller at a time*/
	void sendReply(int what, int status, Object obj){
		mCallerHandler.obtainMessage(what, status, 0, obj).sendToTarget();
	}
	
	
	public Handler getHandler(){
		return mHandler;
	}
	
	
	/*
	 * establish connection
	 * return true if succ
	 * false if failed
	 */
	boolean Connect() {
			
		String host = mTask.mData.getString("host");
		int port = mTask.mData.getInt("port");
		String user = mTask.mData.getString("user");
		String password = mTask.mData.getString("password");
		if (host.equals(mHost) && port == mPort && user.equals(mUser)
				&& password.equals(mPassword) && mFtp!=null && mFtp.isConnected())
		{
			return true;
		}
		
		mHost = host;
		mUser = user;
		mPort = port;
		mPassword = password;
		
		try {
			mFtp = new FTPClient();
			mFtp.connect(mHost, mPort);
			if (!FTPReply.isPositiveCompletion(mFtp.getReplyCode())) {
				mFtp.disconnect();
				return false;
			}

			if (!mFtp.login(mUser, mPassword)) {
				mFtp.logout();
			}
			mFtp.setFileType(FTP.BINARY_FILE_TYPE);
			mFtp.enterLocalPassiveMode();
			mTimer = new Timer();
			mTimer.schedule(new NoopTimerTask(), C.FTP_NOOP_TIME_INTERVAL,
					C.FTP_NOOP_TIME_INTERVAL);
			MyLog.d("Worker", "connection successfully established!");
		} catch (IOException e) {
			e.printStackTrace();
			MyLog.d("Worker", "connection establishment failed replycode: "+mFtp.getReplyCode());
			return false;
		}

		return true;
	}
	
	
	class NoopTimerTask extends TimerTask{
		public void run(){
			try {
				mFtp.sendNoOp();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	void destroyConnection() {

		try {
			mFtp.logout();
			if (mFtp.isConnected()) {
				mFtp.disconnect();
			}

			MyLog.d("Worker", "disconnection success!");
			sendReply(C.MSG_WORKER_DISCONNECT_REPLY, C.FTP_OP_SUCC, null);
			mTimer.cancel();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			MyLog.d("Worker", "disconnection failed!");
			sendReply(C.MSG_WORKER_DISCONNECT_REPLY, C.FTP_OP_FAIL, null);
		}

	}
	
	
	
	void dirOp(){
		String remote = mTask.mData.getString("remote");
		String local = mTask.mData.getString("local");
		int action = mTask.mData.getInt("action");
		long totalSize = mTask.mData.getLong("size");
		mTask.mStatus = Task.STATUS_ING;
		try{
			if(action == C.TASK_ACTION_DOWNLOAD){
				File parent = new File(local);
				parent.mkdir();
				for(FTPFile c : mFtp.listFiles(remote)){			
					String chRemote = remote + "/" + c.getName();
					String chLocal = local + "/" + c.getName();		
					
					Bundle data = C.genTaskBundle(chRemote, chLocal, 
							C.TASK_ACTION_DOWNLOAD, c.getSize(),
							mHost, mPort, mUser, mPassword, 
							c.isDirectory());
					
					MyLog.d("Worker", "add download task remote: "+chRemote+" local: "+chLocal);
					mManager.addTask(data);
				}
			
				
			}else if(action == C.TASK_ACTION_UPLOAD){
				boolean rlt = mFtp.makeDirectory(remote);
				File parent = new File(local);
				for(File c : parent.listFiles()){
					String chRemote = remote + "/" + c.getName();
					String chLocal = local + "/" + c.getName();
					
					Bundle data = C.genTaskBundle(chRemote, chLocal, 
							C.TASK_ACTION_UPLOAD, c.length(),
							mHost, mPort, mUser, mPassword, 
							c.isDirectory());
				
					MyLog.d("Worker", "add download task remote: "+chRemote+" local: "+chLocal);
					mManager.addTask(data);
				}
			}
			
			/*fake*/
			mTask.mData.putString("accSize", C.getSizeStr(totalSize, totalSize));
			mTask.mData.putString("progress", "100%");
			mTask.mData.putInt("progressInt", 100);
			mTask.mData.putString("speed", "1KB/s");
			mTask.mStatus = Task.STATUS_DONE;

			
		}catch(IOException e){
			e.printStackTrace();
		}
		
		
		//MyLog.d("Worker", "END " + ((status == C.FTP_OP_SUCC)? "SUCC " : "FAIL ") + reply);
		sendReply(C.MSG_WORKER_FILEOP_REPLY, C.FTP_OP_SUCC, mTask);
		
	}
	
	
	
	
	

	
	
	void fileOp(){
		String remote = mTask.mData.getString("remote");
		String local = mTask.mData.getString("local");
		int action = mTask.mData.getInt("action");
		long totalSize = mTask.mData.getLong("size");
			
		int len;
		
		long accSize = 0;
		float progress = 0;
		float speed = 0;
		InputStream input;
		OutputStream output;
		
		
		mTask.mStatus = Task.STATUS_ING;
		String reply = "remote:"+remote+" local:"+local+" action:"+action;
		int status = C.FTP_OP_SUCC;	
		MyLog.d("Worker", "START " + reply);

		try {
			
			if (action == C.TASK_ACTION_DOWNLOAD) {
				input = mFtp.retrieveFileStream(remote);
				output = new BufferedOutputStream(new FileOutputStream(local));
				
				if(input==null){
					mTask.mStatus = Task.STATUS_FAIL;
					status = C.FTP_OP_FAIL;
					MyLog.d("Worker", "END FAIL1 replycode: "+mFtp.getReplyCode()+" " + reply);
					sendReply(C.MSG_WORKER_FILEOP_REPLY, status, mTask);
					return;
				}
				
			} else {/* DOWNLOAD */
				output = mFtp.storeFileStream(remote);
				input = new BufferedInputStream(new FileInputStream(local));
				
				if(output==null){
					mTask.mStatus = Task.STATUS_FAIL;
					status = C.FTP_OP_FAIL;
					MyLog.d("Worker", "END FAIL1 replycode: "+mFtp.getReplyCode()+" "+ reply);
					sendReply(C.MSG_WORKER_FILEOP_REPLY, status, mTask);
					return;
				}
			}
			
			long start_time = System.currentTimeMillis();
			long end_time;
			startSpeed();

			while ( !mCanceled && (len = input.read(mBuffer)) != -1) {	
				output.write(mBuffer, 0, len);
				accSize += len;
				
				end_time = System.currentTimeMillis();
				putSpeed(end_time-start_time, len);
				start_time = end_time;
				
				
				
				mTask.mData.putString("accSize", C.getSizeStr(totalSize, accSize));
				
				progress = (float)100*accSize/totalSize;
				
				mTask.mData.putInt("progressInt", (int)progress);
				mTask.mData.putString("progress", mDf.format(progress)+"%");
				speed = getSpeed();
				mTask.mData.putString("speed", mDf.format(speed)+"KB/s");			
		
				
			}
			
			input.close();
			output.close();		
			
			if(mCanceled){
				mFtp.abort();
			}
			
			if (!mFtp.completePendingCommand()) {
				status = C.FTP_OP_FAIL; 
				mFtp.logout();
				mFtp.disconnect();
			}


		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			status = C.FTP_OP_FAIL;
		}
		
		mTask.mStatus = Task.STATUS_DONE;
		if(mCanceled){
			mTask.mStatus = Task.STATUS_CANCEL;
			mCanceled = false;	
		}
		
		MyLog.d("Worker", "END " + ((status == C.FTP_OP_SUCC)? "SUCC " : "FAIL2 ") + reply);
		sendReply(C.MSG_WORKER_FILEOP_REPLY, status, mTask);		
		
	}
	
	
	
	/*we statistic the avg speed of the 
	 * recent 50 read or write calls
	 */
	private static int SPEED_GETTER_Q_LEN = 50;

	class SpeedItem {
		public long time;
		public int length;

		public SpeedItem(long t, int l) {
			time = t;
			length = l;
		}
	}

	private LinkedList<SpeedItem> mSpeedQ;
	
	private void startSpeed(){
		mSpeedQ = new LinkedList<SpeedItem>();
	}
	private void putSpeed(long l, int length) {
		if (mSpeedQ.size() == SPEED_GETTER_Q_LEN) {
			mSpeedQ.removeFirst();
		}
		mSpeedQ.addLast(new SpeedItem(l, length));
	}

	private float getSpeed(){
		int length = 0;
		int time = 0;
		for(SpeedItem item : mSpeedQ){
			time += item.time;
			length += item.length;
		}
		
		return (float)length*1000/(1024*time);
	}

	
	
}
