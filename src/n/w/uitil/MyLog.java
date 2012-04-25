package n.w.uitil;

import android.util.Log;  
  
/** 
 * a wrapper of Log, including the 
 *
 * 
 */  
public class MyLog 
{  
   
    private static String getFunctionName()  
    {  
        StackTraceElement[] sts = Thread.currentThread().getStackTrace();  
        if(sts == null)  
        {  
            return null;  
        }  
        for(StackTraceElement st : sts)  
        {  
            if(st.isNativeMethod())  
            {  
                continue;  
            }  
            if(st.getClassName().equals(Thread.class.getName()))  
            {  
                continue;  
            }  
            if(st.getClassName().equals(MyLog.class.getName()))  
            {  
                continue;  
            }  
            return  "[ " + Thread.currentThread().getName() + ": "  
                    + st.getFileName() + ":" + st.getLineNumber() + " "  
                    + st.getMethodName() + " ]";  
        }  
        return null;  
    }  
      

    public static void v(String tag, String msg)  
    {  
    	Log.v(tag, getFunctionName()+msg);         
    }  
    public static void d(String tag, String msg)  
    {  
    	Log.d(tag, getFunctionName()+msg);         
    } 
    public static void i(String tag, String msg)  
    {  
    	Log.i(tag, getFunctionName()+msg);         
    } 
    public static void w(String tag, String msg)  
    {  
    	Log.w(tag, getFunctionName()+msg);         
    } 
    public static void e(String tag, String msg)  
    {  
    	Log.e(tag, getFunctionName()+msg);         
    } 
  
      
}  