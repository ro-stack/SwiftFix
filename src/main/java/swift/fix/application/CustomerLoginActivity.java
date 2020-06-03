package swift.fix.application;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class CustomerLoginActivity extends AppCompatActivity {

    private EditText nameField;
    private EditText phoneField;
    private EditText Email;
    private EditText Password;
    private Button Login;
    private Button Registration;
    private TextView CustomerStatus;
    private TextView RegisterLink;
    private TextView LoginBackLink;
    private ProgressDialog loadingBar;

    private FirebaseAuth Auth;
    private FirebaseAuth.AuthStateListener firebaseAuthListener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_login);

        loadingBar = new ProgressDialog(this);

        Auth = FirebaseAuth.getInstance();

        // If users already logged in / authorised go to Problem Activity
        firebaseAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(user!=null){
                    Intent intent = new Intent(CustomerLoginActivity.this, CustomerProblemActivity.class); // change --> or intent below
                    startActivity(intent);
                    finish();
                    return;
                }
            }
        };

        nameField = (EditText) findViewById(R.id.customer_name);
        phoneField = (EditText) findViewById(R.id.customer_phone);
        Email = (EditText) findViewById(R.id.customer_email);
        Password = (EditText) findViewById(R.id.customer_password);
        Login = (Button) findViewById(R.id.customer_login);
        Registration = (Button) findViewById(R.id.customer_registration);
        CustomerStatus = (TextView) findViewById(R.id.customer_status2);
        RegisterLink = (TextView) findViewById(R.id.register_customer_link);
        LoginBackLink = (TextView) findViewById(R.id.customer_login_link);

        Registration.setVisibility(View.GONE);
        Registration.setEnabled(false);

        LoginBackLink.setVisibility(View.GONE);


        // When Register links clicked - show/remove suitable fields
        RegisterLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Login.setVisibility(View.GONE);
                RegisterLink.setVisibility(View.GONE);
                CustomerStatus.setText("Registration");

                nameField.setVisibility(View.VISIBLE);
                phoneField.setVisibility(View.VISIBLE);

                LoginBackLink.setVisibility(View.VISIBLE);

                Registration.setVisibility(View.VISIBLE);
                Registration.setEnabled(true);

            }
        });


        // When Login links clicked - show/remove suitable fields
        LoginBackLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Login.setVisibility(View.VISIBLE);
                RegisterLink.setVisibility(View.VISIBLE);
                CustomerStatus.setText("Login");

                nameField.setVisibility(View.GONE);
                phoneField.setVisibility(View.GONE);

                LoginBackLink.setVisibility(View.GONE);

                Registration.setVisibility(View.GONE);
                Registration.setEnabled(false);

            }
        });



        /* -------------------------------
         User required to enter all fields otherwise cannot proceed.
         If all entered, store in Firebase / authenticate.
         Proceed to Problem Activity
         ----------------------------------*/

        Registration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final String name = nameField.getText().toString();
                final String phone = phoneField.getText().toString();
                final String email = Email.getText().toString();
                final String password = Password.getText().toString();

                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(CustomerLoginActivity.this,
                            "Please enter your Name...", Toast.LENGTH_SHORT).show();
                }

                else if (TextUtils.isEmpty(phone)) {
                    Toast.makeText(CustomerLoginActivity.this,
                            "Please enter your Phone Number...", Toast.LENGTH_SHORT).show();
                }

                else if (TextUtils.isEmpty(email)) {
                    Toast.makeText(CustomerLoginActivity.this,
                            "Please enter an Email Address...", Toast.LENGTH_SHORT).show();
                }

                else if (TextUtils.isEmpty(password)) {
                    Toast.makeText(CustomerLoginActivity.this,
                            "Please enter a Password...", Toast.LENGTH_SHORT).show();
                } else {
                    loadingBar.setTitle("Customer Registration");
                    loadingBar.setMessage("Please wait while we check your credentials...");
                    loadingBar.show();

                    Auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(CustomerLoginActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (!task.isSuccessful()) {
                                Toast.makeText(CustomerLoginActivity.this, "Sign up error", Toast.LENGTH_SHORT).show();
                                loadingBar.dismiss();
                            } else {
                                Toast.makeText(CustomerLoginActivity.this, "Customer Registration Successful!", Toast.LENGTH_SHORT).show();
                                loadingBar.dismiss();

                                String name = nameField.getText().toString();
                                String phone= phoneField.getText().toString();

                                HashMap<String,String> user=new HashMap<>();
                                user.put("Name",name);
                                user.put("Phone",phone);

                                String user_id = Auth.getCurrentUser().getUid();
                                DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(user_id);
                                current_user_db.setValue(user);



                                Intent intent = new Intent(CustomerLoginActivity.this, CustomerProblemActivity.class);
                                startActivity(intent);
                                finish();
                                return;

                            }
                        }
                    });
                }
            }
        });

        /* -------------------------------
         User required to enter all fields otherwise cannot proceed.
         If all entered, check with Firebase
         Proceed to Problem Activity if successful.
         ----------------------------------*/
        Login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = Email.getText().toString();
                final String password = Password.getText().toString();

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(CustomerLoginActivity.this,
                            "Please enter an Email Address...", Toast.LENGTH_SHORT).show();
                }

                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(CustomerLoginActivity.this,
                            "Please enter a Password...", Toast.LENGTH_SHORT).show();
                } else {
                    loadingBar.setTitle("Customer Login");
                    loadingBar.setMessage("Please wait while we check your credentials...");
                    loadingBar.show();

                    Auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(CustomerLoginActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (!task.isSuccessful()) {
                                Toast.makeText(CustomerLoginActivity.this, "Sign in error", Toast.LENGTH_SHORT).show();
                                loadingBar.dismiss();
                            } else{

                            Toast.makeText(CustomerLoginActivity.this, "Sign in successful!", Toast.LENGTH_SHORT).show();
                            loadingBar.dismiss();

                                Intent intent = new Intent(CustomerLoginActivity.this, CustomerProblemActivity.class);
                                startActivity(intent);
                                finish();
                                return;

                        }
                        }
                    });

                }
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        Auth.addAuthStateListener(firebaseAuthListener);
    }
    @Override
    protected void onStop() {
        super.onStop();
        Auth.removeAuthStateListener(firebaseAuthListener);
    }
}