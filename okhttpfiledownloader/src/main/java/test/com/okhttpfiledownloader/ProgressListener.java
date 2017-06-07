package test.com.okhttpfiledownloader;

/**
 * Created by Stas on 05.06.17.
 */


public interface ProgressListener {
    void update(long bytesRead, long contentLength, boolean done);
}
