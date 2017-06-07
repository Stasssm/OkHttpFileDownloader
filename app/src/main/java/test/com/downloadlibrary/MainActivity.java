package test.com.downloadlibrary;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import test.com.okhttpfiledownloader.ProgressListener;

public class MainActivity extends AppCompatActivity {

    private Button button;
    private ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
    }

    private void initViews() {
        button = (Button) findViewById(R.id.button_start);
        progressBar = (ProgressBar) findViewById(R.id.progress_downloading);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainService mainService = MainService.getMainService();
                if (mainService != null) {
                    mainService.connectCustomListener(new ProgressListener() {
                        @Override
                        public void update(long bytesRead, long contentLength, boolean done) {
                            int progress = (int) (((float) bytesRead / (float) contentLength) * 100);
                            progressBar.setProgress(progress);
                        }
                    });
                    mainService.start();
                }
            }
        });
    }

}
