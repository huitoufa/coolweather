package com.coolweather.android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.idescout.sql.SqlScoutServer;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**
         * 可以看到，这里 onCreate() 方法 的一开始先从 SharedPreferences 文件中读取缓存数据，
         * 如果不为 null 就说明之前已经请求过天气数据了，那么就没必要让用户再次选择城市，而是直接跳转到 Weather 即可。
         */
//        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
//        String weather = prefs.getString("weather", null);
//        if(weather != null){
//            Intent intent = new Intent(this,WeatherActivity.class);
//            startActivity(intent);
////            finish();
//        }
        //SqlScout所需要的
        SqlScoutServer.create(this, getPackageName());
    }
}
