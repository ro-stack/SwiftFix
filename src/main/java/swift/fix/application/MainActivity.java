package swift.fix.application;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private Button Tradesman;
    private Button Customer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Tradesman = (Button) findViewById(R.id.tradesman_button);
        Customer = (Button) findViewById(R.id.customer_button);

        // Determines which user status they have
        // Will guide them to either Tradesman or Customer login activity
        Tradesman.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TradesmanLoginActivity.class);
                startActivity(intent);
            }
        });

        Customer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CustomerLoginActivity.class);
                startActivity(intent);

            }
        });
    }
}