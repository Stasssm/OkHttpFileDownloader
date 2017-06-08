package test.com.okhttpfiledownloader;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import test.com.okhttpfiledownloader.model.DownloadInfo;

/**
 * Created by Stas on 05.06.17.
 */
public abstract class DownloadService<T> extends Service {

    public static final String TAG = DownloadService.class.getName();
    // this type we use when we want continue to download files after an error
    public static final int POLICY_CONTINUE = 1;
    //this type we use when we want to handle error by ourselves.
    public static final int POLICY_HANDLE = 2;
    //client for downloading the file
    protected OkHttpClient okHttpClient;
    //flag that indicates that file currently downloading
    private volatile boolean isDownloading;
    //current object that we are trying to download
    private volatile T downloadObject;
    private boolean isReconnect = false;
    private Thread thread;
    private Handler handler = new Handler(Looper.getMainLooper());


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(TAG, "onStartCommand");
        isReconnect = isReconnect();
        init();
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public boolean start() {
        if (!isDownloading) {
            downloadObject = getNextDownloadObject();
            if (downloadObject != null) {
                thread = new DownloadThread();
                thread.start();
            }
            return downloadObject != null;
        } else {
            return false;
        }
    }

    public boolean interrupt() {
        if (thread == null || !isDownloading) {
            return false;
        }
        thread.interrupt();
        return true;
    }

    public boolean isDownloading() {
        return isDownloading;
    }

    public T getDownloadObject() {
        return downloadObject;
    }

    protected abstract T getNextDownloadObject();

    protected abstract String getUrl(T downloadObject);

    protected abstract String getFileName(T downloadObject);

    protected abstract ProgressListener getProgressListener();

    protected abstract void success(T obj, DownloadInfo downloadInfo);

    protected abstract @Policy int error(T obj, DownloadInfo downloadInfo);

    protected abstract boolean hasNext();

    protected void init() {
        //this is empty init method, you can init here what you want
    }

    protected void interrupted(T downloadObject, @Nullable File file) {

    }

    protected void initClient() {
        if (okHttpClient == null) {
            OkHttpClient.Builder bd = new OkHttpClient.Builder();
            bd.addNetworkInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                            .body(new ProgressResponseBody(originalResponse.body(), getProgressListener()))
                            .build();
                }
            });
            bd.connectTimeout(25, TimeUnit.SECONDS);
            bd.readTimeout(25, TimeUnit.SECONDS);
            okHttpClient = bd.build();
        }
    }

    protected Request.Builder buildRequest(T object, File file, String url) {
        return new Request.Builder().url(url);
    }


    /**
     * This method method shows reeconnect politics. Do we need to connect
     * again if network was lost and continue to download
     *
     * @return boolean reconnect politics.
     */
    protected boolean isReconnect() {
        return isReconnect;
    }

    private void saveAndStartNext(final DownloadInfo downloadInfo) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                success(downloadObject, downloadInfo);
                clearAll();
                if (hasNext()) {
                    start();
                }
            }
        });
    }

    private void handleError(final DownloadInfo downloadInfo) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                int policy = error(downloadObject, downloadInfo);
                clearAll();
                if (policy == POLICY_CONTINUE && hasNext()) {
                    start();
                }
            }
        });
    }

    protected void clearAll() {
        isDownloading = false;
        downloadObject = null ;
        thread = null;
    }

    @IntDef({POLICY_CONTINUE, POLICY_HANDLE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Policy {
    }

    private final class DownloadThread extends Thread {

        @Override
        public void run() {
            super.run();
            isDownloading = true;
            String url = getUrl(downloadObject);
            String fileName = getFileName(downloadObject);
            if (checkInterruption(null)) return;
            if (!TextUtils.isEmpty(fileName)) {
                File file = new File(fileName);
                try {
                    if (!file.exists()) {
                        //TODO create logic when file not created
                        file.createNewFile();
                    }
                    Request request = buildRequest(downloadObject, file, url).build();
                    initClient();
                    Response response = okHttpClient.newCall(request).execute();

                    InputStream is = response.body().byteStream();
                    BufferedInputStream input = new BufferedInputStream(is);
                    OutputStream output = new FileOutputStream(fileName,true);

                    byte[] data = new byte[1024];
                    int total = 0;
                    boolean interruptFlag = false;
                    while ((total = input.read(data)) != -1) {
                        output.write(data, 0, total);
                        if (checkInterruption(file))  {
                            interruptFlag = true;
                            break;
                        }
                    }
                    //TODO check what happens when exception is thrown
                    // is there any memory leak in input and output buffer?
                    output.flush();
                    output.close();
                    input.close();
                    if (interruptFlag) {
                        return;
                    }
                    DownloadInfo downloadInfo =
                            new DownloadInfo(DownloadInfo.FILE_DOWNLOADED, null, null);
                    saveAndStartNext(downloadInfo);
                } catch (FileNotFoundException e) {
                    DownloadInfo downloadInfo =
                            new DownloadInfo(DownloadInfo.FILE_DOWNLOAD_ERROR, null, e);
                    handleError(downloadInfo);
                    e.printStackTrace();
                } catch (IOException e) {
                    DownloadInfo downloadInfo =
                            new DownloadInfo(DownloadInfo.FILE_DOWNLOAD_ERROR, file, e);
                    handleError(downloadInfo);
                    e.printStackTrace();
                }
            } else {
                throw new NullPointerException("Empty file name");
            }
            isDownloading = false;
        }
    }

    private boolean checkInterruption(final File file) {
        if (Thread.interrupted()) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    interrupted(downloadObject,file);
                    clearAll();
                }
            });
            return true;
        }
        return false;
    }


}