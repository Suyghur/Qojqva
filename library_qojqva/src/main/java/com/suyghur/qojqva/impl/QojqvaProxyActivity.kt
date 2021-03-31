package com.suyghur.qojqva.impl

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.suyghur.qojqva.Qojqva
import com.suyghur.qojqva.internal.IPermissionCallback
import com.suyghur.qojqva.toolkit.LogKit
import com.suyghur.qojqva.toolkit.PermissionChecker
import com.suyghur.qojqva.toolkit.PermissionKit


/**
 * @author #Suyghur.
 * Created on 3/29/2021
 */
class QojqvaProxyActivity : AppCompatActivity() {

    private lateinit var layout: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layout = FrameLayout(this)
        setContentView(layout)
        doRequestPermissions()
    }


    override fun onDestroy() {
        super.onDestroy()
        mPermissions?.clear()
        mPermissions = null
        mCallback = null
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //原来这个finish是在BasePermissionInterceptor中用户点击"前往授权后调用"
        //跳转设置页面后如果（用户按下Home -> 再返回回到设置页 -> 按下返回键），这个流程中外部Activity（不是ProxyActivity）会回调onDestroy
        //所以这里要根据requestCode进行判断然后finish调ProxyActivity
        if (requestCode == Qojqva.REQUEST_CODE && !isFinishing) {
            finish()
        }
    }

    private fun doRequestPermissions() {
        if (mPermissions.isNullOrEmpty()) {
            return
        }
        if (mCallback == null) {
            return
        }


        //当前是否调试模式
        val debugMode = this.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

        //检查当前 Activity 状态是否正常，如果不是直接返回
        val activity = PermissionKit.findFragmentActivity(this) ?: return
        if (!PermissionChecker.checkActivityStatus(activity, debugMode)) {
            return
        }

        //必须要传入正常的权限或者权限组才能申请权限
        if (!PermissionChecker.checkPermissionArgument(mPermissions!!, debugMode)) {
            return
        }

        if (debugMode) {
            //检查申请的存储权限是否符合规范
            PermissionChecker.checkStoragePermission(activity, mPermissions!!, Qojqva.scopedStorage)
            //检查申请的定位权限是否符合规范
            PermissionChecker.checkLocationPermission(mPermissions!!)
            //检查申请的权限和 targetSdkVersion 版本是否能吻合
            PermissionChecker.checkTargetSdkVersion(activity, mPermissions!!)
        }

        //优化所以申请的权限
        PermissionChecker.optimizeDeprecatedPermission(mPermissions!!)

        if (debugMode) {
            //检查权限有没有注册
            PermissionChecker.checkPermissionManifest(activity, mPermissions!!)
        }

        if (PermissionKit.isGrantedPermissions(activity, mPermissions!!)) {
            //证明这些权限已经全部授予过，直接回调成功
            mCallback?.apply {
                onGranted(mPermissions!!, true)
                activity.finish()
            }
            return
        }

        //申请没有授予过的权限
        Qojqva.interceptor.requestPermissions(activity, mPermissions!!, mCallback!!)

    }

    companion object {
        private var mPermissions: ArrayList<String>? = null

        private var mCallback: IPermissionCallback? = null

        fun start(context: Context, permissions: ArrayList<String>, callback: IPermissionCallback) {
            mPermissions?.apply {
                clear()
                mPermissions = null
            }
            if (mCallback != null) {
                mCallback = null
            }
            mPermissions = permissions
            mCallback = callback
            context.startActivity(Intent(context, QojqvaProxyActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }

        fun finish(context: Context) {
            if (context is QojqvaProxyActivity) {
                context.finish()
            }
        }
    }
}