package n.w;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.net.ftp.FTPFile;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class CommonFileListAdapter extends BaseAdapter {
	
	private Context mContext;
	private LayoutInflater mInflater;
	
	private CommonFile[] mList;

	private Drawable mDrawFile;
	private Drawable mDrawDir;
	
	public CommonFileListAdapter(Context ctx, CommonFile[] f){
		mContext = ctx;
		mInflater = LayoutInflater.from(mContext);
		mList = f;
		
		mDrawFile = ctx.getResources().getDrawable(R.drawable.file);
		mDrawDir = ctx.getResources().getDrawable(R.drawable.folder);
	}
	
	
	
	
	public void setData(CommonFile[] f){
		mList  = f;
	}
	

	/*the caller ensure the selection is not null*/
	public CommonFile[] getSelection(long[] selection){
		
		CommonFile[] result = new CommonFile[selection.length];
		for(int i=0; i<selection.length; i++){
			result[i] = mList[(int)selection[i]];
		}
		return result;
	}
	

	public int getCount() {
		// TODO Auto-generated method stub
		int size = (mList==null?0:mList.length);
		MyLog.d("ListDirAdapter", "size: "+size);
		return size;
	}

	public CommonFile getItem(int position) {
		// TODO Auto-generated method stub
		MyLog.d("ListDirAdapter", "position: " + position);
		return mList == null ? null : mList[position];	
	}

	public long getItemId(int position) {
		// TODO Auto-generated method stub
		MyLog.d("ListDirAdapter", "id: "+position);
		return position;
	}
	
	public boolean isEmpty(){
		return getCount() == 0;
	}
	
	static class CommonFileViewHolder{
		TextView name;
		ImageView type;
		TextView size;
		TextView time;
	}


	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		CommonFileViewHolder holder;
		if (convertView == null) {
			holder = new CommonFileViewHolder();
			convertView = mInflater.inflate(R.layout.file_item, null);
			holder.name = (TextView) convertView.findViewById(R.id.file_name);
			holder.type = (ImageView) convertView.findViewById(R.id.file_type);
			holder.size = (TextView) convertView.findViewById(R.id.file_size);
			holder.time = (TextView) convertView.findViewById(R.id.file_time);
			convertView.setTag(holder);
		}else{
			holder = (CommonFileViewHolder)convertView.getTag();
		}

		CommonFile f = getItem(position);
		
		
		holder.name.setText(f.getName());

		if (f.getType() == CommonFile.TYPE_FILE) {
			holder.type.setImageDrawable(mDrawFile);
		} else if (f.getType() == CommonFile.TYPE_DIRECTORY) {
			holder.type.setImageDrawable(mDrawDir);
		}


		holder.size.setText(f.getSizeStr());	
		holder.time.setText(f.getTimeStr());		

		return convertView;
	}

	@Override
	public boolean hasStableIds() {
		// TODO Auto-generated method stub
		return true;
	}

}


class CommonFile{
	
	public static final int TYPE_DIRECTORY = 0;
	public static final int TYPE_FILE = 1;
	
	private boolean mIsFile;
	private File mFile;
	private FTPFile mFtpFile;
	
	public CommonFile(File f){
		mIsFile = true;
		mFile = f;
		mFtpFile = null;
	}
	public CommonFile(FTPFile f){
		mIsFile = false;
		mFile = null;
		mFtpFile = f;
	}
	
	public File getFile(){
		return mFile;
	}
	
	public FTPFile getFTPFile(){
		return mFtpFile;
	}
	
	public boolean IsLocalFile(){
		return mIsFile;
	}
	
	public int getType(){
		if(mIsFile){
			return mFile.isDirectory()? TYPE_DIRECTORY:TYPE_FILE;
		}else{
			return mFtpFile.getType() == FTPFile.DIRECTORY_TYPE? TYPE_DIRECTORY:TYPE_FILE;
		}
	}
	
	String getName(){
		return mIsFile? mFile.getName() : mFtpFile.getName();
	}
	
	long getSize(){
		return mIsFile? mFile.length() : mFtpFile.getSize();
	}
	
	String getSizeStr(){
		long s = getSize();
		long k = s / 1024;
		long m = k / 1024;
		String content = (m != 0) ? String.valueOf(m) + "MB"
				: (k != 0) ? String.valueOf(k) + "KB" : "1KB";
		
		return content;
	}
	
	/* YEAR-MONTH-DAY_OF_MONTH */
	String getTimeStr() {
		Calendar ca = null;
		if (mIsFile) {
			ca = Calendar.getInstance();
			ca.setTime(new Date(mFile.lastModified()));
		} else {
			ca = mFtpFile.getTimestamp();
		}

		return ca.get(Calendar.YEAR) + "-" + (1 + ca.get(Calendar.MONTH)) + "-"
				+ ca.get(Calendar.DAY_OF_MONTH);
	}

}















