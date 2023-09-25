package com.falcon.simbindingsample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.falconfsauth.sdk.DeviceBinding;

public class BindingSuccessActivity extends AppCompatActivity {
    private TextView totpTextView;
    private Button generateTotpButton;
    private void generateTotp() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String deviceToken = extras.getString("deviceToken");
            String totpSecret = extras.getString("totpSecret");
            String requestToken = DeviceBinding.generateRequestToken(deviceToken, totpSecret);
            totpTextView.setText(requestToken);
            Toast.makeText(BindingSuccessActivity.this, "Request Token Generated: " +  requestToken, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_binding_success);
        totpTextView = findViewById(R.id.totpTextView);
        generateTotpButton = findViewById(R.id.submitButton);

        generateTotp();
        generateTotpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                generateTotp();
            }
        });
    }
}