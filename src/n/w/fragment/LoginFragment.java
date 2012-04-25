package n.w.fragment;

import n.w.MainActivity;
import n.w.R;
import n.w.background.BookMarkDB;
import n.w.background.Master;
import n.w.uitil.C;
import n.w.uitil.Global;
import n.w.uitil.MyLog;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class LoginFragment extends Fragment {
	
	public class LoginTabListener implements ActionBar.TabListener{

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
		mDb = new BookMarkDB(mParent);
		mProfiles = mDb.getAllNames();
		mSpinnerAdapter = new ArrayAdapter<String>(mParent,
				android.R.layout.simple_spinner_item, mProfiles);
		mSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	}
	
	class LoginHandler extends Handler{
		public void handleMessage(Message msg){
			switch(msg.what){
			case C.MSG_MASTER_CONNECT_REPLY:
				if (msg.arg1 == C.FTP_OP_SUCC) {
					Global.getInstance().mIsMasterConnected=true;
					Toast.makeText(mParent, "connect success", Toast.LENGTH_SHORT).show();
					setButtonStatus();
				} else {
					Toast.makeText(mParent, "connect fail", Toast.LENGTH_SHORT).show();
				}
				break;
			case C.MSG_MASTER_DISCONNECT_REPLY:
				//if (msg.arg1 == C.FTP_OP_SUCC) 
				{
					Global.getInstance().mIsMasterConnected=false;
					Toast.makeText(mParent, "disconnect success", Toast.LENGTH_SHORT).show();
					setButtonStatus();
				}
			default:
				break;
			}
		}
	}
	
	private void setButtonStatus(){
		boolean b = Global.getInstance().mIsMasterConnected;
		mConnectBt.setEnabled(!b);
		mDisconnectBt.setEnabled(b);
		mSpinner.setEnabled(!b);
	}
	
	
	MainActivity mParent;
	private Master mMaster;
	private Handler mHandler;
	private View mView=null;
	
	private BookMarkDB mDb;
	private String[] mProfiles;
	private int mCurrentProfileIndex=0;
	
	
	private Spinner mSpinner;
	private ArrayAdapter<String>mSpinnerAdapter;

	private Button mConnectBt;
	private Button mDisconnectBt;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		if(mView==null){
			mView = inflater.inflate(R.layout.login, container, false);
			
			mConnectBt = (Button)mView.findViewById(R.id.login_connect_bt);
			mDisconnectBt = (Button)mView.findViewById(R.id.login_disconnect_bt);
			
			mConnectBt.setOnClickListener(new View.OnClickListener() {
				
				public void onClick(View v) {
					// TODO Auto-generated method stub
					if(Global.getInstance().mIsMasterConnected==false&&mProfiles.length!=0){
						BookMarkDB.Record r = mDb.getByName(mProfiles[mCurrentProfileIndex]);			
						mMaster.setDst(r.host, r.user, r.pwd, r.port);	
						mMaster.getHandler().obtainMessage(C.MSG_MASTER_CONNECT).sendToTarget();
					}else{
						C.makeToast(mParent, "Already connected");
					}
				}
			});
			
			mDisconnectBt.setOnClickListener(new View.OnClickListener() {
				
				public void onClick(View v) {
					// TODO Auto-generated method stub
					if(Global.getInstance().mIsMasterConnected==false){
						C.makeToast(mParent, "Not Connected Yet!");
					}else{
						mMaster.getHandler().obtainMessage(C.MSG_MASTER_DISCONNECT).sendToTarget();
					}
				}
			});
			
			
			mSpinner = (Spinner)mView.findViewById(R.id.profile_spinner);
			mSpinner.setAdapter(mSpinnerAdapter);
			mSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

				public void onItemSelected(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {
					mCurrentProfileIndex = arg2;
				}

				public void onNothingSelected(AdapterView<?> arg0) {
								
				}
			});
			mSpinner.setVisibility(View.VISIBLE);
		}
		
		setButtonStatus();
		
		return mView;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		
		inflater.inflate(R.menu.login_option_menu, menu);
	}

	

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch(item.getItemId()){
		case R.id.login_new:
			ProfileFragment profile = new ProfileFragment();
			profile.show(getFragmentManager(), "");
			
			return true;
		case R.id.login_edit:
			if(mProfiles.length!=0){
				BookMarkDB.Record r = mDb.getByName(mProfiles[mCurrentProfileIndex]);
					ProfileFragment profile1 = new ProfileFragment(r);
					profile1.show(getFragmentManager(), "");
				
			}
			return true;		
		case R.id.login_delete:
			if (mProfiles.length != 0) {
				DeleteProfileFragment delte = new DeleteProfileFragment(
						mProfiles[mCurrentProfileIndex]);
				delte.show(getFragmentManager(), "");
			}
			
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void updateSpinner(){
		mProfiles = mDb.getAllNames();
		String tmp="";
		for(String i : mProfiles){
			tmp += " "+i;
		}
		MyLog.d("Login", "profiles: "+tmp);
		//mSpinnerAdapter.notifyDataSetChanged();
		mSpinnerAdapter = new ArrayAdapter<String>(mParent,
				android.R.layout.simple_spinner_item, mProfiles);
		mSpinner.setAdapter(mSpinnerAdapter);
		
	}
	
	
	public void doConnect(View v){
		if(Global.getInstance().mIsMasterConnected==false){
			BookMarkDB.Record r = mDb.getByName(mProfiles[mCurrentProfileIndex]);			
			mMaster.setDst(r.host, r.user, r.pwd, r.port);	
			mMaster.getHandler().obtainMessage(C.MSG_MASTER_CONNECT).sendToTarget();
		}else{
			C.makeToast(mParent, "Already connected");
		}
		
	}
	
	public void doDisconnect(View v){
		if(Global.getInstance().mIsMasterConnected==false){
			C.makeToast(mParent, "Not Connected Yet!");
		}else{
			mMaster.getHandler().obtainMessage(C.MSG_MASTER_DISCONNECT).sendToTarget();
		}
	}
	

	
	
	class DeleteProfileFragment extends DialogFragment {

		private String name;

		public DeleteProfileFragment(String name) {
			this.name = name;
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {

			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

			return builder
					.setTitle(R.string.delete_confirm_title)
					.setPositiveButton("ok",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {

									mDb.delete(name);
									updateSpinner();

								}
							}).setNegativeButton("cancel", null).create();
		}

	}
	
	
	
	class ProfileFragment extends DialogFragment {
		public ProfileFragment(){
			mIsNew = true;
		}
		
		public ProfileFragment(BookMarkDB.Record record){
			mIsNew = false;
			mRecord = record;
		}
		
		View mLayout;
		
		boolean mIsNew;
		BookMarkDB.Record mRecord;
		
		EditText name;
		EditText host;EditText port;
		EditText user;EditText pwd;
		
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {

			AlertDialog.Builder builder = new AlertDialog.Builder(mParent);
			mLayout = mParent.getLayoutInflater().inflate(R.layout.profile, null);
			name = (EditText)mLayout.findViewById(R.id.profile_name_tx);
			host = (EditText)mLayout.findViewById(R.id.profile_host_tx);
			port = (EditText)mLayout.findViewById(R.id.profile_port_tx);
			user = (EditText)mLayout.findViewById(R.id.profile_user_tx);
			pwd =  (EditText)mLayout.findViewById(R.id.profile_pwd_tx);
			
			if(mIsNew==false){
				name.setText(mRecord.name);
				//primary key should not change
				name.setEnabled(false);
				host.setText(mRecord.host);
				port.setText(String.valueOf(mRecord.port));
				user.setText(mRecord.user);
				pwd.setText(mRecord.pwd);
			}
		
			 
			return builder
					.setView(mLayout)
					.setTitle(R.string.profile_title)
					.setPositiveButton("save", new DialogInterface.OnClickListener() {
						
						public void onClick(DialogInterface dialog, int which) {
							
							if(getValidRecord()){
								long shit = mIsNew ? mDb.add(mRecord): mDb.update(mRecord);
							}else{
								Toast.makeText(mParent, "Invalid Input", Toast.LENGTH_SHORT).show();
							}
											
							updateSpinner();		
						}
					})
					.setNegativeButton("cancel", null)
					.create();
		}
		
		
		private boolean getValidRecord(){
			
			String n = name.getText().toString().trim();
			String h = host.getText().toString().trim();
			String pp = port.getText().toString().trim();		
			String u = user.getText().toString().trim();
			String pa = pwd.getText().toString().trim();
	
			if(u.isEmpty()){
				u=pa="Anonymous";
			}
			int p;
			if(pp.isEmpty()){
				p = 21;
			}else{
				p = Integer.parseInt(pp);
			}
			
			if(!n.isEmpty()&&!h.isEmpty()){
				mRecord = mDb.new Record(n,h,p,u,pa);
				return true;
			}
			return false;
		}

	}


	
	
	
	
	

}
