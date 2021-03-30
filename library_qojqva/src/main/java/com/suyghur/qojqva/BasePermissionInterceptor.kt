package com.suyghur.qojqva

import android.app.AlertDialog
import android.os.Build
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.suyghur.qojqva.entity.Permission
import com.suyghur.qojqva.impl.QojqvaFragment
import com.suyghur.qojqva.impl.QojqvaProxyActivity
import com.suyghur.qojqva.inernal.IPermissionCallback
import com.suyghur.qojqva.inernal.IPermissionInterceptor

/**
 * @author #Suyghur.
 * Created on 3/29/2021
 */
open class BasePermissionInterceptor : IPermissionInterceptor {

    override fun requestPermissions(activity: FragmentActivity, permissions: ArrayList<String>, callback: IPermissionCallback) {
        QojqvaFragment.beginRequest(activity, permissions, callback)
    }

    override fun grantedPermissions(activity: FragmentActivity, permissions: ArrayList<String>, all: Boolean, callback: IPermissionCallback) {
        callback.onGranted(permissions, all)
        if (activity is QojqvaProxyActivity) {
            activity.finish()
        }
    }

    override fun deniedPermissions(activity: FragmentActivity, permissions: ArrayList<String>, never: Boolean, callback: IPermissionCallback) {
        callback.onDenied(permissions, never)
        if (never) {
            showPermissionDialog(activity, permissions)
            return
        }
        if (permissions.size == 1 && Permission.ACCESS_BACKGROUND_LOCATION == permissions[0]) {
            Toast.makeText(activity, "没有授予后台定位权限，请您选择\"始终允许\"", Toast.LENGTH_SHORT).show()
            if (activity is QojqvaProxyActivity) {
                activity.finish()
            }
            return
        }
        Toast.makeText(activity, "授权失败，请正确授予权限", Toast.LENGTH_SHORT).show()
        if (activity is QojqvaProxyActivity) {
            activity.finish()
        }
    }

    private fun showPermissionDialog(activity: FragmentActivity, permissions: ArrayList<String>) {
        AlertDialog.Builder(activity)
                .setTitle("授权提醒")
                .setCancelable(false)
                .setMessage(getPermissionHint(permissions))
                .setPositiveButton("前往授权") { dialog, _ ->
                    dialog?.dismiss()
                    Qojqva.startPermissionActivity(activity, permissions)
                }
                .setNegativeButton("取消") { dialog, _ ->
                    dialog?.dismiss()
                    if (activity is QojqvaProxyActivity) {
                        activity.finish()
                    }
                }.show()
    }

    private fun getPermissionHint(permissions: ArrayList<String>): String {
        if (permissions.isNullOrEmpty()) {
            return "获取权限失败，请手动授予权限"
        }
        val hints = arrayListOf<String>()
        for (permission in permissions) {
            when (permission) {
                Permission.READ_EXTERNAL_STORAGE,
                Permission.WRITE_EXTERNAL_STORAGE,
                Permission.MANAGE_EXTERNAL_STORAGE -> {
                    val hint = "存储权限"
                    if (!hints.contains(hint)) {
                        hints.add(hint)
                    }
                }
                Permission.CAMERA -> {
                    val hint = "相机权限"
                    if (!hints.contains(hint)) {
                        hints.add(hint)
                    }
                }
                Permission.RECORD_AUDIO -> {
                    val hint = "麦克风权限"
                    if (!hints.contains(hint)) {
                        hints.add(hint)
                    }
                }
                Permission.ACCESS_FINE_LOCATION,
                Permission.ACCESS_COARSE_LOCATION,
                Permission.ACCESS_BACKGROUND_LOCATION -> {
                    val hint: String = if (!permissions.contains(Permission.ACCESS_FINE_LOCATION)
                            && !permissions.contains(Permission.ACCESS_COARSE_LOCATION)) {
                        "后台定位权限"
                    } else {
                        "定位权限"
                    }
                    if (!hints.contains(hint)) {
                        hints.add(hint)
                    }
                }
                Permission.READ_PHONE_STATE,
                Permission.CALL_PHONE,
                Permission.ADD_VOICEMAIL,
                Permission.USE_SIP,
                Permission.READ_PHONE_NUMBERS,
                Permission.ANSWER_PHONE_CALLS -> {
                    val hint = "电话权限"
                    if (!hints.contains(hint)) {
                        hints.add(hint)
                    }
                }
                Permission.GET_ACCOUNTS,
                Permission.READ_CONTACTS,
                Permission.WRITE_CONTACTS -> {
                    val hint = "通讯录权限"
                    if (!hints.contains(hint)) {
                        hints.add(hint)
                    }
                }
                Permission.READ_CALENDAR,
                Permission.WRITE_CALENDAR -> {
                    val hint = "日历权限"
                    if (!hints.contains(hint)) {
                        hints.add(hint)
                    }
                }
                Permission.READ_CALL_LOG,
                Permission.WRITE_CALL_LOG,
                Permission.PROCESS_OUTGOING_CALLS -> {
                    val hint = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) "通话记录权限" else "电话权限"
                    if (!hints.contains(hint)) {
                        hints.add(hint)
                    }
                }
                Permission.BODY_SENSORS -> {
                    val hint = "身体传感器权限"
                    if (!hints.contains(hint)) {
                        hints.add(hint)
                    }
                }
                Permission.ACTIVITY_RECOGNITION -> {
                    val hint = "健身运动权限"
                    if (!hints.contains(hint)) {
                        hints.add(hint)
                    }
                }
                Permission.SEND_SMS,
                Permission.RECEIVE_SMS,
                Permission.READ_SMS,
                Permission.RECEIVE_WAP_PUSH,
                Permission.RECEIVE_MMS -> {
                    val hint = "短信权限"
                    if (!hints.contains(hint)) {
                        hints.add(hint)
                    }
                }
                Permission.REQUEST_INSTALL_PACKAGES -> {
                    val hint = "安装应用权限"
                    if (!hints.contains(hint)) {
                        hints.add(hint)
                    }
                }
                Permission.NOTIFICATION_SERVICE -> {
                    val hint = "通知栏权限"
                    if (!hints.contains(hint)) {
                        hints.add(hint)
                    }
                }
                Permission.SYSTEM_ALERT_WINDOW -> {
                    val hint = "悬浮窗权限"
                    if (!hints.contains(hint)) {
                        hints.add(hint)
                    }
                }
                Permission.WRITE_SETTINGS -> {
                    val hint = "系统设置权限"
                    if (!hints.contains(hint)) {
                        hints.add(hint)
                    }
                }
            }
        }
        if (!hints.isNullOrEmpty()) {
            val builder = StringBuilder()
            for (text in hints) {
                if (builder.isEmpty()) {
                    builder.append(text)
                } else {
                    builder.append("、").append(text)
                }
            }
            builder.append(" ")
            return "获取权限失败，请手动授予$builder"
        }
        return "获取权限失败，请手动授予"
    }
}