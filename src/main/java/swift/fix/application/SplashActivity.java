package swift.fix.application;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

public class SplashActivity extends AppCompatActivity {

    private ProgressBar Progress;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        Progress = (ProgressBar) findViewById(R.id.splash_screen_progress_bar);


        // Begin loading
        new Thread(new Runnable() {
            public void run() {
                setProgress();
                startApp();
                finish();
            }
        }).start();
    }

    // Set time for progression
    private void setProgress() {
        for (int progress = 0; progress < 120; progress += 20) {
            try {
                Thread.sleep(1000);
                Progress.setProgress(progress);
            } catch (Exception e) {
                e.printStackTrace();

            }
        }
    }

    // Start Main Activity upon finish
    private void startApp() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
    }
}
