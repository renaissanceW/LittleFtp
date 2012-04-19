package n.w;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;


public class LoginActivity extends Activity {

	private Context mCtx; 
	
	private Master mFtpMaster;
	private Handler mHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);
		
		mCtx = this;
		mFtpMaster = Master.getFtpMasterInstance();
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
					Intent intent = new Intent(mCtx, MainActivity.class);
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
		
		//mFtpMaster.setDst("ftp.pku.cn", "Anonymous", "Anonymous",21);
		//mFtpMaster.setDst("166.111.26.3", "Anonymous", "Anonymous",21);
		//mFtpMaster.setDst("10.0.1.224", "share", "share",21);
		mFtpMaster.setDst("10.0.1.230", "wn", "wn",21);
		//mFtpMaster.setDst("10.0.1.229", "Anonymous", "",21);
		mFtpMaster.getHandler().obtainMessage(C.MSG_MASTER_CONNECT).sendToTarget();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch(item.getItemId()){
		case R.id.login_setting:
			SettingFragment setting = new SettingFragment();
			setting.show(getFragmentManager(), "delete");
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		getMenuInflater().inflate(R.menu.login_option_menu, menu);
		return true;
	}

	
	class SettingFragment extends DialogFragment {
		Integer[] mThreadCount={1,2,3};
		View mLayout;
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {

			AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
			mLayout = getLayoutInflater().inflate(R.layout.login_setting, null);
			/*The spinner part*/
			Spinner spinner = (Spinner)mLayout.findViewById(R.id.thread_number_spinner);
			ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(LoginActivity.this,
					android.R.layout.simple_spinner_item,mThreadCount);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spinner.setAdapter(adapter);
			spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

				public void onItemSelected(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {
					Global.getInstance().mWorkerCount=mThreadCount[arg2];				
				}

				public void onNothingSelected(AdapterView<?> arg0) {
					// TODO Auto-generated method stub
					
				}
			});
			spinner.setVisibility(View.VISIBLE); 
			 
			return builder
					.setView(mLayout)
					.setTitle(R.string.thread_number).create();
		}

	}


}
