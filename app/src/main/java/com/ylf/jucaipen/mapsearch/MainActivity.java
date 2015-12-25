package com.ylf.jucaipen.mapsearch;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;

public class MainActivity extends Activity implements View.OnClickListener, BaiduMap.OnMapTouchListener, OnGetPoiSearchResultListener, BaiduMap.OnMapClickListener {
    private MapView mv;
    private Button btn_search;
    private EditText et_where;
    private BaiduMap map;
    private PoiSearch search;
    private LocationClient mLocationClient;
    private String city;
    private BDLocationListener listener = new BDLocationListener() {
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
             city=bdLocation.getCity();
            String where = bdLocation.getProvince() + " " + bdLocation.getCity() + " " + bdLocation.getAddrStr();
            locationMyPosition(bdLocation.getLatitude(),bdLocation.getLongitude());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.content_main);
        initView();
        initLocation();
    }

    private void initLocation() {
        mLocationClient = new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(listener);
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy
        );//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        option.setCoorType("bd09ll");//可选，默认gcj02，设置返回的定位结果坐标系
        int span = 1000;
        option.setScanSpan(span);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
        option.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
        option.setOpenGps(true);//可选，默认false,设置是否使用gps
        option.setLocationNotify(true);//可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
        option.setIsNeedLocationDescribe(true);//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        option.setIsNeedLocationPoiList(true);//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        option.setIgnoreKillProcess(false);//可选，默认false，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认杀死
        option.SetIgnoreCacheException(false);//可选，默认false，设置是否收集CRASH信息，默认收集
        option.setEnableSimulateGps(false);//可选，默认false，设置是否需要过滤gps仿真结果，默认需要
        mLocationClient.setLocOption(option);
        mLocationClient.start();
    }

    private void initView() {
        mv = (MapView) findViewById(R.id.mv);
        btn_search = (Button) findViewById(R.id.btn_search);
        et_where = (EditText) findViewById(R.id.et_where);
        btn_search.setOnClickListener(this);
        map = mv.getMap();
        //是否打开交通地图
        map.setTrafficEnabled(true);
        //是否显示热力图
        map.setBaiduHeatMapEnabled(false);

        //设置地图显示类型（MAP_TYPE_NORMAL  普通模式     MAP_TYPE_SATELLITE  卫星模式 ）
        map.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        map.setOnMapTouchListener(this);
        map.setOnMapClickListener(this);
        //定义Maker坐标点
        LatLng lng = new LatLng(39.963175, 116.400244);
        //构建Marker图标
        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher);
        //构建MarkerOption，用于在地图上添加Marker
        OverlayOptions overlayOptions = new MarkerOptions().position(lng).icon(descriptor);
        map.addOverlay(overlayOptions);
        search = PoiSearch.newInstance();
        search.setOnGetPoiSearchResultListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mv.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mv.onPause();
        search.destroy();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mv.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_search:
                String wh=et_where.getText().toString();
                if(wh.length()>0) {
                    search.searchInCity(new PoiCitySearchOption().city(city).keyword(wh));
                }else {
                    Toast.makeText(this,"无数据",Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    @Override
    public void onTouch(MotionEvent motionEvent) {
        float y = motionEvent.getY();
        float x = motionEvent.getX();
    }

    @Override
    public void onGetPoiResult(PoiResult poiResult) {
        //获取POI检索结果
        if (poiResult.error == SearchResult.ERRORNO.NO_ERROR) {
            //检索成功
            //遍历所有POI，找到类型
            for (PoiInfo info : poiResult.getAllPoi()) {
                double lat=info.location.latitude;
                double alt=info.location.longitude;
                locationMyPosition(lat,alt);
                String address = info.address;
                Log.i("111", "ad:" + address);
            }
        } else {
            //检索失败
        }
    }

    @Override
    public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {
        //获取Place详情页检索结果
    }

    public void locationMyPosition(double lat,double lon) {
        // 开启定位图层
        map.setMyLocationEnabled(true);
        // 构造定位数据
        MyLocationData locationData = new MyLocationData.Builder()
                // 此处设置开发者获取到的方向信息，顺时针0-360
                .direction(100).latitude(lat).longitude(lon).build();
        // 设置定位数据
        map.setMyLocationData(locationData);
        // 设置定位图层的配置（定位模式，是否允许方向信息，用户自定义定位图标）
        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher);
        MyLocationConfiguration configuration = new MyLocationConfiguration(MyLocationConfiguration.LocationMode.FOLLOWING, true, descriptor);
        map.setMyLocationConfigeration(configuration);
        map.setMyLocationEnabled(false);
    }

    @Override
    public void onMapClick(LatLng latLng) {
     locationMyPosition(latLng.latitude,latLng.longitude);
    }

    @Override
    public boolean onMapPoiClick(MapPoi mapPoi) {
        return false;
    }
}
