package com.example.localisation_sender_with_speech;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final String DEFAULT_PHONE_NUMBER = "+33780542575"; // Change this to your default number
    private static final int EMERGENCY_SEQUENCE_REQUIRED = 3; // Number of volume button presses needed
    private static final long EMERGENCY_TIMEOUT_MS = 3000; // 3 seconds timeout for the sequence

    private FusedLocationProviderClient fusedLocationClient;
    private TextView tvLocationStatus;
    private TextView tvLocationDetails;
    private Button btnGetLocation;
    private Button btnSendSMS;
    private EditText etPhoneNumber;
    
    // Emergency Volume Button Detection components
    private TextView tvEmergencyStatus;
    private TextView tvVolumeResult;
    private Button btnToggleEmergency;
    private boolean emergencyModeEnabled = false;
    
    // Volume button sequence tracking
    private int volumeSequenceCount = 0;
    private long lastVolumeButtonTime = 0;
    private Handler emergencyHandler = new Handler();
    private Runnable emergencyTimeoutRunnable;

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
        
        // Auto-get location on startup
        getCurrentLocation();
    }

    private void initializeViews() {
        tvLocationStatus = findViewById(R.id.tvLocationStatus);
        tvLocationDetails = findViewById(R.id.tvLocationDetails);
        btnGetLocation = findViewById(R.id.btnGetLocation);
        btnSendSMS = findViewById(R.id.btnSendSMS);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        
        // Emergency volume button views
        tvEmergencyStatus = findViewById(R.id.tvEmergencyStatus);
        tvVolumeResult = findViewById(R.id.tvVolumeResult);
        btnToggleEmergency = findViewById(R.id.btnToggleEmergency);
    }

    private void initializeLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void setupClickListeners() {
        btnGetLocation.setOnClickListener(v -> getCurrentLocation());
        btnSendSMS.setOnClickListener(v -> sendLocationSMS());
        btnToggleEmergency.setOnClickListener(v -> toggleEmergencyMode());
    }

    private void toggleEmergencyMode() {
        emergencyModeEnabled = !emergencyModeEnabled;
        
        if (emergencyModeEnabled) {
            btnToggleEmergency.setText("Disable Emergency Mode");
            btnToggleEmergency.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_red_dark));
            tvEmergencyStatus.setText("Emergency Detection: ON");
            tvVolumeResult.setText("Emergency mode activated! Press Volume Down + Volume Up 3 times quickly for emergency.");
            tvVolumeResult.setTextColor(getColor(android.R.color.holo_green_dark));
            
            Toast.makeText(this, "Emergency mode enabled! Use volume buttons to send emergency location.", Toast.LENGTH_LONG).show();
        } else {
            btnToggleEmergency.setText("Enable Emergency Mode");
            btnToggleEmergency.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_green_dark));
            tvEmergencyStatus.setText("Emergency Detection: OFF");
            tvVolumeResult.setText("");
            
            // Reset sequence counter
            resetEmergencySequence();
            
            Toast.makeText(this, "Emergency mode disabled.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (emergencyModeEnabled && (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)) {
            handleVolumeButtonPress(keyCode);
            return true; // Consume the event to prevent volume change
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (emergencyModeEnabled && (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)) {
            return true; // Consume the event to prevent volume change
        }
        return super.onKeyUp(keyCode, event);
    }

    private void handleVolumeButtonPress(int keyCode) {
        long currentTime = System.currentTimeMillis();
        
        // Check if this press is within the timeout window
        if (currentTime - lastVolumeButtonTime > EMERGENCY_TIMEOUT_MS) {
            // Reset sequence if too much time has passed
            resetEmergencySequence();
        }
        
        lastVolumeButtonTime = currentTime;
        volumeSequenceCount++;
        
        String buttonName = (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) ? "Volume Down" : "Volume Up";
        tvVolumeResult.setText(String.format(Locale.US, 
            "🚨 %s pressed! (%d/%d)", buttonName, volumeSequenceCount, EMERGENCY_SEQUENCE_REQUIRED));
        tvVolumeResult.setTextColor(getColor(android.R.color.holo_orange_dark));
        
        // Cancel previous timeout and set a new one
        if (emergencyTimeoutRunnable != null) {
            emergencyHandler.removeCallbacks(emergencyTimeoutRunnable);
        }
        
        emergencyTimeoutRunnable = () -> {
            if (volumeSequenceCount < EMERGENCY_SEQUENCE_REQUIRED) {
                tvVolumeResult.setText("Emergency sequence timed out. Try again.");
                tvVolumeResult.setTextColor(getColor(android.R.color.darker_gray));
                resetEmergencySequence();
            }
        };
        emergencyHandler.postDelayed(emergencyTimeoutRunnable, EMERGENCY_TIMEOUT_MS);
        
        // Check if emergency sequence is complete
        if (volumeSequenceCount >= EMERGENCY_SEQUENCE_REQUIRED) {
            triggerEmergency();
        }
    }

    private void resetEmergencySequence() {
        volumeSequenceCount = 0;
        lastVolumeButtonTime = 0;
        if (emergencyTimeoutRunnable != null) {
            emergencyHandler.removeCallbacks(emergencyTimeoutRunnable);
            emergencyTimeoutRunnable = null;
        }
    }

    private void triggerEmergency() {
        resetEmergencySequence();
        
        tvVolumeResult.setText("🚨 EMERGENCY TRIGGERED! Sending location...");
        tvVolumeResult.setTextColor(getColor(android.R.color.holo_red_dark));
        
        Toast.makeText(this, "🚨 EMERGENCY DETECTED! Sending location automatically...", Toast.LENGTH_LONG).show();
        
        // Vibrate if available
        android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(1000); // Vibrate for 1 second
        }
        
        // Send emergency location SMS
        sendEmergencyLocationSMS();
    }

    private void sendEmergencyLocationSMS() {
        if (!locationAvailable) {
            Toast.makeText(this, "Getting location for emergency...", Toast.LENGTH_SHORT).show();
            getLocationForEmergency();
        } else {
            String phoneNumber = etPhoneNumber.getText().toString().trim();
            if (phoneNumber.isEmpty()) {
                phoneNumber = DEFAULT_PHONE_NUMBER;
            }
            
            if (checkSMSPermission()) {
                sendEmergencySMS(phoneNumber);
            } else {
                requestSMSPermission();
            }
        }
    }

    private void getLocationForEmergency() {
        if (!checkLocationPermissions()) {
            Toast.makeText(this, "Location permission needed for emergency!", Toast.LENGTH_LONG).show();
            return;
        }
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        updateLocationUI(location);
                        sendEmergencyLocationSMS();
                    } else {
                        requestFreshLocationForEmergency();
                    }
                });
    }

    private void requestFreshLocationForEmergency() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000)
                .setFastestInterval(2000)
                .setNumUpdates(1);

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null && !locationResult.getLocations().isEmpty()) {
                    Location location = locationResult.getLastLocation();
                    updateLocationUI(location);
                    sendEmergencyLocationSMS();
                } else {
                    Toast.makeText(MainActivity.this, "Unable to get location for emergency", Toast.LENGTH_LONG).show();
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private void sendEmergencySMS(String phoneNumber) {
        try {
            SmsManager smsManager = SmsManager.getDefault();

            String message = String.format(Locale.US,
                    "🚨 EMERGENCY ALERT 🚨\nI need help! My location:\nLatitude: %.6f\nLongitude: %.6f\n\nGoogle Maps: https://maps.google.com/?q=%.6f,%.6f\n\nSent automatically by volume button emergency detection.",
                    currentLatitude, currentLongitude, currentLatitude, currentLongitude);

            smsManager.sendTextMessage(phoneNumber, null, message, null, null);

            Toast.makeText(this, "🚨 EMERGENCY SMS SENT! 🚨", Toast.LENGTH_LONG).show();
            tvVolumeResult.setText("✅ Emergency SMS sent to: " + phoneNumber);
            tvVolumeResult.setTextColor(getColor(android.R.color.holo_green_dark));

        } catch (Exception e) {
            Toast.makeText(this, "Failed to send emergency SMS: " + e.getMessage(), Toast.LENGTH_LONG).show();
            tvVolumeResult.setText("❌ Failed to send emergency SMS");
            tvVolumeResult.setTextColor(getColor(android.R.color.holo_red_dark));
        }
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

        tvLocationStatus.setText("Getting location...");

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
                            "Failed to get location: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    tvLocationStatus.setText("Location: Error occurred");
                    requestFreshLocation();
                });
    }

    private void updateLocationUI(Location location) {
        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();
        locationAvailable = true;

        tvLocationStatus.setText("Location: Available");
        tvLocationDetails.setText(String.format(Locale.US,
                "Lat: %.6f\nLng: %.6f\nAccuracy: %.1fm",
                currentLatitude, currentLongitude, location.getAccuracy()));

        btnSendSMS.setEnabled(true);

        Toast.makeText(MainActivity.this,
                "Location obtained successfully!",
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
                    tvLocationStatus.setText("Location: Not available");
                    tvLocationDetails.setText("Unable to get current location.\nPlease check GPS settings.");
                    Toast.makeText(MainActivity.this,
                            "Unable to get location. Make sure GPS is enabled.",
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

            Toast.makeText(this, "Location SMS sent successfully!", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(this, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (emergencyTimeoutRunnable != null) {
            emergencyHandler.removeCallbacks(emergencyTimeoutRunnable);
        }
    }
}