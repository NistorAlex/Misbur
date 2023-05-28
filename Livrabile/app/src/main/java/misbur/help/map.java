package misbur.help;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.annotation.NonNull;


import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Context;


import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;



public class map extends AppCompatActivity implements OnMapReadyCallback {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FirebaseAuth mAuth;
    private Marker selectedMarker;

    MapView mapView;
    GoogleMap googleMap;
    EditText problemTypeEditText;
    EditText contactDetailsEditText;
    EditText markerStatusEditText;
    DatabaseReference markersRef;
    FloatingActionButton fabMenu;

    private void generateMarker(double latitude, double longitude, String problemType, String contactDetails, String markerStatus) {
        // Create a LatLng object from the provided latitude and longitude
        LatLng location = new LatLng(latitude, longitude);

        // Create a MarkerOptions object and set its position and title
        MarkerOptions markerOptions = new MarkerOptions()
                .position(location)
                .title("Marker");

        Marker marker = googleMap.addMarker(markerOptions);

        saveMarkerInformation(latitude, longitude, problemType, contactDetails, markerStatus);
        googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                marker.setTag(true);
            }
        });

        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (selectedMarker != null) {
                    // Reset the color of previously selected marker
                    selectedMarker.setIcon(BitmapDescriptorFactory.defaultMarker());
                }
                marker.showInfoWindow();
                marker.setTag("help"); // Set the tag to "help" if the marker is selected

                // Change the color of the selected marker to yellow
                marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));

                return false;
            }
        });

        //marker info from database
        String encodedLatitude = String.valueOf(latitude).replace(".", "_");
        String encodedLongitude = String.valueOf(longitude).replace(".", "_");
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference()
                .child("markers")
                .child(encodedLatitude)
                .child(encodedLongitude);
        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    MarkerInformation markerInfo = snapshot.getValue(MarkerInformation.class);
                    if (markerInfo != null) {
                        String problemType = markerInfo.getProblemType();
                        String contactDetails = markerInfo.getContactDetails();
                        String markerStatus = markerInfo.getMarkerStatus();

                        String infoWindowContent = "Problem Type: " + problemType + "\n"
                                + "Contact Details: " + contactDetails + "\n"
                                + "Marker Status: " + markerStatus;
                        marker.setSnippet(infoWindowContent);

                        marker.setTag("help"); //
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        // Save the marker information to the database
        saveMarkerInformation(latitude, longitude, problemType, contactDetails, markerStatus);
    }

    private void saveMarkerInformation(double latitude, double longitude, String problemType, String contactDetails, String markerStatus) {
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();
        MarkerInformation marker = new MarkerInformation();
        marker.setLatitude(latitude);
        marker.setLongitude(longitude);
        marker.setProblemType(problemType);
        marker.setContactDetails(contactDetails);
        marker.setMarkerStatus(markerStatus);
        String encodedLatitude = String.valueOf(latitude).replace(".", "_");
        String encodedLongitude = String.valueOf(longitude).replace(".", "_");
        databaseRef.child("markers").child(encodedLatitude).child(encodedLongitude).setValue(marker);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        markersRef = FirebaseDatabase.getInstance().getReference("markers");
        Button generateMarkerButton = findViewById(R.id.generateMarkerButton);
        mAuth = FirebaseAuth.getInstance();

        View markerMenuView = getLayoutInflater().inflate(R.layout.marker_menu, null);
        problemTypeEditText = markerMenuView.findViewById(R.id.problemTypeEditText);
        contactDetailsEditText = markerMenuView.findViewById(R.id.contactDetailsEditText);
        markerStatusEditText = markerMenuView.findViewById(R.id.markerStatusEditText);

        fabMenu = findViewById(R.id.fabMenu);
        fabMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMenu();
            }
        });

        generateMarkerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(LatLng latLng) {
                        showMarkerMenu(latLng.latitude, latLng.longitude);
                        googleMap.setOnMapClickListener(null);
                    }
                });
            }
        });
    }

    private void showMenu() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Menu Options");
        String[] menuOptions = {"My Profile", "Helping", "Helped", "Option 4", "Option 5"};
        builder.setItems(menuOptions, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int index) {
                switch (index) {
                    case 0:
                        openMyProfile();
                        break;
                    case 1:
                        openHelping();
                        break;
                    case 2:
                        openHelped();
                        break;
                    case 3:
                        openOption4();
                        break;
                    case 4:
                        openOption5();
                        break;
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void openOption5() {
    }

    private void openOption4() {
    }

    private void openHelped() {
    }

    private void openHelping() {
    }

    private void openMyProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String email = currentUser.getEmail();
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("My Profile");
            builder.setMessage("Email: " + email);
            builder.setPositiveButton("OK", null);
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {

        }
    }


    private void showMarkerMenu(final double latitude, final double longitude) {
        // Create an AlertDialog with custom layout
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.marker_menu, null);
        builder.setView(dialogView);

        final EditText problemTypeEditText = dialogView.findViewById(R.id.problemTypeEditText);
        final EditText contactDetailsEditText = dialogView.findViewById(R.id.contactDetailsEditText);
        final EditText markerStatusEditText = dialogView.findViewById(R.id.markerStatusEditText);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String problemType = problemTypeEditText.getText().toString();
                String contactDetails = contactDetailsEditText.getText().toString();
                String markerStatus = markerStatusEditText.getText().toString();

                generateMarker(latitude, longitude, problemType, contactDetails, markerStatus);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }



    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        InfoWindow infoWindow = new InfoWindow(this);
        googleMap.setInfoWindowAdapter(infoWindow);

        googleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View infoWindowView = inflater.inflate(R.layout.markerinfopopup, null);

                TextView titleTextView = infoWindowView.findViewById(R.id.infoPopUp);
                TextView snippetTextView = infoWindowView.findViewById(R.id.infoSnippet);
                Button helpButton = infoWindowView.findViewById(R.id.helpButton);

                titleTextView.setText(marker.getTitle());
                snippetTextView.setText(marker.getSnippet());

                if (marker.getTag() != null && marker.getTag().equals("help")) {
                    helpButton.setVisibility(View.VISIBLE);
                } else {
                    helpButton.setVisibility(View.GONE);
                }

                helpButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showHelpDialog(marker);
                    }
                });

                return infoWindowView;
            }
        });

        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference().child("markers");
        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                googleMap.clear();
                for (DataSnapshot latitudeSnapshot : dataSnapshot.getChildren()) {
                    for (DataSnapshot longitudeSnapshot : latitudeSnapshot.getChildren()) {
                        String encodedLatitude = latitudeSnapshot.getKey();
                        String encodedLongitude = longitudeSnapshot.getKey();
                        MarkerInformation marker = longitudeSnapshot.getValue(MarkerInformation.class);
                        if (marker != null) {
                            double latitude = Double.parseDouble(encodedLatitude.replace("_", "."));
                            double longitude = Double.parseDouble(encodedLongitude.replace("_", "."));
                            generateMarker(latitude, longitude, marker.getProblemType(), marker.getContactDetails(), marker.getMarkerStatus());
                        }
                    }
                }
            }

            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void showHelpDialog(Marker marker) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    googleMap.setMyLocationEnabled(true);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}