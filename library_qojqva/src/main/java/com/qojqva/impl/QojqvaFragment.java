package com.qojqva.impl;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.ArraySet;

import com.qojqva.entity.Permission;
import com.qojqva.internal.IPermissionCallback;
import com.qojqva.toolkit.PermissionKit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * @author #Suyghur.
 * Created on 3/29/2021
 */
@SuppressWarnings("deprecation")
@TargetApi(Build.VERSION_CODES.M)
public final class QojqvaFragment extends Fragment implements Runnable {

    private boolean mSpecialRequest = false;
    private boolean mDangerousRequestr = false;
    /**
     * 权限申请标记
     */
    public boolean mRequestFlag = false;
    public IPermissionCallback mCallback = null;
    private int mScreenOrientation = 0;

    /**
     * 请求的权限组
     */
    private static final String REQUEST_PERMISSIONS = "request_permissions";

    /**
     * 请求码（自动生成）
     */
    private static final String REQUEST_CODE = "request_code";

    /**
     * 权限请求码存放集合
     */
    private static final ArraySet<Integer> REQUEST_CODE_ARRAY = new ArraySet<>();


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Activity activity = getActivity();
        // 如果当前没有锁定屏幕方向就获取当前屏幕方向并进行锁定
        mScreenOrientation = activity.getRequestedOrientation();
        if (mScreenOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            return;
        }
        int activityOrientation = getActivity().getResources().getConfiguration().orientation;
        try {
            // 兼容问题：在 Android 8.0 的手机上可以固定 Activity 的方向，但是这个 Activity 不能是透明的，否则就会抛出异常
            // 复现场景：只需要给 Activity 主题设置 <item name="android:windowIsTranslucent">true</item> 属性即可
            switch (activityOrientation) {
                case Configuration.ORIENTATION_LANDSCAPE:
                    getActivity().setRequestedOrientation(PermissionKit.isActivityReverse(activity) ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    break;
                case Configuration.ORIENTATION_PORTRAIT:
                default:
                    activity.setRequestedOrientation(PermissionKit.isActivityReverse(activity) ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    break;
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 如果当前 Fragment 是通过系统重启应用触发的，则不进行权限申请
        if (!mRequestFlag) {
            detachActivity(getActivity());
            return;
        }

        // 如果在 Activity 不可见的状态下添加 Fragment 并且去申请权限会导致授权对话框显示不出来
        // 所以必须要在 Fragment 的 onResume 来申请权限，这样就可以保证应用回到前台的时候才去申请权限
        if (mSpecialRequest) {
            return;
        }

        mSpecialRequest = true;
        requestSpecialPermission();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Activity activity = getActivity();
        if (activity == null || mScreenOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            return;
        }
        // 为什么这里不用跟上面一样 try catch ？因为这里是把 Activity 方向取消固定，只有设置横屏或竖屏的时候才可能触发 crash
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCallback = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Bundle arguments = getArguments();
        FragmentActivity activity = getActivity();
        if (activity == null || arguments == null || mCallback == null || requestCode != arguments.getInt(REQUEST_CODE)) {
            return;
        }
        IPermissionCallback callback = mCallback;
        mCallback = null;

        // 优化权限回调结果
        PermissionKit.optimizePermissionResults(activity, permissions, grantResults);

        // 释放对这个请求码的占用
        REQUEST_CODE_ARRAY.remove(requestCode);
        // 将Fragment从Activity移除
        detachActivity(activity);

        // 获取已授予的权限
        List<String> grantedPermission = PermissionKit.getGrantedPermissions(permissions, grantResults);

        // 如果请求成功的权限集合大小和请求的数据一样大时证明权限已经全部授予
        if (grantedPermission.size() == permissions.length) {
            callback.onGranted(grantedPermission, true);
            return;
        }

        // 获取被拒绝的权限
        List<String> deiedPermission = PermissionKit.getGrantedPermissions(permissions, grantResults);

        callback.onDenied(deiedPermission, PermissionKit.isPermissionPermanentDenied(activity, deiedPermission));

        // 证明还有一部分权限被成功授予，回调成功接口
        if (!grantedPermission.isEmpty()) {
            callback.onGranted(grantedPermission, false);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Bundle arguments = getArguments();
        FragmentActivity activity = getActivity();

        if (activity == null || arguments == null || mDangerousRequestr || requestCode != arguments.getInt(REQUEST_CODE)) {
            return;
        }

        mDangerousRequestr = true;
        // 需要延迟执行，不然有些华为机型授权了但是获取不到权限
        activity.getWindow().getDecorView().postDelayed(this, 300);
    }

    @Override
    public void run() {
        // 如果用户离开太久，会导致 Activity 被回收掉
        // 所以这里要判断当前 Fragment 是否有被添加到 Activity
        // 可在开发者模式中开启不保留活动来复现这个 Bug
        if (!isAdded()) {
            return;
        }
        // 请求其他危险权限
        requestDangerousPermission();
    }

    /**
     * 申请特殊权限
     */
    public void requestSpecialPermission() {
        Bundle arguments = getArguments();
        FragmentActivity activity = getActivity();
        if (arguments == null || activity == null) {
            return;
        }

        List<String> permissions = arguments.getStringArrayList(REQUEST_PERMISSIONS);

        // 是否需要申请特殊权限
        boolean requestSpecialPermission = false;

        // 判断当前是否包含特殊权限
        for (String permission : permissions) {
            if (PermissionKit.isSpecialPermission(permission)) {
                if (PermissionKit.isGrantedPermission(activity, permission)) {
                    // 已经授予过了，可以跳过
                    continue;
                }
                if (Permission.MANAGE_EXTERNAL_STORAGE.equals(permission) && !PermissionKit.isAndroid11()) {
                    // 当前必须是 Android 11 及以上版本，因为在旧版本上是拿旧权限做的判断
                    continue;
                }
                // 跳转到特殊权限授权页面
                startActivityForResult(PermissionSettingPage.getSmartPermissionIntent(activity, PermissionKit.asArrayList(permission)), getArguments().getInt(REQUEST_CODE));
                requestSpecialPermission = true;
            }
        }

        if (requestSpecialPermission) {
            return;
        }
        // 如果没有跳转到特殊权限授权页面，就直接申请危险权限
        requestDangerousPermission();
    }

    public void requestDangerousPermission() {
        Bundle arguments = getArguments();
        FragmentActivity activity = getActivity();
        if (activity == null || arguments == null) {
            return;
        }

        final int requestCode = arguments.getInt(REQUEST_CODE);

        final ArrayList<String> allPermissions = arguments.getStringArrayList(REQUEST_PERMISSIONS);
        if (allPermissions == null || allPermissions.size() == 0) {
            return;
        }

        ArrayList<String> locationPermission = null;
        // Android 10 定位策略发生改变，申请后台定位权限的前提是要有前台定位权限（授予了精确或者模糊任一权限）
        if (PermissionKit.isAndroid10() && allPermissions.contains(Permission.ACCESS_BACKGROUND_LOCATION)) {
            locationPermission = new ArrayList<>();
            if (allPermissions.contains(Permission.ACCESS_COARSE_LOCATION)) {
                locationPermission.add(Permission.ACCESS_COARSE_LOCATION);
            }

            if (allPermissions.contains(Permission.ACCESS_FINE_LOCATION)) {
                locationPermission.add(Permission.ACCESS_FINE_LOCATION);
            }
        }

        if (!PermissionKit.isAndroid10() || locationPermission == null || locationPermission.isEmpty()) {
            requestPermissions(allPermissions.toArray(new String[allPermissions.size() - 1]), getArguments().getInt(REQUEST_CODE));
            return;
        }

        // 在 Android 10 的机型上，需要先申请前台定位权限，再申请后台定位权限
        QojqvaFragment.beginRequest(activity, locationPermission, new IPermissionCallback() {

            @Override
            public void onGranted(List<String> permissions, boolean all) {
                if (!all || !isAdded()) {
                    return;
                }
                // 前台定位权限授予了，现在申请后台定位权限
                beginRequest(activity, PermissionKit.asArrayList(Permission.ACCESS_BACKGROUND_LOCATION), new IPermissionCallback() {
                    @Override
                    public void onGranted(List<String> permissions, boolean all) {
                        if (!all || !isAdded()) {
                            return;
                        }

                        // 前台定位权限和后台定位权限都授予了
                        int[] grantResults = new int[allPermissions.size()];
                        Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
                        onRequestPermissionsResult(requestCode, allPermissions.toArray(new String[0]), grantResults);
                    }

                    @Override
                    public void onDenied(List<String> permissions, boolean never) {
                        if (!isAdded()) {
                            return;
                        }

                        // 后台定位授权失败，但是前台定位权限已经授予了
                        int[] grantResults = new int[allPermissions.size()];
                        for (int i = 0; i < allPermissions.size(); i++) {
                            grantResults[i] = Permission.ACCESS_BACKGROUND_LOCATION.equals(allPermissions.get(i)) ? PackageManager.PERMISSION_DENIED : PackageManager.PERMISSION_GRANTED;
                        }
                        onRequestPermissionsResult(requestCode, allPermissions.toArray(new String[0]), grantResults);
                    }

                    @Override
                    public void onProxyFinish() {

                    }
                });
            }

            @Override
            public void onDenied(List<String> permissions, boolean never) {
                if (!isAdded()) {
                    return;
                }

                // 前台定位授权失败，并且无法申请后台定位权限
                int[] grantResults = new int[allPermissions.size()];
                Arrays.fill(grantResults, PackageManager.PERMISSION_DENIED);
                onRequestPermissionsResult(requestCode, allPermissions.toArray(new String[0]), grantResults);
            }

            @Override
            public void onProxyFinish() {

            }
        });
    }

    /**
     * 开启权限申请
     */
    public static void beginRequest(FragmentActivity activity, ArrayList<String> permissions, IPermissionCallback callback) {
        QojqvaFragment fragment = new QojqvaFragment();
        Bundle bundle = new Bundle();
        int requestCode;
        // 请求码随机生成，避免随机产生之前的请求码，必须进行循环判断
        do {
            // 新版本的 Support 库限制请求码必须小于 65536
            // 旧版本的 Support 库限制请求码必须小于 256
            requestCode = new Random().nextInt((int) Math.pow(2, 8));
        } while (REQUEST_CODE_ARRAY.contains(requestCode));
        // 标记这个请求码已经被占用
        REQUEST_CODE_ARRAY.add(requestCode);
        bundle.putInt(REQUEST_CODE, requestCode);
        bundle.putStringArrayList(REQUEST_PERMISSIONS, permissions);
        fragment.setArguments(bundle);
        // 设置保留实例，不会因为屏幕方向或配置变化而重新创建
        fragment.setRetainInstance(true);
        // 设置权限申请标记
        fragment.mRequestFlag = true;
        // 设置权限回调监听
        fragment.mCallback = callback;
        // 绑定到 Activity 上面
        fragment.attachActivity(activity);
    }


    /**
     * 绑定Activity
     */
    private void attachActivity(FragmentActivity activity) {
        activity.getSupportFragmentManager().beginTransaction().add(this, this.toString()).commitAllowingStateLoss();
    }

    /**
     * 解绑Activity
     */
    private void detachActivity(FragmentActivity activity) {
        activity.getSupportFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
    }
}