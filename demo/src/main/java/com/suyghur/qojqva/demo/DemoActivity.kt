package com.suyghur.qojqva.demo

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import com.qojqva.Qojqva
import com.qojqva.entity.Permission
import com.qojqva.toolkit.LogKit

/**
 * @author #Suyghur.
 * Created on 3/30/21
 */
class DemoActivity : Activity(), View.OnClickListener {

    private val events: MutableList<Item> = mutableListOf(
            Item(0, "申请单个危险权限"),
            Item(1, "申请多个危险权限"),
            Item(2, "申请定位权限组"),
            Item(3, "申请新版存储权限"),
            Item(4, "申请旧版存储权限"),
            Item(5, "申请安装包权限"),
            Item(6, "申请悬浮窗权限"),
            Item(7, "申请通知栏权限"),
            Item(8, "申请系统设置权限"),
            Item(9, "跳转到应用详情页")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        for (event in events) {
            with(Button(this)) {
                tag = event.id
                text = event.text
                setOnClickListener(this@DemoActivity)
                layout.addView(this)
            }
        }
        setContentView(layout)
    }


    override fun onStart() {
        super.onStart()
        LogKit.d("${DemoActivity::class.java.simpleName}.onStart")
    }

    override fun onResume() {
        super.onResume()
        LogKit.d("${DemoActivity::class.java.simpleName}.onResume")
    }

    override fun onPause() {
        super.onPause()
        LogKit.d("${DemoActivity::class.java.simpleName}.onPause")
    }

    override fun onRestart() {
        super.onRestart()
        LogKit.d("${DemoActivity::class.java.simpleName}.onRestart")
    }

    override fun onStop() {
        super.onStop()
        LogKit.d("${DemoActivity::class.java.simpleName}.onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        LogKit.d("${DemoActivity::class.java.simpleName}.onDestroy")
    }


    override fun onClick(v: View?) {
        v?.apply {
            when (tag as Int) {
                0 -> {
                    Qojqva.with().permission(Permission.CAMERA).request(this@DemoActivity, object : IPermissionCallback {
                        override fun onGranted(permissions: ArrayList<String>, all: Boolean) {
                            if (all) {
                                toast("获取拍照权限成功")
                            }
                        }

                        override fun onDenied(permissions: ArrayList<String>, never: Boolean) {
                        }

                        override fun onProxyFinish() {
                            LogKit.d("onProxyFinish")
                        }

                    })
                }
                1 -> {
                    Qojqva.with().permission(Permission.RECORD_AUDIO).permission(Permission.Group.CALENDAR).request(this@DemoActivity, object : IPermissionCallback {
                        override fun onGranted(permissions: ArrayList<String>, all: Boolean) {
                            toast("获取录音和日历权限成功")
                        }

                        override fun onDenied(permissions: ArrayList<String>, never: Boolean) {
                        }

                        override fun onProxyFinish() {
                        }
                    })
                }
                2 -> {
                    Qojqva.with().permission(Permission.Group.LOCATION).request(this@DemoActivity, object : IPermissionCallback {
                        override fun onGranted(permissions: ArrayList<String>, all: Boolean) {
                            toast("获取定位权限成功")
                        }

                        override fun onDenied(permissions: ArrayList<String>, never: Boolean) {
                        }

                        override fun onProxyFinish() {
                        }
                    })
                }
                3 -> {
                    val delayMillis = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                        toast("当前版本不是 Android 11 以上，会自动变更为旧版的请求方式")
                        2000L
                    } else {
                        0L
                    }
                    postDelayed({
                        //不适配 Android 11 可以这样写permission(Permission.Group.STORAGE)
                        //适配 Android 11 需要这样写，这里无需再写 Permission.Group.STORAGE
                        Qojqva.with().permission(Permission.MANAGE_EXTERNAL_STORAGE).request(this@DemoActivity, object : IPermissionCallback {
                            override fun onGranted(permissions: ArrayList<String>, all: Boolean) {
                                toast("获取存储权限成功")
                            }

                            override fun onDenied(permissions: ArrayList<String>, never: Boolean) {
                            }

                            override fun onProxyFinish() {
                            }
                        })
                    }, delayMillis)
                }
                4->{
                    val delayMillis = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                        toast("当前版本不是 Android 11 以上，会自动变更为旧版的请求方式")
                        2000L
                    } else {
                        0L
                    }
                    postDelayed({
                        //不适配 Android 11 可以这样写permission(Permission.Group.STORAGE)
                        //适配 Android 11 需要这样写，这里无需再写 Permission.Group.STORAGE
                        Qojqva.with().permission(Permission.Group.STORAGE).request(this@DemoActivity, object : IPermissionCallback {
                            override fun onGranted(permissions: ArrayList<String>, all: Boolean) {
                                toast("获取存储权限成功")
                            }

                            override fun onDenied(permissions: ArrayList<String>, never: Boolean) {
                            }

                            override fun onProxyFinish() {
                            }
                        })
                    }, delayMillis)
                }
                5 -> {
                    Qojqva.with().permission(Permission.REQUEST_INSTALL_PACKAGES).request(this@DemoActivity, object : IPermissionCallback {
                        override fun onGranted(permissions: ArrayList<String>, all: Boolean) {
                            toast("获取安装包权限成功")
                        }

                        override fun onDenied(permissions: ArrayList<String>, never: Boolean) {
                        }

                        override fun onProxyFinish() {
                        }
                    })
                }
               6 -> {
                    Qojqva.with().permission(Permission.SYSTEM_ALERT_WINDOW).request(this@DemoActivity, object : IPermissionCallback {
                        override fun onGranted(permissions: ArrayList<String>, all: Boolean) {
                            toast("获取悬浮窗权限成功")
                        }

                        override fun onDenied(permissions: ArrayList<String>, never: Boolean) {
                        }

                        override fun onProxyFinish() {
                        }
                    })
                }
                7 -> {
                    Qojqva.with().permission(Permission.NOTIFICATION_SERVICE).request(this@DemoActivity, object : IPermissionCallback {
                        override fun onGranted(permissions: ArrayList<String>, all: Boolean) {
                            toast("获取通知栏权限成功")
                        }

                        override fun onDenied(permissions: ArrayList<String>, never: Boolean) {
                        }

                        override fun onProxyFinish() {
                        }
                    })
                }
                8 -> {
                    Qojqva.with().permission(Permission.WRITE_SETTINGS).request(this@DemoActivity, object : IPermissionCallback {
                        override fun onGranted(permissions: ArrayList<String>, all: Boolean) {
                            toast("获取系统设置权限成功")
                        }

                        override fun onDenied(permissions: ArrayList<String>, never: Boolean) {
                        }

                        override fun onProxyFinish() {
                        }
                    })
                }
                9 -> Qojqva.startPermissionActivity(this@DemoActivity)
            }
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

}