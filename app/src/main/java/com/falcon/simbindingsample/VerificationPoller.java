package com.falcon.simbindingsample;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import com.falconfsauth.sdk.DeviceBinding;
import com.falconfsauth.sdk.Utils.ApiUtils;
import com.google.gson.Gson;

public class VerificationPoller {

    private static final int MAX_RETRIES = 6;
    private static final int POLL_INTERVAL_MS = 10000; // 10 seconds

    private final Context appContext;
    private final String enterpriseId;
    private final String authorization;
    private final String mobileNumber;
    private final String userId;
    private final String token;
    private final ApiUtils.Callback<String> callback;
    private int retryCount;

    public VerificationPoller(
            Context appContext,
            String enterpriseId,
            String authorization,
            String mobileNumber,
            String userId,
            String token,
            ApiUtils.Callback<String> callback
    ) {
        this.appContext = appContext;
        this.enterpriseId = enterpriseId;
        this.authorization = authorization;
        this.mobileNumber = mobileNumber;
        this.userId = userId;
        this.token = token;
        this.callback = callback;
        this.retryCount = 0;
    }

    public void startPolling() {
        pollVerificationStatus();
    }

    private void pollVerificationStatus() {
        if (retryCount >= MAX_RETRIES) {
            callback.onError(new Exception("Max retry limit reached"));
            return;
        }

        // Create a new HandlerThread with a Looper
        HandlerThread handlerThread = new HandlerThread("VerificationPollerThread");
        handlerThread.start();

        Handler handler = new Handler(handlerThread.getLooper());

        handler.post(new Runnable() {
            @Override
            public void run() {
                DeviceBinding.checkBindingStatus(
                        appContext,
                        enterpriseId,
                        authorization,
                        mobileNumber,
                        userId,
                        token,
                        new ApiUtils.Callback<String>() {
                            @Override
                            public void onSuccess(String s, Integer integer) {
                                Gson gson = new Gson();
                                BindingStatusResponse bindingStatusResponse = gson.fromJson(s, BindingStatusResponse.class);
                                if (bindingStatusResponse.state.equals("VERIFIED")) {
                                    callback.onSuccess(s, integer);
                                } else {
                                    retryCount++;
                                    new Handler(Looper.getMainLooper()).postDelayed(
                                            VerificationPoller.this::pollVerificationStatus,
                                            POLL_INTERVAL_MS
                                    );
                                }
                            }

                            @Override
                            public void onError(Exception error) {
                                callback.onError(error);
                            }
                        }
                );
            }
        });
    }

}