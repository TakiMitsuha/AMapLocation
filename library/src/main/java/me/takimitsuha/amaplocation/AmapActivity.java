package me.takimitsuha.amaplocation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.GeocodeSearch.OnGeocodeSearchListener;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;

/**
 * Created by Taki on 2017/2/4.
 */
public class AmapActivity extends CheckPermissionsActivity implements AMapLocationListener, AMap.OnCameraChangeListener, View.OnClickListener, LocationSource, OnGeocodeSearchListener {
    private AMap mAMap;
    private MapView mMapView;
    private OnLocationChangedListener mOnLocationChangedListener;
    private LatLng mLatLng;
    private Marker centerMarker;
    private BitmapDescriptor movingDescriptor, chooseDescriptor, successDescriptor;
    private ValueAnimator animator;
    private GeocodeSearch geocodeSearch;
    private AMapLocationClient mLocationClient;
    private AMapLocationClientOption mLocationOption = new AMapLocationClientOption();
    private Point mPoint;
    private RelativeLayout rlTitle;
    private FrameLayout containerLayout;
    private TextView tvLocation;

    private boolean isMovingMarker = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_amap);
        mMapView = (MapView) findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);
        tvLocation = (TextView) findViewById(R.id.tv_location);

        initAmap();
        initUI();
        setUpLocationStyle();
    }

    private void initUI() {
        findViewById(R.id.iv_locate).setOnClickListener(this);
        findViewById(R.id.iv_back).setOnClickListener(this);
        findViewById(R.id.iv_right).setOnClickListener(this);
        rlTitle = (RelativeLayout) findViewById(R.id.rl_title);
        containerLayout = (FrameLayout) findViewById(R.id.container);
        introAnimPrepare();
    }

    private void initAmap() {
        if (mAMap == null) {
            mAMap = mMapView.getMap();
        }
        mAMap.setLocationSource(this);// 设置定位监听
        mAMap.setMyLocationEnabled(true);
        mAMap.getUiSettings().setZoomControlsEnabled(false);

        mAMap.setMyLocationType(AMap.LOCATION_TYPE_LOCATE);
        CameraUpdate cameraUpdate = CameraUpdateFactory.zoomTo(15);
        mAMap.moveCamera(cameraUpdate);

        movingDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.ic_location_choose);
        chooseDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.ic_location_choose);
        successDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.ic_location_choose);

        geocodeSearch = new GeocodeSearch(this);
        geocodeSearch.setOnGeocodeSearchListener(this);

        mAMap.setOnMarkerDragListener(new AMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
                hideLocationView();
            }

            @Override
            public void onMarkerDrag(Marker marker) {

            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                LatLng latLng = marker.getPosition();
                LatLonPoint point = new LatLonPoint(latLng.latitude, latLng.longitude);
                RegeocodeQuery query = new RegeocodeQuery(point, 50, GeocodeSearch.AMAP);
                geocodeSearch.getFromLocationAsyn(query);
                centerMarker.setIcon(chooseDescriptor);
                showLocationView();
                mPoint = mAMap.getProjection().toScreenLocation(centerMarker.getPosition());
            }
        });
        mAMap.setOnMapClickListener(new AMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                LatLonPoint point = new LatLonPoint(latLng.latitude, latLng.longitude);
                RegeocodeQuery query = new RegeocodeQuery(point, 50, GeocodeSearch.AMAP);
                geocodeSearch.getFromLocationAsyn(query);
                centerMarker.setIcon(chooseDescriptor);
                centerMarker.setPosition(latLng);
                showLocationView();
                mPoint = mAMap.getProjection().toScreenLocation(centerMarker.getPosition());
            }
        });
    }

    private void setUpLocationStyle() {
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationIcon(null);
        myLocationStyle.strokeWidth(0);
        myLocationStyle.radiusFillColor(Color.TRANSPARENT);
        mAMap.setMyLocationStyle(myLocationStyle);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
        deactivate();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        mMapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation != null && aMapLocation.getErrorCode() == 0) {
            if (mOnLocationChangedListener != null) {
                mOnLocationChangedListener.onLocationChanged(aMapLocation);
            }
            mLatLng = new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude());
            tvLocation.setText(aMapLocation.getProvince() + aMapLocation.getCity() + aMapLocation.getDistrict() + aMapLocation.getPoiName());
            mAMap.clear();//清除系统默认的定位蓝点
            addChooseMarker();
            LatLng latLng = new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude());
            mAMap.addCircle(new CircleOptions().
                    center(latLng).
                    radius(50).
                    fillColor(Color.argb(10, 1, 1, 1)).
                    strokeColor(Color.argb(10, 1, 1, 1)).
                    strokeWidth(10));
        }
    }

    private void addChooseMarker() {
        MarkerOptions centerMarkerOption = new MarkerOptions().position(mLatLng).icon(chooseDescriptor);
        centerMarker = mAMap.addMarker(centerMarkerOption);
        centerMarker.setPositionByPixels(mMapView.getWidth() / 2, mMapView.getHeight() / 2);
        centerMarker.setDraggable(true);
        LatLng latLng = centerMarker.getPosition();
        mPoint = mAMap.getProjection().toScreenLocation(latLng);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                CameraUpdate update = CameraUpdateFactory.zoomTo(17f);
                mAMap.animateCamera(update, 1000, new AMap.CancelableCallback() {
                    @Override
                    public void onFinish() {
                        mAMap.setOnCameraChangeListener(AmapActivity.this);
                    }

                    @Override
                    public void onCancel() {
                    }
                });
            }
        }, 1000);
    }

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        mOnLocationChangedListener = onLocationChangedListener;
        //初始化定位
        mLocationClient = new AMapLocationClient(getApplicationContext());
        //设置定位模式为AMapLocationMode.Hight_Accuracy，高精度模式。
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置定位模式为AMapLocationMode.Battery_Saving，低功耗模式。
//        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Battery_Saving);
//        //设置定位模式为AMapLocationMode.Device_Sensors，仅设备模式。
//        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Device_Sensors);
        //获取一次定位结果：
        //该方法默认为false。
        mLocationOption.setOnceLocation(true);

        //获取最近3s内精度最高的一次定位结果：
        //设置setOnceLocationLatest(boolean b)接口为true，启动定位时SDK会返回最近3s内精度最高的一次定位结果。如果设置其为true，setOnceLocation(boolean b)接口也会被设置为true，反之不会，默认为false。
        mLocationOption.setOnceLocationLatest(true);
        //设置定位间隔,单位毫秒,默认为2000ms，最低1000ms。
        mLocationOption.setInterval(1000);
        //设置是否返回地址信息（默认返回地址信息）
        mLocationOption.setNeedAddress(true);
        //设置是否强制刷新WIFI，默认为true，强制刷新。
        mLocationOption.setWifiScan(false);
        //设置是否允许模拟位置,默认为false，不允许模拟位置
        mLocationOption.setMockEnable(false);
        //单位是毫秒，默认30000毫秒，建议超时时间不要低于8000毫秒。
        mLocationOption.setHttpTimeOut(20000);
        //关闭缓存机制
        mLocationOption.setLocationCacheEnable(false);
        //给定位客户端对象设置定位参数
        mLocationClient.setLocationOption(mLocationOption);
        mLocationClient.setLocationListener(this);
        //启动定位
        mLocationClient.startLocation();
    }

    public void deactivate() {
        if (null != mLocationClient) {
            mLocationClient.onDestroy();
            mLocationClient = null;
            mLocationOption = null;
        }
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        int width = mMapView.getWidth() / 2;
        int height = mMapView.getHeight() / 2;
        if (width == mPoint.x && height == mPoint.y) {
            if (centerMarker != null) {
                animMarker();
            }
        } else {
            centerMarker.setIcon(chooseDescriptor);
            centerMarker.setPositionByPixels(mPoint.x, mPoint.y);
        }
    }

    @Override
    public void onCameraChangeFinish(CameraPosition cameraPosition) {
        showLocationView();
        int width = mMapView.getWidth() / 2;
        int height = mMapView.getHeight() / 2;
        if (width == mPoint.x && height == mPoint.y) {
            if (centerMarker != null) {
                animMarker();
            }
            LatLonPoint latLonPoint = new LatLonPoint(cameraPosition.target.latitude, cameraPosition.target.longitude);
            RegeocodeQuery query = new RegeocodeQuery(latLonPoint, 50, GeocodeSearch.AMAP);
            geocodeSearch.getFromLocationAsyn(query);
        } else {
            centerMarker.setIcon(chooseDescriptor);
            centerMarker.setPositionByPixels(mPoint.x, mPoint.y);
            LatLng latLng = centerMarker.getPosition();
            LatLonPoint latLonPoint = new LatLonPoint(latLng.latitude, latLng.longitude);
            RegeocodeQuery query = new RegeocodeQuery(latLonPoint, 50, GeocodeSearch.AMAP);
            geocodeSearch.getFromLocationAsyn(query);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.iv_locate) {
            CameraUpdate update = CameraUpdateFactory.changeLatLng(mLatLng);
            mAMap.animateCamera(update);
            animMarker();
        } else if (id == R.id.iv_back) {
            finish();
        } else if (id == R.id.iv_right) {
            ToastUtil.showLong(AmapActivity.this, tvLocation.getText().toString().trim());
        }
    }

    private void setMovingMarker() {
        if (isMovingMarker)
            return;

        isMovingMarker = true;
        centerMarker.setIcon(movingDescriptor);
        hideLocationView();
    }

    private void animMarker() {
        if (null == centerMarker) {
            ToastUtil.showShort(AmapActivity.this, "正在定位...");
            return;
        }
        isMovingMarker = false;
        if (animator != null) {
            animator.start();
            return;
        }
        animator = ValueAnimator.ofFloat(mMapView.getHeight() / 2, mMapView.getHeight() / 2 - 30);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.setDuration(150);
        animator.setRepeatCount(1);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Float value = (Float) animation.getAnimatedValue();
                centerMarker.setPositionByPixels(mMapView.getWidth() / 2, Math.round(value));
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                centerMarker.setIcon(chooseDescriptor);
            }
        });
        animator.start();
    }

    private void endAnim() {
        if (animator != null && animator.isRunning())
            animator.end();
    }

    @Override
    public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {
        if (i == 1000) {
            if (regeocodeResult != null && regeocodeResult.getRegeocodeAddress() != null) {
                endAnim();
                centerMarker.setIcon(successDescriptor);
                RegeocodeAddress regeocodeAddress = regeocodeResult.getRegeocodeAddress();
                String formatAddress = regeocodeResult.getRegeocodeAddress().getFormatAddress();
//                String shortAddress = formatAddress.replace(regeocodeAddress.getProvince(), "").replace(regeocodeAddress.getCity(), "").replace(regeocodeAddress.getDistrict(), "");
                tvLocation.setText(formatAddress);
            } else {
                ToastUtil.showShort(AmapActivity.this, R.string.no_result);
            }
        } else {
            ToastUtil.showShort(AmapActivity.this, R.string.error_network);
        }
    }

    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int rCode) {
    }

    private void introAnimPrepare() {
        rlTitle.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                rlTitle.getViewTreeObserver().removeOnPreDrawListener(this);
                rlTitle.setTranslationY(-rlTitle.getHeight());
                return false;
            }
        });
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        containerLayout.post(new Runnable() {
            @Override
            public void run() {
                animIntroduce();
            }
        });
    }

    private void animIntroduce() {
        ObjectAnimator animToolbar = ObjectAnimator.ofFloat(rlTitle, "TranslationY", 0f);
        animToolbar.setDuration(300);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(animToolbar);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mMapView.setVisibility(View.VISIBLE);
                tvLocation.setVisibility(View.VISIBLE);
            }
        });
        animatorSet.start();
    }

    private void hideLocationView() {
        ObjectAnimator animLocation = ObjectAnimator.ofFloat(tvLocation, "TranslationY", -tvLocation.getHeight() * 2);
        AnimatorSet set = new AnimatorSet();
        set.play(animLocation);
        set.setDuration(200);
        set.start();
    }

    private void showLocationView() {
        ObjectAnimator animLocation = ObjectAnimator.ofFloat(tvLocation, "TranslationY", 0);
        AnimatorSet set = new AnimatorSet();
        set.play(animLocation);
        set.setDuration(200);
        set.start();
    }
}
