package n.w;

import java.util.ArrayList;

import org.apache.commons.net.ftp.FTPFile;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class ExplorerActivity extends Activity {
	
	private ListView mFileListView;
	private ListDirAdapter mFileListAdapter = null;
	private Handler mHandler;
	private FtpMaster mFtpMaster;
	private Context mCtx;


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

       
        
        
    }
    
    public void onStart(){
    	super.onStart();
    	MyLog.d("Explorer", "LIFECYCLE: onStart");
    	mFtpMaster.setHandler(mHandler);
    	sendMasterRequest(C.MSG_MASTER_LS,null);
    } 
    public void onPause(){
    	super.onPause();
    	MyLog.d("Explorer", "LIFECYCLE: onPause");
    }
    public void onResume(){
    	super.onResume();
    	MyLog.d("Explorer", "LIFECYCLE: onResum");
    }
    public void onStop(){
    	super.onStop();
    	MyLog.d("Explorer", "LIFECYCLE: onStop");
    }
    public void onDestroy(){
    	super.onDestroy();
    	MyLog.d("Explorer", "LIFECYCLE: onDestroy");
    }
    
    class ExplorerHandler extends Handler{
    	
        	@SuppressWarnings("unchecked")
			public void handleMessage(Message msg){
        		switch(msg.what){
        		case C.MSG_MASTER_CD_REPLY:
        			if (msg.arg1 == C.FTP_OP_SUCC) {
        				sendMasterRequest(C.MSG_MASTER_LS,null);
        			}
        			break;
        		case C.MSG_MASTER_BACK_REPLY:
        			if (msg.arg1 == C.FTP_OP_SUCC) {
        				sendMasterRequest(C.MSG_MASTER_LS,null);
        			}
        			break;
        		case C.MSG_MASTER_LS_REPLY:
					if (msg.arg1 == C.FTP_OP_SUCC) {	
						
					
			
						ArrayList<FTPFile> data = (ArrayList<FTPFile>) msg.obj;
						if(mFileListAdapter == null){
							createFileListUI(data);
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
    
    
   
    private void createFileListUI(ArrayList<FTPFile> data){
 
    	mFileListAdapter = new ListDirAdapter(this, data);
    	mFileListView.setAdapter(mFileListAdapter);
    	mFileListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
    	mFileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			public void onItemClick(AdapterView<?> parent,  View view, int position, long id) {
				// TODO Auto-generated method stub
				FTPFile f = (FTPFile)mFileListAdapter.getItem(position);
				switch(f.getType()){
				case FTPFile.DIRECTORY_TYPE:
					sendMasterRequest(C.MSG_MASTER_CD,f.getName());
					break;
				}
				
			}
		});

    }
    
    
 
    
    
    
   
    
    
    
    



	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.explorer_menu, menu);
	    return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){

		case R.id.explorer_back:
			sendMasterRequest(C.MSG_MASTER_BACK,null);
			return true;	
		case R.id.explorer_download:
			ArrayList<FTPFile> msg = mFileListAdapter.getSelection(mFileListView.getCheckedItemIds());
    		if(msg!=null){
    			mFileListView.clearChoices();
    			mFileListAdapter.notifyDataSetChanged();
    			sendMasterRequest(C.MSG_MASTER_FILE_DOWN, msg);
    		}	
			return true;
		case R.id.explorer_upload:
			
			return true;

		case R.id.explorer_tasklist:
			Intent intent = new Intent(mCtx, TaskListActivity.class);
    		startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}