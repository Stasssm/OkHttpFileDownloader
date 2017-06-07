package test.com.downloadlibrary;

import android.app.Application;
import android.content.Intent;

/**
 * Created by Stas on 07.06.17.
 */

public class App extends Application {

    private static App app;

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        if (MainService.getMainService() == null) {
            Intent intent = new Intent(this, MainService.class);
            startService(intent);
        }


    }

    public static App get() {
        return app;
    }
}
