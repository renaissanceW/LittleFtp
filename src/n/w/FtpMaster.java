package n.w;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class FtpMaster extends Thread{
	
	private static FtpMaster mMaster = null;
	public static FtpMaster getFtpMasterInstance(){
		if(mMaster == null){
			mMaster = new FtpMaster();
		}
		return mMaster;
	}
	
	private String mHost;
	private int mPort=21;
	private String mUser = "Anonymous";
	private String mPassword = "";
	
	
	private String mWorkingDir=".";
	private String mLocalWorkingDir = "/sdcard/Download";
	private FTPClient mFtp;
	private Handler mCallerHandler;
	private Handler mHandler;
	
	private Timer mTimer;
	
	
	
	public TaskManager mManager;

	public FtpMaster(){
		start();
	}
	public void run(){
		setName("Master");
		MyLog.d("Master", "master thread started");
		Looper.prepare();
		mHandler = new FtpMasterHandler();
		mManager = new TaskManager(mHandler);
		Looper.loop();
	}

	
	
	
	/* the master handler will receive one kind of msg
	 *  MSG_MASTER_FILE_OP_REPLY
	 */
	class FtpMasterHandler extends Handler {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case C.MSG_MASTER_CONNECT:
				establishConnection();
				break;
			case C.MSG_MASTER_DISCONNECT:
				destroyConnection();
				break;
			case C.MSG_MASTER_CD:
				String folderName = (String) msg.obj;
				chDir(folderName);
				break;
			case C.MSG_MASTER_BACK:
				backDir();
				break;
			case C.MSG_MASTER_LS:
				lsDir();
				break;
			case C.MSG_MASTER_FILE_DOWN:
				@SuppressWarnings("unchecked")
				ArrayList<FTPFile> f = (ArrayList<FTPFile>)msg.obj;
				addDownTask(f);
				break;
			case C.MSG_MASTER_FILE_UP:
				@SuppressWarnings("unchecked")
				ArrayList<String> l = (ArrayList<String>)msg.obj;
				addUpTask(l);
				break;
			case C.MSG_WORKER_FILEOP_REPLY:
				Task t = (Task)msg.obj;
				mManager.finishTask(t);
				mManager.schedule();
				sendReply(C.MSG_TASKLIST_UPDATE,0,mManager.getAllTask());
				break;
			case C.MSG_MASTER_GET_TASK_STATUS:
				sendReply(C.MSG_TASKLIST_UPDATE,0,mManager.getAllTask());
				break;
			default:
				break;
			}

		}
	}
	
	
	/* 
	 * Bundle format
	 * "remote" - String 
	 * "local" - String
	 * "action" - int 
	 * "size" - long
	 * "host" - String
	 * "port" - String
	 * "user" - String
	 * "password" - String
	 */
	private void addDownTask(ArrayList<FTPFile> files) {
		for (FTPFile f : files) {
			Bundle data = new Bundle();
			String remote = mWorkingDir + "/" + f.getName();
			String local = mLocalWorkingDir + "/" + f.getName();
			data.putString("remote", remote);
			data.putString("local", local);
			data.putInt("action", C.TASK_ACTION_DOWNLOAD);
			data.putLong("size", f.getSize());
			data.putString("host", mHost);
			data.putInt("port", mPort);
			data.putString("user", mUser);
			data.putString("password", mPassword);
			mManager.addTask(data);
			MyLog.d("Master", "add download task remote: "+remote+" local: "+local);
			
		}

		mManager.schedule();
	}

	//todo maybe the ArrayList<String> should be changed to ArrayList<File>
	private void addUpTask(ArrayList<String> files) {

		for (String f : files) {
			Bundle data = new Bundle();
			data.putString("remote", mWorkingDir + "/" + f);
			data.putString("local", mLocalWorkingDir + "/" + f);
			data.putInt("action", C.TASK_ACTION_UPLOAD);
			//todo add size!!
			mManager.addTask(data);
			
		}

		mManager.schedule();
	}
	
	
	
	private void establishConnection() {
		try {
			mFtp = new FTPClient();
			mFtp.connect(mHost);
			// After connection attempt, you should check the reply code to
			// verify success.
			if (!FTPReply.isPositiveCompletion(mFtp.getReplyCode())) {
				mFtp.disconnect();
				sendReply(C.MSG_MASTER_CONNECT_REPLY, C.FTP_OP_FAIL, null);
			} else if (!mFtp.login(mUser, mPassword)) {
				mFtp.logout();
				mFtp.disconnect();
				sendReply(C.MSG_MASTER_CONNECT_REPLY, C.FTP_OP_FAIL, null);
			} else {
				mFtp.setFileType(FTP.BINARY_FILE_TYPE);
				mFtp.enterLocalPassiveMode();
				mTimer = new Timer();
				mTimer.schedule(new NoopTimerTask(), C.FTP_NOOP_TIME_INTERVAL,
						C.FTP_NOOP_TIME_INTERVAL);

				MyLog.d("Master", "connection successfully established!");
				sendReply(C.MSG_MASTER_CONNECT_REPLY, C.FTP_OP_SUCC, null);
			}
			
			
		} catch (IOException e) {
			e.printStackTrace();
			MyLog.d("Master", "connection establishment failed~~~");
			sendReply(C.MSG_MASTER_CONNECT_REPLY, C.FTP_OP_FAIL, null);
		}

	}
	
	class NoopTimerTask extends TimerTask{
		public void run(){
			try {
				mFtp.sendNoOp();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void destroyConnection() {

		try {
			mFtp.logout();
			if (mFtp.isConnected()) {
				mFtp.disconnect();
			}

			mTimer.cancel();
			MyLog.d("Master", "disconnection success!");
			sendReply(C.MSG_MASTER_DISCONNECT_REPLY, C.FTP_OP_SUCC, null);	
			mManager.sendAllWorkerRequest(C.MSG_WORKER_DISCONNECT, null);
		} catch (IOException e) {

			e.printStackTrace();
			MyLog.d("Master", "disconnection failed!");
			sendReply(C.MSG_MASTER_DISCONNECT_REPLY, C.FTP_OP_FAIL, null);
		}

	}
	
	
	
	private void chDir(String folderName) {
		try {
			/* it's the relative path */
			mFtp.changeWorkingDirectory(folderName);
			mWorkingDir = mWorkingDir+"/"+folderName;
			MyLog.d("Master", "cd success" + folderName);
			sendReply(C.MSG_MASTER_CD_REPLY, C.FTP_OP_SUCC, null);
		} catch (IOException e) {
			
			e.printStackTrace();
			MyLog.d("Master", "cd fail " + folderName);
			sendReply(C.MSG_MASTER_CD_REPLY, C.FTP_OP_FAIL, null);
		}

	}
	
	
	private void backDir() {
		if (!mWorkingDir.equals(".")) {
			try {
				boolean rlt = mFtp.changeToParentDirectory();
				mWorkingDir = mWorkingDir.substring(0,mWorkingDir.lastIndexOf('/'));
				MyLog.d("Master", "backDir succ");
				sendReply(C.MSG_MASTER_BACK_REPLY, C.FTP_OP_SUCC, null);
			} catch (IOException e) {
				
				e.printStackTrace();
				MyLog.d("Master", "backDir fail");
				sendReply(C.MSG_MASTER_BACK_REPLY, C.FTP_OP_FAIL, null);
			}

		}
	}
	
	private void lsDir() {
		ArrayList<FTPFile> result = new ArrayList<FTPFile>();
		String names = "";
		try {
			for (FTPFile f : mFtp.listFiles()) {
				if (f != null) {
					result.add(f);
					names += f.getName()+" ";
				}
			}
			MyLog.d("Master", "ls " + mWorkingDir + " success!");
			MyLog.d("Master", "ls content: "+names);
			sendReply(C.MSG_MASTER_LS_REPLY, C.FTP_OP_SUCC, result);
		} catch (IOException e) {
			
			e.printStackTrace();
			MyLog.d("Master", "ls " + mWorkingDir + " failed!");
			sendReply(C.MSG_MASTER_LS_REPLY, C.FTP_OP_FAIL, result);
		}

	}
	
	
	
	public Handler getHandler(){
		return mHandler;
	}
	
	public void setHandler(Handler h){
		mCallerHandler = h;
	}
	
	public void setDst(String h, String u, String p, int port){
		mHost = h; mUser = u; mPassword = p; mPort= port;
	}
	
	public String getWorkingDir(){
		return mWorkingDir;
	}
	
	private void sendReply(int what, int status, Object obj){
		mCallerHandler.obtainMessage(what, status, 0, obj).sendToTarget();
	}
	

}
