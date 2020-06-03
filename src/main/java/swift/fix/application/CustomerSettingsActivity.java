package swift.fix.application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CustomerSettingsActivity extends AppCompatActivity {

    private EditText nameField;
    private EditText phoneField;
    private Button backButton;
    private Button confirmButton;
    private Button logOutBtn;
    private ImageView profileImg;

    private String Name;
    private String Phone;

    private FirebaseAuth Auth;
    private DatabaseReference customerDatabase;

    private String UserID;

    private Uri resultUri;
    private String profileImgUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_settings);

        nameField = (EditText) findViewById(R.id.customer_name);
        phoneField = (EditText) findViewById(R.id.customer_phone);
        backButton = (Button) findViewById(R.id.back);
        confirmButton = (Button) findViewById(R.id.confirm);
        logOutBtn = (Button) findViewById(R.id.logout_button);
        profileImg = (ImageView) findViewById(R.id.customer_profileImage);

        Auth = FirebaseAuth.getInstance();
        UserID = Auth.getCurrentUser().getUid();

        customerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(UserID);

        // Gets users set information from Firebase
        getUserInfo();

        // Select an image from storage
        profileImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Pick image and only image
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 1);
            }
        });

        // Saves the users set information upon click
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserInfo();

            }
        });

        // Returns user to Map Activity
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                return;
            }
        });


        // Logs customer out and returns them to Main/Selection Screen
        logOutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(CustomerSettingsActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

    }

    // Retrieves users information from Firebase
    // Allows us to display it in Settings.
    private void getUserInfo() {
        customerDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("Name") != null) {
                        Name = map.get("Name").toString();
                        nameField.setText(Name);
                    }
                    if (map.get("Phone") != null) {
                        Phone = map.get("Phone").toString();
                        phoneField.setText(Phone);
                    }
                    if (map.get("profileImageUrl") != null) {
                        profileImgUrl = map.get("profileImageUrl").toString();
                        Glide.with(getApplication()).load(profileImgUrl).into(profileImg);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }



    /* ---------------------------------------
    https://github.com/bumptech/glide
    Here we are saving the users new input to Firebase.
    I used Glides exemplar code to implement a way for users to add a profile image.
    Read in an image, store as JPEG, save to user.
     ------------------------------------------- */
    private void saveUserInfo() {
        Name = nameField.getText().toString();
        Phone = phoneField.getText().toString();

        Map userInfo = new HashMap();
        userInfo.put("Name", Name);
        userInfo.put("Phone", Phone);
        customerDatabase.updateChildren(userInfo);

        if (resultUri != null) {
            StorageReference filePath = FirebaseStorage.getInstance().getReference().child("ProfileImages").child(UserID);
            Bitmap bitmap = null;

            try {
                bitmap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(), resultUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 20, baos);
            byte[] data = baos.toByteArray();
            UploadTask uploadTask = filePath.putBytes(data);

            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    finish();
                    return;
                }
            });
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Task<Uri> downloadUrl = taskSnapshot.getMetadata().getReference().getDownloadUrl();

                    Map newImage = new HashMap();
                    newImage.put("profileImageUrl", downloadUrl.toString());
                    customerDatabase.updateChildren(newImage);

                    finish();
                    return;
                }
            });
        } else {
            finish();
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            final Uri imageUri = data.getData();
            resultUri = imageUri;
            profileImg.setImageURI(resultUri);
        }
    }
}
