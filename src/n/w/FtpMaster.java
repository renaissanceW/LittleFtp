package n.w;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
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
	
	/*ftp*/
	private FTPClient mFtp;	
	private String mWorkingDir=".";
	/*local fs*/
	private static final String localRoot = "/sdcard/Download";
	private File mFileRoot = new File(localRoot);
	private File mFile = new File(localRoot);

	
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
				String folderName = (String)msg.obj;
				chDir(folderName);
				break;			
			case C.MSG_MASTER_BACK:
				backDir();
				break;
			case C.MSG_MASTER_LS:
				lsDir();
				break;
			case C.MSG_MASTER_FILE_DOWN:
				addTask((CommonFile)msg.obj,C.TASK_ACTION_DOWNLOAD);
				break;
			case C.MSG_MASTER_FILE_UP:
				addTask((CommonFile) msg.obj,C.TASK_ACTION_UPLOAD);
				break;
					
				
			case C.MSG_MASTER_CD_LOCAL:
				String name = (String)msg.obj;
				chDirLocal(name);
				break;			
			case C.MSG_MASTER_BACK_LOCAL:
				backDirLocal();
				break;
			case C.MSG_MASTER_LS_LOCAL:
				lsDirLocal();
				break;
				
			
			case C.MSG_MASTER_MKDIR:
				mkdir((String)msg.obj);
				break;
			case C.MSG_MASTER_MKDIR_LOCAL:
				mkdirLocal((String)msg.obj);
				break;
				
			case C.MSG_MASTER_DELETE:
				delete((CommonFile)msg.obj);
				break;
			case C.MSG_MASTER_DELETE_LOCAL:
				deleteLocal((CommonFile)msg.obj);
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
	 * 
	 * for UI
	 * "remoteName" - String
	 * "localName" - String
	 * "progress" - String
	 * "accSize" - String
	 * "speed" - String
	 */
	private void addTask(CommonFile f, int action) {

			String remote = mWorkingDir + "/" + f.getName();
			String local = mFile.getAbsolutePath() + "/" + f.getName();
			
			Bundle data = C.genTaskBundle(remote, local, 
					action, f.getSize(), 
					mHost, mPort, mUser, mPassword, 
					f.getType()==CommonFile.TYPE_DIRECTORY);
			
			mManager.addTask(data);
			MyLog.d("Master", "add download task remote: "+remote+" local: "+local);
			
		

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
			mWorkingDir = mWorkingDir + "/" + folderName;
			MyLog.d("Master", "cd success" + folderName);
			sendReply(C.MSG_MASTER_CD_REPLY, C.FTP_OP_SUCC, null);
		} catch (IOException e) {

			e.printStackTrace();
			MyLog.d("Master", "cd fail " + folderName);
			sendReply(C.MSG_MASTER_CD_REPLY, C.FTP_OP_FAIL, null);
		}

	}
	
	
	private void chDirLocal(String folderName){
		mFile = new File(mFile, folderName);
		MyLog.d("Master", "cdLocal succ:" + folderName);
		sendReply(C.MSG_MASTER_CD_LOCAL_REPLY, C.FTP_OP_SUCC, null);
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
	
	
	private void backDirLocal(){
		if(mFile.getAbsolutePath().equals(mFileRoot.getAbsolutePath())){//top
			MyLog.d("Master", "backDirLocal succ:" + mFile.getAbsolutePath());
			sendReply(C.MSG_MASTER_BACK_LOCAL_REPLY, C.FTP_OP_FAIL, null);
			return;
		}
		
		mFile=mFile.getParentFile();
		MyLog.d("Master", "backDirLocal succ:" + mFile.getAbsolutePath());
		sendReply(C.MSG_MASTER_BACK_LOCAL_REPLY, C.FTP_OP_SUCC, null);
	}
	
	
	private void lsDir() {
		String names = "";
		try {
			
			FTPFile[] result = mFtp.listFiles();
			/*seperate folders with files*/
			Arrays.sort(result, new Comparator<FTPFile>(){
				public int compare(FTPFile lhs, FTPFile rhs) {
					if(lhs.getType() == rhs.getType()){
						return lhs.getName().compareTo(rhs.getName());
					}else{
						return lhs.getType() == FTPFile.DIRECTORY_TYPE? -1 : 1;
					}
				}
				
			});		
			
			for (FTPFile f : result){
				names += f.getName()+" ";
			}
			
			CommonFile[] rlt = new CommonFile[result.length];
			for(int i=0;i<result.length;i++){
				rlt[i] = new CommonFile(result[i]);
			}
			
			MyLog.d("Master", "ls SUCC" + mWorkingDir + " content: "+names);
			sendReply(C.MSG_MASTER_LS_REPLY, C.FTP_OP_SUCC, rlt);
		} catch (IOException e) {
			
			e.printStackTrace();
			MyLog.d("Master", "ls " + mWorkingDir + " failed!");
			sendReply(C.MSG_MASTER_LS_REPLY, C.FTP_OP_FAIL, null);
		}

	}
	
	
	
	private void lsDirLocal(){
		File[] result = mFile.listFiles();
		Arrays.sort(result, new Comparator<File>(){

			public int compare(File lhs, File rhs) {
				// TODO Auto-generated method stub
				if(lhs.isDirectory()&&rhs.isDirectory() ||
					lhs.isFile()&&rhs.isFile()){
					return lhs.getName().compareTo(rhs.getName());
				}else{
					return lhs.isDirectory()? -1 : 1;
				}
			}
			
		});
		
		
		String logStr = "";
		for(File f:result){
			logStr += " "+ f.getName();
		}
		
		CommonFile[] rlt = new CommonFile[result.length];
		for(int i=0;i<result.length;i++){
			rlt[i] = new CommonFile(result[i]);
		}
		
		
		MyLog.d("Master", "lsDirLocal SUCC "+mFile.getAbsolutePath()+" content:"+logStr);
		sendReply(C.MSG_MASTER_LS_LOCAL_REPLY, C.FTP_OP_SUCC, rlt);
	}
	
	
	
	
	
	
	private void mkdir(String name){
		
		try {
			boolean result = mFtp.makeDirectory(name);
			MyLog.d("Master", "mkdir : "+result+" "+mWorkingDir+"/"+name);
			sendReply(C.MSG_MASTER_MKDIR_REPLY, result?C.FTP_OP_SUCC:C.FTP_OP_FAIL, null);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void mkdirLocal(String name){
		File f = new File(mFile.getAbsolutePath()+"/"+name);
		boolean result = f.mkdir();	
		MyLog.d("Master", "mkdirLocal "+result+" "+f.getAbsolutePath());
		sendReply(C.MSG_MASTER_MKDIR_LOCAL_REPLY, result?C.FTP_OP_SUCC:C.FTP_OP_FAIL, null);
	}
	
	
	
	private void delete(CommonFile file) {

		FTPFile f = file.getFTPFile();
		try {
			boolean rlt = innerDeleteRemote(f);
			MyLog.d("Master",
					"delete : " + rlt + " " + mWorkingDir + "/" + f.getName());
			sendReply(C.MSG_MASTER_DELETE_REPLY, rlt ? C.FTP_OP_SUCC
					: C.FTP_OP_FAIL, null);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private boolean innerDeleteRemote(FTPFile f) throws IOException{
		
		if(f.getType() ==  FTPFile.FILE_TYPE){
			return mFtp.deleteFile(f.getName());
		}else if(f.getType() == FTPFile.DIRECTORY_TYPE){
			boolean flag = true;
			mFtp.changeWorkingDirectory(f.getName());
			for(FTPFile child : mFtp.listFiles() ){
				if(!innerDeleteRemote(child)){
					flag = false;
					break;
				}
			}
			mFtp.changeToParentDirectory();
			if(flag && mFtp.removeDirectory(f.getName())){
				return true;
			}			
			
		}
		
		return false;
	}
	
	
	
	
	private void deleteLocal(CommonFile file) {
		File f = file.getFile();

		boolean result = innerDeleteLocal(f);
		MyLog.d("Master", "deleteLocal " + result + " " + f.getAbsolutePath());
		sendReply(C.MSG_MASTER_DELETE_LOCAL_REPLY, result ? C.FTP_OP_SUCC
				: C.FTP_OP_FAIL, null);

	}
	
	/*recursively delete*/
	private boolean innerDeleteLocal(File f){
		
		if(f.isFile()){
			return f.delete();
		}else if(f.isDirectory()){
			File[] childs = f.listFiles();
			boolean flag = true;
			for(File c : childs){
				if(!innerDeleteLocal(c)){
					flag = false;
					break;
				}
			}
			
			if(flag && f.delete()){
				return true;
			}
		}	
		
		return false;
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
