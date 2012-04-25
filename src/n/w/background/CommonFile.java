package n.w.background;

import java.io.File;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.net.ftp.FTPFile;

public class CommonFile {
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
	
	public String getName(){
		return mIsFile? mFile.getName() : mFtpFile.getName();
	}
	
	public long getSize(){
		return mIsFile? mFile.length() : mFtpFile.getSize();
	}
	
	public String getSizeStr(){
		long s = getSize();
		long k = s / 1024;
		long m = k / 1024;
		String content = (m != 0) ? String.valueOf(m) + "MB"
				: (k != 0) ? String.valueOf(k) + "KB" 
				: String.valueOf(s) + "B";
		
		return content;
	}
	
	/* YEAR-MONTH-DAY_OF_MONTH */
	public String getTimeStr() {
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
