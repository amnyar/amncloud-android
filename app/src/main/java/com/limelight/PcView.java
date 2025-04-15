package com.limelight;

// ===== Essential Imports =====
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
// import android.os.Build; // Not using SDK 33+ features for now
import android.os.Bundle;
// import android.preference.PreferenceManager; // Keep if R.xml.preferences used
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout; // Keep if pcFragmentContainer is used
import android.widget.TextView;
import android.widget.Toast;

// ===== Security Imports =====
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

// ===== JSON Import =====
import org.json.JSONObject;

// ===== Networking Imports =====
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.util.Scanner;
import java.util.UUID;

// ===== Limelight Specific Imports (Required by new logic) =====
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.PairingManager; // For PairState enum
import com.limelight.preferences.StreamSettings; // For Settings Button
import com.limelight.utils.Dialog; // For Dialogs used in onStop maybe
import com.limelight.utils.HelpLauncher; // For Help Button
import com.limelight.utils.UiHelper; // For Locale and RootView notification
import com.limelight.AppView; // For navigating to game list


public class PcView extends Activity { // NOTE: No AdapterFragmentCallbacks

    // Views for loading status
    private ProgressBar loadingIndicator;
    private TextView statusTextView;

    private boolean isServerInfoFetched = false;

    // Constants for secure storage
    private static final String SECURE_PREFS_NAME = "amnyar_secure_prefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_AUTH_TOKEN = "auth_token";
    // Keep only necessary keys, remove others if not used in this Activity
    private static final String KEY_PHONE_NUMBER = "phone_number";
    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String KEY_EMAIL = "email";
    private static final int DEFAULT_SERVER_PORT = 47989; // Default port


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Re-initialize views after config change
        initializeViews();
    }

    private SharedPreferences getSecurePrefs() {
        try {
            MasterKey masterKey = new MasterKey.Builder(getApplicationContext())
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            return EncryptedSharedPreferences.create(
                    getApplicationContext(),
                    SECURE_PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            Log.e("PcView", "Failed to get EncryptedSharedPreferences", e);
            Toast.makeText(this, "خطای داخلی ذخیره‌سازی امن", Toast.LENGTH_LONG).show();
            logoutUser(); // Log out on critical error
            return null;
        }
    }

    private void initializeViews() {
         // Ensure layout is set only once
         View rootView = findViewById(android.R.id.content);
         if (rootView == null || rootView.getTag() == null || !(rootView.getTag() instanceof Boolean) || !(Boolean)rootView.getTag()) {
             setContentView(R.layout.activity_pc_view);
             findViewById(android.R.id.content).setTag(true); // Mark as initialized
             // Ensure UiHelper is initialized if needed here, maybe move call to onCreate after setContentView?
             // UiHelper.notifyNewRootView(this);
         }


        /* // Commented out for compatibility
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setShouldDockBigOverlays(false);
        }
        */

        // Keep this if R.xml.preferences exists and is needed elsewhere indirectly
        // PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Find necessary views defined in the XML
        loadingIndicator = findViewById(R.id.loadingIndicator); // Ensure this ID exists
        statusTextView = findViewById(R.id.statusTextView); // Ensure this ID exists

        // Hide the old layout explicitly if still present in XML
        View noPcFoundLayoutOriginal = findViewById(R.id.no_pc_found_layout);
        if (noPcFoundLayoutOriginal != null) {
             noPcFoundLayoutOriginal.setVisibility(View.GONE);
        }

        // Set initial UI state based on whether API call has started/finished
        if (!isServerInfoFetched) {
             if (loadingIndicator != null) loadingIndicator.setVisibility(View.VISIBLE);
             if (statusTextView != null) {
                statusTextView.setText("درحال دریافت اطلاعات سرور...");
                statusTextView.setVisibility(View.VISIBLE);
             }
        } else {
             if (loadingIndicator != null) loadingIndicator.setVisibility(View.GONE);
             // Status text visibility handled by handleApiResponse
        }

        // Keep top buttons
        ImageButton settingsButton = findViewById(R.id.settingsButton);
        ImageButton addComputerButton = findViewById(R.id.manuallyAddPc); // Rename ID later
        ImageButton helpButton = findViewById(R.id.helpButton);

         // Add null checks before setting listeners
         if (settingsButton != null) {
             settingsButton.setOnClickListener(v ->
                 startActivity(new Intent(PcView.this, StreamSettings.class))
             );
         } else { Log.e("PcView", "settingsButton not found!"); }

         if (addComputerButton != null) {
             addComputerButton.setOnClickListener(v -> {
                 Toast.makeText(PcView.this, "دکمه پیشخوان - به زودی!", Toast.LENGTH_SHORT).show();
                 // TODO: Launch Dashboard
             });
         } else { Log.e("PcView", "manuallyAddPc button not found!"); }

         if (helpButton != null) {
             helpButton.setOnClickListener(v ->
                  HelpLauncher.launchSetupGuide(PcView.this) // Ensure HelpLauncher doesn't cause issues
             );
             try {
                  if (getPackageManager().hasSystemFeature("amazon.hardware.fire_tv")) {
                      helpButton.setVisibility(View.GONE);
                  }
             } catch (Exception e) { Log.e("PcView", "Error checking Fire TV feature", e); }
         } else { Log.e("PcView", "helpButton not found!"); }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UiHelper.setLocale(this); // Set locale early
        // Set Content View ONCE here
        setContentView(R.layout.activity_pc_view);
        UiHelper.notifyNewRootView(this); // Notify after setting main view
        initializeViews(); // Initialize views after setting content view

        // Fetch server info only once
        if (!isServerInfoFetched) {
            fetchServerInfoFromApi();
        }
    }


    private void fetchServerInfoFromApi() {
        if (isServerInfoFetched) {
            Log.d("PcView", "Server info already fetched or fetch in progress.");
            return;
        }
        isServerInfoFetched = true; // Mark as fetching

        SharedPreferences prefs = getSecurePrefs();
        if (prefs == null) {
             isServerInfoFetched = false; // Reset flag if prefs fail
             return; // Logout called in getSecurePrefs
        }

        int userId = prefs.getInt(KEY_USER_ID, -1);
        String authToken = prefs.getString(KEY_AUTH_TOKEN, null);

        if (userId == -1 || authToken == null) {
            Log.e("PcView", "User ID or Auth Token not found in secure prefs.");
            Toast.makeText(this, "خطا: اطلاعات ورود یافت نشد. لطفاً دوباره وارد شوید.", Toast.LENGTH_LONG).show();
            logoutUser();
            return;
        }

        // Ensure UI state shows loading
        if (loadingIndicator != null) loadingIndicator.setVisibility(View.VISIBLE);
        if (statusTextView != null) {
             statusTextView.setText("درحال دریافت اطلاعات سرور...");
             statusTextView.setVisibility(View.VISIBLE);
        }


        new Thread(() -> {
            HttpURLConnection conn = null;
            String responseString = "";
            int responseCode = -1;

            try {
                URL url = new URL("https://bazicloud.com/wp-json/amncloud/v1/get-server-info");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("X-User-ID", String.valueOf(userId));
                conn.setRequestProperty("X-Auth-Token", authToken);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                responseCode = conn.getResponseCode();
                InputStream inputStream;
                if (responseCode >= 200 && responseCode < 400) {
                    inputStream = conn.getInputStream();
                } else {
                    inputStream = conn.getErrorStream();
                }

                 if (inputStream != null) {
                       try (Scanner scanner = new Scanner(inputStream, "UTF-8")) {
                            responseString = scanner.useDelimiter("\\A").next();
                       } finally {
                            try { inputStream.close(); } catch (IOException ioException) { Log.e("PcView", "Error closing input stream", ioException); }
                       }
                 } else {
                       Log.w("PcView", "InputStream was null for response code: " + responseCode);
                 }

            } catch (Exception e) {
                 Log.e("PcView", "Network error fetching server info", e);
                 responseCode = -1; // Indicate network error
                 responseString = "Network error: " + e.getMessage();
            } finally {
                 if (conn != null) {
                     conn.disconnect();
                 }
                 final int finalResponseCode = responseCode;
                 final String finalResponseString = responseString;
                 // Post result back to main thread
                 runOnUiThread(() -> handleApiResponse(finalResponseCode, finalResponseString));
            }
        }).start();
    }

    private void handleApiResponse(int responseCode, String responseString) {

        // API call attempt finished, hide loading indicator
        if (loadingIndicator != null) loadingIndicator.setVisibility(View.GONE);

        // Handle network error first
        if (responseCode == -1) {
             if (statusTextView != null) {
                 statusTextView.setText("خطا در ارتباط با سرور.");
                 statusTextView.setVisibility(View.VISIBLE);
             }
             isServerInfoFetched = false; // Allow retry on network error
             return;
        }

        JSONObject responseJson = null;
        boolean success = false;
        String apiMessage = "خطا در دریافت اطلاعات سرور";
        JSONObject serverData = null;

        try {
             if (!responseString.isEmpty()) {
                  responseJson = new JSONObject(responseString);
                  if (responseCode == 200) {
                       success = responseJson.optBoolean("success", false);
                       if(success) {
                           serverData = responseJson.optJSONObject("server");
                       }
                  }
                  // Always try to get a message from the response
                  apiMessage = responseJson.optString("message", apiMessage);
             } else {
                 apiMessage = "پاسخ خالی از سرور (" + responseCode + ")";
             }
        } catch (Exception e) {
             Log.e("PcView", "Error parsing server info JSON response", e);
             apiMessage = "پاسخ سرور نامعتبر است (" + responseCode + ")";
        }

        // Handle specific authentication failure (invalid token)
        if (responseCode == 401 || responseCode == 403) {
             Log.e("PcView", "Authentication failed ("+ responseCode + "): " + apiMessage);
             Toast.makeText(this, "نشست شما نامعتبر است. لطفاً دوباره وارد شوید.", Toast.LENGTH_LONG).show();
             logoutUser(); // Force logout
             return;
        }

        // Handle successful response with server data
        if (success && serverData != null) {
            String serverIp = serverData.optString("ip", null);
            String serverName = serverData.optString("name", "Game Server");
            int serverPort = serverData.optInt("port", -1);
            int serverId = serverData.optInt("id", 0); // Get server ID if available

            if (serverIp != null) {
                // Create ComputerDetails for AppView
                String pseudoUuid = serverId != 0
                                    ? "server-" + serverId // Use server ID for a simple unique ID
                                    : UUID.nameUUIDFromBytes(serverIp.getBytes()).toString(); // Fallback

                ComputerDetails computer = new ComputerDetails();
                computer.uuid = pseudoUuid;
                computer.name = serverName;
                computer.state = ComputerDetails.State.ONLINE; // Assume ONLINE if received from API
                computer.pairState = PairingManager.PairState.PAIRED; // Assume PAIRED for now
                if(serverPort != -1) {
                   computer.httpsPort = serverPort; // Assign port if available
                }

                try {
                     // Set activeAddress using the STATIC inner class constructor
                    InetAddress inetAddress = InetAddress.getByName(serverIp);
                    int portForTuple = (serverPort != -1) ? serverPort : DEFAULT_SERVER_PORT; // Use defined constant
                    String ipString = inetAddress.getHostAddress();
                    ComputerDetails.AddressTuple tuple = new ComputerDetails.AddressTuple(ipString, portForTuple);
                    computer.activeAddress = tuple; // Assign

                } catch (UnknownHostException e) {
                     Log.e("PcView", "Invalid IP address received from API: " + serverIp, e);
                     if (statusTextView != null) {
                         statusTextView.setText("آدرس IP سرور نامعتبر است.");
                         statusTextView.setVisibility(View.VISIBLE);
                     }
                     isServerInfoFetched = false; // Allow retry maybe
                     return; // Stop
                } catch (Exception e_inner) {
                     Log.e("PcView", "Failed to create static AddressTuple", e_inner);
                     if (statusTextView != null) {
                          statusTextView.setText("خطا در ساخت آدرس سرور.");
                          statusTextView.setVisibility(View.VISIBLE);
                     }
                     isServerInfoFetched = false;
                     return;
                }

                // Navigate to AppView
                onServerInfoReceived(computer);

            } else {
                // Handle case where success=true but IP is missing
                if (statusTextView != null) {
                     statusTextView.setText("اطلاعات IP سرور از سرور دریافت نشد.");
                     statusTextView.setVisibility(View.VISIBLE);
                }
                Log.e("PcView", "Server IP missing in successful API response.");
                isServerInfoFetched = false; // Allow retry?
            }
        } else {
             // Handle API call failure (non-200 or success:false)
             if (statusTextView != null) {
                 statusTextView.setText(apiMessage); // Show error message from API
                 statusTextView.setVisibility(View.VISIBLE);
             }
             // Handle specific 'no_active_servers' code
             if (responseJson != null && "no_active_servers".equals(responseJson.optString("code"))) {
                 if (statusTextView != null) statusTextView.setText("متاسفانه سرور فعالی برای شما یافت نشد.");
             }
             isServerInfoFetched = false; // Allow retry on failure?
        }
    }

    private void onServerInfoReceived(ComputerDetails computerDetails) {
        if (statusTextView != null) {
           statusTextView.setVisibility(View.GONE); // Hide status text on success
        }
        // Navigate to the AppView immediately
        doAppList(computerDetails, false, false);
    }


    // Simplified version, assuming AppView takes necessary details via Intent
    private void doAppList(ComputerDetails computer, boolean newlyPaired, boolean showHiddenGames) {
        if (computer == null) {
            Toast.makeText(PcView.this,"خطای اطلاعات سرور", Toast.LENGTH_SHORT).show();
            if(statusTextView != null) statusTextView.setText("خطای اطلاعات سرور");
            isServerInfoFetched = false; // Reset flag if ComputerDetails is null
            return;
        }
         // Check the inner address field of the tuple
         if (computer.activeAddress == null || computer.activeAddress.address == null || computer.activeAddress.address.isEmpty()) {
             Log.e("PcView", "Cannot start AppView, activeAddress or its inner address string is null/empty");
             Toast.makeText(this, "خطای آدرس سرور.", Toast.LENGTH_SHORT).show();
             if(statusTextView != null) statusTextView.setText("خطای آدرس سرور.");
             isServerInfoFetched = false; // Reset flag
             return;
         }


        Intent i = new Intent(this, AppView.class);
        i.putExtra(AppView.NAME_EXTRA, computer.name);

        String uuid = computer.uuid;
        if(uuid == null || uuid.isEmpty()){
            Log.w("PcView", "Computer UUID is missing, generating one based on IP.");
            // Use address field from AddressTuple (which is String) to generate UUID
            try {
                String ipString = computer.activeAddress.address;
                InetAddress inet = InetAddress.getByName(ipString);
                uuid = UUID.nameUUIDFromBytes(inet.getAddress()).toString();
            } catch (Exception e) {
                Log.e("PcView", "Failed to generate UUID for AppView from IP: " + (computer.activeAddress != null ? computer.activeAddress.address : "null"), e);
                uuid = UUID.randomUUID().toString(); // Fallback to random
            }
        }
        i.putExtra(AppView.UUID_EXTRA, uuid);
        i.putExtra(AppView.NEW_PAIR_EXTRA, newlyPaired);
        i.putExtra(AppView.SHOW_HIDDEN_APPS_EXTRA, showHiddenGames);

        // Use address field from AddressTuple for logging IP
        Log.d("PcView", "Starting AppView for server: " + computer.name + " (IP: " + computer.activeAddress.address + ", UUID: " + uuid + ")");
        startActivity(i);
        finish(); // Finish PcView after launching AppView
    }


    private void logoutUser() {
        isServerInfoFetched = false; // Reset flag
        SharedPreferences prefs = getSecurePrefs();
        if (prefs != null) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear().apply(); // Clear all secure prefs
        }
        Intent intent = new Intent(PcView.this, HomePhoneLoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Close current activity
    }


    // Keep essential lifecycle methods
    @Override
    public void onDestroy() { super.onDestroy(); }
    @Override
    protected void onResume() { super.onResume(); }
    @Override
    protected void onPause() { super.onPause(); }
    @Override
    protected void onStop() {
        super.onStop();
        // Check if Dialog class exists and method exists before calling (more robust)
        try {
             java.lang.reflect.Method method = Dialog.class.getMethod("isDialogShowing");
             Boolean showing = (Boolean) method.invoke(null);
             if (showing != null && showing) {
                  Dialog.closeDialogs();
             }
        } catch (NoSuchMethodException nsme) {
            // Method doesn't exist, maybe Dialog class changed or is different
            Log.w("PcView", "Dialog.isDialogShowing() method not found. Cannot check if dialog needs closing.");
        } catch (Exception e) {
            // Catch other reflection/invocation errors
            Log.w("PcView", "Error checking/closing dialogs via reflection", e);
            // Attempt to close anyway if method might exist but checking failed
             try { Dialog.closeDialogs(); } catch (Exception ignore) {}
        }
    }

} 