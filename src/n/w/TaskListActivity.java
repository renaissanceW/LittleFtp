package n.w;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
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
		//mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		mListView.setOnItemClickListener(new TaskItemClickListener());
		
		
		
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
	
	
	class TaskItemClickListener implements AdapterView.OnItemClickListener{

		public void onItemClick(AdapterView<?> parent, View view, int position, long id){
			//MyLog.d("TaskList", "item "+position+" checked");
			mListAdapter.select(position);		
		}
		
	}
	
	
	class TaskListAdapter extends BaseAdapter {
		public TreeSet<Integer> mSelection = new TreeSet<Integer>();
		
		public boolean select(int i){
			if(mSelection.contains(i)){
				mSelection.remove(i);			
				return false;
			}else{
				mSelection.add(i);
				return true;
			}			
		}
		
		public void markTaskCancel() {
			Task[] t = new Task[mSelection.size()];
			int k = 0;
			for (int i : mSelection) {
				t[k++] = mTasks.get(i);		
			}
			mFtpMaster.mManager.cancelTask(t);	
			mSelection.clear();
			C.sendMessage(mFtpMaster.getHandler(),C.MSG_MASTER_GET_TASK_STATUS);
		}
		
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
				convertView = mInflater.inflate(R.layout.task_item_new, null);
			}
			
			Task task = mTasks.get(position);
			Bundle data = mTasks.get(position).mData;
			ImageView type = (ImageView)convertView.findViewById(R.id.task_type);
			TextView name = (TextView)convertView.findViewById(R.id.task_name);
			TextView progress = (TextView)convertView.findViewById(R.id.task_progress);
			TextView speed = (TextView)convertView.findViewById(R.id.task_speed);
			TextView status = (TextView)convertView.findViewById(R.id.task_status);
			TextView size = (TextView)convertView.findViewById(R.id.task_size);
			
			ProgressBar progress_bar = (ProgressBar)convertView.findViewById(R.id.task_progress_bar);
			
			/*check box*/
			CheckBox cb = (CheckBox)convertView.findViewById(R.id.task_check_box);
			if(mSelection.contains(position)){
				cb.setChecked(true);
			}else{
				cb.setChecked(false);
			}
			
			/*type & name*/
			if(data.getInt("action")==C.TASK_ACTION_DOWNLOAD){
				type.setImageResource(R.drawable.download);
				String remote = data.getString("remote");			
				String fileName = remote.substring(remote.lastIndexOf("/")+1);
				name.setText(fileName);
			}else{
				type.setImageResource(R.drawable.upload);
				String local = data.getString("local");	
				String fileName = local.substring(local.lastIndexOf("/")+1);
				name.setText(fileName);
			}

			/*progress*/
			float fP =(float)((int)(10*data.getFloat("progress")))/10;
			progress.setText(""+fP+"%");
			progress_bar.setProgress((int)fP);
			
			/*speed*/
			float fS =(float)((int)(10*data.getFloat("speed")))/10;
			speed.setText(fS+"KB/s");
			
			/*status*/
			if(task.mStatus==Task.STATUS_WAIT){
				status.setText(R.string.tasklist_status_wait);
			}else{
				status.setText(R.string.tasklist_status_transfer);
			}
			/*size*/
			long totalSize = data.getLong("size");
			long accSize = data.getLong("accSize");
			long k = totalSize/1024;
			long m = k/1024;
		
			String s = (m!=0)? ""+accSize/(1024*1024)+"MB/"+m+"MB":
				(k!=0)?""+accSize/1024+"KB/"+k+"KB":
					""+accSize+"/"+totalSize;
			
			size.setText(s);
			
	
			return convertView;
		}
		

	}



	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		
		switch(item.getItemId()){
		case R.id.tasklist_delete:
			mListAdapter.markTaskCancel();
			return true;
		case R.id.tasklist_back:
			finish();
			return true;			
		default:
			return super.onOptionsItemSelected(item);
		}
		

	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.tasklist_menu, menu);
	    return true;
	}

}



