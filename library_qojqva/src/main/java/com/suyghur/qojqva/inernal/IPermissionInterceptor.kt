package com.suyghur.qojqva.inernal

import androidx.fragment.app.FragmentActivity


/**
 * @author #Suyghur.
 * Created on 3/29/2021
 */
interface IPermissionInterceptor {

    /**
     * 权限申请拦截，可在此处先弹 Dialog 再申请权限
     */
    fun requesetPermission(activity: FragmentActivity, permissions: MutableList<String>, callback: IPermissionCallback)

    /**
     * 权限授予回调拦截 [IPermissionCallback.onGranted]
     */
    fun grantedPermissions(activity: FragmentActivity, permissions: MutableList<String>, all: Boolean, callback: IPermissionCallback)

    /**
     * 权限拒绝回调拦截 [IPermissionCallback.onDenied]
     */
    fun deniedPermissions(activity: FragmentActivity, permissions: MutableList<String>, never: Boolean, callback: IPermissionCallback)
}