package com.coolweather.android;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.util.Util;
import com.coolweather.android.gson.Forecast;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.w3c.dom.Text;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    public String weather_id;

    private ProgressDialog progressDialog;

    private ScrollView weatherLayout;

    private TextView titleCity;

    private TextView titleUpdatTime;

    private TextView degreeText;

    private TextView weatherInfoText;

    private LinearLayout forecastLayout;

    private TextView aqiText;

    private TextView  pm25Text;

    private TextView comforText;

    private TextView carWashText;

    private TextView sportText;

    private ImageView imageView;

    public SwipeRefreshLayout swipeRefreshLayout;

    public DrawerLayout drawerLayout;

    private Button navButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**
         *使系统状态栏与背景图融合，提升视觉效果
         */
        if(Build.VERSION.SDK_INT >= 21){
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        setContentView(R.layout.activity_weather);

        //初始化各控件
        this.weatherLayout  = (ScrollView) findViewById(R.id.weather_layout);
        this.titleCity = (TextView)findViewById(R.id.title_city);
        this.titleUpdatTime = (TextView)findViewById(R.id.title_update_time);
        this.degreeText = (TextView)findViewById(R.id.degree_text);
        this.weatherInfoText = (TextView)findViewById(R.id.weather_info_text);
        this.forecastLayout = (LinearLayout) findViewById(R.id.forecast_layout);
        this.aqiText = (TextView)findViewById(R.id.aqi_text);
        this.pm25Text = (TextView)findViewById(R.id.pm25_text);
        this.comforText = (TextView)findViewById(R.id.comfort_text);
        this.carWashText = (TextView)findViewById(R.id.car_wash_text);
        this.sportText = (TextView)findViewById(R.id.sport_text);

        /**
         * 使用缓存或者网络加载天气信息数据
         */
        this.weather_id = getIntent().getStringExtra("weather_id");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather", null);
        if(weatherString != null){
            //有缓存时直接解析申请数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            this.weather_id = weather.basic.weatherId;
            this.showWeatherInfo(weather);
        }else{
            //注意，请求数据的时候先将ScrollView 进行隐藏 ，不然空数据的界面看上去会很奇怪
            //无缓存时，去服务器查询天气，
            this.requestWeather(this.weather_id);
        }

        /**
         * 设置背景图片，提升视觉效果
         */
        this.imageView = (ImageView) findViewById(R.id.bing_pic_img);
        String bingPic = prefs.getString("bing_pic", null);
        if(bingPic != null){
            Glide.with(this).load(bingPic).into(imageView);
        }
        else{
            this.loadBingPic();
        }


        /**
         * 使用 滑动刷新 实现 手动刷新天气信息
         */
        this.swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refersh);
        this.swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        this.swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(WeatherActivity.this.weather_id);
            }
        });


        this.drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        this.navButton = (Button) findViewById(R.id.nav_button);
        //点击导航图标 ，显示滑动菜单
        this.navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WeatherActivity.this.drawerLayout.openDrawer(GravityCompat.START);
            }
        });
    }

    /**
     * 加载 必应 每日一图
     */
    private void loadBingPic() {
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(imageView);
                    }
                });
            }
        });
    }

    /**
     * 根据天气ID请求   城市天气信息
     * @param weather_id
     */
    public void requestWeather(String weather_id) {
        showProgressDialog();
        String weatherUrl = "http://guolin.tech/api/weather?cityid="+weather_id+"&key=b52d7356607546189dbc60c40caadbdf";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        if(weather != null && "ok".equals(weather.status)){
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather",responseText);
                            editor.apply();
                            showWeatherInfo(weather);
                        }else{
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        }
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        });
    }


    /**
     * 黑醋栗并展示 Weather实体类中的数据
     */
    public void showWeatherInfo(Weather weather){
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "℃";
        String weatherInfo = weather.now.more.info;

        this.titleCity.setText(cityName);
        this.titleUpdatTime.setText(updateTime);
        this.degreeText.setText(degree);
        this.weatherInfoText.setText(weatherInfo);
        this.forecastLayout.removeAllViews();
        for(Forecast forecast : weather.forecastList){
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false);
            TextView dateText = (TextView)view.findViewById(R.id.date_text);
            TextView infoText = (TextView)view.findViewById(R.id.info_text);
            TextView maxText = (TextView) view.findViewById(R.id.max_text);
            TextView minText = (TextView) view.findViewById(R.id.min_text);
            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);
            this.forecastLayout.addView(view);
        }

        if(weather.aqi != null){
            this.aqiText.setText(weather.aqi.city.aqi);
            this.pm25Text.setText(weather.aqi.city.pm25);
        }

        String comfort = "舒适度" + weather.suggestion.comfort.info;
        String carWash = "洗车指数"  + weather.suggestion.carWash.info;
        String sport = "运动建议" + weather.suggestion.sport.info;
        this.comforText.setText(comfort);
        this.carWashText.setText(carWash);
        this.sportText.setText(sport);
        this.weatherLayout.setVisibility(View.VISIBLE);
    }


    /**
     * 显示进度对话框
     */
    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("正在加载....");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    /**
     * 关闭进度对话框
     */
    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }
}
