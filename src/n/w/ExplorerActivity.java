package n.w;

import java.util.ArrayList;

import org.apache.commons.net.ftp.FTPFile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

        
        
        findViewById(R.id.explorer_back).setOnClickListener(new BackBtnListener());
        findViewById(R.id.explorer_download).setOnClickListener(new DownloadBtnListener());
        findViewById(R.id.explorer_tasks).setOnClickListener(new TaskBtnListener());   
        
        
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
    
    
 
    
    
    
    class BackBtnListener implements View.OnClickListener{
    	public void onClick(View v) {
			// TODO Auto-generated method stub
			sendMasterRequest(C.MSG_MASTER_BACK,null);
		}
    }
  
    class DownloadBtnListener implements View.OnClickListener{
    	public void onClick(View v) {		
    		ArrayList<FTPFile> msg = mFileListAdapter.getSelection(mFileListView.getCheckedItemIds());
    		if(msg!=null){
    			mFileListView.clearChoices();
    			mFileListAdapter.notifyDataSetChanged();
    			sendMasterRequest(C.MSG_MASTER_FILE_DOWN, msg);
    		}	
		}
    }
    
    
    class TaskBtnListener implements View.OnClickListener{
    	public void onClick(View v) {  		
    		Intent intent = new Intent(mCtx, TaskListActivity.class);
    		startActivity(intent);
		}
    }
}