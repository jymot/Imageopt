package im.wangchao.imageopt;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static im.wangchao.imageopt.BitmapUtils.checkNotNull;

/**
 * <p>Description  : Imageopt.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 17/5/17.</p>
 * <p>Time         : 下午4:37.</p>
 */
public class Imageopt {
    private static final String COMPRESS_PREFIX = "OptCompressed-";
    private static final String THUMBNAIL_PREFIX = "OptThumbnail-";

    private final Context mContext;

    private final File mTargetFile;
    private final File mCompressFile;
    private final File mThumbnailFile;

    private final String mThumbnailCacheKey;
    private final int mQuality;
    private final int mMinQuality;
    private final long mTargetSize;

    private Imageopt(Builder builder){
        this.mContext = builder.mContext;
        this.mTargetFile = builder.mTargetFile;
        this.mCompressFile = builder.mCompressFile;
        this.mThumbnailFile = builder.mThumbnailFile;
        this.mThumbnailCacheKey = builder.mThumbnailCacheKey;
        this.mQuality = builder.mQuality;
        this.mMinQuality = builder.mMinQuality;
        this.mTargetSize = builder.mTargetSize;
    }

    public static void clearAllCache(Context context){
        File cacheDir = context.getExternalCacheDir();
        if (cacheDir == null){
            return;
        }
        File[] tempFiles = cacheDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                String name = pathname.getName();
                return name.startsWith(COMPRESS_PREFIX) || name.startsWith(THUMBNAIL_PREFIX);
            }
        });

        if (tempFiles == null){
            return;
        }

        for (File file: tempFiles){
            file.delete();
        }
    }

    public static void clearThumbnailCache(Context context){
        File cacheDir = context.getExternalCacheDir();
        if (cacheDir == null){
            return;
        }
        File[] tempFiles = cacheDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                String name = pathname.getName();
                return name.startsWith(THUMBNAIL_PREFIX);
            }
        });

        if (tempFiles == null){
            return;
        }

        for (File file: tempFiles){
            file.delete();
        }
    }

    public static void clearCompressCache(Context context){
        File cacheDir = context.getExternalCacheDir();
        if (cacheDir == null){
            return;
        }
        File[] tempFiles = cacheDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                String name = pathname.getName();
                return name.startsWith(COMPRESS_PREFIX);
            }
        });

        if (tempFiles == null){
            return;
        }

        for (File file: tempFiles){
            file.delete();
        }
    }


    /**
     * @return {@link Builder}.
     */
    public static Builder with(Context context){
        return new Builder(context);
    }

    /**
     * Create {@link Builder} with current {@link Imageopt}.
     */
    public Builder newBuilder(){
        return new Builder(this);
    }

    /**
     * Get thumbnail.
     */
    public Bitmap thumbnail(int reqWidth, int reqHeight){
        // get memory thumbnail.
        Bitmap memBitmap = BitmapUtils.getMemoryCache(mThumbnailCacheKey);
        if (memBitmap != null){
            return memBitmap;
        }

        // check thumbnail local file.
        if (mThumbnailFile != null && mThumbnailFile.exists()){
            final Bitmap thumbnailBm = BitmapUtils.thumbnail(mThumbnailFile.getAbsolutePath(), reqWidth, reqHeight, true);
            BitmapUtils.addMemoryCache(mThumbnailCacheKey, thumbnailBm);
            return thumbnailBm;
        }

        // check compress local file.
        final File tempFile = mCompressFile != null && mCompressFile.exists() ? mCompressFile : mTargetFile;
        final Bitmap thumbnailBm = BitmapUtils.thumbnail(tempFile.getAbsolutePath(), reqWidth, reqHeight, true);
        BitmapUtils.addMemoryCache(mThumbnailCacheKey, thumbnailBm);

        // async save thumbnail local file.
        AndroidExecutors.background().execute(new Runnable() {
            @Override public void run() {
                try {
                    createThumbnailImageFile();
                    compress(thumbnailBm, mThumbnailFile.getAbsolutePath(), mQuality);
                } catch (Exception ignore) {}
            }
        });
        return thumbnailBm;
    }

    /**
     * Compress bitmap.
     */
    public void compress(){
        this.compress(CompressListener.DEFAULT);
    }

    /**
     * Compress bitmap.
     */
    public void compress(int reqWidth, int reqHeight){
        this.compress(reqWidth, reqHeight, CompressListener.DEFAULT);
    }

    /**
     * Compress bitmap.
     */
    public void compress(CompressListener listener){
        DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
        final int width = dm.widthPixels / 2;
        final int height = dm.heightPixels / 2;
        this.compress(width, height, listener);
    }

    /**
     * Compress bitmap.
     */
    public void compress(final int reqWidth, final int reqHeight, final CompressListener listener){
        try {
            final String originalImagePath = mTargetFile.getAbsolutePath();
            AndroidExecutors.background().execute(new Runnable() {
                @Override public void run() {
                    try {
                        createCompressImageFile();

                        Bitmap originBitmap = BitmapUtils.decodeSampledBitmapFromResource(originalImagePath, reqWidth, reqHeight);
                        // rotate bitmap if possible
                        originBitmap = BitmapUtils.rotate(originBitmap, BitmapUtils.exifRotateAngle(originalImagePath));

                        final String compressPath = mCompressFile.getAbsolutePath();

                        long available = 0L;
                        int innerQuality = mQuality;
                        do {
                            compress(originBitmap, compressPath, innerQuality);

                            if (mTargetSize == 0) {
                                break;
                            }

                            try {
                                FileInputStream fis = new FileInputStream(compressPath);
                                available = fis.available();
                            } catch (Exception ignore) {
                            }

                            if (available <= mTargetSize) {
                                break;
                            } else {
                                innerQuality -= 5;
                            }

                        } while (innerQuality > mMinQuality);

                        final int finalQuality = innerQuality;
                        AndroidExecutors.uiThread().execute(new Runnable() {
                            @Override
                            public void run() {
                                listener.success(Imageopt.this, mCompressFile, finalQuality);
                            }
                        });
                    } catch (final Exception e) {
                        AndroidExecutors.uiThread().execute(new Runnable() {
                            @Override
                            public void run() {
                                listener.failure(e);
                            }
                        });
                    }
                }
            });
        } catch (final Exception e) {
            AndroidExecutors.uiThread().execute(new Runnable() {
                @Override public void run() {
                    listener.failure(e);
                }
            });
        }
    }

    private boolean compress(Bitmap bitmap, String outfile, int quality) throws Exception{
        boolean isSuccess = false;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(outfile);
            isSuccess = bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            //avoid v6.0+ occur crash without permission
            throw new RuntimeException(e);
        } finally {
            if (fos != null){
                try {
                    fos.close();
                } catch (IOException ignore) {}
            }
        }
        return isSuccess;
    }

    private void createCompressImageFile() throws Exception {
        if (mCompressFile.exists()){
            return;
        }
        mCompressFile.createNewFile();
    }

    private void createThumbnailImageFile() throws Exception {
        if (mThumbnailFile.exists()){
            return;
        }
        mThumbnailFile.createNewFile();
    }

    private static File getCompressFile(Context context, String cacheKey){
        try {
            String imageFileName = COMPRESS_PREFIX + cacheKey;
            String suffix = ".jpg";
            File storageDir = context.getExternalCacheDir();
            return new File(storageDir, imageFileName + suffix);
        } catch (Exception e) {
            return null;
        }
    }

    private static File getThumbnailFile(Context context, String cacheKey){
        try {
            String imageFileName = THUMBNAIL_PREFIX + cacheKey;
            String suffix = ".jpg";
            File storageDir = context.getExternalCacheDir();
            return new File(storageDir, imageFileName + suffix);
        } catch (Exception e){
            return null;
        }
    }


    public static class Builder{
        Context mContext;
        File mTargetFile;
        File mCompressFile;
        File mThumbnailFile;
        String mThumbnailCacheKey;
        // default value.
        int mQuality = 70;
        int mMinQuality = 20;
        long mTargetSize = 0L;

        Builder(Context context){
            this.mContext = context.getApplicationContext();
        }

        Builder(Imageopt imageopt){
            this.mContext = imageopt.mContext;
            this.mTargetFile = imageopt.mTargetFile;
            this.mCompressFile = imageopt.mCompressFile;
            this.mThumbnailFile = imageopt.mThumbnailFile;
            this.mThumbnailCacheKey = imageopt.mThumbnailCacheKey;
            this.mQuality = imageopt.mQuality;
            this.mMinQuality = imageopt.mMinQuality;
            this.mTargetFile = imageopt.mTargetFile;
        }

        public Builder quality(int quality){
            this.mQuality = quality;
            return this;
        }

        public Builder minQuality(int minQuality){
            this.mMinQuality = minQuality;
            return this;
        }

        public Builder targetSize(long targetSize){
            this.mTargetSize = targetSize;
            return this;
        }

        public Builder load(File targetFile){
            this.mTargetFile = checkNotNull(targetFile, "load file can not be null.");
            if (!this.mTargetFile.exists()){
                throw new RuntimeException("Target file(" + targetFile.getAbsolutePath() + ") is not exists.");
            }
            this.mThumbnailCacheKey = DigestUtils.md5(this.mTargetFile.getAbsolutePath());
            this.mThumbnailFile = getThumbnailFile(this.mContext, this.mThumbnailCacheKey);
            this.mCompressFile = getCompressFile(this.mContext, this.mThumbnailCacheKey);
            return this;
        }

        public Imageopt build(){
            checkNotNull(this.mTargetFile, "Target file can not be null.");
            return new Imageopt(this);
        }
    }
}
