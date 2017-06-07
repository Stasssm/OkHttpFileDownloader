package test.com.okhttpfiledownloader.model;

import android.support.annotation.IntDef;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Stas on 07.06.17.
 */

public class DownloadInfo {

    public static final int FILE_DOWNLOADED = 1;
    public static final int FILE_DOWNLOAD_ERROR = 2;

    private int status;
    private File file;
    private Exception exception;

    @IntDef({FILE_DOWNLOADED, FILE_DOWNLOAD_ERROR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Status {}

    public DownloadInfo(@Status int status, File file, Exception exception) {
        this.status = status;
        this.file = file;
        this.exception = exception;
    }

    public boolean isSuccessful() {
        return status == FILE_DOWNLOADED;
    }


    public int getStatus() {
        return status;
    }

    public void setStatus(@Status int status) {
        this.status = status;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }
}
