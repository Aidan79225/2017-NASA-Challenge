package com.a2017taipeinasachallenge.aaa;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {
    private final String SEND_POINTS_URL = “ip/FightFires/map/postAreaData";
    private final String GET_POINTS_URL = “ip/FightFires/map/getAreaData";
    final int MY_PERMISSION_ACCESS_COURSE_LOCATION = 0x1;
    private GoogleMap mMap;
    private Double longitude = 0.0;
    private Double latitude = 0.0;
    private Sensor mSensor;
    private Sensor aSensor;
    private float[] mValues;
    private long time = 0L;
    private TextView printTextView,setTextView,clearTextView,locationTextView,getTextView,demoTextView;
    private EditText idEditText;
    private TextView sendTextView;
    private List<LatLng> points =  new ArrayList<>();
    private List<Polygon> polygons = new ArrayList<>();
    private double radius = 1;
    private Marker marker;
    private Handler handler;
    private SensorManager manager;
    private String bestProvider = LocationManager.GPS_PROVIDER;
    private boolean isDemo = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        handler = new Handler(getApplicationContext().getMainLooper());
        initMapFragment();
        findView();
        setViewClick();
        getPermissions();
        getSensor();
    }
    private void initMapFragment(){
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }
    private void findView(){
        printTextView = (TextView)findViewById(R.id.printTextView);
        setTextView = (TextView)findViewById(R.id.setTextView);
        clearTextView = (TextView)findViewById(R.id.clearTextView);
        locationTextView = (TextView)findViewById(R.id.locationTextView);
        sendTextView = (TextView)findViewById(R.id.sendTextView);
        idEditText = (EditText)findViewById(R.id.idEditText);
        getTextView = (TextView)findViewById(R.id.getTextView);
        demoTextView = (TextView)findViewById(R.id.demoTextView);
    }

    private void setViewClick(){
        printTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                print();
            }
        });
        setTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setmValues();
            }
        });
        clearTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clear();
            }
        });
        locationTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getPermissions();
            }
        });
        sendTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendData();
            }
        });
        getTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getData();
            }
        });
        demoTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                demoTime();
            }
        });
    }
    private void demoTime(){
        if(isDemo){
            isDemo = false;
        }else{
            isDemo = true;
            latitude = 25.021909;
            longitude = 121.534992;
            addNewMark(latitude, longitude);
        }
    }
    private void sendData(){
        if(points.size()<2)Toast.makeText(this,"請標示兩個點",Toast.LENGTH_SHORT).show();
        String id = idEditText.getText().toString();
        if(id.length() == 0)Toast.makeText(this,"請輸入Id",Toast.LENGTH_SHORT).show();
        JSONObject jsonObject = new JSONObject();
        try{
            jsonObject.put("id",id);
            jsonObject.put("firstx",points.get(0).latitude);
            jsonObject.put("firsty",points.get(0).longitude);
            jsonObject.put("secondx",points.get(1).latitude);
            jsonObject.put("secondy",points.get(1).longitude);
            jsonObject.put("myx",latitude);
            jsonObject.put("myy",longitude);
            ConnectService.sendPost(SEND_POINTS_URL, jsonObject.toString(), new ConnectService.Callback() {
                @Override
                public void callback(boolean success, String json) {
                    if(success){
                        Toast.makeText(MapsActivity.this,"上傳成功",Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(MapsActivity.this,"上傳失敗",Toast.LENGTH_SHORT).show();
                    }
                }
            },handler);


        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void getData(){
        ConnectService.sendGet(GET_POINTS_URL, new ConnectService.Callback() {
            @Override
            public void callback(boolean success, String json) {
                if(success){
                    try {
                        clear();
                        parseData(new JSONArray(json));
                        Toast.makeText(MapsActivity.this,"下載成功",Toast.LENGTH_SHORT).show();
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                }else{
                    Toast.makeText(MapsActivity.this,"下載失敗",Toast.LENGTH_SHORT).show();
                }
            }
        },handler);
    }
    private void parseData(JSONArray array){
        Random random = new Random();
        for(int i =0;i<array.length();i++){
            try{
                JSONObject jsonObject = array.getJSONObject(i);
                List<LatLng> points =  new ArrayList<>();
                points.add(new LatLng(jsonObject.getDouble("firstx"),jsonObject.getDouble("firsty")));
                points.add(new LatLng(jsonObject.getDouble("secondx"),jsonObject.getDouble("secondy")));
                points.add(new LatLng(jsonObject.getDouble("myx"),jsonObject.getDouble("myy")));

                int color = random.nextInt() % 0x00FFFFFF;
                color += 0x66000000;
                print(color,points);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    private void setmValues(){
        double x = mValues[0];
        x = x * Math.PI / 180.0 ;
        points.add(new LatLng( Math.cos(x) * radius + latitude,  Math.sin(x) * radius + longitude));
    }
    private void print(int color,List<LatLng> points){
        Polygon polygon = mMap.addPolygon(new PolygonOptions().addAll(points)
                .strokeColor(0x00FFFFFF)
                .fillColor(color));
        polygons.add(polygon);
    }
    private void print(){
        Polygon polygon = mMap.addPolygon(new PolygonOptions().addAll(points).add(new LatLng(latitude,longitude))
                .strokeColor(0x00FFFFFF)
                .fillColor(0x660000FF));
        polygons.add(polygon);
    }
    private void clear(){
        points.clear();
        for(Polygon polygon : polygons){
            polygon.remove();
        }
        polygons.clear();
    }
    private void getPermissions(){
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            //如果GPS或網路定位開啟，呼叫locationServiceInitial()更新位置
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSION_ACCESS_COURSE_LOCATION);
            } else {
                locationServiceInitial();
            }
        } else {
            Toast.makeText(this, "請開啟定位服務", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));    //開啟設定頁面
        }
    }

    private void getSensor() {
        SensorManager manager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //得到方向传感器
        mSensor = manager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        SensorEventListener mListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                mValues = event.values;
                String temp = "";
                temp = "x : " + mValues[0] + ", y : " + mValues[1] + ", z : " + mValues[2];
                if (Calendar.getInstance().getTimeInMillis() - time > 1000) {
                    Log.e(MapsActivity.class.getName(), temp);
                    time = Calendar.getInstance().getTimeInMillis();
                }

            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                //精确改变，适用游戏

            }
        };
        manager.registerListener(mListener, mSensor, SensorManager.SENSOR_DELAY_GAME);


    }

    private LocationManager lms;

    private void locationServiceInitial() {
        lms = (LocationManager) getSystemService(LOCATION_SERVICE);    //取得系統定位服務
        try {
            Location location = lms.getLastKnownLocation(LocationManager.GPS_PROVIDER);    //使用GPS定位座標
            lms.requestLocationUpdates(bestProvider, 1000, 1, this);
            getLocation(location);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void getLocation(Location location) {    //將定位資訊顯示在畫面中
        if(isDemo)return;
        if (location != null) {
            longitude = location.getLongitude();    //取得經度
            latitude = location.getLatitude();    //取得緯度
            addNewMark(latitude,longitude);
        } else {
            Toast.makeText(this, "無法定位座標", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_ACCESS_COURSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationServiceInitial();
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {
                    Toast.makeText(this, "開啟權限失敗", Toast.LENGTH_LONG).show();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                break;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (googleMap == null) return;
        mMap = googleMap;
        addNewMark(latitude,longitude);
    }

    private void addNewMark(double latitude,double longitude){
        if(mMap == null)return;
        if(marker != null)marker.remove();

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(latitude, longitude);

        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        marker = mMap.addMarker(new MarkerOptions().position(sydney).title("My Location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.e(MapsActivity.class.getName(),"location change");
        getLocation(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
