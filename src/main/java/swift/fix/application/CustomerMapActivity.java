package swift.fix.application;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback {


    private GoogleMap Map;
    private FusedLocationProviderClient FusedLocationClient;
    private SupportMapFragment mapFragment;
    Location lastLocation;
    LocationRequest locationRequest;
    private Button requestTradesmanBtn;
    private ImageButton settingsBtn;
    private Button cancelTradesman;
    private Button problemBtn;
    private LatLng customerLocation;
    private Boolean requestBol = false;
    private Marker customerMarker;
    private String tradeType;
    private LinearLayout tradesmanInfo;
    private ImageView tradesmanProfileImg;
    private TextView tradesmanName;
    private TextView tradesmanPhone;
    private TextView tradesmanType;
    private RadioGroup radioGroup;

    private String UserID;
    private FirebaseAuth Auth;
    private DatabaseReference customerDatabase;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        FusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        tradesmanInfo = (LinearLayout) findViewById(R.id.tradesman_information);

        tradesmanProfileImg = (ImageView) findViewById(R.id.tradesman_profileImage);
        tradesmanName = (TextView) findViewById(R.id.tradesman_name);
        tradesmanPhone = (TextView) findViewById(R.id.tradesman_phone);
        tradesmanType = (TextView) findViewById(R.id.tradesman_type);


        radioGroup = (RadioGroup) findViewById(R.id.radio_group);
        radioGroup.check(R.id.plumber); // Set default to Plumber

        requestTradesmanBtn = (Button) findViewById(R.id.request_tradesman);
        settingsBtn = (ImageButton) findViewById(R.id.settings_button);
        cancelTradesman = (Button) findViewById(R.id.cancel_tradesman);
        problemBtn = (Button) findViewById(R.id.problem_button);


        Auth = FirebaseAuth.getInstance();
        UserID = Auth.getCurrentUser().getUid();
        customerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(UserID);


        // Allow customer to cancel request - ends it.
        cancelTradesman.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (requestBol) {
                    endRequest();

                    Toast toast = Toast.makeText(getApplicationContext(), "Request Cancelled", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
                    toast.show();


                    cancelTradesman.setVisibility(View.GONE);

                }
            }
        });


        /* ---------------------------------
        Customer can request to find a tradesman, finds nearest one.
        Creates CustomerRequest in Firebase with Location lat/lng.
        Adds marker for customer location.
         ---------------------------------- */
        requestTradesmanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                int selectID = radioGroup.getCheckedRadioButtonId();

                final RadioButton radioButton = (RadioButton) findViewById(selectID);

                if (radioButton.getText() == null) {
                    return;
                }

                tradeType = radioButton.getText().toString();

                requestBol = true;

                String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();

                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("CustomerRequest");
                GeoFire geoFire = new GeoFire(ref);
                geoFire.setLocation(userID, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {

                    }
                });

                customerLocation = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                customerMarker = Map.addMarker(new MarkerOptions().position(customerLocation).title("Request Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));


                radioGroup.setVisibility(View.GONE);

                requestTradesmanBtn.setText("Getting your Tradesman...");

                cancelTradesman.setVisibility(View.VISIBLE);


                getClosestTradesman();
            }


        });

        // Opens Customer settings activity
        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CustomerMapActivity.this, CustomerSettingsActivity.class);

                startActivity(intent);
                return;
            }
        });


        // Allows user to edit problem - opens Problem activity.
        problemBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CustomerMapActivity.this, CustomerProblemActivity.class);
                startActivity(intent);
                return;
            }
        });

    }


    /* ---------------------------------
        Here we are searching for Tradesman Available within Firebase.
        If a Tradesman has a matching trade type to a Customer Request they match.
        The closest Tradesman to the Customer is matched first, based on latitude/longitude
        If no tradesman is found within 1km it increments.
        To avoid a Customer waiting forever, I added the first if statement to stop the increment,
        when it reaches 100km - this would have been avoided but due to a problem stated in the
        report I didn't know how to avoid it.
      ---------------------------------- */
    private int radius = 1; // 1 km
    private Boolean tradesmanFound = false;
    private String tradesmanFoundID;
    GeoQuery geoQuery;

    private void getClosestTradesman() {

        if (!tradesmanFound && radius >= 100) {

            requestBol = false;
            tradesmanFound = false;
            radius = 1;
            String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();

            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("CustomerRequest");
            GeoFire geoFire = new GeoFire(ref);
            geoFire.removeLocation(userID, new GeoFire.CompletionListener() {
                @Override
                public void onComplete(String key, DatabaseError error) {

                }
            });
            Intent intent = new Intent(CustomerMapActivity.this, CustomerMapActivity.class);
            startActivity(intent);
            finish();
            return;

        }

        final DatabaseReference tradesmanLocation = FirebaseDatabase.getInstance().getReference().child("TradesmanAvailable");

        final GeoFire geoFire = new GeoFire(tradesmanLocation);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(customerLocation.latitude, customerLocation.longitude), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!tradesmanFound && requestBol) {
                    DatabaseReference customerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Tradesman").child(key);
                    customerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                                Map<String, Object> tradesmanMap = (Map<String, Object>) dataSnapshot.getValue();
                                if (tradesmanFound) {
                                    return;
                                }

                                if (tradesmanMap.get("Trade").equals(tradeType)) {
                                    tradesmanFound = true;
                                    tradesmanFoundID = dataSnapshot.getKey();

                                    DatabaseReference tradesmanRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Tradesman").child(tradesmanFoundID).child("CustomerRequest");
                                    String customerID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                    HashMap map = new HashMap();
                                    map.put("CustomerRequestID", customerID);
                                    tradesmanRef.updateChildren(map);

                                    getTradesmanLocation();
                                    getTradesmanInfo();
                                    isRequestFinished();
                                    requestTradesmanBtn.setText("Looking for Tradesman's Location...");
                                }
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                        }
                    });
                }

            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }


            @Override
            public void onGeoQueryReady() {
                if (!tradesmanFound) {

                    geoQuery.removeAllListeners();

                    radius++;
                    getClosestTradesman();

                }
                // Here I tried to call endRequest() should the radius reach i.e. 50.
                // But it worked partly - removed the reqiest from Firebase, but nothing changed
                // for the Front-End i.e. buttons stayed the same.

            }


            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }


    /*--------------------------------------------
      Here we are finding the Tradesmans location, and checking for movements as its updated.
      We work out the distance between tradesman and customer.
      This allows us to notify the customer when the tradesmans near or has arrived.
      We also add a marker for the tradesmans location.
    ----------------------------------------------------*/
    private Marker tradesmanMarker;
    private DatabaseReference tradesmanLocationRef;
    private ValueEventListener tradesmanLocationRefListener;

    private void getTradesmanLocation() {
        tradesmanLocationRef = FirebaseDatabase.getInstance().getReference().child("TradesmanWorking").child(tradesmanFoundID).child("l");
        tradesmanLocationRefListener = tradesmanLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && requestBol) {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    if (map.get(0) != null) {
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1) != null) {
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng tradesmanLatLng = new LatLng(locationLat, locationLng);
                    if (tradesmanMarker != null) {
                        tradesmanMarker.remove();
                    }
                    Location loc1 = new Location("");
                    loc1.setLatitude(customerLocation.latitude);
                    loc1.setLongitude(customerLocation.longitude);

                    Location loc2 = new Location("");
                    loc2.setLatitude(tradesmanLatLng.latitude);
                    loc2.setLongitude(tradesmanLatLng.longitude);

                    float distance = loc1.distanceTo(loc2);

                    if (distance < 100) {
                        requestTradesmanBtn.setText("Tradesman's Arrived");

                    } else {
                        requestTradesmanBtn.setText("Tradesman Found: " + String.valueOf(distance)); // notifications / pop up

                    }

                    tradesmanMarker = Map.addMarker(new MarkerOptions().position(tradesmanLatLng).title("Your Tradesman").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    /*--------------------------------------------
     Here we are retrieving the Tradesman's stored information.
     This allows us to display it for the Customer upon matching.
   -------------------------------------------------*/
    private void getTradesmanInfo() {
        tradesmanInfo.setVisibility(View.VISIBLE);
        DatabaseReference customerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Tradesman").child(tradesmanFoundID);
        customerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    if (dataSnapshot.child("Name") != null) {
                        tradesmanName.setText(dataSnapshot.child("Name").getValue().toString());
                    }
                    if (dataSnapshot.child("Phone") != null) {
                        tradesmanPhone.setText(dataSnapshot.child("Phone").getValue().toString());
                    }
                    if (dataSnapshot.child("Trade") != null) {
                        tradesmanType.setText(dataSnapshot.child("Trade").getValue().toString());
                    }

                    if (dataSnapshot.child("profileImageUrl").getValue() != null) {
                        Glide.with(getApplication()).load(dataSnapshot.child("profileImageUrl").getValue().toString()).into(tradesmanProfileImg);
                    }

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }


    /* ---------------------------------
        Here I began to implement a way for a customer to rate there service.
        I would have liked to add more and tidy it up, which I began to by adding a history element,
        to the firebase.

        But, here we have a dialog which appears upon end of a request which uses a rating bar.
        The customer can add a rating or decline, it stores it in Firebase.
         ---------------------------------- */

    private void requestFeedback() {
        customerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(UserID);

        AlertDialog.Builder feedback = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.problem)); // test this
        View layout = null;
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layout = inflater.inflate(R.layout.rating, null);
        final RatingBar ratingBar = (RatingBar) layout.findViewById(R.id.ratingBar);
        feedback.setTitle("Rate your Tradesman"); // maybe change this
        feedback.setMessage("Thank you for using SwiftFix, we hope you use our services soon!");

        feedback.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Float value = ratingBar.getRating();
                Toast.makeText(CustomerMapActivity.this, "Rating is : " + value, Toast.LENGTH_LONG).show();
                Map userInfo = new HashMap();
                userInfo.put("Recent Rating", value);
                customerDatabase.updateChildren(userInfo);

            }
        });
        feedback.setNegativeButton("No,Thanks", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

            }
        });
        feedback.setCancelable(false);
        feedback.setView(layout);
        feedback.show();


    }


    /* ---------------------------------
        Here we are seeing if a request is over.
        We check to see if the requestID has been removed from Firebase.
        If it exists is hasn't ended, if it doesn't then end request and open feedback dialog.
         ---------------------------------- */

    private DatabaseReference requestFinishedRef;
    private ValueEventListener requestFinishedRefListener;

    private void isRequestFinished() {
        requestFinishedRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Tradesman").child(tradesmanFoundID).child("CustomerRequest").child("CustomerRequestID");
        requestFinishedRefListener = requestFinishedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {

                } else {
                    endRequest();
                    requestFeedback();
                    cancelTradesman.setVisibility(View.GONE);


                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }


    /* ---------------------------------
        Here we are ending a request. Here remove all listeners and the request from Firebase.
        We can reset the radius and remove all markers from the map.
        And reset the buttons to be able to make a new request.
      -------------------------------------- */
    private void endRequest() {
        requestBol = false;

        if (geoQuery != null) {
            geoQuery.removeAllListeners();
        }
        if (tradesmanLocationRefListener != null && requestFinishedRefListener != null) {
            tradesmanLocationRef.removeEventListener(tradesmanLocationRefListener);
            requestFinishedRef.removeEventListener(requestFinishedRefListener);
        }

        if (tradesmanFoundID != null) {
            DatabaseReference tradesmanRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Tradesman").child(tradesmanFoundID).child("CustomerRequest");
            tradesmanRef.removeValue();
            tradesmanFoundID = null;

        }
        tradesmanFound = false;
        radius = 1;
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("CustomerRequest");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userID, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {

            }
        });

        if (customerMarker != null) {
            customerMarker.remove();
        }
        if (tradesmanMarker != null) {
            tradesmanMarker.remove();
        }
        requestTradesmanBtn.setText("Call Tradesman");

        radioGroup.setVisibility(View.VISIBLE);

        tradesmanInfo.setVisibility(View.GONE);
        tradesmanName.setText("");
        tradesmanPhone.setText("");

        // Below makes app crash not sure why - didn't get time to debug it
        // Think it was due to the set image
        // tradesmanProfileImg.setImageResource(R.mipmap.icon_swift);
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
                //LatLng UK = new LatLng(54.6, -2.6);
                //Map.moveCamera(CameraUpdateFactory.newLatLngZoom(UK, 6));
                FusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                Map.setMyLocationEnabled(true);

            } else {
                checkLocationPermission();
            }
        } else {
            // Two lines below caused some problem, has to move them into the above if statement.
            FusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            Map.setMyLocationEnabled(true);
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
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                new android.app.AlertDialog.Builder(this)
                        .setTitle("give permission")
                        .setMessage("give permission message")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                            }
                        })
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
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
                        FusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                        Map.setMyLocationEnabled(true);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Please provide the permission", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }


}


