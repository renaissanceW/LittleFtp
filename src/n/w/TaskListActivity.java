package n.w;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
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
	
	/*UI RESOURCE*/
	private Drawable mDrawDownload;
	private Drawable mDrawUpload;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.tasklist);	
		mListView = (ListView)findViewById(R.id.tasklist_listview);
		mFtpMaster = FtpMaster.getFtpMasterInstance();
		mHandler = new TaskListHandler();
		
		mListAdapter = new TaskListAdapter(this,null);
		mListView.setAdapter(mListAdapter);
		mListView.setOnItemClickListener(new TaskItemClickListener());
		
		mDrawDownload = getResources().getDrawable(R.drawable.download);
		mDrawUpload = getResources().getDrawable(R.drawable.upload);
		
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
			ViewHolder holder;
			if(convertView == null){
				convertView = mInflater.inflate(R.layout.task_item_new, null);		
				holder = new ViewHolder();			
				holder.type = (ImageView)convertView.findViewById(R.id.task_type);
				holder.name = (TextView)convertView.findViewById(R.id.task_name);
				holder.progress = (TextView)convertView.findViewById(R.id.task_progress);
				holder.speed = (TextView)convertView.findViewById(R.id.task_speed);
				holder.status = (TextView)convertView.findViewById(R.id.task_status);
				holder.size = (TextView)convertView.findViewById(R.id.task_size);	
				holder.progress_bar = (ProgressBar)convertView.findViewById(R.id.task_progress_bar);
				holder.cb = (CheckBox)convertView.findViewById(R.id.task_check_box);
				convertView.setTag(holder);
			}else{
				holder = (ViewHolder)convertView.getTag();
			}
			
			Task task = mTasks.get(position);
			Bundle data = mTasks.get(position).mData;	
			
			/*checkbox*/
			holder.cb.setChecked(mSelection.contains(position));
			
			/*type & name*/
			if(data.getInt("action")==C.TASK_ACTION_DOWNLOAD){
				holder.type.setImageDrawable(mDrawDownload);
				holder.name.setText(data.getString("remoteName"));
			}else{
				holder.type.setImageDrawable(mDrawUpload);
				holder.name.setText(data.getString("localName"));
			}

			/*progress*/
			holder.progress.setText(data.getString("progress"));
			holder.progress_bar.setProgress(data.getInt("progressInt"));
			
			/*speed*/
			holder.speed.setText(data.getString("speed"));
			
			/*status*/
			if(task.mStatus==Task.STATUS_WAIT){
				holder.status.setText(R.string.tasklist_status_wait);
			}else{
				holder.status.setText(R.string.tasklist_status_transfer);
			}
			/*size*/	
			holder.size.setText(data.getString("accSize"));
			
	
			return convertView;
		}
		

	}

	static class ViewHolder{
		ImageView type;
		TextView name;
		TextView progress;
		TextView speed;
		TextView status;
		TextView size;
		ProgressBar progress_bar;
		CheckBox cb;
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



