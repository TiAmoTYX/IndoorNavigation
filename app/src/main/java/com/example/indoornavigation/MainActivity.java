package com.example.indoornavigation;



import android.app.Activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import android.view.View;


import com.example.indoornavigation.utils.FileUtils;
import com.fengmap.android.FMDevice;
import com.fengmap.android.FMErrorMsg;

import com.fengmap.android.analysis.navi.FMNaviAnalyser;
import com.fengmap.android.analysis.navi.FMNaviOption;
import com.fengmap.android.analysis.navi.FMNavigation;
import com.fengmap.android.analysis.navi.FMNavigationInfo;
import com.fengmap.android.analysis.navi.FMPointOption;
import com.fengmap.android.analysis.navi.FMSimulateNavigation;
import com.fengmap.android.analysis.navi.OnFMNavigationListener;
import com.fengmap.android.data.OnFMDownloadProgressListener;
import com.fengmap.android.map.FMMap;
import com.fengmap.android.map.FMMapUpgradeInfo;
import com.fengmap.android.map.FMMapView;
import com.fengmap.android.map.animator.FMLinearInterpolator;
import com.fengmap.android.map.event.OnFMMapInitListener;
import com.fengmap.android.map.event.OnFMSwitchGroupListener;
import com.fengmap.android.map.geometry.FMGeoCoord;
import com.fengmap.android.map.geometry.FMMapCoord;
import com.fengmap.android.utils.FMLog;
import com.fengmap.android.widget.FMFloorControllerComponent;
import com.fengmap.android.widget.FMMultiFloorControllerButton;
import com.fengmap.android.widget.FMTextButton;


public class MainActivity extends Activity implements OnFMNavigationListener,OnFMMapInitListener, OnFMSwitchGroupListener{
    //地图
    FMMap mFMMap;
    private FMFloorControllerComponent mFloorComponent;
    private boolean mAnimateEnded = true;
    private FMMapView mMapView;
    //多层单层按钮
    FMMultiFloorControllerButton mMultiFloorBtn;
    // 导航对象
    protected FMNavigation mNavigation;
    // 导航配置
    protected FMNaviOption mNaviOption;
    // 点移距离视图中心点超过最大距离5米，就会触发移动动画
    protected static final double NAVI_MOVE_CENTER_MAX_DISTANCE = 5;
    // 进入导航时地图显示级别
    protected static final int NAVI_ZOOM_LEVEL = 20;
    // 起点标志物配置
    protected FMPointOption mStartOption = new FMPointOption();
    // 终点标志物配置
    protected FMPointOption mEndOption = new FMPointOption();
    // 路径是否计算完成
    protected boolean isRouteCalculated;
    // 地图是否加载完成
    protected boolean isMapLoaded;
    // 默认起点
    protected FMGeoCoord mStartCoord = new FMGeoCoord(1,
            new FMMapCoord(12613383.488510681, 2642576.1474729534));
    // 默认终点
    protected FMGeoCoord mEndCoord = new FMGeoCoord(3,
            new FMMapCoord(12613464.576599307, 2642647.724973787));
    // 是否为第一人称
    private boolean mIsFirstView = true;
    // 是否为跟随状态
    private boolean mHasFollowed = true;
    // 总共距离
    private double mTotalDistance;

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
        //创建多楼层显示按钮
        mMultiFloorBtn = new FMMultiFloorControllerButton(this);
        //初始状态
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
        // 创建模拟导航对象
        mNavigation = new FMSimulateNavigation(mFMMap);
        // 创建真实导航对象
        // mNavigation = new FMActualNavigation(mFMMap);
        // 设置导航文字语种，目前支持中文英文两种("zh"，"en")
        // mNavigation.setNaviLanguage(this, "en");
        // 设置是否启用走过导航线变化,默认开启
        // mNavigation. setNaviAcrossChange(true);
        // 创建导航配置对象
        mNaviOption = new FMNaviOption();
        // 设置跟随模式，默认跟随
        mNaviOption.setFollowPosition(true);
        // 设置跟随角度（第一人视角），默认跟随
        mNaviOption.setFollowAngle(true);
        // 点移距离视图中心点超过最大距离5米，就会触发移动动画；若设为0，则实时居中
        mNaviOption.setNeedMoveToCenterMaxDistance(NAVI_MOVE_CENTER_MAX_DISTANCE);
        // 设置导航开始时的缩放级别，true: 导航结束时恢复开始前的缩放级别，false：保持现状
        mNaviOption.setZoomLevel(NAVI_ZOOM_LEVEL, false);
        // 设置配置
        mNavigation.setNaviOption(mNaviOption);
        initNavi();
    }

    private void initNavi() {
        // 创建模拟导航对象
        mNavigation = new FMSimulateNavigation(mFMMap);
        // 设置导航文字语种，目前支持中文（CH）英文(EN)两种模式，不区分大小写，默认中文
        mNavigation.setNaviLanguage(this, "Ch");
        // 创建模拟导航配置对象
        mNaviOption = new FMNaviOption();
        // 设置跟随模式，默认跟随
        mNaviOption.setFollowPosition(mHasFollowed);
        // 设置跟随角度（第一人视角），默认跟随
        mNaviOption.setFollowAngle(mIsFirstView);
        // 点移距离视图中心点超过最大距离5米，就会触发移动动画；若设为0，则实时居中
        mNaviOption.setNeedMoveToCenterMaxDistance(NAVI_MOVE_CENTER_MAX_DISTANCE);
        // 设置导航开始时的缩放级别，true: 导航结束时恢复开始前的缩放级别，false：保持现状
        mNaviOption.setZoomLevel(NAVI_ZOOM_LEVEL, false);
        // 设置配置
        mNavigation.setNaviOption(mNaviOption);
        // 设置导航监听接口
        mNavigation.setOnNavigationListener(this);
        // 路径规划
        analyzeNavigation(mStartCoord, mEndCoord);
        // 总长
        mTotalDistance = mNavigation.getSceneRouteLength();
        isMapLoaded = true;
    }

    //导航
    void analyzeNavigation(FMGeoCoord startPt, FMGeoCoord endPt) {
        // 设置起点
        Bitmap startBmp = BitmapFactory.decodeResource(getResources(), R.drawable.start);
        mStartOption.setBitmap(startBmp);
        mNavigation.setStartPoint(startPt);
        mNavigation.setStartOption(mStartOption);

        // 设置终点
        Bitmap endBmp = BitmapFactory.decodeResource(getResources(), R.drawable.end);
        mEndOption.setBitmap(endBmp);
        mNavigation.setEndPoint(endPt);
        mNavigation.setEndOption(mEndOption);

        // 路径规划
        int ret = mNavigation.analyseRoute(FMNaviAnalyser.FMNaviModule.MODULE_SHORTEST);
        if (ret == FMNaviAnalyser.FMRouteCalcuResult.ROUTE_SUCCESS) {
            mNavigation.drawNaviLine();
            isRouteCalculated = true;
        } else {
            FMLog.le("failed",FMNaviAnalyser.FMRouteCalcuResult.getErrorMsg(ret));
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


    @Override
    public void onCrossGroupId(int i, int i1) {

    }

    @Override
    public void onWalking(FMNavigationInfo fmNavigationInfo) {

    }

    @Override
    public void onComplete() {

    }
}