package n.w;

import java.util.ArrayList;

import org.apache.commons.net.ftp.FTPFile;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ListDirAdapter extends BaseAdapter {
	
	private Context mContext;
	private LayoutInflater mInflater;
	
	private ArrayList<FTPFile> mFileList;

	
	
	public ListDirAdapter(Context ctx, ArrayList<FTPFile> l){
		mContext = ctx;
		mInflater = LayoutInflater.from(mContext);
		setData(l);
	}
	
	public void setData(ArrayList<FTPFile> l){
		mFileList = l;
	}
	

	
	public ArrayList<FTPFile> getSelection(long[] selection){
		ArrayList<FTPFile> result = new ArrayList<FTPFile>();
		
		if(mFileList != null){
			for(long i : selection){
				result.add(mFileList.get((int)i));
			}
		}
		
		return result.size() == 0? null:result;
	}

	public int getCount() {
		// TODO Auto-generated method stub
		int size = (mFileList==null?0:mFileList.size());
		MyLog.d("ListDirAdapter", "size: "+size);
		return size;
	}

	public Object getItem(int position) {
		// TODO Auto-generated method stub
		MyLog.d("ListDirAdapter", "position: "+position);
		return mFileList==null? null:mFileList.get(position) ;
	}

	public long getItemId(int position) {
		// TODO Auto-generated method stub
		MyLog.d("ListDirAdapter", "id: "+position);
		return position;
	}
	
	public boolean isEmpty(){
		return getCount() == 0;
	}
	


	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.file_item, null);
		}

		FTPFile f = mFileList.get(position);
		TextView name = (TextView) convertView.findViewById(R.id.file_name);
		ImageView type = (ImageView) convertView.findViewById(R.id.file_type);
		TextView size = (TextView) convertView.findViewById(R.id.file_size);

		name.setText(f.getName());

		if (f.getType() == FTPFile.FILE_TYPE) {
			type.setImageResource(R.drawable.file);
		} else if (f.getType() == FTPFile.DIRECTORY_TYPE) {
			type.setImageResource(R.drawable.folder);
		}

		long s = f.getSize();
		long k = s / 1024;
		long m = k / 1024;
		String content = (m != 0) ? String.valueOf(m) + "MB"
				: (k != 0) ? String.valueOf(k) + "KB" : "1KB";
		size.setText(content);

		return convertView;
	}

	@Override
	public boolean hasStableIds() {
		// TODO Auto-generated method stub
		return true;
	}

}
