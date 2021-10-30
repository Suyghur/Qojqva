package com.qojqva;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;

import com.qojqva.impl.PermissionSettingPage;
import com.qojqva.impl.QojqvaProxyActivity;
import com.qojqva.internal.IPermissionCallback;
import com.qojqva.internal.IPermissionInterceptor;
import com.qojqva.toolkit.PermissionKit;

import java.util.ArrayList;
import java.util.List;

/**
 * @author #Suyghur.
 * Created on 2021/10/29
 */
@SuppressWarnings({"unused", "deprecation"})
public final class Qojqva {

    private List<String> mPermission = null;
    public static final int REQUEST_CODE = 1024 + 1;
    public static final IPermissionInterceptor sInterceptor = new BasePermissionInterceptor();
    public static boolean scopedStorage = false;

    private Qojqva() {

    }

    public static Qojqva with() {
        return new Qojqva();
    }

    public Qojqva permission(String permission) {
        if (mPermission == null) {
            mPermission = new ArrayList<>();
        }
        mPermission.add(permission);
        return this;
    }

    public Qojqva permission(List<String> permissions) {
        if (mPermission == null) {
            mPermission = permissions;
        } else {
            mPermission.addAll(permissions);
        }
        return this;
    }

    public Qojqva permission(String[] permissions) {
        return permission(PermissionKit.asArrayList(permissions));
    }

    public void request(Context context, IPermissionCallback callback) {
        if (mPermission == null) {
            mPermission = new ArrayList<>();
        }
        QojqvaProxyActivity.start(context, mPermission, callback);
    }

    /**
     * 判断一个或多个权限是否全部授予了
     */
    public static boolean isGranted(Context context, String permission) {
        return PermissionKit.isGrantedPermission(context, permission);
    }

    /**
     * 判断一个或多个权限是否全部授予了
     */
    public static boolean isGranted(Context context, String[] permissions) {
        return PermissionKit.isGrantedPermissions(context, PermissionKit.asArrayList(permissions));
    }

    /**
     * 判断一个或多个权限是否全部授予了
     */
    public static boolean isGranted(Context context, List<String> permissions) {
        return PermissionKit.isGrantedPermissions(context, permissions);
    }

    /**
     * 获取没有授予的权限
     */
    public static List<String> getDenied(Context context, String[] permissions) {
        return getDenied(context, PermissionKit.asArrayList(permissions))
    }

    /**
     * 获取没有授予的权限
     */
    public static List<String> getDenied(Context context, List<String> permissions) {
        return PermissionKit.getDeniedPermissions(context, permissions);
    }

    /**
     * 判断某个权限是否是特殊权限
     */
    public static boolean isSpedial(String permission) {
        return PermissionKit.isSpecialPermission(permission)
    }

    /**
     * 判断一个或多个权限是否被永久拒绝了（注意不能在请求权限之前调用，应该在 {@link IPermissionCallback#onDenied(List, boolean)} 方法中调用）
     */
    public static boolean isPermissionDenied(Activity activity, String permission) {
        return PermissionKit.isPermissionPermanentDenied(activity, permission)
    }

    /**
     * 判断一个或多个权限是否被永久拒绝了（注意不能在请求权限之前调用，应该在 {@link IPermissionCallback#onDenied(List, boolean)} 方法中调用）
     */
    public static boolean isPermissionDenied(Activity activity, String[] permissions) {
        return isPermissionDenied(activity, PermissionKit.asArrayList(permissions))
    }

    /**
     * 判断一个或多个权限是否被永久拒绝了（注意不能在请求权限之前调用，应该在 {@link IPermissionCallback#onDenied(List, boolean)} 方法中调用）
     */
    public static boolean isPermissionDenied(Activity activity, List<String> permissions) {
        return PermissionKit.isPermissionPermanentDenied(activity, permissions)
    }

    public static void startPermissionActivity(Context context) {
        startPermissionActivity(context, null)
    }

    public static void startPermissionActivity(Context context, String permission) {
        startPermissionActivity(context, PermissionKit.asArrayList(permission))
    }

    public static void startPermissionActivity(Context context, List<String> permissions) {
        Activity activity = PermissionKit.findActivity(context);
        if (activity != null) {
            startPermissionActivity(activity, permissions)
            return;
        }
        context.startActivity(PermissionSettingPage.getSmartPermissionIntent(context, permissions).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    public static void startPermissionActivity(Context context, String[] permissions) {
        startPermissionActivity(context, PermissionKit.asArrayList( * permissions))
    }

    public static void startPermissionActivity(Activity activity, String permission) {
        startPermissionActivity(activity, PermissionKit.asArrayList(permission))
    }

    public static void startPermissionActivity(Activity activity, String[] permissions) {
        startPermissionActivity(activity, PermissionKit.asArrayList(permissions))
    }

    public static void startPermissionActivity(Activity activity, List<String> permissions) {
        activity.startActivityForResult(PermissionSettingPage.getSmartPermissionIntent(activity, permissions), REQUEST_CODE)
    }

    public static void startPermissionActivity(Fragment fragment) {
        startPermissionActivity(fragment, new ArrayList<>();
    }


    public static void startPermissionActivity(Fragment fragment, String[] permissions) {
        startPermissionActivity(fragment, PermissionKit.asArrayList(permissions));
    }

    public static void startPermissionActivity(Fragment fragment, List<String> permissions) {
        fragment.startActivityForResult(PermissionSettingPage.getSmartPermissionIntent(fragment.getActivity(), permissions), REQUEST_CODE)
    }
}