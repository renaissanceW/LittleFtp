package n.w;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
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
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

public class ExplorerActivity extends Activity {
	
	private ListView mFileListView;
	private CommonFileListAdapter mFileListAdapter = null;
	private Handler mHandler;
	private FtpMaster mFtpMaster;
	private Context mCtx;
	
	//TODO when we start this activity, we should 
	//tell whether it's a local or a remote
	private boolean mIsLocal = false;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.explorer);
        MyLog.d("Explorer", "LIFECYCLE: onCreate");
        mCtx = this;
        mFileListView = (ListView)findViewById(R.id.explorer_listview);   
        mFtpMaster = FtpMaster.getFtpMasterInstance();
        mHandler = new ExplorerHandler();      

       mIsLocal = getIntent().getBooleanExtra("isLocal", false);
        
        
    }
    
    public void onStart(){
    	super.onStart();
    	MyLog.d("Explorer", "LIFECYCLE: onStart");
    	mFtpMaster.setHandler(mHandler);
    	if(mIsLocal){
    		sendMasterRequest(C.MSG_MASTER_LS_LOCAL,null);
    	}else{
    		sendMasterRequest(C.MSG_MASTER_LS,null);
    	}
    	
    } 

    
    class ExplorerHandler extends Handler{
    	
        	public void handleMessage(Message msg){
        		switch(msg.what){
        		case C.MSG_MASTER_CD_REPLY:
        		case C.MSG_MASTER_BACK_REPLY:
        		case C.MSG_MASTER_MKDIR_REPLY:
        			if (msg.arg1 == C.FTP_OP_SUCC) {
        				sendMasterRequest(C.MSG_MASTER_LS,null);
        			}
        			break;
        		case C.MSG_MASTER_CD_LOCAL_REPLY:
        		case C.MSG_MASTER_BACK_LOCAL_REPLY:
        		case C.MSG_MASTER_MKDIR_LOCAL_REPLY:
        			if (msg.arg1 == C.FTP_OP_SUCC) {
        				sendMasterRequest(C.MSG_MASTER_LS_LOCAL,null);
        			}
        			break;
        		
        		case C.MSG_MASTER_LS_REPLY:
        		case C.MSG_MASTER_LS_LOCAL_REPLY:
					if (msg.arg1 == C.FTP_OP_SUCC) {	
						
						CommonFile[] data = (CommonFile[]) msg.obj;
						if(mFileListAdapter == null){
							mFileListAdapter = new CommonFileListAdapter(mCtx, data);
							mFileListView.setAdapter(mFileListAdapter);
					    	mFileListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
					    	mFileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

								public void onItemClick(AdapterView<?> parent,  View view, int position, long id) {
									//  Auto-generated method stub
									CommonFile f = mFileListAdapter.getItem(position);
									switch(f.getType()){
									case CommonFile.TYPE_DIRECTORY:
										if(mIsLocal){
											sendMasterRequest(C.MSG_MASTER_CD_LOCAL,f.getName());
										}else{
											sendMasterRequest(C.MSG_MASTER_CD,f.getName());	
										}
										break;
									}
									
								}
							});
						}else{
							mFileListAdapter.setData(data);
							mFileListView.clearChoices();
							mFileListAdapter.notifyDataSetChanged();
						}
					}
        			break;
        			
        		
        	
        		}
        	}
        
    }
    
    

    private void sendMasterRequest(int what, Object obj){
    	mFtpMaster.getHandler().obtainMessage(what, obj).sendToTarget();
    }
    
    



	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//  Auto-generated method stub
		MenuInflater inflater = getMenuInflater();
		if(mIsLocal){
		    inflater.inflate(R.menu.explorer_menu_local, menu);		
		}else{
		    inflater.inflate(R.menu.explorer_menu, menu);			
		}

	    return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){

		case R.id.explorer_back:
			if(mIsLocal){
				sendMasterRequest(C.MSG_MASTER_BACK_LOCAL,null);
			}else{
				sendMasterRequest(C.MSG_MASTER_BACK,null);
			}
			
			return true;	
		case R.id.explorer_download:
			CommonFile[] msg = mFileListAdapter.getSelection(mFileListView.getCheckedItemIds());
    		if(msg!=null){
    			mFileListView.clearChoices();
    			mFileListAdapter.notifyDataSetChanged();
    			sendMasterRequest(C.MSG_MASTER_FILE_DOWN, msg);
    		}	
			return true;
		case R.id.explorer_upload:
			CommonFile[] msg1 = mFileListAdapter.getSelection(mFileListView.getCheckedItemIds());
    		if(msg1!=null){
    			mFileListView.clearChoices();
    			mFileListAdapter.notifyDataSetChanged();
    			sendMasterRequest(C.MSG_MASTER_FILE_UP, msg1);
    		}	
			return true;
			
		case R.id.explorer_local:
			Intent intent1 = new Intent(mCtx, ExplorerActivity.class);
			intent1.putExtra("isLocal", true);
			startActivity(intent1);
			return true;

		case R.id.explorer_tasklist:
			Intent intent = new Intent(mCtx, TaskListActivity.class);
    		startActivity(intent);
			return true;
			
		case R.id.explorer_new:
			DialogFragment newFragment = new NewDirInputFragment();
			newFragment.show(getFragmentManager(), "dialog");	    
			return true;
			
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private void mkdir(String name){
		MyLog.d("Explorer", "making dir "+name);
		if(mIsLocal){
			sendMasterRequest(C.MSG_MASTER_MKDIR_LOCAL, name);
		}else{
			sendMasterRequest(C.MSG_MASTER_MKDIR, name);
		}
	}
	
	public  class NewDirInputFragment extends DialogFragment {

		View mLayout;
	    @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {	        
	        
	        AlertDialog.Builder builder  = new AlertDialog.Builder(getActivity());
	        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(LAYOUT_INFLATER_SERVICE);
	        mLayout = inflater.inflate(R.layout.new_input,null);
	        
	        return  builder.setView(mLayout)
	        		.setPositiveButton("ok",
	                    new ConfirmListener()
	                )
	                .setNegativeButton("cancel", null)
	                .create();
	    }
	       
	    class ConfirmListener implements DialogInterface.OnClickListener{

			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				EditText et = (EditText)mLayout.findViewById(R.id.new_input_text);
	            ((ExplorerActivity)getActivity()).mkdir(et.getText().toString());
			}
			
		}
	    
	}
	
	
	
	
	
}