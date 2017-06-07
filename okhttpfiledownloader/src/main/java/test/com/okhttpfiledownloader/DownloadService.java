package test.com.okhttpfiledownloader;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
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
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Stas on 05.06.17.
 */
public abstract class DownloadService<T> extends Service {

    public static final String TAG = DownloadService.class.getName();

    private volatile boolean isDownloading;
    private volatile T downloadObject;
    private boolean isReconnect = false;
    private Thread thread;
    private Handler handler = new Handler(Looper.getMainLooper());
    protected OkHttpClient okHttpClient;


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

    public void start() {
        downloadObject = getNextDownloadObject();
        if (downloadObject != null) {
            thread = new Thread(backgroundRunnable);
            thread.start();
        }
    }

    public boolean isDownloading() {
        return isDownloading;
    }

    public T getDownloadObject() {
        return downloadObject;
    }

    protected abstract T getNextDownloadObject();

    protected abstract String getUrl(T downloadObject);

    protected abstract String getFileName();

    protected abstract ProgressListener getProgressListener();

    protected abstract void save(T obj);

    protected abstract boolean hasNext();

    protected void init() {
        //this is empty init method, you can init here what you want
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

    private void saveAndStartNext() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                save(downloadObject);
                downloadObject = null;
                isDownloading = false;
                if (hasNext()) {
                    start();
                }
            }
        });
    }

    private Runnable backgroundRunnable = new Runnable() {
        @Override
        public void run() {
            isDownloading = true;
            String url = getUrl(downloadObject);
            String fileName = getFileName();
            if (!TextUtils.isEmpty(fileName)) {
                File file = new File(fileName);
                boolean isCreated = true;
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
                    OutputStream output = new FileOutputStream(fileName);

                    byte[] data = new byte[1024];
                    int total = 0;
                    while ((total = input.read(data)) != -1) {
                        output.write(data, 0, total);
                    }
                    //TODO check what happens when exeption is thrown
                    // is there any memory leak in input and output buffer?
                    output.flush();
                    output.close();
                    input.close();
                    saveAndStartNext();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                //TODO
            }
            isDownloading = false;
        }
    };

}