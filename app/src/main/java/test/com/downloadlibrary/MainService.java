package test.com.downloadlibrary;

import android.util.Log;

import test.com.downloadlibrary.model.SomeObject;
import test.com.okhttpfiledownloader.DownloadService;
import test.com.okhttpfiledownloader.ProgressListener;
import test.com.okhttpfiledownloader.model.DownloadInfo;

/**
 * Created by Stas on 07.06.17.
 */

public class MainService extends DownloadService<SomeObject> {

    private static MainService mainService;
    private SomeObject someObject = new SomeObject();
    private ProgressListener progressListener;

    @Override
    protected void init() {
        super.init();
        if (mainService == null) {
            mainService = this;
        }
    }

    protected  void connectCustomListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }


    public static MainService getMainService() {
        Log.d(TAG, "getMainService");
        return mainService;
    }


    @Override
    public SomeObject getNextDownloadObject() {
        Log.d(TAG, "getNextDownloadObject");
        return someObject;
    }

    @Override
    public String getUrl(SomeObject downloadObject) {
        Log.d(TAG, "getUrl");
        return downloadObject.getFileUrl();
    }

    @Override
    public String getFileName() {
        Log.d(TAG, "getFileName");
        return "sdcard/barbara.mp3";
    }

    @Override
    public ProgressListener getProgressListener() {
        return progressListener;
    }

    @Override
    public void success(SomeObject obj, DownloadInfo  downloadInfo) {
        Log.d(TAG, "success");
    }

    @Override
    protected int error(SomeObject obj, DownloadInfo downloadInfo) {
        return POLICY_CONTINUE;
    }

    @Override
    public boolean hasNext() {
        Log.d(TAG, "hasNext");
        return false;
    }
}
