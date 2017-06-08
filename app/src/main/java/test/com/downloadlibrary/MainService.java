package test.com.downloadlibrary;

import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.PriorityQueue;

import okhttp3.Request;
import test.com.downloadlibrary.model.SomeObject;
import test.com.okhttpfiledownloader.DownloadService;
import test.com.okhttpfiledownloader.ProgressListener;
import test.com.okhttpfiledownloader.model.DownloadInfo;

/**
 * Created by Stas on 07.06.17.
 */

public class MainService extends DownloadService<SomeObject> {

    private static MainService mainService;
    private PriorityQueue<SomeObject> queue;
    private ProgressListener progressListener;

    @Override
    protected void init() {
        super.init();
        if (mainService == null) {
            mainService = this;
        }
        generateSomeRandomData();
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
        return queue.poll();
    }

    @Override
    public String getUrl(SomeObject downloadObject) {
        Log.d(TAG, "getUrl");
        return downloadObject.getFileUrl();
    }

    @Override
    public String getFileName(SomeObject someObject) {
        Log.d(TAG, "getFileName");
        return someObject.getFileName();
    }

    @Override
    protected Request.Builder buildRequest(SomeObject object, File file, String url) {
        Request.Builder builder =  super.buildRequest(object, file, url);
        if (file.length() != 0) {
            builder.addHeader("Range", "bytes=" + file.length() + "-");
        }
        return builder;
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
        obj.incrementPriority();
        queue.add(obj);
        return POLICY_HANDLE;
    }

    @Override
    protected void interrupted(SomeObject downloadObject, File file) {
        super.interrupted(downloadObject, file);
        Toast.makeText(this, "Download interrupted", Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean hasNext() {
        Log.d(TAG, "hasNext");
        return !queue.isEmpty();
    }


    private void generateSomeRandomData() {
        queue = new PriorityQueue<>(2);
        SomeObject someObject1 = new SomeObject();
        someObject1.setFileUrl("http://cdndl.zaycev.net/807943/4468107/ESTRADARADA_-_%D0%92%D0%B8%D1%82%D0%B5+%D0%9D%D0%B0%D0%B4%D0%BE+%D0%92%D1%8B%D0%B9%D1%82%D0%B8.mp3");
        someObject1.setFileName("sdcard/vitya.mp3");
        SomeObject someObject2 = new SomeObject();
        someObject1.setFileUrl("http://cdndl.zaycev.net/823571/4107190/MiyaGi+%26+%D0%AD%D0%BD%D0%B4%D1%88%D0%BF%D0%B8%D0%BB%D1%8C_-_%23%D0%A2%D0%90%D0%9C%D0%90%D0%94%D0%90.mp3");
        someObject1.setFileName("sdcard/tamada.mp3");
        queue.add(someObject1);
        queue.add(someObject2);
    }

}
