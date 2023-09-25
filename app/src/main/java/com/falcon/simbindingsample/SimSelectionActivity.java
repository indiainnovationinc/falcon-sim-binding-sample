package com.falcon.simbindingsample;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.falconfsauth.sdk.DeviceBinding;
import com.falconfsauth.sdk.SendSMSCallback;
import com.falconfsauth.sdk.Utils.ApiUtils;
import com.google.gson.Gson;

public class SimSelectionActivity extends AppCompatActivity {
    private RadioGroup radioGroup;
    private Button submitBtn;
    private TextView simSelectionInfoText;
    private LinearLayout stepProgressContainer;
    private ProgressBar step1Indicator, step2Indicator;

    @SuppressLint("MissingPermission")
    private void populateSimCards() {
        SubscriptionManager subscriptionManager = SubscriptionManager.from(this);
        SubscriptionInfo activeSubscriptionInfo = null;
        int simCount = 0;

        for (SubscriptionInfo subscriptionInfo : subscriptionManager.getActiveSubscriptionInfoList()) {
            simCount++;
            activeSubscriptionInfo = subscriptionInfo;
            RadioButton radioButton = new RadioButton(this);
            radioButton.setId(simCount);
            radioButton.setText(activeSubscriptionInfo.getCarrierName());
            radioGroup.addView(radioButton);
        }

        // Disable the submit button if no SIM cards are available
        if (simCount == 0) {
            submitBtn.setEnabled(false);
        }
    }

    private void navigateToBindingSuccess(String deviceToken, String totpSecret) {
        Intent intent = new Intent(this, BindingSuccessActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("deviceToken", deviceToken);
        bundle.putString("totpSecret", totpSecret);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sim_selection);
        Bundle extras = getIntent().getExtras();
        String mobileNumber = extras.getString("mobileNumber");

        radioGroup = findViewById(R.id.radioGroup);
        submitBtn = findViewById(R.id.submitBtn);
        simSelectionInfoText = findViewById(R.id.simSelectionTitle);
        simSelectionInfoText.setText("Select a sim slot with the number " + mobileNumber);
        step1Indicator = findViewById(R.id.step1Indicator);
        step2Indicator = findViewById(R.id.step2Indicator);
        stepProgressContainer = findViewById(R.id.stepProgressContainer);
        populateSimCards();

        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int selectedId = radioGroup.getCheckedRadioButtonId();
                submitBtn.setEnabled(false);
                if (selectedId == -1) {
                    Toast.makeText(SimSelectionActivity.this, "Please select a SIM card.", Toast.LENGTH_SHORT).show();
                } else {
                    RadioButton selectedRadioButton = findViewById(selectedId);
                    String selectedCarrierName = selectedRadioButton.getText().toString();
                    if (extras != null) {
                        String virtualMobileNumber = extras.getString("virtualMobileNumber");
                        String keyword = extras.getString("keyword");
                        String token = extras.getString("token");
                        stepProgressContainer.setVisibility(View.VISIBLE);
                        step1Indicator.setVisibility(View.VISIBLE);
                        DeviceBinding.sendRegistrationSMS(SimSelectionActivity.this, virtualMobileNumber, keyword + " " + token, selectedId, true, new SendSMSCallback() {
                            @Override
                            public void onSuccess(String result) {
                                step1Indicator.setVisibility(View.GONE);
                                step2Indicator.setVisibility(View.VISIBLE);
                                VerificationPoller poller = new VerificationPoller(
                                        SimSelectionActivity.this,
                                        Constants.enterpriseId,
                                        Constants.authorization,
                                        mobileNumber,
                                        Constants.userId,
                                        token,
                                        new ApiUtils.Callback<String>() {
                                            @Override
                                            public void onSuccess(String s, Integer integer) {
                                                Gson gson = new Gson();
                                                BindingStatusResponse bindingStatusResponse = gson.fromJson(s, BindingStatusResponse.class);
                                                runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        step2Indicator.setVisibility(View.GONE);
                                                        navigateToBindingSuccess(bindingStatusResponse.deviceToken, bindingStatusResponse.secret);
                                                    }
                                                });
                                            }

                                            @Override
                                            public void onError(Exception error) {
                                                runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        Toast.makeText(SimSelectionActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                            }
                                        }
                                );
                                poller.startPolling();
                            }

                            @Override
                            public void onError(String error) {
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        Toast.makeText(SimSelectionActivity.this, error, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        });
                    } else {
                        Toast.makeText(SimSelectionActivity.this, "Required parameters not received from device binding API", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }
}