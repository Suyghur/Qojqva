package com.suyghur.qojqva.toolkit

import android.app.Activity
import android.content.Context
import android.content.res.XmlResourceParser
import android.os.Build
import androidx.fragment.app.FragmentActivity
import com.suyghur.qojqva.entity.Permission
import com.suyghur.qojqva.exception.ManifestRegisterException
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

/**
 * 权限错误检测类
 * @author #Suyghur.
 * Created on 3/29/2021
 */
object PermissionChecker {

    /**
     * 检查 Activity 的状态是否正常
     *
     * @param debugMode 是否是调试模式
     * @return  是否检查通过
     */
    fun checkActivityStatus(activity: Activity, debugMode: Boolean): Boolean {
        // 检查当前 Activity 状态是否是正常的，如果不是则不请求权限
        if (activity !is FragmentActivity) {
            require(!debugMode) {
                //这个 Activity 对象必须是 FragmentActivity 的子类，请直接继承 AppCompatActivity
                "The Activity must be a subclass of FragmentActivity, Please directly inherit AppCompatActivity"
            }
            return false
        }
        if (activity.isFinishing()) {
            check(!debugMode) {
                //这个 Activity 对象当前不能是关闭状态，这种情况常出现在执行异步请求后申请权限，请自行在外层判断 Activity 状态是否正常之后再进入权限申请
                "The Activity has been finishing, Please manually determine the status of the Activity"
            }
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed) {
            check(!debugMode) {
                //这个 Activity 对象当前不能是销毁状态，这种情况常出现在执行异步请求后申请权限，请自行在外层判断 Activity 状态是否正常之后再进入权限申请
                "The Activity has been destroyed, Please manually determine the status of the Activity"
            }
            return false
        }
        return true
    }

    /**
     * 检查传入的权限是否符合要求
     *
     * @param requestPermissions    请求的权限组
     * @param debugMode 是否是调试模式
     * @return 是否检查通过
     */
    fun checkPermissionArgument(requestPermissions: ArrayList<String>, debugMode: Boolean): Boolean {
        if (requestPermissions.isNullOrEmpty()) {
            require(!debugMode) {
                "The requested permission cannot be empty"
            }
            return false
        }
        if (debugMode) {
            val allPermissions = arrayListOf<String>()
            val fields = Permission.javaClass.declaredFields
            for (field in fields) {
                if (String::class.java != field.type) {
                    continue
                }
                try {
                    allPermissions.add(field.get(null) as String)
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                }
            }
            for (permission in requestPermissions) {
                if (!allPermissions.contains(permission)) {
                    //请不要申请危险权限和特殊权限之外的权限
                    throw IllegalArgumentException("The $permission is not a dangerous permission or special permission")
                }
            }
        }
        return true
    }

    /**
     * 检查存储权限
     *
     * @param requestPermissions    请求的权限组
     * @param scopedStorage 是否适配了分区存储
     */
    fun checkStoragePermission(context: Context, requestPermissions: ArrayList<String>, scopedStorage: Boolean) {
        //如果请求的权限中没有包含外部存储相关的权限，那么就直接返回
        if (!requestPermissions.contains(Permission.MANAGE_EXTERNAL_STORAGE) && !requestPermissions.contains(Permission.READ_EXTERNAL_STORAGE)
                && !requestPermissions.contains(Permission.WRITE_EXTERNAL_STORAGE)) {
            return
        }
        val cookie = PermissionKit.findApkPathCookie(context)
        if (cookie == -1) {
            return
        }
        try {
            val parser = context.assets.openXmlResourceParser(cookie, "AndroidManifest.xml")
            //是否督导文件尾
            // 是否读取到文件尾
            while (parser.eventType != XmlResourceParser.END_DOCUMENT) {
                // 当前节点是否为标签头部
                if (parser.eventType == XmlResourceParser.START_TAG) {
                    // 获取标签名称
                    val nodeName: String = parser.name
                    if ("application" == nodeName) {
                        val namespace = "http://schemas.android.com/apk/res/android"
                        val targetSdkVersion = context.applicationInfo.targetSdkVersion
                        val requestLegacyExternalStorage: Boolean = parser.getAttributeBooleanValue(namespace, "requestLegacyExternalStorage", false)
                        //如果在已经适配 Android 10 的情况下
                        check(!(targetSdkVersion >= Build.VERSION_CODES.Q && !requestLegacyExternalStorage
                                && (requestPermissions.contains(Permission.MANAGE_EXTERNAL_STORAGE) || !scopedStorage))) {
                            //请在清单文件 Application 节点中注册 android:requestLegacyExternalStorage="true" 属性
                            //否则就算申请了权限，也无法在 Android 10 的设备上正常读写外部存储上的文件
                            //如果你的项目已经全面适配了分区存储，请调用 XXPermissions.setScopedStorage(true) 来跳过该检查
                            "Please register the android:requestLegacyExternalStorage=\"true\" attribute in the manifest file"
                        }

                        //如果在已经适配 Android 11 的情况下
                        require(!(targetSdkVersion >= Build.VERSION_CODES.R && !requestPermissions.contains(Permission.MANAGE_EXTERNAL_STORAGE) && !scopedStorage)) {
                            //1. 适配分区存储的特性，并在 Application 初始化时调用 XXPermissions.setScopedStorage(true)
                            //2. 如果不想适配分区存储，则需要使用 Permission.MANAGE_EXTERNAL_STORAGE 来申请权限
                            //上面两种方式需要二选一，否则无法在 Android 11 的设备上正常读写外部存储上的文件
                            "Please adapt the scoped storage, or use the MANAGE_EXTERNAL_STORAGE permission"
                        }
                        break
                    }
                }
                // 读取下一个标签
                parser.next()
            }
            parser.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: XmlPullParserException) {
            e.printStackTrace()
        }
    }

    /**
     * 检查定位权限
     *
     * @param requestPermissions    请求的权限组
     */
    fun checkLocationPermission(requestPermissions: ArrayList<String>) {
        //判断是否包含后台定位权限
        if (!requestPermissions.contains(Permission.ACCESS_BACKGROUND_LOCATION)) {
            return
        }
        for (permission in requestPermissions) {
            if (Permission.ACCESS_FINE_LOCATION == permission || Permission.ACCESS_COARSE_LOCATION == permission || Permission.ACCESS_BACKGROUND_LOCATION == permission) {
                continue
            }
            //因为包含了后台定位权限，所以请不要申请和定位无关的权限，因为在 Android 11 上面，后台定位权限不能和其他非定位的权限一起申请
            //否则会出现只申请了后台定位权限，其他权限会被回绝掉的情况，因为在 Android 11 上面，后台定位权限是要跳 Activity，并非弹 Dialog
            //如果你的项目没有后台定位的需求，请不要一同申请 Permission.ACCESS_BACKGROUND_LOCATION 权限
            throw IllegalArgumentException("Because it includes background location permissions, do not apply for permissions unrelated to location")
        }
    }

    /**
     * 检查targetSdkVersion 是否符合要求
     *
     * @param requestPermissions    请求的权限组
     */
    fun checkTargetSdkVersion(context: Context, requestPermissions: ArrayList<String>) {
        //targetSdkVersion最低版本要求
        val targetSdkMinVersion = if (requestPermissions.contains(Permission.MANAGE_EXTERNAL_STORAGE)) {
            //必须设置 targetSdkVersion >= 30 才能正常检测权限，否则请使用 Permission.Group.STORAGE 来申请存储权限
            Build.VERSION_CODES.R
        } else if (requestPermissions.contains(Permission.ACCEPT_HANDOVER)) {
            Build.VERSION_CODES.P
        } else if (requestPermissions.contains(Permission.ACCESS_BACKGROUND_LOCATION) || requestPermissions.contains(Permission.ACTIVITY_RECOGNITION)
                || requestPermissions.contains(Permission.ACCESS_MEDIA_LOCATION)) {
            Build.VERSION_CODES.Q
        } else if (requestPermissions.contains(Permission.REQUEST_INSTALL_PACKAGES) || requestPermissions.contains(Permission.ANSWER_PHONE_CALLS)
                || requestPermissions.contains(Permission.READ_PHONE_NUMBERS)) {
            Build.VERSION_CODES.O
        } else {
            Build.VERSION_CODES.M
        }
        //必须设置正确的 targetSdkVersion 才能正常检测权限
        if (context.applicationInfo.targetSdkVersion < targetSdkMinVersion) {
            throw RuntimeException("The targetSdkVersion SDK must be $targetSdkMinVersion or more")
        }
    }

    /**
     * 处理和优化已经过时的权限
     *
     * @param requestPermissions    请求的权限组
     */
    fun optimizeDeprecatedPermission(requestPermissions: ArrayList<String>) {
        //如果本次申请包含了 Android 11 存储权限
        if (requestPermissions.contains(Permission.MANAGE_EXTERNAL_STORAGE)) {
            if (requestPermissions.contains(Permission.READ_EXTERNAL_STORAGE) || requestPermissions.contains(Permission.WRITE_EXTERNAL_STORAGE)) {
                //检测是否有旧版的存储权限，有的话直接抛出异常，请不要自己动态申请这两个权限
                throw IllegalArgumentException("Please do not apply for these two permissions dynamically")
            }
            if (!PermissionKit.isAndroid11) {
                //自动添加旧版的存储权限，因为旧版的系统不支持申请新版的存储权限
                requestPermissions.add(Permission.READ_EXTERNAL_STORAGE)
                requestPermissions.add(Permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        if (!PermissionKit.isAndroid8 && requestPermissions.contains(Permission.READ_PHONE_NUMBERS) && !requestPermissions.contains(Permission.READ_PHONE_STATE)) {
            //自动添加旧版的读取电话号码权限，因为旧版的系统不支持申请新版的权限
            requestPermissions.add(Permission.READ_PHONE_STATE)
        }

        if (!PermissionKit.isAndroid10 && requestPermissions.contains(Permission.ACTIVITY_RECOGNITION) && !requestPermissions.contains(Permission.BODY_SENSORS)) {
            //自动添加传感器权限，因为这个权限是从 Android 10 开始才从传感器权限中剥离成独立权限
            requestPermissions.add(Permission.BODY_SENSORS)
        }
    }

    /**
     * 检测权限有没有在清单文件中注册
     *
     * @param requestPermissions    请求的权限组
     */
    fun checkPermissionManifest(context: Context, requestPermissions: ArrayList<String>) {
        val manifestPermissions = PermissionKit.getManifestPermissions(context)
        if (manifestPermissions.isNullOrEmpty()) {
            throw ManifestRegisterException()
        }

        val minSdkVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.applicationInfo.minSdkVersion
        } else {
            Build.VERSION_CODES.M
        }

        for (permission in requestPermissions) {
            if (minSdkVersion < Build.VERSION_CODES.R) {
                if (Permission.MANAGE_EXTERNAL_STORAGE == permission) {
                    if (!manifestPermissions.contains(Permission.READ_EXTERNAL_STORAGE)) {
                        //为了保证能够在旧版的系统上正常运行，必须要在清单文件中注册此权限
                        throw ManifestRegisterException(Permission.READ_EXTERNAL_STORAGE)
                    }
                    if (!manifestPermissions.contains(Permission.WRITE_EXTERNAL_STORAGE)) {
                        //为了保证能够在旧版的系统上正常运行，必须要在清单文件中注册此权限
                        throw ManifestRegisterException(Permission.WRITE_EXTERNAL_STORAGE)
                    }
                }
            }

            if (minSdkVersion < Build.VERSION_CODES.Q) {
                if (Permission.ACTIVITY_RECOGNITION == permission && !manifestPermissions.contains(Permission.BODY_SENSORS)) {
                    //为了保证能够在旧版的系统上正常运行，必须要在清单文件中注册此权限
                    throw ManifestRegisterException(Permission.BODY_SENSORS)
                }
            }

            if (minSdkVersion < Build.VERSION_CODES.O) {
                if (Permission.READ_PHONE_NUMBERS == permission && !manifestPermissions.contains(Permission.READ_PHONE_STATE)) {
                    //为了保证能够在旧版的系统上正常运行，必须要在清单文件中注册此权限
                    throw ManifestRegisterException(Permission.READ_PHONE_STATE)
                }
            }

            if (Permission.NOTIFICATION_SERVICE == permission) {
                //不检测通知栏权限有没有在清单文件中注册，因为这个权限是框架虚拟出来的，有没有在清单文件中注册都没关系
                continue
            }

            if (!manifestPermissions.contains(permission)) {
                throw ManifestRegisterException(permission)
            }
        }
    }

}