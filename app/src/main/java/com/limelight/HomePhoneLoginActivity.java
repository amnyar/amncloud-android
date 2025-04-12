package com.limelight;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log; // <-- Import اضافه شد
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.InputStream; // <-- Import اضافه شد (برای InputStream)
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;
import java.util.Scanner;

public class HomePhoneLoginActivity extends AppCompatActivity {

    EditText phoneInput, nameInput, familyInput, emailInput, codeInput;
    Button sendCodeButton, registerButton, verifyButton, backToLoginButton, resendCodeButton;
    TextView statusText, timerText;

    CountDownTimer timer;
    int secondsRemaining = 0;
    boolean registrationMode = false;
    boolean codeSent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("amnyar", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("is_logged_in", false);

        if (isLoggedIn) {
            startActivity(new Intent(this, PcView.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_home_phone_login);
        ImageView logo = findViewById(R.id.logo);

        ImageView bottomImage = findViewById(R.id.bottomImage);
        Handler handler = new Handler();
        Random random = new Random();

        Runnable animationRunnable = new Runnable() {
            @Override
            public void run() {
                int type = random.nextInt(5); // 0 to 4

                AnimatorSet animatorSet = new AnimatorSet();

                switch (type) {
                    case 0:
                        ObjectAnimator upDown = ObjectAnimator.ofFloat(bottomImage, "translationY", 0f, -50f, 0f);
                        upDown.setDuration(800);
                        animatorSet.play(upDown);
                        break;

                    case 1:
                        ObjectAnimator leftRight = ObjectAnimator.ofFloat(bottomImage, "translationX", 0f, 50f, -50f, 0f);
                        leftRight.setDuration(900);
                        animatorSet.play(leftRight);
                        break;

                    case 2:
                        float angle = random.nextBoolean() ? 360f : -360f;
                        ObjectAnimator rotate = ObjectAnimator.ofFloat(bottomImage, "rotation", 0f, angle);
                        rotate.setDuration(1000);
                        animatorSet.play(rotate);
                        break;

                    case 3:
                        ObjectAnimator blinkOut = ObjectAnimator.ofFloat(bottomImage, "alpha", 1f, 0f);
                        blinkOut.setDuration(200);
                        ObjectAnimator blinkIn = ObjectAnimator.ofFloat(bottomImage, "alpha", 0f, 1f);
                        blinkIn.setDuration(200);
                        animatorSet.playSequentially(blinkOut, blinkIn, blinkOut, blinkIn);
                        break;

                    case 4:
                        ObjectAnimator scaleX = ObjectAnimator.ofFloat(bottomImage, "scaleX", 1f, 1.4f, 1f);
                        ObjectAnimator scaleY = ObjectAnimator.ofFloat(bottomImage, "scaleY", 1f, 1.4f, 1f);
                        scaleX.setDuration(700);
                        scaleY.setDuration(700);
                        animatorSet.playTogether(scaleX, scaleY);
                        break;
                }

                animatorSet.start();
                handler.postDelayed(this, 1500 + random.nextInt(800));
            }
        };
        handler.post(animationRunnable);

        phoneInput = findViewById(R.id.phoneInput);
        nameInput = findViewById(R.id.nameInput);
        familyInput = findViewById(R.id.familyInput);
        emailInput = findViewById(R.id.emailInput);
        codeInput = findViewById(R.id.codeInput);

        sendCodeButton = findViewById(R.id.sendCodeButton);
        registerButton = findViewById(R.id.registerButton);
        verifyButton = findViewById(R.id.verifyButton);
        backToLoginButton = findViewById(R.id.backToLoginButton);
        resendCodeButton = findViewById(R.id.resendCodeButton);

        statusText = findViewById(R.id.statusText);
        timerText = findViewById(R.id.timerText);

        resendCodeButton.setEnabled(false);
        resendCodeButton.setAlpha(0.4f);
        resendCodeButton.setTextColor(Color.parseColor("#AAAAAA"));

        sendCodeButton.setOnClickListener(v -> {
            String phone = phoneInput.getText().toString().trim();
            if (!phone.matches("^09\\d{9}$")) {
                statusText.setText("شماره موبایل باید با ۰۹ شروع شود و ۱۱ رقم باشد");
                return;
            }
            sendCodeToApi(phone); // <-- تابع اصلاح شده فراخوانی می شود
        });

        registerButton.setOnClickListener(v -> {
            String phone = phoneInput.getText().toString().trim();
            String name = nameInput.getText().toString().trim();
            String family = familyInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();

            if (name.isEmpty() || family.isEmpty()) {
                statusText.setText("نام و نام خانوادگی را وارد کنید");
                return;
            }

            if (!email.contains("@")) { // Basic check, consider Patterns.EMAIL_ADDRESS.matcher(email).matches()
                statusText.setText("ایمیل معتبر نیست");
                return;
            }

            sendRegisterRequest(phone, name, family, email);
        });

        verifyButton.setOnClickListener(v -> {
            String phone = phoneInput.getText().toString().trim();
            String code = codeInput.getText().toString().trim();

            if (!code.matches("^\\d{6}$")) {
                statusText.setText("کد ۶ رقمی معتبر نیست");
                return;
            }

            verifyCode(phone, code);
        });

        backToLoginButton.setOnClickListener(v -> {
            registrationMode = false;
            codeSent = false;
             if (timer != null) { // Cancel timer when going back
                 timer.cancel();
                 timer = null;
             }
            showLoginMode();
        });

        resendCodeButton.setOnClickListener(v -> {
            // No need to manage visibility/enabled state here, startTimer does it
            // Just call send code again and start timer
            sendCodeToApi(phoneInput.getText().toString().trim()); // Resend login/check code
        });

        showLoginMode();
        phoneInput.requestFocus();
        phoneInput.postDelayed(() -> {
           InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
           if (imm != null) {
                imm.showSoftInput(phoneInput, InputMethodManager.SHOW_IMPLICIT);
           }
        }, 300);
    }


    private void showLoginMode() {
        nameInput.setVisibility(View.GONE);
        familyInput.setVisibility(View.GONE);
        emailInput.setVisibility(View.GONE);
        registerButton.setVisibility(View.GONE);
        codeInput.setVisibility(View.GONE);
        verifyButton.setVisibility(View.GONE);
        backToLoginButton.setVisibility(View.GONE);
        resendCodeButton.setVisibility(View.GONE);
        timerText.setVisibility(View.GONE);

        sendCodeButton.setVisibility(View.VISIBLE);
        sendCodeButton.setEnabled(true); // Ensure enabled when shown

        phoneInput.setEnabled(true);
        phoneInput.setAlpha(1.0f);
        phoneInput.setText(""); // Clear phone input
        statusText.setText(""); // Clear status
    }

    private void showRegisterForm() {
        registrationMode = true;
        nameInput.setVisibility(View.VISIBLE);
        familyInput.setVisibility(View.VISIBLE);
        emailInput.setVisibility(View.VISIBLE);
        registerButton.setVisibility(View.VISIBLE);
        registerButton.setEnabled(true); // Ensure enabled

        backToLoginButton.setVisibility(View.VISIBLE);
        sendCodeButton.setVisibility(View.GONE);
        codeInput.setVisibility(View.GONE);
        verifyButton.setVisibility(View.GONE);
        resendCodeButton.setVisibility(View.GONE);
        timerText.setVisibility(View.GONE);

         phoneInput.setEnabled(false); // Keep phone disabled
         phoneInput.setAlpha(0.4f);
         nameInput.requestFocus(); // Focus on name field
    }

    private void showCodeInput() {
        codeSent = true;
        codeInput.setVisibility(View.VISIBLE);
        codeInput.setText(""); // Clear previous code
        verifyButton.setVisibility(View.VISIBLE);
        verifyButton.setEnabled(true); // Ensure enabled

        timerText.setVisibility(View.VISIBLE); // Timer text shown by timer itself
        // resendCodeButton visibility managed by timer finish/start
        resendCodeButton.setVisibility(View.GONE); // Hide initially, timer will show it

        registerButton.setVisibility(View.GONE);
        sendCodeButton.setVisibility(View.GONE);
        nameInput.setVisibility(View.GONE);
        familyInput.setVisibility(View.GONE);
        emailInput.setVisibility(View.GONE);

        backToLoginButton.setVisibility(View.VISIBLE); // Allow going back

        phoneInput.setEnabled(false); // Keep phone disabled
        phoneInput.setAlpha(0.4f);

        codeInput.requestFocus();
        codeInput.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
             if (imm != null) {
                 imm.showSoftInput(codeInput, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 300);
         // Timer should be started by the function that calls showCodeInput if needed
    }


    // ========================================================================
    // START OF REPLACED FUNCTION sendCodeToApi
    // ========================================================================
    private void sendCodeToApi(String phone) {
        statusText.setText("در حال ارسال کد . . .");
        sendCodeButton.setEnabled(false);
        // Note: Timer might be better started AFTER successful API call indication
        // But keeping original logic for now, ensure it's cancelled correctly
        startTimer(); // Starts or restarts the timer

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("https://bazicloud.com/wp-json/amncloud/v1/send-code");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000); // 15 seconds connection timeout
                conn.setReadTimeout(15000);    // 15 seconds read timeout

                JSONObject json = new JSONObject();
                json.put("phone", phone);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8")); // Specify UTF-8 encoding
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode(); // Get the HTTP status code first

                InputStream inputStream;
                // Use getInputStream for success codes (2xx), getErrorStream for others
                if (responseCode >= 200 && responseCode < 400) {
                    inputStream = conn.getInputStream();
                } else {
                    inputStream = conn.getErrorStream(); // Use error stream for 404 and other errors
                }

                String responseString = "";
                if (inputStream != null) {
                    // Try-with-resources for Scanner ensures it's closed
                    try (Scanner scanner = new Scanner(inputStream, "UTF-8")) {
                         responseString = scanner.useDelimiter("\\A").next();
                    }
                } else {
                     Log.w("sendCodeToApi", "InputStream was null for response code: " + responseCode);
                }

                // Process result on UI thread
                final int finalResponseCode = responseCode;
                final String finalResponseString = responseString; // Use final variable for lambda

                runOnUiThread(() -> {
                    // --- UI updates and state reset ---
                    // Timer is managed by startTimer/onFinish/onTick
                    // We only need to re-enable the primary button if needed
                    sendCodeButton.setEnabled(true); // Always re-enable the button eventually

                    // --- Handle response codes ---
                    if (finalResponseCode == 200) {
                        // Existing user, code sent successfully
                        statusText.setText("کد با موفقیت ارسال شد");
                        // Timer is already running via startTimer() call above
                        showCodeInput(); // Show code input field
                    } else if (finalResponseCode == 404) {
                        // User not found - Stop the timer, show registration form
                        if (timer != null) {
                             timer.cancel();
                             timer = null;
                        }
                        timerText.setVisibility(View.GONE);
                        resendCodeButton.setVisibility(View.GONE);

                        statusText.setText("کاربری با این شماره وجود ندارد، لطفاً ثبت‌نام کنید.");
                        showRegisterForm(); // Show the registration form
                    } else {
                        // Handle other potential errors - Stop the timer, revert to login mode
                         if (timer != null) {
                            timer.cancel();
                            timer = null;
                         }
                         timerText.setVisibility(View.GONE);
                         resendCodeButton.setVisibility(View.GONE);

                        String errorMessage = "خطا در ارسال کد"; // Default
                        try {
                            if (!finalResponseString.isEmpty()) {
                                // Try to parse error message from JSON response body
                                JSONObject errorJson = new JSONObject(finalResponseString);
                                errorMessage = errorJson.optString("message", errorMessage);
                            } else {
                                // Provide more specific default messages based on code if possible
                                if (finalResponseCode == 400) errorMessage = "درخواست نامعتبر (کد ۴۰۰)";
                                else if (finalResponseCode == 500) errorMessage = "خطای داخلی سرور (کد ۵۰۰)";
                                else errorMessage = "خطا در ارسال کد (" + finalResponseCode + ")";
                            }
                        } catch (Exception parseEx) {
                            Log.e("sendCodeToApi", "Error parsing error JSON response", parseEx);
                            errorMessage = "خطای نامشخص در پاسخ سرور (" + finalResponseCode + ")";
                        }
                        statusText.setText(errorMessage);
                        showLoginMode(); // Revert to login mode on unexpected errors
                    }
                });

            } catch (Exception e) {
                // Log the actual exception for debugging
                Log.e("sendCodeToApi", "Network or processing error", e);
                runOnUiThread(() -> {
                    // --- UI updates and state reset in catch block ---
                     if (timer != null) {
                        timer.cancel();
                        timer = null;
                     }
                    sendCodeButton.setEnabled(true); // Re-enable button
                    timerText.setVisibility(View.GONE); // Hide timer elements
                    resendCodeButton.setVisibility(View.GONE);

                    statusText.setText("خطا در ارتباط با سرور"); // Show generic error to user
                    showLoginMode(); // Revert to login mode on network error
                });
            } finally {
                if (conn != null) {
                    conn.disconnect(); // Ensure connection is always closed
                }
            }
        }).start();
    }
    // ========================================================================
    // END OF REPLACED FUNCTION sendCodeToApi
    // ========================================================================


    // --- sendRegisterRequest (Original - with getErrorStream fix) ---
    private void sendRegisterRequest(String phone, String name, String family, String email) {
        statusText.setText("در حال ثبت ‌نام . . .");
        registerButton.setEnabled(false);
        // Decide if timer should start here or only after success? Assuming only after success for register
        // startTimer(); // Commented out - Start timer only when code is actually sent (in success case)

        new Thread(() -> {
            HttpURLConnection conn = null; // Initialize
            try {
                URL url = new URL("https://bazicloud.com/wp-json/amncloud/v1/request-registration-code");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                JSONObject json = new JSONObject();
                json.put("name", name);
                json.put("family", family);
                json.put("email", email);
                json.put("phone", phone);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();

                InputStream inputStream;
                if (responseCode >= 200 && responseCode < 400) {
                    inputStream = conn.getInputStream();
                } else {
                    inputStream = conn.getErrorStream(); // Use error stream here too!
                }

                String responseString = "";
                 if (inputStream != null) {
                     try (Scanner scanner = new Scanner(inputStream, "UTF-8")) {
                         responseString = scanner.useDelimiter("\\A").next();
                     }
                 } else {
                      Log.w("sendRegisterRequest", "InputStream was null for response code: " + responseCode);
                 }

                final int finalResponseCode = responseCode;
                final String finalResponseString = responseString;

                runOnUiThread(() -> {
                    registerButton.setEnabled(true); // Re-enable button in most cases

                    JSONObject responseJson = null;
                    boolean success = false;
                    String apiMessage = "ثبت‌نام با خطا مواجه شد"; // Default message

                    try {
                         if (!finalResponseString.isEmpty()) {
                              responseJson = new JSONObject(finalResponseString);
                              // Check success *only* for 200 OK
                              if (finalResponseCode == 200) {
                                   success = responseJson.optBoolean("success", false);
                              }
                              apiMessage = responseJson.optString("message", apiMessage); // Get message regardless of code
                         }
                    } catch (Exception parseEx) {
                         Log.e("sendRegisterRequest", "Error parsing JSON response", parseEx);
                         apiMessage = "خطای نامشخص در پاسخ سرور (" + finalResponseCode + ")";
                    }


                    if (finalResponseCode == 200 && success) {
                        statusText.setText("کد ارسال شد ، لطفاً آن را وارد کنید");
                        startTimer(); // Start timer only on success
                        showCodeInput();
                    } else {
                        // Handle errors including 409
                        statusText.setText(apiMessage); // Show message from API or default error

                        if (finalResponseCode == 409) {
                            // User exists - revert to login mode
                            registrationMode = false;
                            codeSent = false;
                             if (timer != null) { // Ensure timer is stopped if it was running
                                 timer.cancel();
                                 timer = null;
                             }
                            showLoginMode();
                            // Append instruction to message
                            statusText.setText(apiMessage + "\n" + "لطفاً به جای ثبت‌نام، وارد شوید.");
                        } else {
                            // For other errors, just display the message and keep user on register form
                            registerButton.setEnabled(true); // Ensure button is enabled
                        }
                    }
                });

            } catch (Exception e) {
                Log.e("sendRegisterRequest", "Network or processing error", e);
                runOnUiThread(() -> {
                    statusText.setText("خطا در ثبت‌نام یا ارتباط با سرور");
                    registerButton.setEnabled(true); // Re-enable button
                });
            } finally {
                 if (conn != null) {
                     conn.disconnect();
                 }
            }
        }).start();
    }


    // --- verifyCode (Original - with getErrorStream fix and better response handling) ---
    private void verifyCode(String phone, String code) {
        statusText.setText("در حال بررسی کد . . .");
        verifyButton.setEnabled(false); // Disable button while verifying

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                // Determine URL based on mode
                URL url = new URL(registrationMode
                        ? "https://bazicloud.com/wp-json/amncloud/v1/complete-registration"
                        : "https://bazicloud.com/wp-json/amncloud/v1/verify-code"); // Assuming this endpoint exists

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                JSONObject json = new JSONObject();
                json.put("phone", phone);
                json.put("code", code);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();

                InputStream inputStream;
                 if (responseCode >= 200 && responseCode < 400) {
                    inputStream = conn.getInputStream();
                 } else {
                    inputStream = conn.getErrorStream();
                 }

                 String responseString = "";
                 if (inputStream != null) {
                     try (Scanner scanner = new Scanner(inputStream, "UTF-8")) {
                         responseString = scanner.useDelimiter("\\A").next();
                     }
                 } else {
                      Log.w("verifyCode", "InputStream was null for response code: " + responseCode);
                 }

                final int finalResponseCode = responseCode;
                final String finalResponseString = responseString;

                runOnUiThread(() -> {
                    // Always stop the timer after verification attempt
                    if (timer != null) {
                         timer.cancel();
                         timer = null;
                    }
                    timerText.setVisibility(View.GONE);
                    resendCodeButton.setVisibility(View.GONE); // Hide resend button


                    JSONObject responseJson = null;
                    boolean success = false;
                    String apiMessage = "کد اشتباه یا منقضی شده"; // Default error

                     try {
                         if (!finalResponseString.isEmpty()) {
                              responseJson = new JSONObject(finalResponseString);
                               // Check success only for 200 OK
                              if (finalResponseCode == 200) {
                                   success = responseJson.optBoolean("success", false);
                              }
                              // Try to get a more specific message even on error
                              apiMessage = responseJson.optString("message", apiMessage);
                         }
                    } catch (Exception parseEx) {
                         Log.e("verifyCode", "Error parsing JSON response", parseEx);
                         apiMessage = "خطای نامشخص در پاسخ سرور (" + finalResponseCode + ")";
                    }


                    if (finalResponseCode == 200 && success) {
                        statusText.setText(registrationMode ? "ثبت‌نام و ورود موفقیت‌آمیز" : "ورود موفقیت‌آمیز");

                        // --- Save Login State ---
                        SharedPreferences prefs = getSharedPreferences("amnyar", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("is_logged_in", true);

                        // Optionally save user data received from API if available
                        if (responseJson != null && responseJson.has("data")) {
                             JSONObject userData = responseJson.optJSONObject("data");
                             if (userData != null) {
                                 editor.putInt("user_id", userData.optInt("user_id", -1));
                                 editor.putString("display_name", userData.optString("display_name", ""));
                                 editor.putString("email", userData.optString("email", ""));
                                 // Add other fields if needed
                             }
                        }
                        editor.apply();
                        // --- End Save Login State ---


                        startActivity(new Intent(HomePhoneLoginActivity.this, PcView.class));
                        finish(); // Close this login activity
                    } else {
                        // Handle verification failure
                        statusText.setText(apiMessage); // Show specific error from API or default
                        verifyButton.setEnabled(true); // Re-enable button to allow retry
                         // Maybe show resend button again? Depends on UX choice
                         // If code is wrong, user might need to resend.
                         resendCodeButton.setVisibility(View.VISIBLE);
                         resendCodeButton.setEnabled(true); // Enable resend on failure
                         resendCodeButton.setAlpha(1.0f);
                         resendCodeButton.setTextColor(Color.parseColor("#000000"));
                    }
                });

            } catch (Exception e) {
                 Log.e("verifyCode", "Network or processing error", e);
                runOnUiThread(() -> {
                     // UI reset in case of network/unexpected error
                     if (timer != null) {
                          timer.cancel();
                          timer = null;
                     }
                     timerText.setVisibility(View.GONE);
                     resendCodeButton.setVisibility(View.GONE);
                     verifyButton.setEnabled(true); // Re-enable verify button

                    statusText.setText("خطا در بررسی کد");
                });
            } finally {
                 if (conn != null) {
                     conn.disconnect();
                 }
            }
        }).start();
    }


    private void startTimer() {
         // Cancel any existing timer first
         if (timer != null) {
             timer.cancel();
         }

        secondsRemaining = 59; // Reset seconds
        timerText.setText("ارسال مجدد تا " + secondsRemaining + " ثانیه دیگر");
        timerText.setVisibility(View.VISIBLE); // Show timer text

        resendCodeButton.setEnabled(false); // Disable resend button initially
        resendCodeButton.setAlpha(0.4f);
        resendCodeButton.setTextColor(Color.parseColor("#AAAAAA"));
        resendCodeButton.setVisibility(View.GONE); // Hide resend button initially, show on finish


        timer = new CountDownTimer(59000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                secondsRemaining--;
                 if (secondsRemaining >= 0) { // Prevent showing negative number
                    timerText.setText("ارسال مجدد تا " + secondsRemaining + " ثانیه دیگر");
                 } else {
                     timerText.setText("ارسال مجدد تا ۰ ثانیه دیگر"); // Show 0 at the end
                 }
                 // Keep resend hidden during countdown
                 resendCodeButton.setVisibility(View.GONE);
            }

            @Override
            public void onFinish() {
                timer = null; // Timer is finished
                resendCodeButton.setEnabled(true);
                resendCodeButton.setAlpha(1.0f);
                resendCodeButton.setTextColor(Color.parseColor("#000000")); // Assuming black for enabled text
                resendCodeButton.setVisibility(View.VISIBLE); // Show resend button
                timerText.setVisibility(View.GONE); // Hide timer text
            }
        }.start();
    }

     @Override
     protected void onDestroy() {
         super.onDestroy();
         // Cancel timer if activity is destroyed to prevent leaks
         if (timer != null) {
             timer.cancel();
             timer = null;
         }
     }
}