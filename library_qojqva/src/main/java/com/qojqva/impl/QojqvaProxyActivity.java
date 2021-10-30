package com.qojqva.impl;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.widget.FrameLayout;

import com.qojqva.Qojqva;
import com.qojqva.internal.IPermissionCallback;
import com.qojqva.toolkit.PermissionChecker;
import com.qojqva.toolkit.PermissionKit;

import java.util.List;

/**
 * @author #Suyghur.
 * Created on 2021/10/29
 */
public class QojqvaProxyActivity extends FragmentActivity {

    private FrameLayout layout = null;
    private static List<String> mPermissions = null;
    private static IPermissionCallback mCallback = null;

    public static void start(Context context, List<String> permissions, IPermissionCallback callback) {
        if (mPermissions != null) {
            mPermissions.clear();
            mPermissions = null;
        }
        if (mCallback != null) {
            mCallback = null;
        }
        mPermissions = permissions;
        mCallback = callback;
        context.startActivity(new Intent(context, QojqvaProxyActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    public static void finish(Context context) {
        if (context instanceof QojqvaProxyActivity) {
            ((QojqvaProxyActivity) context).finish();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        layout = new FrameLayout(this);
        setContentView(layout);
        doRequestPermissions();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPermissions != null) {
            mPermissions.clear();
            mPermissions = null;
        }
        mCallback = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Qojqva.REQUEST_CODE && !isFinishing()) {
            if (mCallback != null) {
                mCallback.onProxyFinish();
                finish();
            }
        }
    }

    private void doRequestPermissions() {
        if (mPermissions == null || mPermissions.isEmpty()) {
            return;
        }
        if (mCallback == null) {
            return;
        }
        // 判断当前是否调试模式
        boolean debugMode = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        FragmentActivity activity = PermissionKit.findActivity(this);
        // 检查当前Activity状态是否正常，如果不是直接返回
        if (!PermissionChecker.checkActivityStatus(activity, debugMode)) {
            return;
        }

        // 必须传入正常的权限或者权限组才能申请权限
        if (!PermissionChecker.checkPermissionArgument(mPermissions, debugMode)) {
            return;
        }

        if (debugMode) {
            //检查申请的存储权限是否符合规范
            PermissionChecker.checkStoragePermission(activity, mPermissions);
            //检查申请的定位权限是否符合规范
            PermissionChecker.checkLocationPermission(activity, mPermissions);
            //检查申请的权限和 targetSdkVersion 版本是否能吻合
            PermissionChecker.checkTargetSdkVersion(activity, mPermissions);
        }

        // 优化所以申请的权限
        PermissionChecker.optimizeDeprecatedPermission(mPermissions);

        if (debugMode) {
            // 检查权限有没有注册
            PermissionChecker.checkManifestPermissions(activity, mPermissions);
        }

        if (PermissionKit.isGrantedPermissions(activity, mPermissions)) {
            // 证明这些权限已经全部授予过，直接回调成功
            if (mCallback != null) {
                mCallback.onGranted(mPermissions, true);
                activity.finish();
            }
            return;
        }

        //申请没有授予过的权限
        Qojqva.sInterceptor.requestPermissions(activity, mPermissions, mCallback);
    }
}
