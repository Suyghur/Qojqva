package com.qojqva.internal;

import android.support.v4.app.FragmentActivity;

import java.util.List;

/**
 * @author #Suyghur.
 * Created on 2021/10/29
 */
public interface IPermissionInterceptor {

    /**
     * 权限申请拦截，可在此处先弹 Dialog 再申请权限
     */
    void requestPermissions(FragmentActivity activity, List<String> permissions, IPermissionCallback callback);

    /**
     * 权限授予回调拦截 {@link IPermissionCallback#onGranted(List, boolean)}
     */
    void grantedPermissions(FragmentActivity activity, List<String> permissions, boolean all, IPermissionCallback callback);

    /**
     * 权限拒绝回调拦截 {@link IPermissionCallback#onDenied(List, boolean)}
     */
    void deniedPermissions(FragmentActivity activity, List<String> permissions, boolean never, IPermissionCallback callback);
}
