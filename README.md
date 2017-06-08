# OkHttpFileDownloader

This library is needed to download a lot of files in the background by using only OkHttp. For example, you can simply use it to download song mp3 files in your app. Also it gives ability to simply show the progress of downloading.   

## Implementation

To use this library you need to create you Service that will extend DownloadService and implement the folloving methods. 

```java
    protected abstract T getNextDownloadObject();

    protected abstract String getUrl(T downloadObject);

    protected abstract String getFileName(T downloadObject);

    protected abstract ProgressListener getProgressListener();

    protected abstract void success(T obj, DownloadInfo downloadInfo);

    protected abstract @Policy int error(T obj, DownloadInfo downloadInfo);

    protected abstract boolean hasNext();
```

Simple Implementation is [HERE](https://github.com/Stasssm/OkHttpFileDownloader/blob/master/app/src/main/java/test/com/downloadlibrary/MainService.java)

Example how to use it is [HERE](https://github.com/Stasssm/OkHttpFileDownloader/blob/master/app/src/main/java/test/com/downloadlibrary/MainActivity.java)

Also do not forget to start your service and add Modify AndroidManifest.xml

```xml
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />


    <application>
		............
        <service android:name="your service name" />
        ............
   	</application>    


```

## Service work diagram

