# Qojqva

* 适配 Android 11 的权限请求框架
* 适配所有 Android 版本的权限请求框架
* 简洁易用：采用链式调用的方式，使用只需一句代码
* 向下兼容属性：新权限在旧系统可以正常申请，框架会做自动适配，无需调用者适配
* 自动检测错误：如果出现低级错误框架会主动抛出异常给调用者（仅在 Debug 下判断，把 Bug 扼杀在摇篮中）
* 允许在AppCompatActivity、FragmentActivity甚至是Activity中申请权限