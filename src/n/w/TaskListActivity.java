package n.w;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class TaskListActivity extends Activity {

	private ListView mListView;
	private TaskListAdapter mListAdapter;
	private FtpMaster mFtpMaster;
	private Handler mHandler;
	private Context mCtx;
	private LayoutInflater mInflater;
	
	private Timer mTimer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.tasklist);	
		mListView = (ListView)findViewById(R.id.tasklist_listview);
		mFtpMaster = FtpMaster.getFtpMasterInstance();
		mHandler = new TaskListHandler();
		
		mListAdapter = new TaskListAdapter(this,null);
		mListView.setAdapter(mListAdapter);
		mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		
		
		
	}
	public void onStart() {
		super.onStart();
		mFtpMaster.setHandler(mHandler);
		C.sendMessage(mFtpMaster.getHandler(), C.MSG_MASTER_GET_TASK_STATUS);

		mTimer = new Timer();
		mTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				C.sendMessage(mHandler, C.MSG_TASKLIST_TIMER_UPDATE);
			}

		}, 0, 300);
	}
	
	public void onPause(){
		super.onPause();
		mTimer.cancel();
	}

	
	public void handleBack(View v){
		finish();
	}
	public void handleCancel(View v){
		long[] selection = mListView.getCheckedItemIds();
		if(selection.length>0){
			mListAdapter.markTaskCancel(selection);
			mListView.clearChoices();
		}		
	}
	
	class TaskListHandler extends Handler{
		@SuppressWarnings("unchecked")
		public void handleMessage(Message msg){
			switch(msg.what){
			case C.MSG_TASKLIST_UPDATE:
				mListAdapter.setData((ArrayList<Task>)msg.obj);
			case C.MSG_TASKLIST_TIMER_UPDATE:
				mListAdapter.notifyDataSetChanged();
				break;
				
			}
		}
	}
	
	class TaskListAdapter extends BaseAdapter {
		private ArrayList<Task> mTasks;
		public TaskListAdapter(Context ctx, ArrayList<Task> t){
			mCtx = ctx;
			mInflater = LayoutInflater.from(mCtx);
			mTasks = t;
		}
		
		public void setData(ArrayList<Task> t){
			mTasks = t;
		}

		public int getCount() {
			return mTasks==null? 0:mTasks.size();
		}

		public Object getItem(int position) {
			return mTasks==null? null:mTasks.get(position);
		}

		public long getItemId(int position) {
			return position;
		}
		
		public boolean isEmpty(){
			return getCount() == 0;
		}
		
		@Override
		public boolean hasStableIds() {
			// TODO Auto-generated method stub
			return true;
		}
		
		public View getView(int position, View convertView, ViewGroup parent) {
			if(convertView == null){
				convertView = mInflater.inflate(R.layout.task_item, null);
			}
			
			Bundle task = mTasks.get(position).mData;
			TextView type = (TextView)convertView.findViewById(R.id.task_type);
			TextView name = (TextView)convertView.findViewById(R.id.task_name);
			TextView progress = (TextView)convertView.findViewById(R.id.task_progress);
			TextView speed = (TextView)convertView.findViewById(R.id.task_speed);
			
			
			
			if(task.getInt("action")==C.TASK_ACTION_DOWNLOAD){
				type.setText("Download");
				String remote = task.getString("remote");	
				name.setText(remote.substring(remote.lastIndexOf("/")+1));
			}else{
				type.setText("Upload");
				String local = task.getString("local");	
				name.setText(local.substring(local.lastIndexOf("/")+1));
			}
			
			float fP =(float)((int)(10*task.getFloat("progress")))/10;
			progress.setText(""+fP+"%");
			float fS =(float)((int)(10*task.getFloat("speed")))/10;
			speed.setText(fS+"KB/s");
			
	
			return convertView;
		}
		
		
		public void markTaskCancel(long[] selection) {
			for (long i : selection) {
				Task t = mTasks.get((int) i);
				mFtpMaster.mManager.cancelTask(t);
				C.sendMessage(mFtpMaster.getHandler(),C.MSG_MASTER_GET_TASK_STATUS);
			}
		}
		

	}

}



