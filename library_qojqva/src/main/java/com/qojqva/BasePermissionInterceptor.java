package com.qojqva;

import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import com.qojqva.entity.Permission;
import com.qojqva.impl.QojqvaFragment;
import com.qojqva.impl.QojqvaProxyActivity;
import com.qojqva.internal.IPermissionCallback;
import com.qojqva.internal.IPermissionInterceptor;

import java.util.List;

/**
 * @author #Suyghur.
 * Created on 2021/10/29
 */
public class BasePermissionInterceptor implements IPermissionInterceptor {
    @Override
    public void requestPermissions(FragmentActivity activity, List<String> permissions, IPermissionCallback callback) {
        QojqvaFragment.beginRequest(activity, permissions, callback);
    }

    @Override
    public void grantedPermissions(FragmentActivity activity, List<String> permissions, boolean all, IPermissionCallback callback) {
        callback.onGranted(permissions, all);
        if (all) {
            QojqvaProxyActivity.finish(activity);
        }
    }

    @Override
    public void deniedPermissions(FragmentActivity activity, List<String> permissions, boolean never, IPermissionCallback callback) {
        callback.onDenied(permissions, never);
        if (never) {
            showPermissionDialog(activity, permissions);
            return;
        }
        if (permissions.size() == 1 && Permission.ACCESS_BACKGROUND_LOCATION == permissions[0]) {
            Toast.makeText(activity, "没有授予后台定位权限，请您选择\"始终允许\"", Toast.LENGTH_SHORT).show();
            QojqvaProxyActivity.finish(activity);
            return;
        }
        Toast.makeText(activity, "授权失败，请正确授予权限", Toast.LENGTH_SHORT).show();
        QojqvaProxyActivity.finish(activity);
    }
}
