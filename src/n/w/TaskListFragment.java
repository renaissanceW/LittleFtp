package n.w;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
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

public class TaskListFragment extends Fragment {
	
	class TaskListTabListener implements ActionBar.TabListener {

	    public void onTabSelected(Tab tab, FragmentTransaction ft) {
	        ft.add(android.R.id.content, TaskListFragment.this, null);
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

	    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
	        ft.remove(TaskListFragment.this);
	        mTimer.cancel();
	        mTimer=null;
	    }

		public void onTabReselected(Tab tab, FragmentTransaction ft) {
			// TODO Auto-generated method stub
		}
			
	}
	
	
	MainActivity mParent;	
	private TaskListAdapter mListAdapter;
	private ListView mListView;
	private FtpMaster mFtpMaster;
	private Timer mTimer;
	private Handler mHandler;
	private View mView;
	
	public TaskListFragment(MainActivity parent){
		mParent = parent;
		mFtpMaster = FtpMaster.getFtpMasterInstance();
		mListAdapter = new TaskListAdapter(mParent,null);
		
		mHandler = new TaskListHandler();
	}
	

	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		if (mView == null) {
			mView = inflater.inflate(R.layout.tasklist, container, false);
			mListView = (ListView) mView.findViewById(R.id.tasklist_listview);
			mListView.setAdapter(mListAdapter);
			mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

						public void onItemClick(AdapterView<?> parent,
								View view, int position, long id) {
							// TODO Auto-generated method stub
							mListAdapter.select(position);
						}

					});
		}
		return mView;
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
	


	
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// TODO Auto-generated method stub
		inflater.inflate(R.menu.tasklist_menu, menu);
	}



	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch(item.getItemId()){
		case R.id.tasklist_delete:
			mListAdapter.markTaskCancel();
			return true;		
		default:
			return super.onOptionsItemSelected(item);
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
				convertView = LayoutInflater.from(mParent).inflate(R.layout.task_item_new, null);		
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
				holder.type.setImageDrawable(mParent.mDrawDownload);
				holder.name.setText(data.getString("remoteName"));
			}else{
				holder.type.setImageDrawable(mParent.mDrawDownload);
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


}
