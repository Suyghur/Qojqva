package com.suyghur.qojqva.demo

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import com.suyghur.qojqva.Qojqva
import com.suyghur.qojqva.entity.Permission
import com.suyghur.qojqva.inernal.IPermissionCallback
import com.suyghur.qojqva.toolkit.LogKit

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
            Item(4, "申请安装包权限"),
            Item(5, "申请悬浮窗权限"),
            Item(6, "申请通知栏权限"),
            Item(7, "申请系统设置权限"),
            Item(8, "跳转到应用详情页")
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

                    })
                }
            }
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }


}