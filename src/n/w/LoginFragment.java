package n.w;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class LoginFragment extends Fragment {
	
	class LoginTabListener implements ActionBar.TabListener{

		public void onTabReselected(Tab tab, FragmentTransaction ft) {
		
		}

		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			ft.add(android.R.id.content, LoginFragment.this, null);
	        mMaster.setHandler(mHandler);
			
		}

		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			
			ft.remove(LoginFragment.this);
			
		}
		
	}
	
	public LoginFragment(){
		
	}
	public void init(MainActivity parent){
		mParent = parent;
		mMaster = Master.getFtpMasterInstance();	
		mHandler = new LoginHandler();
	}
	
	class LoginHandler extends Handler{
		public void handleMessage(Message msg){
			switch(msg.what){
			case C.MSG_MASTER_CONNECT_REPLY:
				if (msg.arg1 == C.FTP_OP_SUCC) {
					Global.getInstance().mIsMasterConnected=true;
					enableEdit(false);
				} else {
					Toast.makeText(mParent, "wrong input", Toast.LENGTH_SHORT).show();
				}
				break;
			case C.MSG_MASTER_DISCONNECT_REPLY:
				//if (msg.arg1 == C.FTP_OP_SUCC) 
				{
					Global.getInstance().mIsMasterConnected=false;
					enableEdit(true);
				}
			default:
				break;
			}
		}
	}
	
	
	MainActivity mParent;
	private Master mMaster;
	private Handler mHandler;
	private View mView=null;
	
	private EditText mHostText;
	private EditText mUserText;
	private EditText mPwdText;
	
	private void setDefault(String h, String u, String p){
		mHostText.setText(h);mUserText.setText(u);mPwdText.setText(p);
	}
	
	private void enableEdit(boolean flag){
		mHostText.setEnabled(flag);
		mUserText.setEnabled(flag);
		mPwdText.setEnabled(flag);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		if(mView==null){
			mView = inflater.inflate(R.layout.login, container, false);
			mHostText = (EditText) mView.findViewById(R.id.login_host);
			mUserText = (EditText) mView.findViewById(R.id.login_user);
			mPwdText = (EditText)  mView.findViewById(R.id.login_password);
			
			setDefault("10.0.1.231", "Anonymous", "Anonymous");
			//setDefault("ftp.pku.cn", "Anonymous", "Anonymous");
		}
		
		if (Global.getInstance().mIsMasterConnected==true) {
			enableEdit(false);
		}
		
		return mView;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		
		inflater.inflate(R.menu.login_option_menu, menu);
	}

	

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch(item.getItemId()){
		case R.id.login_connect:
			doConnect();
			return true;
		case R.id.login_disconnect:
			doDisconnect();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	
	private void doConnect(){
		if(Global.getInstance().mIsMasterConnected==false){
			String h = mHostText.getText().toString();
			String u = mUserText.getText().toString();
			String p = mPwdText.getText().toString();				
			mMaster.setDst(h, u, p,21);	
			mMaster.getHandler().obtainMessage(C.MSG_MASTER_CONNECT).sendToTarget();
		}else{
			C.makeToast(mParent, "Already connected");
		}
		
	}
	
	private void doDisconnect(){
		if(Global.getInstance().mIsMasterConnected==false){
			C.makeToast(mParent, "Not Connected Yet!");
		}else{
			mMaster.getHandler().obtainMessage(C.MSG_MASTER_DISCONNECT).sendToTarget();
		}
	}
	

	

}
