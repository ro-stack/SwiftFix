package swift.fix.application;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Map;

public class TradesmanMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap Map;
    Location lastLocation;
    LocationRequest locationRequest;
    private SupportMapFragment mapFragment;
    private FusedLocationProviderClient FusedLocationClient;
    private ImageButton settingsButton;
    private Button jobStatus;
    private Button cancelRequest;
    private String CustomerID = "";
    private Boolean isLoggingOut = false;
    private LinearLayout customerInfo;
    private ImageView customerProfileImg;
    private TextView customerName;
    private TextView customerPhone;
    private TextView customerProblem;
    private int status = 0;
    private LatLng customerLatLng;
    private Switch availableSwitch;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tradesman_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        FusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        customerInfo = (LinearLayout) findViewById(R.id.customer_information);
        customerProfileImg = (ImageView) findViewById(R.id.customer_profileImage);
        customerName = (TextView) findViewById(R.id.customer_name);
        customerPhone = (TextView) findViewById(R.id.customer_phone);
        customerProblem = (TextView) findViewById(R.id.customer_problem);

        settingsButton = (ImageButton) findViewById(R.id.settings_button);
        cancelRequest = (Button) findViewById(R.id.cancel_request);


        // Allows tradesman to cancel request, should they not want to accept it.
        cancelRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endRequest();

                Toast toast = Toast.makeText(getApplicationContext(), "Request Cancelled", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0);
                toast.show();

                cancelRequest.setVisibility(View.GONE);
            }
        });

        // Allow us to accept it and say when it is complete.
        // I wanted to add more to this - so when you first accept it displays route.
        // 1st click = accept, 2nd click = end.
        jobStatus = (Button) findViewById(R.id.job_status);
        jobStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Request Accepted!", Toast.LENGTH_LONG).show();

                switch (status) {
                    case 1:
                        status = 2;

                        jobStatus.setText("Request Complete?");
                        cancelRequest.setVisibility(View.GONE);

                        break;
                    case 2:

                        endRequest();

                        Toast.makeText(getApplicationContext(), "Request Completed", Toast.LENGTH_LONG).show();
                        break;
                }
            }
        });


        /* ---------------------------
          Here I added a switch which allows the tradesman to say if they're available.
          This stops them being able to match customers 24/7.
          When on they are Available within Firebase. When off they cannot be matched.
         -----------------------------------*/
        availableSwitch = (Switch) findViewById(R.id.availableSwitch);
        availableSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    connectTradesman();

                } else {
                    disconnectTradesman();

                }
            }
        });


        // Opens tradesman settings.
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                availableSwitch.setChecked(false);
                Intent intent = new Intent(TradesmanMapActivity.this, TradesmanSettingsActivity.class);
                startActivity(intent);
                return;
            }
        });

        // Find assigned customer
        getAssignedCustomer();

    }


    /* ---------------------------
         We can find required information for a request.
         Using CustomerRequestID we can get the pickup location and their information.
         -----------------------------------*/
    private void getAssignedCustomer() {
        String tradesmanID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Tradesman").child(tradesmanID).child("CustomerRequest").child("CustomerRequestID");
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    status = 1;
                    CustomerID = dataSnapshot.getValue().toString();
                    getAssignedCustomerPickupLocation();
                    getAssignedCustomerInformation();
                    cancelRequest.setVisibility(View.VISIBLE);
                } else {
                    endRequest();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

        /* ---------------------------
          Find the location of our assigned customer from Firebase and add a marker on our map.
         -----------------------------------*/

    Marker customerMarker;
    private DatabaseReference assignedCustomerPickupLocationRef;
    private ValueEventListener assignedCustomerPickupLocationRefListener;
    private void getAssignedCustomerPickupLocation() {
        assignedCustomerPickupLocationRef = FirebaseDatabase.getInstance().getReference().child("CustomerRequest").child(CustomerID).child("l");
        assignedCustomerPickupLocationRefListener = assignedCustomerPickupLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && !CustomerID.equals("")) {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    if (map.get(0) != null) {
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1) != null) {
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    customerLatLng = new LatLng(locationLat, locationLng);
                    customerMarker = Map.addMarker(new MarkerOptions().position(customerLatLng).title("Customer Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

        /* ---------------------------
          Retrieve the customers information from Firebase.
          This allows us to display it upon matching.
         -----------------------------------*/

    private void getAssignedCustomerInformation() {
        customerInfo.setVisibility(View.VISIBLE);
        DatabaseReference customerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(CustomerID);
        customerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("Name") != null) {
                        customerName.setText(map.get("Name").toString());

                    }
                    if (map.get("Phone") != null) {
                        customerPhone.setText(map.get("Phone").toString());
                    }
                    if (map.get("Problem") != null) {
                        customerProblem.setText(map.get("Problem").toString());
                    }
                    if (map.get("profileImageUrl") != null) {
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(customerProfileImg);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    /* ---------------------------
      Remove request from Firebase.
      Reset information, remove customer marker.
     -----------------------------------*/
    private void endRequest() {
        jobStatus.setText("Accept Request?");

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference tradesmanRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Tradesman").child(userId).child("CustomerRequest");
        tradesmanRef.removeValue();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("CustomerRequest");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(CustomerID, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
            }
        });

        CustomerID = "";


        if (customerMarker != null) {
            customerMarker.remove();
        }
        if (assignedCustomerPickupLocationRefListener != null) {
            assignedCustomerPickupLocationRef.removeEventListener(assignedCustomerPickupLocationRefListener);
        }
        customerInfo.setVisibility(View.GONE);
        customerName.setText("");
        customerPhone.setText("");

        // Like Customer Map - this caused a problem below
        // customerProfileImg.setImageResource(R.mipmap.icon_swift);
    }


    /* ---------------------------------
        Here we are finding the users location and updating it in intervals.
        The permissions need to be checked for location to be provided.
        We add a small circle which displays the current location, and it focuses/zooms around it.
         ---------------------------------- */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Map = googleMap;
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                LatLng UK = new LatLng(54.6, -2.6);
                Map.moveCamera(CameraUpdateFactory.newLatLngZoom(UK, 6));


            } else {
                checkLocationPermission();
            }
        }
    }


    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                if (getApplicationContext() != null) {

                    lastLocation = location;

                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    Map.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    Map.animateCamera(CameraUpdateFactory.zoomTo(11));

                    //tradesmanLocation = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                    //tradesmanMarker = Map.addMarker(new MarkerOptions().position(tradesmanLocation).title("Your Location"));

                    String UserID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("TradesmanAvailable");
                    DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("TradesmanWorking");
                    GeoFire geoFireAvailable = new GeoFire(refAvailable);
                    GeoFire geoFireWorking = new GeoFire(refWorking);

                    switch (CustomerID) {
                        case "":
                            geoFireWorking.removeLocation(UserID, new GeoFire.CompletionListener() {
                                @Override
                                public void onComplete(String key, DatabaseError error) {

                                }
                            });
                            geoFireAvailable.setLocation(UserID, new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {
                                @Override
                                public void onComplete(String key, DatabaseError error) {

                                }
                            });
                            break;

                        default:
                            geoFireAvailable.removeLocation(UserID, new GeoFire.CompletionListener() {
                                @Override
                                public void onComplete(String key, DatabaseError error) {

                                }
                            });
                            geoFireWorking.setLocation(UserID, new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {
                                @Override
                                public void onComplete(String key, DatabaseError error) {

                                }
                            });
                            break;
                    }

                }
            }
        }
    };

    /* ---------------------------------
           Here we are checking that the permission has been allowed for location.
           We first check to see by using a dialog.
           Then if permission allowed we display the users location on the map.
             ---------------------------------- */
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("give permission")
                        .setMessage("give permission message")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(TradesmanMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                            }
                        })
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(TradesmanMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        // PROBLEM: When logging in - says available when not checked ... could be below
                        //FusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                        //Map.setMyLocationEnabled(true);

                        //Move map over England
                        LatLng UK = new LatLng(54.6, -2.6);
                        Map.moveCamera(CameraUpdateFactory.newLatLngZoom(UK, 6));
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Please provide the permission", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    // Adds tradesman's location
    private void connectTradesman() {
        checkLocationPermission();
        FusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        Map.setMyLocationEnabled(true);
    }


    // Removes tradesman from being available and removes location.
    private void disconnectTradesman() {

        if (FusedLocationClient != null) {
            FusedLocationClient.removeLocationUpdates(locationCallback);
        }

        String UserID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("TradesmanAvailable");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(UserID, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
            }
        });

    }

    // If tradesman not logging out, disconnect them - we remove them from being available if they are.
    @Override
    protected void onStop() {
        super.onStop();
        if (!isLoggingOut) {
            disconnectTradesman();
        }
    }

}

