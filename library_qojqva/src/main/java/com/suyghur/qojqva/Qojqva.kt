package com.suyghur.qojqva

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import com.suyghur.qojqva.impl.QojqvaProxyActivity
import com.suyghur.qojqva.impl.PermissionSettingPage
import com.suyghur.qojqva.inernal.IPermissionCallback
import com.suyghur.qojqva.inernal.IPermissionInterceptor
import com.suyghur.qojqva.toolkit.LogKit
import com.suyghur.qojqva.toolkit.PermissionKit

/**
 * @author #Suyghur.
 * Created on 3/29/2021
 */
class Qojqva private constructor() {

    /**
     * 权限列表
     */
    private var mPermission: ArrayList<String>? = null

    /**
     * 添加权限
     */
    fun permission(permission: String): Qojqva {
        if (mPermission == null) {
            mPermission = arrayListOf()
        }
        mPermission?.add(permission)
        return this
    }

    /**
     * 添加权限组
     */
    fun permission(permissions: Array<String>): Qojqva {
        return permission(PermissionKit.asArrayList(*permissions))
    }

    fun permission(permissions: ArrayList<String>): Qojqva {
        if (mPermission == null) {
            mPermission = permissions
        } else {
            mPermission?.addAll(permissions)
        }
        return this
    }

    fun request(context: Context, callback: IPermissionCallback) {
        if (mPermission == null) {
            mPermission = arrayListOf()
        }
        QojqvaProxyActivity.start(context, mPermission!!, callback)
    }

    companion object {

        /**
         * 权限设置页跳转请求码
         */
        const val REQUEST_CODE = 1024 + 1

        /**
         * 权限请求拦截器
         */
        var interceptor: IPermissionInterceptor = BasePermissionInterceptor()

        /**
         * 分区存储
         */
        var scopedStorage = false

        fun with(): Qojqva {
            return Qojqva()
        }

        /**
         * 判断一个或多个权限是否全部授予了
         */
        fun isGranted(context: Context, permission: String): Boolean {
            return PermissionKit.isGrantedPermission(context, permission)
        }

        fun isGranted(context: Context, permissions: ArrayList<String>): Boolean {
            return PermissionKit.isGrantedPermissions(context, permissions)
        }

        fun isGranted(context: Context, permissions: Array<String>): Boolean {
            return isGranted(context, PermissionKit.asArrayList(*permissions))
        }

        /**
         * 获取没有授予的权限
         */
        fun getDenied(context: Context, permissions: ArrayList<String>): ArrayList<String> {
            return PermissionKit.getDeniedPermissions(context, permissions)
        }

        fun getDenied(context: Context, permissions: Array<String>): ArrayList<String> {
            return getDenied(context, PermissionKit.asArrayList(*permissions))
        }

        /**
         * 判断某个权限是否是特殊权限
         */
        fun isSpecial(permission: String): Boolean {
            return PermissionKit.isSpecialPermission(permission)
        }

        /**
         * 判断一个或多个权限是否被永久拒绝了（注意不能在请求权限之前调用，应该在 [IPermissionCallback.onDenied] 方法中调用）
         */
        fun isPermissionDenied(activity: Activity, permission: String): Boolean {
            return PermissionKit.isPermissionPermanentDenied(activity, permission)
        }

        fun isPermissionDenied(activity: Activity, permissions: ArrayList<String>): Boolean {
            return PermissionKit.isPermissionPermanentDenied(activity, permissions)
        }

        fun isPermissionDenied(activity: Activity, permissions: Array<String>): Boolean {
            return isPermissionDenied(activity, PermissionKit.asArrayList(*permissions))
        }

        fun startPermissionActivity(context: Context) {
            startPermissionActivity(context, arrayListOf<String>())
        }

        fun startPermissionActivity(context: Context, permission: String) {
            startPermissionActivity(context, PermissionKit.asArrayList(permission))
        }

        fun startPermissionActivity(context: Context, permissions: ArrayList<String>) {
            val activity = PermissionKit.findFragmentActivity(context)
            if (activity != null) {
                startPermissionActivity(activity, permissions)
                return
            }
            context.startActivity(PermissionSettingPage.getSmartPermissionIntent(context, permissions).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }

        fun startPermissionActivity(context: Context, permissions: Array<String>) {
            startPermissionActivity(context, PermissionKit.asArrayList(*permissions))
        }

        fun startPermissionActivity(activity: Activity, permission: String) {
            startPermissionActivity(activity, PermissionKit.asArrayList(permission))
        }

        fun startPermissionActivity(activity: Activity, permissions: Array<String>) {
            startPermissionActivity(activity, PermissionKit.asArrayList(*permissions))
        }

        fun startPermissionActivity(activity: Activity, permissions: ArrayList<String>) {
            activity.startActivityForResult(PermissionSettingPage.getSmartPermissionIntent(activity, permissions), REQUEST_CODE)
        }

        fun startPermissionActivity(fragment: Fragment, permissions: ArrayList<String>) {
            val activity = fragment.requireActivity()
            fragment.startActivityForResult(PermissionSettingPage.getSmartPermissionIntent(activity, permissions), REQUEST_CODE)
        }

        fun startPermissionActivity(fragment: Fragment) {
            startPermissionActivity(fragment, arrayListOf())
        }

        fun startPermissionActivity(fragment: Fragment, permissions: String) {
            startPermissionActivity(fragment, PermissionKit.asArrayList(permissions))
        }

        fun startPermissionActivity(fragment: Fragment, permissions: Array<String>) {
            startPermissionActivity(fragment, PermissionKit.asArrayList(*permissions))
        }

    }
}