package im.wangchao.imageoptapp;

import android.app.ActivityManager;
import android.content.Context;

import static android.content.Context.ACTIVITY_SERVICE;

/**
 * <p>Description  : MemoryUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 17/5/3.</p>
 * <p>Time         : 上午8:09.</p>
 */
public class MemoryUtils {
    private static long mCurrentAvailMem = 0L;

    private MemoryUtils(){}

    public static void start(Context context){
        ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(memoryInfo);

        mCurrentAvailMem = memoryInfo.availMem;
    }

    public static void end(Context context){
        ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(memoryInfo);

        long count = mCurrentAvailMem - memoryInfo.availMem;
    }
}
