package n.w;

import android.os.Handler;

public class C {
	
	/*
	 * the message code
	 * all the code comform to the following format
	 */
	
	/*FTP request and reply*/
	
	
	

	/*worker*/
	public static final int MSG_WORKER_CONNECT 					= 0;
	public static final int MSG_WORKER_DISCONNECT 				= MSG_WORKER_CONNECT+1;
	/* message is a Bundle
	 * "remote" - String 
	 * "local" - String
	 * "action" - int
	 * "size" - long
	 * "port" - String
	 * "user" - String
	 * "password" - int
	 * 
	 * "progress" - float
	 * "accSize" - long
	 * "speed" - float
	 */
	public static final int MSG_WORKER_FILEOP				 	= MSG_WORKER_DISCONNECT+1;
	
	
	public static final int MSG_WORKER_CONNECT_REPLY 			= 100;
	public static final int MSG_WORKER_DISCONNECT_REPLY 		= MSG_WORKER_CONNECT_REPLY+1;
	public static final int MSG_WORKER_FILEOP_REPLY		 		= MSG_WORKER_DISCONNECT_REPLY+1;
	
	/*master*/
	public static final int MSG_MASTER_CONNECT 					= 200;
	public static final int MSG_MASTER_DISCONNECT 				= MSG_MASTER_CONNECT+1;
	public static final int MSG_MASTER_CD		 				= MSG_MASTER_DISCONNECT+1;
	public static final int MSG_MASTER_BACK		 				= MSG_MASTER_CD+1;
	public static final int MSG_MASTER_LS		 				= MSG_MASTER_BACK+1;
	/* message ArrayList<FTPFile> */
	public static final int MSG_MASTER_FILE_DOWN 				= MSG_MASTER_LS+1;
	/* message ArrayList<String> */
	public static final int MSG_MASTER_FILE_UP 					= MSG_MASTER_FILE_DOWN+1;
	public static final int MSG_MASTER_GET_TASK_STATUS			= MSG_MASTER_FILE_UP+1;
	
	public static final int MSG_MASTER_CONNECT_REPLY 			= 300;
	public static final int MSG_MASTER_DISCONNECT_REPLY 		= MSG_MASTER_CONNECT_REPLY+1;
	public static final int MSG_MASTER_CD_REPLY 				= MSG_MASTER_DISCONNECT_REPLY+1;
	public static final int MSG_MASTER_BACK_REPLY 				= MSG_MASTER_CD_REPLY+1;
	public static final int MSG_MASTER_LS_REPLY 				= MSG_MASTER_BACK_REPLY+1;
	public static final int MSG_MASTER_FILEOP_REPLY				= MSG_MASTER_LS_REPLY+1;
	
	
	public static final int MSG_TASKLIST_UPDATE					= 400;
	public static final int MSG_TASKLIST_TIMER_UPDATE			= MSG_TASKLIST_UPDATE+1;
	
	
	/* reply status */
	public static final int FTP_OP_SUCC				= 0;
	public static final int FTP_OP_FAIL				= FTP_OP_SUCC+1;

	
	/*task action*/
	public static final int TASK_ACTION_UPLOAD			= 0;
	public static final int TASK_ACTION_DOWNLOAD		= TASK_ACTION_UPLOAD+1;
	
	
	/*each minute we send a noop to maintain connection*/
	public static final long	FTP_NOOP_TIME_INTERVAL	= 60*1000;
	
	
	
	/*hate to type so many... and always forget the sendToTarget!*/

	public static void sendMessage(Handler h, int what){
		h.obtainMessage(what).sendToTarget();
	}
	public static void sendMessage(Handler h, int what, Object obj){
		h.obtainMessage(what, obj).sendToTarget();	
	}
	
	public static void sendMessage(Handler h, int what, int arg1, Object obj){
		h.obtainMessage(what, arg1, 0, obj).sendToTarget();
	}
}
