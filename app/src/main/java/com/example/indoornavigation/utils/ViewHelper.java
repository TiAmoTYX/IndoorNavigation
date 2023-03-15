package com.example.indoornavigation.utils;

import android.app.Activity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

/**
 * 控件使用帮助类

 */
public class ViewHelper {

    /**
     * 获取控件
     *
     * @param activity Activity
     * @param id       控件id
     * @param <T>      控件继承于View
     * @return id对应的控件
     */
    public static <T extends View> T getView(Activity activity, int id) {
        return (T) activity.findViewById(id);
    }

    /**
     * 获取控件
     *
     * @param view 视图
     * @param id   控件id
     * @param <T>  继承于View
     * @return id对应的控件
     */
    public static <T extends View> T getView(View view, int id) {
        return (T) view.findViewById(id);
    }

    /**
     * 设置控件的点击事件
     *
     * @param activity Activity
     * @param id       控件id
     * @param listener 点击监听事件
     */
    public static void setViewClickListener(Activity activity, int id, View.OnClickListener listener) {
        View view = getView(activity, id);
        view.setOnClickListener(listener);
    }

    /**
     * 设置控件的点击事件
     *
     * @param activity Activity
     * @param id       控件id
     * @param listener CheckBox选中状态改变事件
     */
    public static void setViewCheckedChangeListener(Activity activity, int id,
                                                    CompoundButton.OnCheckedChangeListener listener) {
        CheckBox view = getView(activity, id);
        view.setOnCheckedChangeListener(listener);
    }

    /**
     * 设置控件的点击事件
     *
     * @param activity   Activity
     * @param id         控件id
     * @param visibility 控件显示状态
     */
    public static void setViewVisibility(Activity activity, int id, int visibility) {
        View view = getView(activity, id);
        view.setVisibility(visibility);
    }
}
