package im.wangchao.imageoptapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.FileProvider;

import java.io.File;

/**
 * <p>Description  : TestProvider.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 17/5/11.</p>
 * <p>Time         : 下午5:13.</p>
 */
public class TestProvider extends FileProvider {
    public static final String AUTHORITY = "im.wangchao.imageoptapp.fileprovider";

    public static Uri compatUriFromFile(Context context, File file){
        return compatUriFromFile(context, file, null);
    }

    public static Uri compatUriFromFile(Context context, File file, Intent intent) {
        if (!needUseProvider()){
            return Uri.fromFile(file);
        }

        if (intent != null){
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        return getUriForFile(context, AUTHORITY, file);
    }

    public static boolean needUseProvider(){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }

}
