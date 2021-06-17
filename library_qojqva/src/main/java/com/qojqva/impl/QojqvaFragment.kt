package com.qojqva.impl

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.util.SparseBooleanArray
import com.qojqva.Qojqva

import com.qojqva.entity.Permission
import com.qojqva.internal.IPermissionCallback
import com.qojqva.toolkit.LogKit
import com.qojqva.toolkit.PermissionKit
import java.util.*

/**
 * @author #Suyghur.
 * Created on 3/29/2021
 */
class QojqvaFragment : Fragment(), Runnable {

    private var mSpecialRequest = false
    private var mDangerousRequest = false
    private var callback: IPermissionCallback? = null
    private var mScreenOrientation = 0

    override fun onAttach(context: Context) {
        super.onAttach(context)
        //如果当前没有锁定屏幕方向就获取当前屏幕方向并进行锁定
        mScreenOrientation = activity.requestedOrientation
        if (mScreenOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            return
        }
        val activityOrientation = activity.resources.configuration.orientation
        try {
            //java.lang.IllegalStateException: Only fullscreen activities can request orientation
            //8.0系统BUG，在 Android 8.0 的手机上可以固定 Activity 的方向，但是这个 Activity 不能是透明的，否则就会抛出异常
            //只需要给 Activity 主题设置 <item name="android:windowIsTranslucent">true</item> 属性即可
            if (activityOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else if (activityOrientation == Configuration.ORIENTATION_PORTRAIT) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        //如果在 Activity 不可见的状态下添加 Fragment 并且去申请权限会导致授权对话框显示不出来
        //所以必须要在 Fragment 的 onResume 来申请权限，这样就可以保证应用回到前台的时候才去申请权限
        if (mSpecialRequest) {
            return
        }
        mSpecialRequest = true
        requestSpecialPermission()
    }

    override fun onDestroy() {
        super.onDestroy()
        callback = null
    }

    override fun onDetach() {
        super.onDetach()
        if (mScreenOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            return
        }
        //取消方向固定
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (callback == null || requestCode != arguments.getInt(REQUEST_CODE)) {
            return
        }
        val tmpCallback = callback!!
        for (i in permissions.indices) {
            val permission = permissions[i]
            if (PermissionKit.isSpecialPermission(permission)) {
                LogKit.d("这个权限是特殊权限，重新开始检查")
                //如果这个权限是特殊权限，那么就重新开始检查
                grantResults[i] = PermissionKit.getPermissionStatus(activity, permission)
                continue
            }

            //重新检查11.0后台定位权限
            if (PermissionKit.isAndroid11 && Permission.ACCESS_BACKGROUND_LOCATION == permission) {
                LogKit.d("重新检查11.0后台定位权限")
                //这个权限是后台定位权限并且当前手机版本是 Android 11 及以上，那么就需要重新进行检查
                //因为只要申请这个后台定位权限，grantResults 数组总对这个权限申请的结果返回 -1（拒绝）
                grantResults[i] = PermissionKit.getPermissionStatus(activity, permission)
                continue
            }

            //重新检查10.0的三个新权限
            if (!PermissionKit.isAndroid10 && (Permission.ACCESS_BACKGROUND_LOCATION == permission
                            || Permission.ACTIVITY_RECOGNITION == permission) || Permission.ACCESS_MEDIA_LOCATION == permission) {
                LogKit.d("重新检查10.0的三个新权限")
                //如果当前版本不符合最低要求，那么就重新进行检查
                grantResults[i] = PermissionKit.getPermissionStatus(activity, permission)
                continue
            }

            //重新检查9.0的新权限
            if (!PermissionKit.isAndroid9 && Permission.ACCEPT_HANDOVER == permission) {
                //如果当前版本不符合最低要求，那么就重新进行检查
                LogKit.d("重新检查9.0的新权限")
                grantResults[i] = PermissionKit.getPermissionStatus(activity, permission)
                continue
            }

            //重新检查8.0的两个新权限
            if (!PermissionKit.isAndroid8 && (Permission.ANSWER_PHONE_CALLS == permission || Permission.READ_PHONE_NUMBERS == permission)) {
                //如果当前版本不符合最低要求，那么就重新进行检查
                LogKit.d("重新检查8.0的两个新权限")
                grantResults[i] = PermissionKit.getPermissionStatus(activity, permission)
            }
        }

        //释放请求码
        requestCodeArray.delete(requestCode)
        //将 Fragment 从 Activity 移除
        detachActivity(activity)

        //获取已授予的权限
        val grantedPermissions = PermissionKit.getGrantedPermissions(permissions, grantResults)
        //如果请求成功的权限集合大小和请求的数组一致则证明全部授予
        if (grantedPermissions.size == permissions.size) {
            Qojqva.interceptor.grantedPermissions(activity, grantedPermissions, true, tmpCallback)
            return
        }

        //获取被拒绝的权限
        val deniedPermissions = PermissionKit.getDeniedPermissions(permissions, grantResults)
        //申请的权限有不同意的，如果有个权限被永久拒绝就返回true，让用户跳转到设置界面开启
        LogKit.d("申请的权限有不同意的，如果有个权限被永久拒绝就返回true，让用户跳转到设置界面开启")
        Qojqva.interceptor.deniedPermissions(activity, deniedPermissions, PermissionKit.isPermissionPermanentDenied(activity, deniedPermissions), tmpCallback)

        if (grantedPermissions.isNotEmpty()) {
            //证明还有一部分权限被成功授予，回调成功接口
            Qojqva.interceptor.grantedPermissions(activity, grantedPermissions, false, tmpCallback)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != arguments.getInt(REQUEST_CODE) || mDangerousRequest) {
            return
        }
        mDangerousRequest = true
        //需要延迟执行，不然有些华为机型授权了但是没效果
        activity.window.decorView.postDelayed(this, 200)
    }

    override fun run() {
        //如果用户离开太久，会导致 Activity 被回收掉
        //所以这里要判断当前 Fragment 是否有被添加到 Activity
        //可在开发者模式中开启不保留活动来复现这个 Bug
        if (!isAdded) {
            return
        }
        //请求其他危险权限
        requestDangerousPermission()
    }

    /**
     * 绑定Activity
     */
    private fun attachActivity(activity: FragmentActivity) {
        activity.supportFragmentManager.beginTransaction().add(this, this.toString()).commitAllowingStateLoss()
    }

    /**
     * 解绑Activity
     */
    private fun detachActivity(activity: FragmentActivity) {
        activity.supportFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
    }

    private fun requestSpecialPermission() {
        val permissions = arguments.getStringArrayList(REQUEST_PERMISSIONS)
        if (permissions.isNullOrEmpty()) {
            //如果空则直接返回
            return
        }
        //是否需要申请特殊权限
        var requestSpecialPermission = false
        //判断当前是否包含特殊权限
        if (PermissionKit.containsSpecialPermission(permissions)) {
            if (permissions.contains(Permission.MANAGE_EXTERNAL_STORAGE) && !PermissionKit.isGrantedSettingPermission(activity)) {
                //当前必须是 Android 11 及以上版本，因为 hasStoragePermission 在旧版本上是拿旧权限做的判断，所以这里需要多判断一次版本
                if (PermissionKit.isAndroid11) {
                    //跳转到存储权限设置界面
                    startActivityForResult(PermissionSettingPage.getStoragePermissionIntent(activity), arguments.getInt(REQUEST_CODE))
                    requestSpecialPermission = true
                }
            }

            if (permissions.contains(Permission.REQUEST_INSTALL_PACKAGES) && !PermissionKit.isGrantedInstallPermission(activity)) {
                //调整到安装权限设置界面
                startActivityForResult(PermissionSettingPage.getInstallPermissionIntent(activity), arguments.getInt(REQUEST_CODE))
                requestSpecialPermission = true
            }

            if (permissions.contains(Permission.SYSTEM_ALERT_WINDOW) && !PermissionKit.isGrantedWindowPermission(activity)) {
                //跳转到悬浮窗设置界面
                startActivityForResult(PermissionSettingPage.getWindowPermissionIntent(activity), arguments.getInt(REQUEST_CODE))
                requestSpecialPermission = true
            }

            if (permissions.contains(Permission.NOTIFICATION_SERVICE) && !PermissionKit.isGrantedNotifyPermission(activity)) {
                // 跳转到通知栏权限设置页面
                startActivityForResult(PermissionSettingPage.getNotifyPermissionIntent(activity), arguments.getInt(REQUEST_CODE))
                requestSpecialPermission = true
            }

            if (permissions.contains(Permission.WRITE_SETTINGS) && !PermissionKit.isGrantedSettingPermission(activity)) {
                // 跳转到系统设置权限设置页面
                startActivityForResult(PermissionSettingPage.getSettingPermissionIntent(activity), arguments.getInt(REQUEST_CODE))
                requestSpecialPermission = true
            }
        }
        //当前必须没有跳转到悬浮窗或安装权限界面
        if (!requestSpecialPermission) {
            requestDangerousPermission()
        }
    }

    /**
     * 申请危险权限
     */
    private fun requestDangerousPermission() {
        val allPermissions = arguments.getStringArrayList(REQUEST_PERMISSIONS)
        if (allPermissions.isNullOrEmpty()) {
            //如果空则直接返回
            return
        }
        val locationPermissions = arrayListOf<String>()
        //Android 10 定位策略发生改变，申请后台定位权限的前提是要有前台定位权限（授予了精确或者模糊任一权限）
        if (PermissionKit.isAndroid10 && allPermissions.contains(Permission.ACCESS_BACKGROUND_LOCATION)) {
            if (allPermissions.contains(Permission.ACCESS_COARSE_LOCATION) && !PermissionKit.isGrantedPermission(activity, Permission.ACCESS_COARSE_LOCATION)) {
                locationPermissions.add(Permission.ACCESS_COARSE_LOCATION)
            }
            if (allPermissions.contains(Permission.ACCESS_FINE_LOCATION) && !PermissionKit.isGrantedPermission(activity, Permission.ACCESS_FINE_LOCATION)) {
                locationPermissions.add(Permission.ACCESS_FINE_LOCATION)
            }
        }

        //如果不需要申请前台定位权限就直接申请危险权限
        if (locationPermissions.isNullOrEmpty()) {
            requestPermissions(allPermissions.toArray(arrayOfNulls(allPermissions.size - 1)), arguments.getInt(REQUEST_CODE))
            return
        }

        //在 Android 10 的机型上，需要先申请前台定位权限，再申请后台定位权限
        beginRequest(activity, locationPermissions, object : IPermissionCallback {
            override fun onGranted(permissions: ArrayList<String>, all: Boolean) {
                if (!all || !isAdded) {
                    return
                }
                requestPermissions(allPermissions.toArray(arrayOfNulls(allPermissions.size - 1)), arguments.getInt(REQUEST_CODE))
            }

            override fun onDenied(permissions: ArrayList<String>, never: Boolean) {
                if (!isAdded) {
                    return
                }
                //如果申请的权限里面只包含定位相关权限，那么就直接返回失败
                if (permissions.size == allPermissions.size - 1) {
                    val grantResults = IntArray(allPermissions.size) { 0 }
                    Arrays.fill(grantResults, PackageManager.PERMISSION_DENIED)
                    onRequestPermissionsResult(arguments.getInt(REQUEST_CODE), allPermissions.toTypedArray(), grantResults)
                    return
                }
                //如果还有其他类型的权限组就继续申请
                requestPermissions(allPermissions.toArray(arrayOfNulls(allPermissions.size - 1)), arguments.getInt(REQUEST_CODE))
            }

            override fun onProxyFinish() {
            }
        })
    }


    companion object {
        /**
         * 请求的权限组
         */
        private const val REQUEST_PERMISSIONS = "request_permissions"

        /**
         * 请求码（自动生成）
         */
        private const val REQUEST_CODE = "request_code"

        /**
         * 权限请求码存放集合
         */
        private val requestCodeArray = SparseBooleanArray()

        /**
         * 开启权限申请
         */
        fun beginRequest(activity: FragmentActivity, permissions: ArrayList<String>, callback: IPermissionCallback) {
            val fragment = QojqvaFragment()
            val bundle = Bundle()
            var requestCode: Int
            //请求码随机生成，避免随机产生之前的请求码，必须进行循环判断
            do {
                requestCode = PermissionKit.getRandomRequestCode()
            } while (requestCodeArray.get(requestCode))
            //标记这个请求码已被占用
            requestCodeArray.put(requestCode, true)
            bundle.putInt(REQUEST_CODE, requestCode)
            bundle.putStringArrayList(REQUEST_PERMISSIONS, permissions)
            fragment.arguments = bundle
            //设置保存实例，不会因为屏幕方向或配置变化重新创建
            fragment.retainInstance = true
            //设置权限回调
            fragment.callback = callback
            //绑定到Activity上
            fragment.attachActivity(activity)
        }
    }
}