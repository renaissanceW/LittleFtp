package n.w.background;


import n.w.uitil.MyLog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class BookMarkDB {
	
	 	private static final String TAG = "BookMarkDB";
	    private static final String DATABASE_NAME = "bookmarkDB";
	    private static final String TABLE_NAME = "table_bookmark";
	    private static final int DATABASE_VERSION = 2;

	    private final BookMarkDBOpenHelper mDatabaseOpenHelper;
	
	    public BookMarkDB(Context context) {
	        mDatabaseOpenHelper = new BookMarkDBOpenHelper(context);
	    }
	    
	    public long add(Record r){
	    	ContentValues initialValues = new ContentValues();
	    	initialValues.put("name", r.name);
            initialValues.put("host", r.host);
            initialValues.put("port", r.port);
            initialValues.put("user", r.user);
            initialValues.put("pwd", r.pwd);
	    	return mDatabaseOpenHelper.getWritableDatabase()
	    			.insert(TABLE_NAME, null, initialValues);
	    }
	    
	    public int update(Record r){
	    	ContentValues initialValues = new ContentValues();
	    	initialValues.put("name", r.name);
            initialValues.put("host", r.host);
            initialValues.put("port", r.port);
            initialValues.put("user", r.user);
            initialValues.put("pwd", r.pwd);
            return mDatabaseOpenHelper.getWritableDatabase()
            		.update(TABLE_NAME, initialValues, 
            				"name=\""+r.name+"\"", null);
	    }
	    
	    
	    public class Record{
	    	public String name;
	    	public String host;
	    	public int port;
	    	public String user;
	    	public String pwd;
	    	public Record(String n, String h, int port, String u, String pwd){
	    		name = n; host = h; 
	    		this.port = port; user = u; this.pwd = pwd;
	    	}
	    }
	    
	    /*
	     * get all rowid & name
	     */
	    public String[] getAllNames(){
	    	Cursor cursor = mDatabaseOpenHelper.getReadableDatabase().
	    			query(TABLE_NAME, null, null, null, null, null, null);
//	    	if(cursor.getCount()==0)
//	    		return null;
	    	
	    	String[] result = new String[cursor.getCount()];
	    	cursor.moveToFirst();
	    	for(int i=0;i<cursor.getCount();i++){
	    		result[i] = cursor.getString(cursor.getColumnIndex("name"));
	    		cursor.moveToNext();
	    	}
	    	
	    	return result;
	    }
	    
	    
	    
	    public Record getByName(String name){
	    	Cursor cursor = mDatabaseOpenHelper.getReadableDatabase()
	    			.query(TABLE_NAME, null, "name=\""+name+"\"", 
	    					null, null, null, null);
	    	cursor.moveToFirst();
	    	Record result = new Record(
		    	 cursor.getString(cursor.getColumnIndex("name"))
		    	,cursor.getString(cursor.getColumnIndex("host"))
		    	,cursor.getInt(cursor.getColumnIndex("port"))
		    	,cursor.getString(cursor.getColumnIndex("user"))
		    	,cursor.getString(cursor.getColumnIndex("pwd"))
		    	);
	    	return result;
	    }
	    
	    public int delete(String name){
	    	return mDatabaseOpenHelper.getWritableDatabase().
	    			delete(TABLE_NAME, "name=\""+name+"\"", null);
	    }
	    
	 
	
	 /**
     * This creates/opens the database.
     */
    private static class BookMarkDBOpenHelper extends SQLiteOpenHelper {

        private SQLiteDatabase mDatabase;

        /* "rowid" is automatically used as a unique
         * identifier, so when making requests, we will use "_id" as an alias for "rowid"
         */
        private static final String TABLE_CREATE =
                    "CREATE TABLE "+TABLE_NAME+
                    " (name TEXT PRIMARY KEY UNIQUE, " +
                    "host TEXT, port INTEGER, user TEXT, pwd TEXT);"; 

        BookMarkDBOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            mDatabase = db;
            mDatabase.execSQL("DROP TABLE IF EXISTS "+TABLE_NAME);
            mDatabase.execSQL(TABLE_CREATE);
        }
        
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            MyLog.d(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            
            onCreate(db);
        }

     
  
    }


}
