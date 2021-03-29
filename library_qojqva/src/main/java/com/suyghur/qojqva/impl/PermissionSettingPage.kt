package com.suyghur.qojqva.impl

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import com.suyghur.qojqva.toolkit.PermissionKit

/**
 * @author #Suyghur.
 * Created on 3/29/2021
 */
object PermissionSettingPage {

    /**
     * 根据传入的权限自动选择最合适的权限设置页
     */
    fun getSmartPermissionIntent(context: Context, deniedPermissions: MutableList<String>): Intent? {
        //如果失败的权限里面不包含特殊权限
if (deniedPermissions==null||deniedPermissions.isEmpty()||!PermissionKit)
    }

    /**
     * 获取应用详情界面意图
     */
    fun getApplicationDetailsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(getPackageNameUri(context))
    }

    /**
     * 获取存储权限设置界面意图
     */
    fun getStoragePermissionIntent(context: Context): Intent? {
        var intent: Intent? = null
        if (PermissionKit.isAndroid11) {
            intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).setData(getPackageNameUri(context))
        }
        if (intent == null || !isActivityExists(context, intent)) {
            intent = getApplicationDetailsIntent(context)
        }
        return intent
    }

    /**
     * 获取安装权限设置界面意图
     */
    fun getInstallPermissionIntent(context: Context): Intent? {
        var intent: Intent? = null
        if (PermissionKit.isAndroid8) {
            intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData(getPackageNameUri(context))
        }
        if (intent == null || !isActivityExists(context, intent)) {
            intent = getApplicationDetailsIntent(context)
        }
        return intent
    }


    /**
     * 获取悬浮窗权限设置界面意图
     */
    fun getWindowPermissionIntent(context: Context): Intent? {
        var intent: Intent? = null
        if (PermissionKit.isAndroid6) {
            intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            //在 Android 11 上面不能加包名跳转，因为就算加了也没有效果
            //还有人反馈在 Android 11 的 TV 模拟器上会出现崩溃的情况
            //https://developer.android.google.cn/reference/android/provider/Settings#ACTION_MANAGE_OVERLAY_PERMISSION
            if (!PermissionKit.isAndroid11) {
                intent.data = getPackageNameUri(context)
            }
        }
        if (intent == null || !isActivityExists(context, intent)) {
            intent = getApplicationDetailsIntent(context)
        }
        return intent
    }

    /**
     * 获取通知栏权限设置界面意图
     */
    fun getNotifyPermissionIntent(context: Context): Intent? {
        var intent: Intent? = null
        if (PermissionKit.isAndroid8) {
            intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        if (intent == null || !isActivityExists(context, intent)) {
            intent = getApplicationDetailsIntent(context)
        }
        return intent
    }

    /**
     * 获取系统设置权限界面意图
     */
    fun getSettingPermissionIntent(context: Context): Intent? {
        var intent: Intent? = null
        if (PermissionKit.isAndroid6) {
            intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).setData(getPackageNameUri(context))
        }
        if (intent == null || !isActivityExists(context, intent)) {
            intent = getApplicationDetailsIntent(context)
        }
        return intent
    }

    /**
     * 判断这个意图的 Activity 是否存在
     */
    private fun isActivityExists(context: Context, intent: Intent?): Boolean {
        return context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()
    }

    /**
     * 获取包名 Uri 对象
     */
    private fun getPackageNameUri(context: Context): Uri {
        return Uri.parse("package:${context.packageName}")
    }
}