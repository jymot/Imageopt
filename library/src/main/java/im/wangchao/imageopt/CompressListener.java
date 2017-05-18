package im.wangchao.imageopt;

import java.io.File;

/**
 * <p>Description  : CompressListener.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 17/5/18.</p>
 * <p>Time         : 上午11:20.</p>
 */
public interface CompressListener {
    CompressListener DEFAULT = new CompressListener() {
        @Override public void success(Imageopt me, File saveFile, int quality) {
        }

        @Override public void failure(Exception e) {
        }
    };

    /**
     * Compress success.
     *
     * @param me {@link Imageopt}.
     * @param saveFile Compress file.
     * @param quality The final compress for quality.
     */
    void success(Imageopt me, File saveFile, int quality);

    /**
     * Compress failure.
     */
    void failure(Exception e);
}
