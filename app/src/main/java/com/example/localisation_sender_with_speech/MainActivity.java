package com.example.localisation_sender_with_speech;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.text.TextUtils;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int SMS_PERMISSION_REQUEST_CODE = 1003;
    private static final String DEFAULT_PHONE_NUMBER = "+33780542575"; // Change this to your default number

    private FusedLocationProviderClient fusedLocationClient;
    private TextView tvLocationStatus;
    private TextView tvLocationDetails;
    private Button btnGetLocation;
    private Button btnSendSMS;
    private EditText etPhoneNumber;
    
    // SMS Auto-Response components
    private TextView tvAutoResponseStatus;
    private TextView tvAutoResponseInfo;
    private Button btnToggleAutoResponse;
    private boolean autoResponseEnabled = false;

    private double currentLatitude = 0;
    private double currentLongitude = 0;
    private boolean locationAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        initializeLocationClient();
        setupClickListeners();

        // Set default phone number
        etPhoneNumber.setText(DEFAULT_PHONE_NUMBER);
        
        // Initialize auto-response status
        initializeAutoResponseStatus();
        
        // Auto-get location on startup
        getCurrentLocation();
    }

    private void initializeViews() {
        tvLocationStatus = findViewById(R.id.tvLocationStatus);
        tvLocationDetails = findViewById(R.id.tvLocationDetails);
        btnGetLocation = findViewById(R.id.btnGetLocation);
        btnSendSMS = findViewById(R.id.btnSendSMS);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        
        // SMS Auto-Response views
        tvAutoResponseStatus = findViewById(R.id.tvAutoResponseStatus);
        tvAutoResponseInfo = findViewById(R.id.tvAutoResponseInfo);
        btnToggleAutoResponse = findViewById(R.id.btnToggleAutoResponse);
    }

    private void initializeLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void setupClickListeners() {
        btnGetLocation.setOnClickListener(v -> getCurrentLocation());
        btnSendSMS.setOnClickListener(v -> sendLocationSMS());
        btnToggleAutoResponse.setOnClickListener(v -> toggleAutoResponseMode());
    }

    private void initializeAutoResponseStatus() {
        autoResponseEnabled = SmsAutoResponseReceiver.isAutoResponseEnabled(this);
        
        // If auto-response was enabled before, restart the service
        if (autoResponseEnabled) {
            Intent serviceIntent = new Intent(this, SmsAutoResponseService.class);
            serviceIntent.setAction("START_AUTO_RESPONSE");
            startForegroundService(serviceIntent);
        }
        
        updateAutoResponseUI();
    }
    
    private void toggleAutoResponseMode() {
        if (!autoResponseEnabled) {
            // Check SMS permissions before enabling
            if (checkSmsReceivePermissions()) {
                enableAutoResponse();
            } else {
                requestSmsReceivePermissions();
            }
        } else {
            disableAutoResponse();
        }
    }
    
    private void enableAutoResponse() {
        autoResponseEnabled = true;
        SmsAutoResponseReceiver.setAutoResponseEnabled(this, true);
        
        // Start a foreground service to help keep the receiver active
        Intent serviceIntent = new Intent(this, SmsAutoResponseService.class);
        serviceIntent.setAction("START_AUTO_RESPONSE");
        startForegroundService(serviceIntent);
        
        updateAutoResponseUI();
        Toast.makeText(this, "SMS Auto-Response enabled! I will automatically respond with location when someone asks.", Toast.LENGTH_LONG).show();
    }
    
    private void disableAutoResponse() {
        autoResponseEnabled = false;
        SmsAutoResponseReceiver.setAutoResponseEnabled(this, false);
        
        // Stop the background service
        Intent serviceIntent = new Intent(this, SmsAutoResponseService.class);
        stopService(serviceIntent);
        
        updateAutoResponseUI();
        Toast.makeText(this, "SMS Auto-Response disabled.", Toast.LENGTH_SHORT).show();
    }
    
    private void updateAutoResponseUI() {
        if (autoResponseEnabled) {
            btnToggleAutoResponse.setText("💬 Disable SMS Auto-Response");
            btnToggleAutoResponse.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.danger_red));
            tvAutoResponseStatus.setText("SMS Auto-Response: ON");
            tvAutoResponseStatus.setTextColor(getColor(R.color.success_green));
            tvAutoResponseInfo.setText("✅ Will auto-reply with location when receiving:\n• \"give me your location\"\n• \"send location\"\n• \"where are you\"\n• \"share location\"\n• \"location please\"");
            tvAutoResponseInfo.setTextColor(getColor(R.color.success_green));
        } else {
            btnToggleAutoResponse.setText("💬 Enable SMS Auto-Response");
            btnToggleAutoResponse.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.success_green));
            tvAutoResponseStatus.setText("SMS Auto-Response: OFF");
            tvAutoResponseStatus.setTextColor(getColor(R.color.text_secondary));
            tvAutoResponseInfo.setText("");
        }
    }
    
    private boolean checkSmsReceivePermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) 
                == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) 
                == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) 
                == PackageManager.PERMISSION_GRANTED;
    }
    
    private void requestSmsReceivePermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.READ_SMS,
                        Manifest.permission.SEND_SMS
                },
                SMS_PERMISSION_REQUEST_CODE);
    }

    private void getCurrentLocation() {
        if (checkLocationPermissions()) {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGpsEnabled && !isNetworkEnabled) {
                Toast.makeText(this, "Please enable GPS in your device settings", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
                return;
            }

            getLocation();
        } else {
            requestLocationPermissions();
        }
    }

    private boolean checkLocationPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                PERMISSION_REQUEST_CODE);
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        tvLocationStatus.setText("🔄 Getting location...");
        tvLocationStatus.setTextColor(getColor(R.color.warning_orange));

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            long locationAge = System.currentTimeMillis() - location.getTime();
                            if (locationAge < 2 * 60 * 1000) {
                                updateLocationUI(location);
                            } else {
                                requestFreshLocation();
                            }
                        } else {
                            requestFreshLocation();
                        }
                    }
                })
                .addOnFailureListener(this, e -> {
                    Toast.makeText(MainActivity.this,
                            "❌ Failed to get location: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    tvLocationStatus.setText("❌ Location: Error occurred");
                    tvLocationStatus.setTextColor(getColor(R.color.danger_red));
                    requestFreshLocation();
                });
    }

    private void updateLocationUI(Location location) {
        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();
        locationAvailable = true;

        tvLocationStatus.setText("✅ Location: Available");
        tvLocationStatus.setTextColor(getColor(R.color.success_green));
        tvLocationDetails.setText(String.format(Locale.US,
                "📍 Lat: %.6f\n📍 Lng: %.6f\n🎯 Accuracy: %.1fm",
                currentLatitude, currentLongitude, location.getAccuracy()));
        tvLocationDetails.setTextColor(getColor(R.color.text_primary));

        btnSendSMS.setEnabled(true);

        Toast.makeText(MainActivity.this,
                "🎉 Location obtained successfully!",
                Toast.LENGTH_SHORT).show();
    }

    private void requestFreshLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000)
                .setFastestInterval(5000)
                .setNumUpdates(1);

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null && !locationResult.getLocations().isEmpty()) {
                    Location location = locationResult.getLastLocation();
                    updateLocationUI(location);
                } else {
                    tvLocationStatus.setText("⚠️ Location: Not available");
                    tvLocationStatus.setTextColor(getColor(R.color.warning_orange));
                    tvLocationDetails.setText("Unable to get current location.\nPlease check GPS settings.");
                    tvLocationDetails.setTextColor(getColor(R.color.text_secondary));
                    Toast.makeText(MainActivity.this,
                            "⚠️ Unable to get location. Make sure GPS is enabled.",
                            Toast.LENGTH_LONG).show();
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private void sendLocationSMS() {
        if (!locationAvailable) {
            Toast.makeText(this, "Please get location first", Toast.LENGTH_SHORT).show();
            return;
        }

        String phoneNumber = etPhoneNumber.getText().toString().trim();
        if (phoneNumber.isEmpty()) {
            Toast.makeText(this, "Please enter a phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        if (checkSMSPermission()) {
            sendSMS(phoneNumber);
        } else {
            requestSMSPermission();
        }
    }

    private boolean checkSMSPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestSMSPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.SEND_SMS},
                PERMISSION_REQUEST_CODE + 1);
    }

    private void sendSMS(String phoneNumber) {
        try {
            SmsManager smsManager = SmsManager.getDefault();

            String message = String.format(Locale.US,
                    "My current location:\nLatitude: %.6f\nLongitude: %.6f\n\nGoogle Maps: https://maps.google.com/?q=%.6f,%.6f",
                    currentLatitude, currentLongitude, currentLatitude, currentLongitude);

            smsManager.sendTextMessage(phoneNumber, null, message, null, null);

            Toast.makeText(this, "✅ Location SMS sent successfully!", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(this, "❌ Failed to send SMS: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == PERMISSION_REQUEST_CODE + 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                String phoneNumber = etPhoneNumber.getText().toString().trim();
                if (!phoneNumber.isEmpty()) {
                    sendSMS(phoneNumber);
                }
            } else {
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                enableAutoResponse();
                Toast.makeText(this, "SMS permissions granted! Auto-response enabled.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "SMS permissions denied - Auto-response cannot be enabled", Toast.LENGTH_LONG).show();
                autoResponseEnabled = false;
                updateAutoResponseUI();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}