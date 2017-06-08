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

    /**
     * Main method to start downloading process
     * @return false - if downloading can not be started or another object is downloading now
     */
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

    /**
     * Stops downloading of the current object
     * process is asynchronous {@link #interrupted(T,File)}
     * @return false - if nothing to stop
     */
    public boolean interrupt() {
        if (thread == null || !isDownloading) {
            return false;
        }
        thread.interrupt();
        return true;
    }

    /**
     * @return true - if an object is downloading
     */
    public boolean isDownloading() {
        return isDownloading;
    }


    /**
     * @return current download object
     */
    public T getDownloadObject() {
        return downloadObject;
    }


    /**
     * @return next download object, that we want to download.
     */
    protected abstract T getNextDownloadObject();

    /**
     * @param downloadObject - object that we want to download
     * @return download url for this link
     */
    protected abstract String getUrl(T downloadObject);

    /**
     * @param downloadObject - object that we want to download
     * @return full file name where we want to save our data
     */
    protected abstract String getFileName(T downloadObject);

    /**
     * @return listener for download progress
     */
    protected abstract ProgressListener getProgressListener();

    /**
     * Called when object was downloaded successfully
     * @param obj - downloaded object
     * @param downloadInfo - all information about download
     */
    protected abstract void success(T obj, DownloadInfo downloadInfo);

    /**
     * @param obj - downloaded object with errors
     * @param downloadInfo - all information about download
     * @return Policy {@link #POLICY_CONTINUE},{@link #POLICY_HANDLE}
     */
    protected abstract @Policy int error(T obj, DownloadInfo downloadInfo);

    /**
     * @return do we have the next file to continue downloading
     */
    protected abstract boolean hasNext();


    /**
     *  init all what we want , it is called from {@link #onStartCommand(Intent, int, int)}
     */
    protected void init() {
        //this is empty init method, you can init here what you want
    }


    /**
     * This is callback for {@link #interrupt()}
     * @param downloadObject - object thar was interrupted
     * @param file - can be null if interruption was before downloading process
     */
    protected void interrupted(T downloadObject, @Nullable File file) {

    }


    /**
     *  Init okhttp client  for downloading
     */
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

    /**
     * Generate request for current object that we want to download
     * @param object - object that we want to download
     * @param file - where we want to save it {@link #getFileName(Object)}
     * @param url - generated Url {@link #getUrl(Object)}
     * @return builder for request
     */
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


    /**
     * Called from download thread when the file is downloaded.
     * @param downloadInfo
     */
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

    /**
     * Called from download thread when the file is downloaded with errors or
     * not downloaded at all.
     * @param downloadInfo
     */
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


    /**
     * Clears all data after each downloading
     */
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