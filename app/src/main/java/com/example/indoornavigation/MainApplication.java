package com.example.indoornavigation;

import android.app.Application;

import com.fengmap.android.FMMapSDK;

public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        //初始化SDK
        FMMapSDK.init(this);
        // 自定义缓存目录，需要申请读写权限
        // FMMapSDK.init(this,path);
    }
}
