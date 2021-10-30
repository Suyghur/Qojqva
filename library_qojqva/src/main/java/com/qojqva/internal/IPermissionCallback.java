package com.qojqva.internal;

import java.util.List;

/**
 * @author #Suyghur.
 * Created on 2021/10/29
 */
public interface IPermissionCallback {
    /**
     * 有权限被同意授予时回调
     *
     * @param permissions 请求成功的权限组
     * @param all         是否全部授予了
     */
    void onGranted(List<String> permissions, boolean all);

    /**
     * 有权限被拒绝授予时回调
     *
     * @param permissions 请求失败的权限组
     * @param never       是否有某个权限被永久拒绝了
     */
    void onDenied(List<String> permissions, boolean never);

    /**
     * 代理Activity是否结束
     */
    void onProxyFinish();
}
