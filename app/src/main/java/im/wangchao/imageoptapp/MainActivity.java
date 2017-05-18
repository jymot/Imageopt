package im.wangchao.imageoptapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import im.wangchao.imageopt.CompressListener;
import im.wangchao.imageopt.Imageopt;

/**
 * 1. 小于 7.0 JPEG
 * 2. PNG
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener, RadioGroup.OnCheckedChangeListener{
    private static final int CAPTURE_CUSTOM_FILE_REQUEST_CODE = 3;
    private static final int JPEG = 1;
    private static final int PNG = 2;

    static {
        System.loadLibrary("native-lib");
    }

    int pattern = JPEG;

    TextView consoleView;
    ImageView originalView, compressedView;

    static String mCurrentImagePath;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView(){
        consoleView = (TextView) findViewById(R.id.consoleView);
        consoleView.setText(stringFromJNI());

        originalView = (ImageView) findViewById(R.id.originalView);
        compressedView = (ImageView) findViewById(R.id.compressedView);

        ((RadioGroup)findViewById(R.id.radioGroup)).setOnCheckedChangeListener(this);

        findViewById(R.id.photoButton).setOnClickListener(this);
        findViewById(R.id.compressButton).setOnClickListener(this);
        findViewById(R.id.clearButton).setOnClickListener(this);

    }

    public native String stringFromJNI();

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "Image-" + timeStamp;
        String suffix = "";
        switch (pattern){
            case JPEG:
                suffix = ".jpg";
                break;
            case PNG:
                suffix = ".png";
                break;
        }
//        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File storageDir = getExternalCacheDir();
        File target = File.createTempFile(imageFileName, suffix, storageDir);
        mCurrentImagePath = target.getAbsolutePath();
        return target;
    }

    private File createCompressImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "Compressed-" + timeStamp;
        String suffix = "";
        switch (pattern){
            case JPEG:
                suffix = ".jpg";
                break;
            case PNG:
                suffix = ".png";
                break;
        }
//        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File storageDir = getExternalCacheDir();
        return File.createTempFile(imageFileName, suffix, storageDir);
    }

    private void setBitmap(ImageView imageView, String photoPath, String prefix) {
        // Get the dimensions of the View
        int targetW = imageView.getWidth();
        int targetH = imageView.getHeight();
        if (targetH == 0){
            targetH = 300;
        }
        if (targetW == 0){
            targetW = 300;
        }

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(photoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        long available = 0L;
        try {
            FileInputStream fis = new FileInputStream(photoPath);
            available = fis.available();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Bitmap bitmap = BitmapUtils.thumbnail(photoPath, targetW, targetH, true);
        Log.e("wcwcwc", prefix + " image -> \n" +
                ">>> height: " + photoW + "\n" +
                ">>> width: " + photoH + "\n" +
                ">>> size: " + Formatter.formatFileSize(this, available) + "\n" +
                ">>> memory size: " + Formatter.formatFileSize(this, getMemorySize(bitmap)));
        imageView.setImageBitmap(bitmap);
    }

    private void normalCompress() {
        // Get the dimensions of the View
        final int targetW = compressedView.getWidth();
        final int targetH = compressedView.getHeight();

        Imageopt.with(this).load(new File(mCurrentImagePath)).build().compress(768, 1024, new CompressListener() {
            @Override
            public void success(Imageopt me, File saveFile, int quality) {
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(saveFile.getAbsolutePath(), options);

                long available = 0L;
                try {
                    FileInputStream fis = new FileInputStream(saveFile.getAbsolutePath());
                    available = fis.available();
                } catch (Exception ignore) {
                }

                Bitmap bitmap = me.thumbnail(targetW, targetH);
                Log.e("wcwcwc", "normal compress image -> \n" +
                        ">>> quality: " + quality + "\n" +
                        ">>> height: " + options.outHeight + "\n" +
                        ">>> width: " + options.outWidth + "\n" +
                        ">>> size: " + Formatter.formatFileSize(MainActivity.this, available) + "   " + available + "\n" +
                        ">>> memory size: " + Formatter.formatFileSize(MainActivity.this, getMemorySize(bitmap)));
                compressedView.setImageBitmap(bitmap);
            }

            @Override
            public void failure(Exception e) {

            }
        });

//            Bitmap bitmap = BitmapFactory.decodeFile(compressPath, options);
//            Log.e("wcwcwc", " ---- " + Formatter.formatFileSize(this, getMemorySize(bitmap)));
//            bitmap = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true);
    }

    private long getMemorySize(Bitmap bitmap){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return bitmap.getAllocationByteCount();
        }
        return bitmap.getByteCount();
    }

    private void clearDir(File tempStorageDir){
        File[] tempFiles = tempStorageDir.listFiles(new FileFilter() {
            @Override public boolean accept(File pathname) {
                String name = pathname.getName();
                return name.startsWith("Image-") || name.startsWith("Compressed-");
            }
        });

        for (File file: tempFiles){
            FileUtils.deleteFile(file);
        }
    }

    @Override public void onClick(View v) {
        switch (v.getId()){
            case R.id.photoButton:
                Intent photoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                File file = null;
                try {
                    file = createImageFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (file != null){
                    photoIntent.putExtra(MediaStore.EXTRA_OUTPUT, TestProvider.compatUriFromFile(this, file, photoIntent));
                    startActivityForResult(photoIntent, CAPTURE_CUSTOM_FILE_REQUEST_CODE);
                }
                break;
            case R.id.compressButton:
                normalCompress();
                break;
            case R.id.clearButton:
                Imageopt.clearAllCache(this);
                clearDir(getExternalCacheDir());
                break;
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK){
            switch (requestCode){
                case CAPTURE_CUSTOM_FILE_REQUEST_CODE:
                    if (mCurrentImagePath != null){
                        setBitmap(originalView, mCurrentImagePath, "original");
                    }
                    break;
            }
        }
    }

    @Override public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
        mCurrentImagePath = null;
        switch (checkedId){
            case R.id.jpegRadio:
                pattern = JPEG;
                break;
            case R.id.pngRadio:
                pattern = PNG;
                break;
        }
    }
}
