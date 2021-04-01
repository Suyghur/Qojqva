# Qojqva

* 适配 Android 11 的权限请求框架
* 适配所有 Android 版本的权限请求框架
* 简洁易用：采用链式调用的方式，使用只需一句代码
* 向下兼容属性：新权限在旧系统可以正常申请，框架会做自动适配，无需调用者适配
* 自动检测错误：如果出现低级错误框架会主动抛出异常给调用者（仅在 Debug 下判断，把 Bug 扼杀在摇篮中）
* 允许在AppCompatActivity、FragmentActivity甚至是Activity中申请权限

## 集成步骤

* 项目级build.gradle下增加jitpack的maven地址
```groovy
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```

* 应用级build.gradle下引入框架
```groovy
dependencies {
	...
	implementation 'com.github.Suyghur:Qojqva:v1.0.0'
}
```

## 如何使用

### 分区存储

如果项目已经适配了 Android 10 分区存储特性，请在 Application#onCreate中设置

```kotlin
class DemoActivity : Activity(), View.OnClickListener {
	override fun onCreate() {
		super.onCreate()
		Qojqva.scopedStorage = true
	}
}
```

### 申请权限

```kotlin
Qojqva.with()
	.permission(Permission.RECORD_AUDIO)
	.permission(Permission.Group.CALENDAR)
	.request(this@DemoActivity, object : IPermissionCallback {
		override fun onGranted(permissions: ArrayList<String>, all: Boolean) {
			//permissions表示同意的权限
			//all表示申请的权限是否全部被授权
          }
          	override fun onDenied(permissions: ArrayList<String>, never: Boolean) {
			//permissions表示拒绝的权限
			//never表示申请的权限用户是否点击了不再询问
			//此时Qojqva会弹窗引导用户跳转到设置页面
          }
        })
```

### 跳转设置页面

```kotlin
Qojqva.startPermissionActivity(context)
```


