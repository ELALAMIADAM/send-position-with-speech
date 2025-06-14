package com.example.localisation_sender_with_speech;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.widget.Toast;
import android.util.Log;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.Locale;
import java.util.regex.Pattern;

public class SmsAutoResponseReceiver extends BroadcastReceiver {
    
    private static final String TAG = "SmsAutoResponse";
    
    // Trigger phrases that will activate auto-response (case insensitive)
    private static final String[] TRIGGER_PHRASES = {
        "give me your location",
        "send location",
        "where are you",
        "share location",
        "your location",
        "location please",
        "send me location"
    };
    
    private static final String PREFS_NAME = "SmsAutoResponsePrefs";
    private static final String KEY_AUTO_RESPONSE_ENABLED = "auto_response_enabled";
    private static final String KEY_ALLOWED_CONTACTS = "allowed_contacts";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "SMS received, checking auto-response...");
        
        // Ensure we don't consume the broadcast - let other apps handle it too
        setResultCode(Activity.RESULT_OK);
        
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            Log.d(TAG, "Not an SMS_RECEIVED_ACTION, ignoring");
            return;
        }
        
        // Check if auto-response feature is enabled
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean autoResponseEnabled = prefs.getBoolean(KEY_AUTO_RESPONSE_ENABLED, false);
        
        Log.d(TAG, "Auto-response enabled: " + autoResponseEnabled);
        
        if (!autoResponseEnabled) {
            Log.d(TAG, "Auto-response is disabled, returning");
            return;
        }
        
        // Check SMS permissions
        if (!hasSmsPermissions(context)) {
            Log.e(TAG, "SMS permissions not granted");
            return;
        }
        
        // Check location permissions
        if (!hasLocationPermissions(context)) {
            Log.e(TAG, "Location permissions not granted");
            return;
        }
        
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Object[] pdus = (Object[]) bundle.get("pdus");
            String format = bundle.getString("format");
            
            if (pdus != null) {
                Log.d(TAG, "Processing " + pdus.length + " SMS messages");
                
                for (Object pdu : pdus) {
                    SmsMessage smsMessage;
                    try {
                        if (format != null) {
                            smsMessage = SmsMessage.createFromPdu((byte[]) pdu, format);
                        } else {
                            smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                        }
                        
                        if (smsMessage != null) {
                            String sender = smsMessage.getOriginatingAddress();
                            String messageBody = smsMessage.getMessageBody();
                            
                            Log.d(TAG, "SMS from: " + sender + ", message: " + messageBody);
                            
                            // Check if message contains trigger phrase
                            if (containsTriggerPhrase(messageBody)) {
                                Log.d(TAG, "Trigger phrase detected! Processing location request...");
                                
                                // Check if sender is allowed (optional - can be configured later)
                                if (isSenderAllowed(context, sender)) {
                                    handleLocationRequest(context, sender, messageBody);
                                } else {
                                    Log.d(TAG, "Sender not allowed: " + sender);
                                }
                            } else {
                                Log.d(TAG, "No trigger phrase found in message");
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing SMS: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            } else {
                Log.e(TAG, "No PDUs found in SMS intent");
            }
        } else {
            Log.e(TAG, "No extras found in SMS intent");
        }
    }
    
    private boolean containsTriggerPhrase(String message) {
        if (message == null) return false;
        
        String lowerMessage = message.toLowerCase().trim();
        Log.d(TAG, "Checking message for trigger phrases: " + lowerMessage);
        
        for (String trigger : TRIGGER_PHRASES) {
            if (lowerMessage.contains(trigger.toLowerCase())) {
                Log.d(TAG, "Found trigger phrase: " + trigger);
                return true;
            }
        }
        return false;
    }
    
    private boolean isSenderAllowed(Context context, String sender) {
        // For now, allow all senders. This can be enhanced later to check whitelist
        // You could implement a whitelist feature here
        Log.d(TAG, "Sender allowed: " + sender);
        return true;
    }
    
    private void handleLocationRequest(Context context, String sender, String originalMessage) {
        Log.d(TAG, "Handling location request for sender: " + sender);
        
        // Get current location and send response
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        
        try {
            fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    Log.d(TAG, "Location retrieved: " + (location != null ? "success" : "null"));
                    if (location != null) {
                        sendLocationResponse(context, sender, location);
                    } else {
                        Log.d(TAG, "No last known location, requesting fresh location...");
                        // Try to get fresh location
                        sendErrorResponse(context, sender, "Unable to get current location. Please make sure GPS is enabled.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get location: " + e.getMessage());
                    sendErrorResponse(context, sender, "Failed to get location: " + e.getMessage());
                });
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when accessing location: " + e.getMessage());
            sendErrorResponse(context, sender, "Location access denied");
        }
    }
    
    private void sendLocationResponse(Context context, String sender, Location location) {
        Log.d(TAG, "Sending location response to: " + sender);
        
        try {
            SmsManager smsManager = SmsManager.getDefault();
            
            String response = String.format(Locale.US,
                    "📍 My current location:\n" +
                    "Latitude: %.6f\n" +
                    "Longitude: %.6f\n" +
                    "Accuracy: %.1fm\n\n" +
                    "🗺️ Google Maps: https://maps.google.com/?q=%.6f,%.6f\n\n" +
                    "⏰ Sent automatically in response to your request",
                    location.getLatitude(), location.getLongitude(), location.getAccuracy(),
                    location.getLatitude(), location.getLongitude());
            
            // For long messages, use multipart SMS
            if (response.length() > 160) {
                smsManager.sendMultipartTextMessage(sender, null, 
                    smsManager.divideMessage(response), null, null);
            } else {
                smsManager.sendTextMessage(sender, null, response, null, null);
            }
            
            Log.d(TAG, "Location SMS sent successfully to: " + sender);
            
            // Show notification that auto-response was sent
            showNotification(context, "Auto-response sent to " + sender);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to send location SMS: " + e.getMessage());
            sendErrorResponse(context, sender, "Failed to send location");
        }
    }
    
    private void sendErrorResponse(Context context, String sender, String error) {
        Log.d(TAG, "Sending error response to: " + sender + ", error: " + error);
        
        try {
            SmsManager smsManager = SmsManager.getDefault();
            String response = "❌ Unable to share location: " + error;
            smsManager.sendTextMessage(sender, null, response, null, null);
            Log.d(TAG, "Error SMS sent successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to send error SMS: " + e.getMessage());
        }
    }
    
    private void showNotification(Context context, String message) {
        // You could implement a proper notification here
        // For now, we'll use a simple approach
        Log.d(TAG, "Showing notification: " + message);
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
    
    private boolean hasSmsPermissions(Context context) {
        boolean receivePermission = ActivityCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) 
                == PackageManager.PERMISSION_GRANTED;
        boolean sendPermission = ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) 
                == PackageManager.PERMISSION_GRANTED;
        boolean readPermission = ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) 
                == PackageManager.PERMISSION_GRANTED;
        
        Log.d(TAG, "SMS Permissions - Receive: " + receivePermission + ", Send: " + sendPermission + ", Read: " + readPermission);
        
        return receivePermission && sendPermission && readPermission;
    }
    
    private boolean hasLocationPermissions(Context context) {
        boolean fineLocation = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED;
        boolean coarseLocation = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED;
        
        Log.d(TAG, "Location Permissions - Fine: " + fineLocation + ", Coarse: " + coarseLocation);
        
        return fineLocation || coarseLocation;
    }
    
    // Static methods to control the feature from MainActivity
    public static void setAutoResponseEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_AUTO_RESPONSE_ENABLED, enabled).apply();
        Log.d("SmsAutoResponse", "Auto-response enabled set to: " + enabled);
    }
    
    public static boolean isAutoResponseEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(KEY_AUTO_RESPONSE_ENABLED, false);
        Log.d("SmsAutoResponse", "Auto-response enabled status: " + enabled);
        return enabled;
    }
} 