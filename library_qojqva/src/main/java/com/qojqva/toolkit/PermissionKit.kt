package com.qojqva.toolkit

import android.app.Activity
import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.support.v4.app.FragmentActivity
import com.qojqva.Qojqva
import com.qojqva.entity.Permission
import java.lang.reflect.InvocationTargetException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.pow

/**
 * @author #Suyghur.
 * Created on 3/29/2021
 */
object PermissionKit {

    /**
     * 是否是 Android 11 及以上版本
     */
    val isAndroid11: Boolean
        get() {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        }

    /**
     * 是否是 Android 10 及以上版本
     */
    val isAndroid10: Boolean
        get() {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        }

    /**
     * 是否是 Android 9 及以上版本
     */
    val isAndroid9: Boolean
        get() {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
        }

    /**
     * 是否是 Android 8 及以上版本
     */
    val isAndroid8: Boolean
        get() {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        }

    /**
     * 是否是 Android 7 及以上版本
     */
    val isAndroid7: Boolean
        get() {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
        }

    /**
     * 是否是 Android 6 及以上版本
     */
    val isAndroid6: Boolean
        get() {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        }

    /**
     * 返回应用程序在清单文件中注册的权限
     */
    fun getManifestPermissions(context: Context): ArrayList<String> {
        return try {
            val requestedPermissions = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions
            asArrayList(*requestedPermissions)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            arrayListOf()
        }
    }

    /**
     * 是否有存储权限
     */
    fun isGrantedStoragePermission(context: Context): Boolean {
        if (isAndroid11) {
            return Environment.isExternalStorageManager()
        }
        return Qojqva.isGranted(context, Permission.Group.STORAGE)
    }

    /**
     * 是否有安装权限
     */
    fun isGrantedInstallPermission(context: Context): Boolean {
        if (isAndroid8) {
            return context.packageManager.canRequestPackageInstalls()
        }
        return true
    }

    /**
     * 是否有悬浮窗权限
     */
    fun isGrantedWindowPermission(context: Context): Boolean {
        if (isAndroid6) {
            return Settings.canDrawOverlays(context)
        }
        return true
    }

    /**
     * 是否有通知栏权限
     */
    fun isGrantedNotifyPermission(context: Context): Boolean {
        if (isAndroid7) {
            return context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
        }

        if (isAndroid6) {
            // 参考 Support 库中的方法： NotificationManagerCompat.from(context).areNotificationsEnabled()
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            return try {
                val method = appOps.javaClass.getMethod("checkOpNoThrow", Integer.TYPE, Integer.TYPE, String::class.java)
                val field = appOps.javaClass.getDeclaredField("OP_POST_NOTIFICATION")
                val value = field[Int::class.java] as Int
                method.invoke(appOps, value, context.applicationInfo.uid, context.packageName) as Int == AppOpsManager.MODE_ALLOWED
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
                true
            } catch (e: NoSuchFieldException) {
                e.printStackTrace()
                true
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
                true
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
                true
            } catch (e: RuntimeException) {
                e.printStackTrace()
                true
            }
        }
        return true
    }

    /**
     * 是否有系统设置权限
     */
    fun isGrantedSettingPermission(context: Context): Boolean {
        if (isAndroid6) {
            return Settings.System.canWrite(context)
        }
        return true
    }

    /**
     * 判断某个权限集合是否包含特殊权限
     */
    fun containsSpecialPermission(permissions: ArrayList<String>): Boolean {
        if (permissions.isEmpty()) {
            return false
        }

        for (permission in permissions) {
            if (isSpecialPermission(permission)) {
                return true
            }
        }
        return false
    }

    /**
     * 判断某个权限是否是特殊权限
     */
    fun isSpecialPermission(permission: String): Boolean {
        return Permission.MANAGE_EXTERNAL_STORAGE == permission || Permission.REQUEST_INSTALL_PACKAGES == permission
                || Permission.SYSTEM_ALERT_WINDOW == permission || Permission.NOTIFICATION_SERVICE == permission
                || Permission.WRITE_SETTINGS == permission
    }


    /**
     * 判断某些权限是否全部被授予
     */
    fun isGrantedPermissions(context: Context, permissions: ArrayList<String>): Boolean {
        //如果是安卓 6.0 以下版本就直接返回 true
        if (!isAndroid6) {
            return true
        }

        for (permission in permissions) {
            if (!isGrantedPermission(context, permission)) {
                return false
            }
        }
        return true
    }

    /**
     * 获取没有授予的权限
     */
    fun getDeniedPermissions(context: Context, permissions: ArrayList<String>): ArrayList<String> {
        val deniedPermissions = arrayListOf<String>()

        //如果是安卓 6.0 以下版本就默认授予
        if (!isAndroid6) {
            return permissions
        }

        for (permission in permissions) {
            if (!isGrantedPermission(context, permission)) {
                deniedPermissions.add(permission)
            }
        }
        return deniedPermissions
    }

    /**
     * 获取没有授予的权限
     *
     * @param permissions   需要请求的权限组
     * @param grantResults  允许结果组
     */
    fun getDeniedPermissions(permissions: Array<String>, grantResults: IntArray): ArrayList<String> {
        val deniedPermissions = arrayListOf<String>()
        for (i in grantResults.indices) {
            //把没有授予过的权限加入到集合中
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                deniedPermissions.add(permissions[i])
            }
        }
        return deniedPermissions
    }


    /**
     * 判断某个权限是否授予
     */
    fun isGrantedPermission(context: Context, permission: String): Boolean {
        //如果是安卓 6.0 以下版本就默认授予
        if (!isAndroid6) {
            return true
        }

        //检测存储权限
        if (Permission.MANAGE_EXTERNAL_STORAGE == permission) {
            return isGrantedStoragePermission(context)
        }

        //检测安装权限
        if (Permission.REQUEST_INSTALL_PACKAGES == permission) {
            return isGrantedInstallPermission(context)
        }

        //检测悬浮窗权限
        if (Permission.SYSTEM_ALERT_WINDOW == permission) {
            return isGrantedWindowPermission(context)
        }

        //检测通知栏权限
        if (Permission.NOTIFICATION_SERVICE == permission) {
            return isGrantedNotifyPermission(context)
        }

        //检测系统权限
        if (Permission.WRITE_SETTINGS == permission) {
            return isGrantedSettingPermission(context)
        }

        //检测10.0的三个新权限
        if (!isAndroid10) {
            if (Permission.ACCESS_BACKGROUND_LOCATION == permission || Permission.ACCESS_MEDIA_LOCATION == permission) {
                return true
            }

            if (Permission.ACTIVITY_RECOGNITION == permission) {
                return context.checkSelfPermission(Permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED
            }
        }

        //检测9.0的一个新权限
        if (!isAndroid9 && Permission.ACCEPT_HANDOVER == permission) {
            return true
        }

        //检测8.0的两个新权限
        if (!isAndroid8) {
            if (Permission.ANSWER_PHONE_CALLS == permission) {
                return true
            }

            if (Permission.READ_PHONE_NUMBERS == permission) {
                return context.checkSelfPermission(Permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
            }
        }

        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 获取某个权限的状态
     * @return  已授权返回[PackageManager.PERMISSION_GRANTED]
     * 未授权返回[PackageManager.PERMISSION_DENIED]
     */
    fun getPermissionStatus(context: Context, permission: String): Int {
        return if (isGrantedPermission(context, permission)) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
    }

    /**
     * 在权限组中检查是否有某个权限是否被永久拒绝
     *
     * @param activity  Activity对象
     * @param permissions   请求的权限
     */
    fun isPermissionPermanentDenied(activity: Activity, permissions: ArrayList<String>): Boolean {
        for (permission in permissions) {
            if (isPermissionPermanentDenied(activity, permission)) {
                return true
            }
        }
        return false
    }

    /**
     * 判断某个权限是否被永久拒绝
     *
     * @param activity  Activity对象
     * @param permission    请求的权限
     */
    fun isPermissionPermanentDenied(activity: Activity, permission: String): Boolean {
        if (!isAndroid6) {
            return false
        }

        //特殊权限不算，本身申请方式和危险权限申请方式不同，因为没有永久拒绝的选项，所以这里返回 false
        if (isSpecialPermission(permission)) {
            return false
        }

        //检测10.0三个新权限
        if (!isAndroid10) {
            if (Permission.ACCESS_BACKGROUND_LOCATION == permission || Permission.ACCESS_MEDIA_LOCATION == permission) {
                return false
            }

            if (Permission.ACTIVITY_RECOGNITION == permission) {
                return activity.checkSelfPermission(Permission.BODY_SENSORS) == PackageManager.PERMISSION_DENIED && !activity.shouldShowRequestPermissionRationale(permission)
            }
        }

        //检测9.0的一个新权限
        if (!isAndroid9 && Permission.ACCEPT_HANDOVER == permission) {
            return false
        }

        //检测8.0的两个新权限
        if (!isAndroid8) {
            if (Permission.ANSWER_PHONE_CALLS == permission) {
                return true
            }

            if (Permission.READ_PHONE_NUMBERS == permission) {
                return activity.checkSelfPermission(Permission.READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED && !activity.shouldShowRequestPermissionRationale(permission)
            }
        }

        return activity.checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED && !activity.shouldShowRequestPermissionRationale(permission)
    }

    /**
     * 获取已授予的权限
     *
     * @param permissions   需要请求的权限组
     * @param grantResults  允许结果组
     */
    fun getGrantedPermissions(permissions: Array<String>, grantResults: IntArray): ArrayList<String> {
        val grantedPermissions = arrayListOf<String>()
        for (i in grantResults.indices) {
            //把授予过的权限加入到集合中
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                grantedPermissions.add(permissions[i])
            }
        }
        return grantedPermissions
    }

    /**
     * 将数组转换成 ArrayList
     * 这里解释一下为什么不用 Arrays.asList
     * 第一是返回的类型不是 java.util.ArrayList 而是 java.util.Arrays.ArrayList
     * 第二是返回的 ArrayList 对象是只读的，也就是不能添加任何元素，否则会抛异常
     */
    fun <T> asArrayList(vararg array: T): ArrayList<T> {
        if (array.isEmpty()) {
            return arrayListOf()
        }
        val list = ArrayList<T>(array.size)
        for (t in array) {
            list.add(t)
        }
        return list
    }

    /**
     * 获得随机的 RequestCode
     */
    fun getRandomRequestCode(): Int {
        // 新版本的 Support 库限制请求码必须小于 65536
        // 旧版本的 Support 库限制请求码必须小于 256
        return Random().nextInt(2.0.pow(8.0).toInt())
    }

    /**
     * 寻找上下文中的 Activity 对象
     */
    fun findFragmentActivity(context: Context?): FragmentActivity? {
        do {
            when (context) {
                is FragmentActivity -> {
                    return context
                }
                is ContextWrapper -> {
                    context.baseContext
                }
                else -> {
                    return null
                }
            }
        } while (context == null)
        return null
    }

    /**
     * 获取当前应用 Apk 在 AssetManager 中的 Cookie
     */
    fun findApkPathCookie(context: Context): Int {
        val assets = context.assets
        val path = context.applicationInfo.sourceDir
        var cookie = 0
        try {
            try {
                val method = assets.javaClass.getDeclaredMethod("addOverlayPath", String::class.java)
                cookie = method.invoke(assets, path) as Int
            } catch (e: Exception) {
                //NoSuchMethodException
                //IllegalAccessException
                //InvocationTargetException
                e.printStackTrace()
                val method = assets.javaClass.getDeclaredMethod("getApkPaths")
                val apkPaths = method.invoke(assets) as Array<String>
                for (i in apkPaths.indices) {
                    if (apkPaths[i] == path) {
                        cookie = i + 1
                        break
                    }
                }
            }
        } catch (e: Exception) {
            //NoSuchMethodException
            //IllegalAccessException
            //InvocationTargetException
            e.printStackTrace()
        }
        return cookie
    }
}