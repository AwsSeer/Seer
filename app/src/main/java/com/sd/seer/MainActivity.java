package com.sd.seer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.snackbar.Snackbar;
import com.sd.seer.common.CommonUtil;
import com.sd.seer.common.Constants;
import com.sd.seer.helper.AndroidManager;
import com.sd.seer.helper.GoogleManager;
import com.sd.seer.model.BPM;
import com.sd.seer.model.Location;
import com.sd.seer.model.Tracking;
import com.sd.seer.model.User;
import com.sd.seer.rest.HistoryService;
import com.sd.seer.rest.ServiceFactory;
import com.sd.seer.rest.UserService;
import com.sd.seer.service.RunnerService;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import lombok.SneakyThrows;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private Activity activity;

    private TextView mName;
    private TextView mEmail;
    private LineChart mChart;
    private LinearLayout mLocationView;

    private HistoryService mHistoryService;
    private UserService mUserService;

    private Timer mTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        activity = this;

        mName = findViewById(R.id.main_name);
        mEmail = findViewById(R.id.main_email);
        mChart = findViewById(R.id.chart);
        mLocationView = findViewById(R.id.location_container);

        mHistoryService = ServiceFactory.getServiceInstance(HistoryService.class);
        mUserService = ServiceFactory.getServiceInstance(UserService.class);

        if(AndroidManager.checkAndRequestPermissions(this)){
            startGoogleSignIn();
        }
    }

    private void startGoogleSignIn() {
        GoogleManager.startSignIn(this);
    }

    @SneakyThrows
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case Constants.REQUEST_CODE_GOOGLE_START:
            case Constants.REQUEST_CODE_GOOGLE_PERMISSIONS:
                if(GoogleManager.checkAndRequestPermission(this, data)) {
                    checkRegistration();
                }
                break;
            case Constants.REQUEST_CODE_USER_REGISTRATION:
                start(CommonUtil.MAPPER.readValue(data.getStringExtra("user"), User.class));
        }
    }

    private void checkRegistration() {
        mUserService.getUser(GoogleManager.getAccount().getEmail()).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                Log.d(TAG, response.toString());
                if(response.isSuccessful()) start(response.body());
                else onFailure(call, new Exception());
            }

            @SneakyThrows
            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Intent intent = new Intent(activity, RegisterActivity.class);
                intent.putExtra("user", CommonUtil.MAPPER.writeValueAsString(new User(GoogleManager.getAccount().getEmail())));
                startActivityForResult(intent, Constants.REQUEST_CODE_USER_REGISTRATION);
            }
        });
    }

    private void start(User user) {
        mName.setText(user.getName());
        mEmail.setText(user.getEmail());

        startUpdating();

        RunnerService.start(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(AndroidManager.checkAndRequestPermissions(this)){
            startGoogleSignIn();
        }
    }

    private void startUpdating() {
        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                new Thread(() -> {
                    mHistoryService.getTracking(GoogleManager.getAccount().getEmail(), 10, 0).enqueue(new Callback<Tracking>() {
                        @Override
                        public void onResponse(Call<Tracking> call, Response<Tracking> response) {
                            if(response.body() != null) {
                                if (response.body().getBpms() != null && !response.body().getBpms().isEmpty()) {
                                    updateBpms(response.body().getBpms());
                                }
                                if (response.body().getLocations() != null && !response.body().getLocations().isEmpty()) {
                                    updateLocations(response.body().getLocations());
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<Tracking> call, Throwable t) {
                            Snackbar.make(findViewById(R.id.content), "BPM failed to load", Snackbar.LENGTH_LONG).show();
                        }
                    });
                }).start();
            }
        }, 0, Constants.PERIOD_MILLIS);
    }

    private void updateBpms(List<BPM> bpmList) {
        float minX = bpmList.stream().mapToLong(bpm -> bpm.getTime().getTime()).min().getAsLong();
        List<Entry> entries = bpmList.stream()
                .sorted((t1, t2) -> t1.getTime().compareTo(t2.getTime()))
                .map(bpm -> new Entry((bpm.getTime().getTime() - minX) / 1000, bpm.getBpm()))
                .collect(Collectors.toList());
        LineDataSet dataSet = new LineDataSet(entries, "BPM"); // add entries to dataset
        dataSet.setColor(Color.CYAN);
        dataSet.setValueTextColor(Color.BLUE); // styling, ...
        LineData lineData = new LineData(dataSet);
        runOnUiThread(() -> {
            mChart.setData(lineData);
            mChart.invalidate();
        });
    }

    private void updateLocations(List<Location> locationList) {
        mLocationView.removeAllViews();
        locationList.forEach(location -> {
            CardView card = new CardView(activity);
            CardView.LayoutParams layoutParams = new CardView.LayoutParams(
                    CardView.LayoutParams.MATCH_PARENT, CardView.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(0, 0, 0, CommonUtil.dpToPx(8));
            card.setLayoutParams(layoutParams);
            card.setCardElevation(CommonUtil.dpToPx(4));
            card.setMaxCardElevation(CommonUtil.dpToPx(4));
            card.setRadius(CommonUtil.dpToPx(4));
            card.setContentPadding(CommonUtil.dpToPx(16), CommonUtil.dpToPx(16), CommonUtil.dpToPx(16), CommonUtil.dpToPx(16));
            card.setOnClickListener((v -> {
                Uri gmmIntentUri = Uri.parse(String.format("geo:%f,%f?z=%d",
                        location.getLatitude(), location.getLongitude(), 21));
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                }
            }));

            LinearLayout innerLayout = new LinearLayout(activity);
            innerLayout.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams innerLayoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            innerLayoutParams.setMargins(0, 0, 0, CommonUtil.dpToPx(2));
            innerLayout.setLayoutParams(innerLayoutParams);

            TextView heading = new TextView(activity);
            ViewGroup.LayoutParams textLayoutParams = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                    );
            heading.setLayoutParams(textLayoutParams);
            heading.setText(new SimpleDateFormat("dd MMM h:ma").format(location.getTime()));
            heading.setTextAppearance(R.style.TextAppearance_AppCompat_Large);
            innerLayout.addView(heading);

            TextView body = new TextView(activity);
            body.setLayoutParams(textLayoutParams);
            body.setText(String.format("Location : %f, %f",
                    location.getLatitude(), location.getLongitude()));
            body.setTextAppearance(R.style.TextAppearance_AppCompat_Medium);
            innerLayout.addView(body);

            card.addView(innerLayout);
            mLocationView.addView(card);
        });
        runOnUiThread(() -> mLocationView.invalidate());
    }

    private void stopUpdating() {
        if(mTimer != null) mTimer.cancel();
    }

}