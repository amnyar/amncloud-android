package com.limelight;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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

            if (!email.contains("@")) {
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
            showLoginMode();
        });

        resendCodeButton.setOnClickListener(v -> {
            resendCodeButton.setEnabled(false);
            resendCodeButton.setAlpha(0.4f);
            resendCodeButton.setTextColor(Color.parseColor("#AAAAAA"));

            secondsRemaining = 59;
            startTimer();
            sendCodeToApi(phoneInput.getText().toString().trim());
        });

        showLoginMode();
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
        phoneInput.setEnabled(true);
        phoneInput.setAlpha(1.0f);
    }

    private void showRegisterForm() {
        registrationMode = true;
        nameInput.setVisibility(View.VISIBLE);
        familyInput.setVisibility(View.VISIBLE);
        emailInput.setVisibility(View.VISIBLE);
        registerButton.setVisibility(View.VISIBLE);
        backToLoginButton.setVisibility(View.VISIBLE);
        sendCodeButton.setVisibility(View.GONE);
    }

    private void showCodeInput() {
        codeSent = true;
        codeInput.setVisibility(View.VISIBLE);
        verifyButton.setVisibility(View.VISIBLE);
        timerText.setVisibility(View.VISIBLE);
        resendCodeButton.setVisibility(View.VISIBLE);
        resendCodeButton.setEnabled(false);
        resendCodeButton.setAlpha(0.4f);
        resendCodeButton.setTextColor(Color.parseColor("#AAAAAA"));

        registerButton.setVisibility(View.GONE);
        sendCodeButton.setVisibility(View.GONE);
        phoneInput.setEnabled(false);
        phoneInput.setAlpha(0.4f);
    }

    private void sendCodeToApi(String phone) {
        statusText.setText("در حال ارسال کد . . .");
        sendCodeButton.setEnabled(false);
        startTimer();

        new Thread(() -> {
            try {
                URL url = new URL("https://bazicloud.com/wp-json/amncloud/v1/send-code");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("phone", phone);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes());
                os.close();

                int code = conn.getResponseCode();
                Scanner in = new Scanner(conn.getInputStream());
                StringBuilder response = new StringBuilder();
                while (in.hasNextLine()) response.append(in.nextLine());
                in.close();

                runOnUiThread(() -> {
                    if (code == 200) {
                        statusText.setText("کد ارسال شد");
                        showCodeInput();
                    } else if (code == 404) {
                        statusText.setText("کاربری با این شماره وجود ندارد ، لطفاً ثبت ‌نام کنید");
                        showRegisterForm();
                    } else {
                        statusText.setText("خطا در ارسال کد");
                        sendCodeButton.setEnabled(true);
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    statusText.setText("خطا در ارتباط با سرور");
                    sendCodeButton.setEnabled(true);
                });
            }
        }).start();
    }

    private void sendRegisterRequest(String phone, String name, String family, String email) {
        statusText.setText("در حال ثبت ‌نام . . .");
        registerButton.setEnabled(false);
        startTimer();

        new Thread(() -> {
            try {
                URL url = new URL("https://bazicloud.com/wp-json/amncloud/v1/request-registration-code");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("name", name);
                json.put("family", family);
                json.put("email", email);
                json.put("phone", phone);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes());
                os.close();

                int responseCode = conn.getResponseCode();
                Scanner in = new Scanner(conn.getInputStream());
                StringBuilder response = new StringBuilder();
                while (in.hasNextLine()) response.append(in.nextLine());
                in.close();

                JSONObject responseJson = new JSONObject(response.toString());

                runOnUiThread(() -> {
                    if (responseCode == 200 && responseJson.optBoolean("success", false)) {
                        statusText.setText("کد ارسال شد ، لطفاً آن را وارد کنید");
                        showCodeInput();
                    } else {
                        String msg = responseJson.optString("message", "ثبت‌نام با خطا مواجه شد");
                        statusText.setText(msg);
                        registerButton.setEnabled(true);
                        if (responseCode == 409) {
                            backToLoginButton.setVisibility(View.VISIBLE);
                        }
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    statusText.setText("خطا در ثبت‌نام یا ارتباط با سرور");
                    registerButton.setEnabled(true);
                });
            }
        }).start();
    }

    private void verifyCode(String phone, String code) {
        statusText.setText("در حال بررسی کد . . .");

        new Thread(() -> {
            try {
                URL url = new URL(registrationMode
                        ? "https://bazicloud.com/wp-json/amncloud/v1/complete-registration"
                        : "https://bazicloud.com/wp-json/amncloud/v1/verify-code");

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("phone", phone);
                json.put("code", code);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes());
                os.close();

                int codeResponse = conn.getResponseCode();

                runOnUiThread(() -> {
                    if (codeResponse == 200) {
                        statusText.setText("ورود موفقیت‌آمیز بود");
                        SharedPreferences prefs = getSharedPreferences("amnyar", MODE_PRIVATE);
                        prefs.edit().putBoolean("is_logged_in", true).apply();

                        startActivity(new Intent(HomePhoneLoginActivity.this, PcView.class));
                        finish();
                    } else {
                        statusText.setText("کد اشتباه یا منقضی شده");
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> statusText.setText("خطا در بررسی کد"));
            }
        }).start();
    }

    private void startTimer() {
        secondsRemaining = 59;
        timerText.setVisibility(View.VISIBLE);
        resendCodeButton.setEnabled(false);
        resendCodeButton.setAlpha(0.4f);
        resendCodeButton.setTextColor(Color.parseColor("#AAAAAA"));

        timer = new CountDownTimer(59000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                secondsRemaining--;
                timerText.setText("ارسال مجدد تا " + secondsRemaining + " ثانیه دیگر");
            }

            @Override
            public void onFinish() {
                resendCodeButton.setEnabled(true);
                resendCodeButton.setAlpha(1.0f);
                resendCodeButton.setTextColor(Color.parseColor("#000000"));
                timerText.setVisibility(View.GONE);
            }
        }.start();
    }
}
