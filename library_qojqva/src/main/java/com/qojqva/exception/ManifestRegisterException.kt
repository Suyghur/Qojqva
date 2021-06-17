package com.qojqva.exception

/**
 * @author #Suyghur.
 * Created on 3/29/2021
 */
class ManifestRegisterException : RuntimeException {

    /**
     * 清单文件中没有注册任何权限
     */
    constructor() : super("No permissions are registered in the manifest file")

    /**
     * 申请的危险权限没有在清单文件中注册
     */
    constructor(permission: String) : super("The permission $permission you requested is not registered in the manifest file")
}