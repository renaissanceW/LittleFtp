package n.w;

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
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class ExplorerFragment extends Fragment {

	class ExplorerTabListener implements ActionBar.TabListener {

		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			ft.add(android.R.id.content, ExplorerFragment.this, null);
			mMaster.setHandler(mHandler);

		}

		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			ft.remove(ExplorerFragment.this);
		}

		public void onTabReselected(Tab tab, FragmentTransaction ft) {
			// TODO Auto-generated method stub
		}
	}

	private MainActivity mParent;

	private ListView mFileListView;
	private CommonFileListAdapter mFileListAdapter = null;
	private Handler mHandler;
	private Master mMaster;
	private View mView = null;

	private boolean mIsLocal = false;

	public ExplorerFragment(MainActivity parent, boolean isLocal) {
		mParent = parent;
		mIsLocal = isLocal;
		mMaster = Master.getFtpMasterInstance();
		mHandler = new ExplorerHandler();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		sendMasterRequest(C.MSG_MASTER_LS, null);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		if (mView == null) {
			mView = inflater.inflate(R.layout.explorer, container, false);
			mFileListView = (ListView) mView
					.findViewById(R.id.explorer_listview);
			registerForContextMenu(mFileListView);
		}
		return mView;
	}

	private void sendMasterRequest(int what, Object obj) {
		C.sendMessage(mMaster.getHandler(), what, mIsLocal, obj);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// TODO Auto-generated method stub
		inflater.inflate(R.menu.option_menu, menu);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		// TODO Auto-generated method stub
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = mParent.getMenuInflater();
		inflater.inflate(mIsLocal ? R.menu.local_context_menu
				: R.menu.remote_context_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.explorer_refresh:
			sendMasterRequest(C.MSG_MASTER_LS, null);
			return true;
		case R.id.explorer_back:
			sendMasterRequest(C.MSG_MASTER_BACK, null);
			return true;
		case R.id.explorer_new:
			DialogFragment newFragment = new NewDirInputFragment();
			newFragment.show(getFragmentManager(), "dialog");
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		CommonFile f = mFileListAdapter.getItem(info.position);
		switch (item.getItemId()) {
		case R.id.remote_ctx_menu_delete:
		case R.id.local_ctx_menu_delete:
			DeleteConfirmFragment confirmFragment = new DeleteConfirmFragment(f);
			confirmFragment.show(getFragmentManager(), "delete");
			return true;
		case R.id.remote_ctx_menu_download:
			sendMasterRequest(C.MSG_MASTER_FILE_DOWN, f);
			return true;

		case R.id.local_ctx_menu_upload:
			sendMasterRequest(C.MSG_MASTER_FILE_UP, f);

			return true;
		default:
			return super.onContextItemSelected(item);
		}

	}

	class ExplorerHandler extends Handler {

		public void handleMessage(Message msg) {
			switch (msg.what) {
			case C.MSG_MASTER_CD_REPLY:
			case C.MSG_MASTER_BACK_REPLY:
			case C.MSG_MASTER_MKDIR_REPLY:
			case C.MSG_MASTER_DELETE_REPLY:
				if (msg.arg1 == C.FTP_OP_SUCC) {
					sendMasterRequest(C.MSG_MASTER_LS, null);
				} else {
					Toast.makeText(mParent, "Action Failed", Toast.LENGTH_SHORT).show();
				}
				break;

			case C.MSG_MASTER_LS_REPLY:
				if (msg.arg1 == C.FTP_OP_SUCC) {

					updateUI((CommonFile[]) msg.obj);
				}
				break;

			}
		}

	}
	
	private void updateUI(CommonFile[] data) {
		if (mFileListAdapter == null) {
			mFileListAdapter = new CommonFileListAdapter(mParent, data);
			mFileListView.setAdapter(mFileListAdapter);
			mFileListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			mFileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
						public void onItemClick(AdapterView<?> parent,
								View view, int position, long id) {
							// Auto-generated method stub
							CommonFile f = mFileListAdapter.getItem(position);
							switch (f.getType()) {
							case CommonFile.TYPE_DIRECTORY:
								sendMasterRequest(C.MSG_MASTER_CD, f.getName());
								break;
							}

						}
					});
		} else {
			mFileListAdapter.setData(data);
			mFileListView.clearChoices();
			mFileListAdapter.notifyDataSetChanged();
		}
	}

	class DeleteConfirmFragment extends DialogFragment {

		private CommonFile mF;

		public DeleteConfirmFragment(CommonFile f) {
			mF = f;
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {

			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

			return builder
					.setTitle(R.string.delete_confirm)
					.setPositiveButton("ok",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {

									sendMasterRequest(C.MSG_MASTER_DELETE, mF);

								}
							}).setNegativeButton("cancel", null).create();
		}

	}

	class NewDirInputFragment extends DialogFragment {

		View mLayout;

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {

			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			LayoutInflater inflater = mParent.getLayoutInflater();
			mLayout = inflater.inflate(R.layout.one_input_dialog, null);

			return builder
					.setView(mLayout)
					.setTitle(R.string.mkdir_title)
					.setPositiveButton("ok",
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int which) {
									// TODO Auto-generated method stub
									EditText et = (EditText) mLayout
											.findViewById(R.id.new_input_text);
									String name = et.getText().toString();
									sendMasterRequest(C.MSG_MASTER_MKDIR, name);

								}

							}).setNegativeButton("cancel", null).create();
		}

	}

}
