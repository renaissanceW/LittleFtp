package n.w;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class LoginActivity extends Activity {

	private Context mCtx; 
	
	private FtpMaster mFtpMaster;
	private Handler mHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);
		
		mCtx = this;
		mFtpMaster = FtpMaster.getFtpMasterInstance();
		mHandler = new LoginHandler();
		mFtpMaster.setHandler(mHandler);
	
	}
	
	public void onStart(){
    	super.onStart();
    	MyLog.d("Login", "LIFECYCLE: onStart");
    	mFtpMaster.setHandler(mHandler);
    } 
	
	class LoginHandler extends Handler {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case C.MSG_MASTER_CONNECT_REPLY:
				if (msg.arg1 == C.FTP_OP_SUCC) {
					Intent intent = new Intent(mCtx, ExplorerActivity.class);
					intent.putExtra("isLocal", false);
					startActivity(intent);
				} else {
					Toast.makeText(mCtx, "wrong input", Toast.LENGTH_SHORT).show();
				}
				break;
			default:
				break;
			}
		}
	}
	
	
	public void doConnect(View v){
	
		String h = ((EditText)findViewById(R.id.login_host)).getText().toString();
		String u = ((EditText)findViewById(R.id.login_user)).getText().toString();
		String p = ((EditText)findViewById(R.id.login_password)).getText().toString();
		
		mFtpMaster.setDst(h, u, p,21);
		
		//mFtpMaster.setDst("10.0.1.224", "share", "share",21);
		mFtpMaster.setDst("10.0.1.230", "wn", "wn",21);
		//mFtpMaster.setDst("10.0.1.229", "Anonymous", "",21);
		mFtpMaster.getHandler().obtainMessage(C.MSG_MASTER_CONNECT).sendToTarget();
	}

}
