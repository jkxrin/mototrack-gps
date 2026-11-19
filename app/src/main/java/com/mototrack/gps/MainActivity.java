package com.mototrack.gps;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;

import com.mototrack.gps.view.SuperbikeGaugeView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity implements LocationListener {

    private static final int PERMISSION_REQ_CODE = 4001;

    private SuperbikeGaugeView gaugeView;
    private TextView tvDistance;
    private TextView tvMaxSpeed;
    private TextView tvAvgSpeed;
    private TextView tvGpsStatus;
    private TextView tvRouteLog;
    private Button btnResetTrip;
    private Button btnSaveTrip;

    private LocationManager locationManager;
    private Location lastValidLocation = null;

    private double totalDistanceMeters = 0.0;
    private float maxSpeedKmh = 0f;
    private long movingTimeSeconds = 0;
    private long tripStartTime = 0;
    private boolean isTracking = false;

    private StringBuilder routePointsLog = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gaugeView = findViewById(R.id.gaugeView);
        tvDistance = findViewById(R.id.tvDistance);
        tvMaxSpeed = findViewById(R.id.tvMaxSpeed);
        tvAvgSpeed = findViewById(R.id.tvAvgSpeed);
        tvGpsStatus = findViewById(R.id.tvGpsStatus);
        tvRouteLog = findViewById(R.id.tvRouteLog);
        btnResetTrip = findViewById(R.id.btnResetTrip);
        btnSaveTrip = findViewById(R.id.btnSaveTrip);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        btnResetTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetTrip();
                Toast.makeText(MainActivity.this, "Trip Reset!", Toast.LENGTH_SHORT).show();
            }
        });

        btnSaveTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTrip();
            }
        });

        resetTrip();
        checkPermissionsAndStart();
    }

    private void checkPermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                }, PERMISSION_REQ_CODE);
                return;
            }
        }
        startLocationUpdates();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                tvGpsStatus.setText("● PERMISSION DENIED");
                tvGpsStatus.setTextColor(0xFFFF1744);
            }
        }
    }

    private void startLocationUpdates() {
        try {
            if (locationManager != null) {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            1000L,
                            1.0f,
                            this
                    );
                    tvGpsStatus.setText("● GPS ACTIVE (HIGH ACCURACY)");
                    tvGpsStatus.setTextColor(0xFF00E676);
                } else {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            1000L,
                            1.0f,
                            this
                    );
                    tvGpsStatus.setText("● NETWORK GPS (LOW ACCURACY)");
                    tvGpsStatus.setTextColor(0xFFFFD600);
                }
            }
        } catch (SecurityException e) {
            tvGpsStatus.setText("● SEC ERROR");
            tvGpsStatus.setTextColor(0xFFFF1744);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location == null) return;

        // GPS Drift Filter: ignore inaccurate fixes (> 20 meters accuracy)
        if (location.hasAccuracy() && location.getAccuracy() > 20.0f) {
            return;
        }

        // Calculate speed in KM/H strictly
        float currentSpeedKmh = 0f;
        if (location.hasSpeed()) {
            currentSpeedKmh = location.getSpeed() * 3.6f;
        }

        // Stationary filter (speed < 1.8 km/h is considered noise / stationary)
        if (currentSpeedKmh < 1.8f) {
            currentSpeedKmh = 0f;
        }

        if (lastValidLocation != null) {
            float deltaMeters = lastValidLocation.distanceTo(location);

            // Filter out small GPS jitter when stationary
            if (currentSpeedKmh > 0f && deltaMeters >= 1.5f) {
                totalDistanceMeters += deltaMeters;
                movingTimeSeconds += 1;
            }
        }
        lastValidLocation = location;

        if (currentSpeedKmh > maxSpeedKmh) {
            maxSpeedKmh = currentSpeedKmh;
        }

        // Update Superbike Gauge
        gaugeView.setSpeed(currentSpeedKmh);

        // Calculate total KM and average KM/H
        double totalKm = totalDistanceMeters / 1000.0;
        float avgSpeedKmh = 0f;
        if (movingTimeSeconds > 0) {
            avgSpeedKmh = (float) ((totalDistanceMeters / movingTimeSeconds) * 3.6);
        }

        tvDistance.setText(String.format(Locale.US, "%.2f", totalKm));
        tvMaxSpeed.setText(String.valueOf(Math.round(maxSpeedKmh)));
        tvAvgSpeed.setText(String.valueOf(Math.round(avgSpeedKmh)));

        // Feed log
        String timeStr = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date(location.getTime()));
        String pointInfo = String.format(Locale.US, "[%s] Lat: %.5f, Lon: %.5f | Speed: %.1f km/h\n",
                timeStr, location.getLatitude(), location.getLongitude(), currentSpeedKmh);
        routePointsLog.insert(0, pointInfo);
        tvRouteLog.setText(routePointsLog.toString());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {
        tvGpsStatus.setText("● GPS ENABLED");
        tvGpsStatus.setTextColor(0xFF00E676);
    }

    @Override
    public void onProviderDisabled(String provider) {
        tvGpsStatus.setText("● GPS DISABLED");
        tvGpsStatus.setTextColor(0xFFFF1744);
    }

    private void resetTrip() {
        totalDistanceMeters = 0.0;
        maxSpeedKmh = 0f;
        movingTimeSeconds = 0;
        tripStartTime = System.currentTimeMillis();
        lastValidLocation = null;
        routePointsLog = new StringBuilder();

        gaugeView.setSpeed(0f);
        tvDistance.setText("0.00");
        tvMaxSpeed.setText("0");
        tvAvgSpeed.setText("0");
        tvRouteLog.setText("Session reset.\nReady to track trip metrics.");
    }

    private void saveTrip() {
        double totalKm = totalDistanceMeters / 1000.0;
        String summary = String.format(Locale.US,
                "Trip Saved!\nDistance: %.2f km\nMax Speed: %d km/h\nMoving Time: %d sec",
                totalKm, Math.round(maxSpeedKmh), movingTimeSeconds);
        Toast.makeText(this, summary, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }
}
