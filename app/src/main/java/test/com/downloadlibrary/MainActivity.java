package test.com.downloadlibrary;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import test.com.okhttpfiledownloader.ProgressListener;

public class MainActivity extends AppCompatActivity {

    private Button mDownloadButton;
    private Button mInterruptButton;
    private ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
    }

    private void initViews() {
        mDownloadButton = (Button) findViewById(R.id.button_start);
        mInterruptButton = (Button) findViewById(R.id.button_stop);
        progressBar = (ProgressBar) findViewById(R.id.progress_downloading);
        mDownloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainService mainService = MainService.getMainService();
                if (mainService != null) {
                    mainService.connectCustomListener(new ProgressListener() {
                        @Override
                        public void update(long bytesRead, long contentLength, boolean done) {
                            Log.d(MainService.TAG, "bytesRead = " + bytesRead +
                                    " contentLength = " + contentLength +
                                    " done = " + done);
                            int progress = (int) (((float) bytesRead / (float) contentLength) * 100);
                            progressBar.setProgress(progress);
                        }
                    });
                    mainService.start();
                }
            }
        });
        mInterruptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isStopPossible = MainService.getMainService().interrupt();
                if (!isStopPossible) {
                    Toast.makeText(MainActivity.this,"Nothing to stop", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

}
