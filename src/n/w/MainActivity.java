package n.w;


import n.w.R;
import n.w.R.drawable;
import n.w.R.string;
import n.w.fragment.ExplorerFragment;
import n.w.fragment.LoginFragment;
import n.w.fragment.TaskListFragment;
import android.app.ActionBar;
import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

public class MainActivity extends Activity {
	
	   @Override
	    protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        
	        loadResource();
	        
	        LoginFragment loginFrag = new LoginFragment();
	        loginFrag.init(this);
	        ExplorerFragment remoteFrag = new ExplorerFragment();
	        remoteFrag.init(this, false);
	        ExplorerFragment localFrag = new ExplorerFragment();
	        localFrag.init(this, true);
	        TaskListFragment taskFrag = new TaskListFragment();
	        taskFrag.init(this);
	        loginFrag.setHasOptionsMenu(true);
	        remoteFrag.setHasOptionsMenu(true);
	        localFrag.setHasOptionsMenu(true);
	        taskFrag.setHasOptionsMenu(true);

	        final ActionBar bar = getActionBar();
	        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
	        /*collapse navigation bar to main action bar*/
	        bar.setDisplayShowTitleEnabled(false);
	        bar.setDisplayShowHomeEnabled(false);
	        
	        bar.addTab(bar.newTab()
	        		.setText(R.string.Tab_Login)
	        		.setTabListener(loginFrag.new LoginTabListener()));
	        bar.addTab(bar.newTab()
	                .setText(R.string.Tab_Remote)
	                .setTabListener(remoteFrag.new ExplorerTabListener()));
	        bar.addTab(bar.newTab()
	                .setText(R.string.Tab_Local)
	                .setTabListener(localFrag.new ExplorerTabListener()));
	        bar.addTab(bar.newTab()
	                .setText(R.string.Tab_TaskList)
	                .setTabListener(taskFrag.new TaskListTabListener()));
	
	    }
	
	
	
	/*
	 * THE RESOURCE PART
	 * 	
	 */
	public Drawable mDrawDownload;
	public Drawable mDrawUpload;
	void loadResource(){
		mDrawDownload = getResources().getDrawable(R.drawable.download);
		mDrawUpload = getResources().getDrawable(R.drawable.upload);
	}
	
	void unloadResource(){
		mDrawDownload = null;
		mDrawUpload = null;
	}

	
}

	