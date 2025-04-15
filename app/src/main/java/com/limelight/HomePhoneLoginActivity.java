package com.limelight;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
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

    private static final String SECURE_PREFS_NAME = "amnyar_secure_prefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_AUTH_TOKEN = "auth_token";
    private static final String KEY_PHONE_NUMBER = "phone_number";
    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String KEY_EMAIL = "email";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isUserLoggedIn()) {
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
               int type = random.nextInt(5);
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
            sendCodeToApi(phone);
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
            if (!email.contains("@")) { // Basic check, consider more robust validation
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
             if (timer != null) {
                 timer.cancel();
                 timer = null;
             }
            showLoginMode();
        });

        resendCodeButton.setOnClickListener(v -> {
            sendCodeToApi(phoneInput.getText().toString().trim());
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
            Log.e("HomePhoneLogin", "Failed to get EncryptedSharedPreferences", e);
            runOnUiThread(()-> statusText.setText("خطای داخلی ذخیره‌سازی امن"));
            return null; // Return null on error
        }
    }


    private boolean isUserLoggedIn() {
        SharedPreferences prefs = getSecurePrefs();
        if (prefs != null) {
            return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
        }
        return false;
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
        sendCodeButton.setEnabled(true);
        phoneInput.setEnabled(true);
        phoneInput.setAlpha(1.0f);
        phoneInput.setText("");
        statusText.setText("");
    }

    private void showRegisterForm() {
        registrationMode = true;
        nameInput.setVisibility(View.VISIBLE);
        familyInput.setVisibility(View.VISIBLE);
        emailInput.setVisibility(View.VISIBLE);
        registerButton.setVisibility(View.VISIBLE);
        registerButton.setEnabled(true);
        backToLoginButton.setVisibility(View.VISIBLE);
        sendCodeButton.setVisibility(View.GONE);
        codeInput.setVisibility(View.GONE);
        verifyButton.setVisibility(View.GONE);
        resendCodeButton.setVisibility(View.GONE);
        timerText.setVisibility(View.GONE);
        phoneInput.setEnabled(false);
        phoneInput.setAlpha(0.4f);
        nameInput.requestFocus();
    }

    private void showCodeInput() {
        codeSent = true;
        codeInput.setVisibility(View.VISIBLE);
        codeInput.setText("");
        verifyButton.setVisibility(View.VISIBLE);
        verifyButton.setEnabled(true);
        timerText.setVisibility(View.VISIBLE);
        resendCodeButton.setVisibility(View.GONE); // Hide initially until timer finishes
        registerButton.setVisibility(View.GONE);
        sendCodeButton.setVisibility(View.GONE);
        nameInput.setVisibility(View.GONE);
        familyInput.setVisibility(View.GONE);
        emailInput.setVisibility(View.GONE);
        backToLoginButton.setVisibility(View.VISIBLE);
        phoneInput.setEnabled(false);
        phoneInput.setAlpha(0.4f);
        codeInput.requestFocus();
        codeInput.postDelayed(() -> {
           InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
             if (imm != null) {
                 imm.showSoftInput(codeInput, InputMethodManager.SHOW_IMPLICIT);
             }
        }, 300);
    }


    private void sendCodeToApi(String phone) {
        statusText.setText("در حال ارسال کد . . .");
        sendCodeButton.setEnabled(false);
        startTimer();

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("https://bazicloud.com/wp-json/amncloud/v1/send-code");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                JSONObject json = new JSONObject();
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
                    inputStream = conn.getErrorStream();
                }

                String responseString = "";
                if (inputStream != null) {
                    try (Scanner scanner = new Scanner(inputStream, "UTF-8")) {
                         responseString = scanner.useDelimiter("\\A").next();
                    }
                } else {
                     Log.w("sendCodeToApi", "InputStream was null for response code: " + responseCode);
                }

                final int finalResponseCode = responseCode;
                final String finalResponseString = responseString;

                runOnUiThread(() -> {
                    sendCodeButton.setEnabled(true); // Re-enable button even if timer is running for resend logic

                    if (finalResponseCode == 200) {
                        statusText.setText("کد با موفقیت ارسال شد");
                        showCodeInput(); // Only show code input on success
                    } else if (finalResponseCode == 404) {
                        if (timer != null) { timer.cancel(); timer = null; }
                        timerText.setVisibility(View.GONE);
                        resendCodeButton.setVisibility(View.GONE);
                        statusText.setText("کاربری با این شماره وجود ندارد، لطفاً ثبت‌نام کنید.");
                        showRegisterForm();
                    } else { // Handle other errors
                        if (timer != null) { timer.cancel(); timer = null; }
                        timerText.setVisibility(View.GONE);
                        resendCodeButton.setVisibility(View.GONE);

                        String errorMessage = "خطا در ارسال کد";
                        try {
                            if (!finalResponseString.isEmpty()) {
                                JSONObject errorJson = new JSONObject(finalResponseString);
                                errorMessage = errorJson.optString("message", errorMessage);
                            } else {
                                if (finalResponseCode == 400) errorMessage = "درخواست نامعتبر (کد ۴۰۰)";
                                else if (finalResponseCode == 500) errorMessage = "خطای داخلی سرور (کد ۵۰۰)";
                                else errorMessage = "خطا در ارسال کد (" + finalResponseCode + ")";
                            }
                        } catch (Exception parseEx) {
                            Log.e("sendCodeToApi", "Error parsing error JSON response", parseEx);
                            errorMessage = "خطای نامشخص در پاسخ سرور (" + finalResponseCode + ")";
                        }
                        statusText.setText(errorMessage);
                        showLoginMode(); // Go back to login mode on error
                    }
                });

            } catch (Exception e) {
                Log.e("sendCodeToApi", "Network or processing error", e);
                runOnUiThread(() -> {
                     if (timer != null) { timer.cancel(); timer = null; }
                     sendCodeButton.setEnabled(true);
                     timerText.setVisibility(View.GONE);
                     resendCodeButton.setVisibility(View.GONE);
                     statusText.setText("خطا در ارتباط با سرور");
                     showLoginMode(); // Go back to login mode on error
                });
            } finally {
                 if (conn != null) {
                     conn.disconnect();
                 }
            }
        }).start();
    }


    private void sendRegisterRequest(String phone, String name, String family, String email) {
        statusText.setText("در حال ثبت ‌نام . . .");
        registerButton.setEnabled(false);

        new Thread(() -> {
            HttpURLConnection conn = null;
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
                    inputStream = conn.getErrorStream();
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
                    registerButton.setEnabled(true);

                    JSONObject responseJson = null;
                    boolean success = false;
                    String apiMessage = "ثبت‌نام با خطا مواجه شد";

                    try {
                         if (!finalResponseString.isEmpty()) {
                              responseJson = new JSONObject(finalResponseString);
                              if (finalResponseCode == 200) {
                                   success = responseJson.optBoolean("success", false);
                              }
                              apiMessage = responseJson.optString("message", apiMessage);
                         }
                    } catch (Exception parseEx) {
                         Log.e("sendRegisterRequest", "Error parsing JSON response", parseEx);
                         apiMessage = "خطای نامشخص در پاسخ سرور (" + finalResponseCode + ")";
                    }


                    if (finalResponseCode == 200 && success) {
                        statusText.setText("کد ارسال شد ، لطفاً آن را وارد کنید");
                        startTimer();
                        showCodeInput();
                    } else {
                        statusText.setText(apiMessage);
                        // Handle 409 Conflict (user already exists)
                        if (finalResponseCode == 409) {
                            registrationMode = false;
                            codeSent = false;
                             if (timer != null) { timer.cancel(); timer = null; }
                            showLoginMode();
                            statusText.setText(apiMessage + "\n" + "لطفاً به جای ثبت‌نام، وارد شوید.");
                        } else {
                            // Re-enable button for other errors to allow retry
                            registerButton.setEnabled(true);
                        }
                    }
                });

            } catch (Exception e) {
                Log.e("sendRegisterRequest", "Network or processing error", e);
                runOnUiThread(() -> {
                    statusText.setText("خطا در ثبت‌نام یا ارتباط با سرور");
                    registerButton.setEnabled(true);
                });
            } finally {
                 if (conn != null) {
                     conn.disconnect();
                 }
            }
        }).start();
    }


    private void verifyCode(String phone, String code) {
        statusText.setText("در حال بررسی کد . . .");
        verifyButton.setEnabled(false);

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(registrationMode
                        ? "https://bazicloud.com/wp-json/amncloud/v1/complete-registration"
                        : "https://bazicloud.com/wp-json/amncloud/v1/verify-code");

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
                    if (timer != null) { timer.cancel(); timer = null; }
                    timerText.setVisibility(View.GONE);
                    resendCodeButton.setVisibility(View.GONE);

                    JSONObject responseJson = null;
                    boolean success = false;
                    String apiMessage = "کد اشتباه یا منقضی شده";

                    try {
                         if (!finalResponseString.isEmpty()) {
                              responseJson = new JSONObject(finalResponseString);
                              if (finalResponseCode == 200) {
                                   success = responseJson.optBoolean("success", false);
                              }
                              apiMessage = responseJson.optString("message", apiMessage);
                         }
                    } catch (Exception parseEx) {
                         Log.e("verifyCode", "Error parsing JSON response", parseEx);
                         apiMessage = "خطای نامشخص در پاسخ سرور (" + finalResponseCode + ")";
                    }


                    if (finalResponseCode == 200 && success) {
                         statusText.setText(registrationMode ? "ثبت‌نام و ورود موفقیت‌آمیز" : "ورود موفقیت‌آمیز");

                         SharedPreferences securePrefs = getSecurePrefs();
                         if (securePrefs == null) {
                             statusText.setText("خطای ذخیره‌سازی امن اطلاعات");
                             verifyButton.setEnabled(true);
                             // Consider showing resend button here too maybe
                             return;
                         }

                         if (responseJson != null && responseJson.has("data")) {
                             JSONObject userData = responseJson.optJSONObject("data");
                             if (userData != null) {
                                 int userId = userData.optInt("user_id", -1);
                                 String token = userData.optString("token", null);
                                 String displayName = userData.optString("display_name", "");
                                 String email = userData.optString("email", "");
                                 String currentPhone = phone;

                                 if (userId != -1 && token != null && !token.isEmpty()) {
                                      SharedPreferences.Editor editor = securePrefs.edit();
                                      editor.putBoolean(KEY_IS_LOGGED_IN, true);
                                      editor.putInt(KEY_USER_ID, userId);
                                      editor.putString(KEY_AUTH_TOKEN, token);
                                      editor.putString(KEY_PHONE_NUMBER, currentPhone);
                                      editor.putString(KEY_DISPLAY_NAME, displayName);
                                      editor.putString(KEY_EMAIL, email);
                                      editor.apply();

                                      startActivity(new Intent(HomePhoneLoginActivity.this, PcView.class));
                                      finish();
                                      // Successful exit point

                                 } else {
                                      Log.e("HomePhoneLogin", "User ID or Token missing in successful API response.");
                                      statusText.setText("پاسخ سرور ناقص است. لطفاً دوباره تلاش کنید.");
                                      verifyButton.setEnabled(true);
                                      showResendMaybe();
                                 }
                             } else {
                                 Log.e("HomePhoneLogin", "'data' object is null in successful API response.");
                                 statusText.setText("خطای دریافت اطلاعات کاربر از سرور.");
                                 verifyButton.setEnabled(true);
                                 showResendMaybe();
                             }
                         } else {
                              Log.e("HomePhoneLogin", "'data' object missing in successful API response.");
                              statusText.setText("پاسخ سرور نامعتبر است.");
                              verifyButton.setEnabled(true);
                              showResendMaybe();
                         }

                    } else { // Handle API call failure (non-200 or success:false)
                        statusText.setText(apiMessage);
                        verifyButton.setEnabled(true);
                        showResendMaybe();
                    }
                });

            } catch (Exception e) {
                 Log.e("verifyCode", "Network or processing error", e);
                 runOnUiThread(() -> {
                     if (timer != null) { timer.cancel(); timer = null; }
                     timerText.setVisibility(View.GONE);
                     resendCodeButton.setVisibility(View.GONE);
                     verifyButton.setEnabled(true);
                     statusText.setText("خطا در بررسی کد یا ارتباط با سرور");
                 });
            } finally {
                 if (conn != null) {
                     conn.disconnect();
                 }
            }
        }).start();
    }


    private void startTimer() {
        if (timer != null) {
             timer.cancel();
        }

        secondsRemaining = 59;
        timerText.setText("ارسال مجدد تا " + secondsRemaining + " ثانیه دیگر");
        timerText.setVisibility(View.VISIBLE);

        resendCodeButton.setEnabled(false);
        resendCodeButton.setAlpha(0.4f);
        resendCodeButton.setTextColor(Color.parseColor("#AAAAAA"));
        resendCodeButton.setVisibility(View.GONE); // Initially hidden

        timer = new CountDownTimer(59000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                secondsRemaining--;
                 if (secondsRemaining >= 0) {
                     timerText.setText("ارسال مجدد تا " + secondsRemaining + " ثانیه دیگر");
                 } else {
                     timerText.setText("ارسال مجدد تا ۰ ثانیه دیگر"); // Should not happen if logic is correct
                 }
                 resendCodeButton.setVisibility(View.GONE); // Keep hidden during countdown
            }

            @Override
            public void onFinish() {
                timer = null;
                showResendMaybe(); // Call helper to show resend button
            }
        }.start();
    }

    private void showResendMaybe() {
         // Only show resend if we are in the code input phase
         if(codeSent && codeInput.getVisibility() == View.VISIBLE) {
              resendCodeButton.setEnabled(true);
              resendCodeButton.setAlpha(1.0f);
              resendCodeButton.setTextColor(Color.parseColor("#000000"));
              resendCodeButton.setVisibility(View.VISIBLE);
              timerText.setVisibility(View.GONE);
         }
    }


     @Override
     protected void onDestroy() {
         super.onDestroy();
         if (timer != null) {
             timer.cancel();
             timer = null;
         }
     }
}