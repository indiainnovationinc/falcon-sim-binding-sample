package com.falcon.simbindingsample;

import android.Manifest;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.falconfsauth.sdk.DeviceBinding;
import com.falconfsauth.sdk.SendSMSCallback;
import com.falconfsauth.sdk.Utils.ApiUtils;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.gson.Gson;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_SEND_SMS = 1;

    Boolean hasRequiredPermissions = false;

    private void checkPermissions() {
        boolean hasSendSmsPermission = ContextCompat.checkSelfPermission(
                this, Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED;

        boolean hasReadPhoneStatePermission = ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED;

        if (!hasSendSmsPermission || !hasReadPhoneStatePermission) {
            // Request missing permissions
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.SEND_SMS,
                            Manifest.permission.READ_PHONE_STATE
                    },
                    PERMISSION_REQUEST_SEND_SMS // You can use any unique request code
            );
        } else {
            hasRequiredPermissions = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_SEND_SMS) {
            // Check if both permissions are granted
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                hasRequiredPermissions = true;
            } else {
                Toast.makeText(MainActivity.this, "Can not proceed with device binding without mandatory permissions.", Toast.LENGTH_SHORT).show();
            }
        }
    }



    private void navigateToSimSelectionActivity(DeviceBindingResponse bindingResponse, String mobileNumber) {
        Intent intent = new Intent(this, SimSelectionActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("virtualMobileNumber", bindingResponse.virtualMobileNumber);
        bundle.putString("keyword", bindingResponse.keyword);
        bundle.putString("token", bindingResponse.token);
        bundle.putString("mobileNumber", mobileNumber);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final EditText mobileNumberInput = findViewById(R.id.editTextPhone);
        Button submitButton = findViewById(R.id.submitButton);
        checkPermissions();

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mobileNumber = mobileNumberInput.getText().toString();
                if (!hasRequiredPermissions) {
                    checkPermissions();
                } else if (mobileNumber.isEmpty() || mobileNumber.length() < 10) {
                    Toast.makeText(MainActivity.this, "Please enter a mobile number", Toast.LENGTH_SHORT).show();
                } else {
                    DeviceBinding.initiateDeviceBinding(MainActivity.this, Constants.enterpriseId, Constants.authorization, mobileNumber, Constants.userId, new ApiUtils.Callback<String>() {
                        @Override
                        public void onSuccess(String response, Integer integer) {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(MainActivity.this, response, Toast.LENGTH_SHORT).show();
                                }
                            });
                            Gson gson = new Gson();
                            DeviceBindingResponse bindingResponse = gson.fromJson(response, DeviceBindingResponse.class);
                            navigateToSimSelectionActivity(bindingResponse, mobileNumber);
                        }

                        @Override
                        public void onError(Exception error) {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                }
            }
        });
    }
}