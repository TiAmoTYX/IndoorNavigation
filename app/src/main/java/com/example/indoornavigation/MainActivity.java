package com.example.indoornavigation;



import android.app.Activity;

import android.os.Bundle;

import android.view.View;


import com.example.indoornavigation.utils.FileUtils;
import com.fengmap.android.FMDevice;
import com.fengmap.android.FMErrorMsg;

import com.fengmap.android.data.OnFMDownloadProgressListener;
import com.fengmap.android.map.FMMap;
import com.fengmap.android.map.FMMapUpgradeInfo;
import com.fengmap.android.map.FMMapView;
import com.fengmap.android.map.animator.FMLinearInterpolator;
import com.fengmap.android.map.event.OnFMMapInitListener;
import com.fengmap.android.map.event.OnFMSwitchGroupListener;
import com.fengmap.android.widget.FMFloorControllerComponent;
import com.fengmap.android.widget.FMMultiFloorControllerButton;
import com.fengmap.android.widget.FMTextButton;


public class MainActivity extends Activity implements OnFMMapInitListener, OnFMSwitchGroupListener{
    //地图
    FMMap mFMMap;
    private FMFloorControllerComponent mFloorComponent;
    private boolean mAnimateEnded = true;
    private FMMapView mMapView;
    //多层单层按钮
    FMMultiFloorControllerButton mMultiFloorBtn;
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FMMapView mapView = (FMMapView) findViewById(R.id.mapview);
        mFMMap = mapView.getFMMap();       //获取地图操作对象
        String bid = "1627576819222937601";             //地图id
        mFMMap.openMapById(bid, true);          //打开地图
        openMapByPath();
    }


    /**
     * 加载地图数据
     */
    private void openMapByPath() {
        mMapView = (FMMapView) findViewById(R.id.mapview);
        mFMMap = mMapView.getFMMap();
        mFMMap.setOnFMMapInitListener(this);
        //加载离线数据
        String path = FileUtils.getDefaultMapPath(this);
        mFMMap.openMapByPath(path);
    }

    /**
     * 初始化多楼层控件位置
     */
    private void initMultiFloorControllerComponent() {
        mMultiFloorBtn = new FMMultiFloorControllerButton(this);
        mMultiFloorBtn.initState(false);
        mMultiFloorBtn.measure(0, 0);
        int width = mMultiFloorBtn.getMeasuredWidth();

        //单/多层楼层切换控件
        mMapView.addComponent(mMultiFloorBtn, FMDevice.getDeviceWidth() - 10 - width, 60);
        //单、多楼层点击切换
        mMultiFloorBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FMTextButton button = (FMTextButton) v;
                if (button.isSelected()) {
                    button.setSelected(false);
                    setSingleDisplay();
                } else {
                    button.setSelected(true);
                    setMultiDisplay();
                }
            }
        });
    }

    /**
     * 单层显示
     */
    private void setSingleDisplay() {
        int[] gids = {mFMMap.getFocusGroupId()};       //获取当前地图焦点层id
        mFMMap.setMultiDisplay(gids, 0, null);
    }

    /**
     * 多层显示
     */
    private void setMultiDisplay() {
        int[] gids = mFMMap.getMapGroupIds();    //获取地图所有的group
        int focus = 0;
        for (int i = 0; i < gids.length; i++) {
            if (gids[i] == mFMMap.getFocusGroupId()) {
                focus = i;
                break;
            }
        }
        mFMMap.setMultiDisplay(gids, focus, null);
    }

    /**
     * 切换楼层
     *
     * @param groupId 楼层id
     */
    void switchFloor(int groupId) {
        mFMMap.setFocusByGroupIdAnimated(groupId, new FMLinearInterpolator(), (OnFMSwitchGroupListener) this);
    }

    /**
     * 地图销毁调用
     */

    @Override
    public void onBackPressed () {
        if (mFMMap != null) {
            mFMMap.onDestroy();
        }
        super.onBackPressed();
    }

    /**
     * 地图加载成功回调事件
     *
     * @param path 地图所在sdcard路径
     */
    @Override
    public void onMapInitSuccess(String path) {
        //加载离线主题文件
        mFMMap.loadThemeByPath(FileUtils.getDefaultThemePath(this));
        initMultiFloorControllerComponent();
        if (mFloorComponent == null) {
            initFloorControllerComponent();
        }
    }

    /**
     * 楼层切换控件初始化
     */
    private void initFloorControllerComponent() {
        // 创建楼层切换控件
        mFloorComponent = new FMFloorControllerComponent(this);
        mFloorComponent.setMaxItemCount(4);//设置楼层最大显示数量
        //楼层切换事件监听
        mFloorComponent.setOnFMFloorControllerComponentListener
                (new FMFloorControllerComponent.OnFMFloorControllerComponentListener() {
                    @Override
                    public void onSwitchFloorMode(View view, FMFloorControllerComponent.FMFloorMode currentMode) {
                        if (currentMode == FMFloorControllerComponent.FMFloorMode.SINGLE) {

                        } else {

                        }
                    }

                    @Override
                    public boolean onItemSelected(int groupId, String floorName) {
                        if (mAnimateEnded) {
                            switchFloor(groupId);
                            return true;
                        }
                        return false;
                    }
                });
        //设置为单层模式
        mFloorComponent.setFloorMode(FMFloorControllerComponent.FMFloorMode.SINGLE);
        int groupId = 1; //设置默认显示楼层id
        mFloorComponent.setFloorDataFromFMMapInfo(mFMMap.getFMMapInfo(), groupId);
        mMapView.addComponent(mFloorComponent, 10, 400);//添加楼层控件并设置位置
    }

    /**
     * 地图加载失败回调事件
     *
     * @param path      地图所在sdcard路径
     * @param errorCode 失败加载错误码，可以通过{@link FMErrorMsg#getErrorMsg(int)}获取加载地图失败详情
     */
    @Override
    public void onMapInitFailure(String path, int errorCode) {
        //TODO 可以提示用户地图加载失败原因，进行地图加载失败处理
    }

    /**
     * 当{@link FMMap#openMapById(String, boolean)}设置openMapById(String, false)时地图不自动更新会
     * 回调此事件，可以调用{@link FMMap#upgrade(FMMapUpgradeInfo, OnFMDownloadProgressListener)}进行
     * 地图下载更新
     *
     * @param upgradeInfo 地图版本更新详情,地图版本号{@link FMMapUpgradeInfo#getVersion()},<br/>
     *                    地图id{@link FMMapUpgradeInfo#getMapId()}
     * @return 如果调用了{@link FMMap#upgrade(FMMapUpgradeInfo, OnFMDownloadProgressListener)}地图下载更新，
     * 返回值return true,因为{@link FMMap#upgrade(FMMapUpgradeInfo, OnFMDownloadProgressListener)}
     * 会自动下载更新地图，更新完成后会加载地图;否则return false。
     */
    @Override
    public boolean onUpgrade(FMMapUpgradeInfo upgradeInfo) {
        //TODO 获取到最新地图更新的信息，可以进行地图的下载操作
        return false;
    }

    /**
     * 组切换开始之前。
     */
    @Override
    public void beforeGroupChanged() {
        mAnimateEnded = false;
    }

    /**
     * 组切换结束之后。
     */
    @Override
    public void afterGroupChanged() {
        mAnimateEnded = true;
    }


}