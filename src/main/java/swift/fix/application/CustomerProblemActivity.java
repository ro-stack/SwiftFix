package swift.fix.application;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class CustomerProblemActivity extends AppCompatActivity {


    private String cust_prob = "";
    private String UserID;
    private String Problem;
    private EditText problem;
    private TextView currentProblem;
    private DatabaseReference customerDatabase;
    private FirebaseAuth Auth;
    private Button confirmButton;
    private Button blankButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.customer_problem);

        Auth = FirebaseAuth.getInstance();
        UserID = Auth.getCurrentUser().getUid();
        customerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(UserID);

        problem = (EditText) findViewById(R.id.customer_problem);

        currentProblem = (TextView) findViewById(R.id.current_problem);

        confirmButton = (Button) findViewById(R.id.confirm);
        blankButton = (Button) findViewById(R.id.blank);

        // Get the users previous/current problem or if they're new show as empty.
        getCurrentProblem();


        // Allow user to set problem as blank
        // Proceed to Map Activity
        blankButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Map userInfo = new HashMap();
                userInfo.put("Problem", "Problem not stated.");
                customerDatabase.updateChildren(userInfo);
                Toast.makeText(CustomerProblemActivity.this,
                        "Problem left blank.", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(CustomerProblemActivity.this, CustomerMapActivity.class);
                startActivity(intent);
                finish();
                return;

            }
        });


        // Allow user to set they're problem
        // Proceed to Map Activity
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                cust_prob = problem.getText().toString();

                if (TextUtils.isEmpty(cust_prob)) {
                    Toast.makeText(CustomerProblemActivity.this,
                            "Please enter your problem...", Toast.LENGTH_SHORT).show();
                } else {
                    Map userInfo = new HashMap();
                    userInfo.put("Problem", cust_prob);
                    customerDatabase.updateChildren(userInfo);
                    Toast.makeText(CustomerProblemActivity.this,
                            "Problem saved!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(CustomerProblemActivity.this, CustomerMapActivity.class);
                    startActivity(intent);
                    finish();
                    return;

                }
            }
        });


    }

    // Finds users current problem they set. Or if they're new show a default problem.
    private void getCurrentProblem() {
        customerDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("Problem") != null) {
                        Problem = map.get("Problem").toString();
                        currentProblem.setText(Problem);
                    } else if (map.get("Problem") == null) {
                        currentProblem.setText("No problem stated.");
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


}