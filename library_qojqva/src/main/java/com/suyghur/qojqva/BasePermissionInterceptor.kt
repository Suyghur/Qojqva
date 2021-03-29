package com.suyghur.qojqva

import androidx.fragment.app.FragmentActivity
import com.suyghur.qojqva.inernal.IPermissionCallback
import com.suyghur.qojqva.inernal.IPermissionInterceptor

/**
 * @author #Suyghur.
 * Created on 3/29/2021
 */
open class BasePermissionInterceptor : IPermissionInterceptor {

    override fun requesetPermission(activity: FragmentActivity, permissions: MutableList<String>, callback: IPermissionCallback) {
        TODO("Not yet implemented")
    }

    override fun grantedPermissions(activity: FragmentActivity, permissions: MutableList<String>, all: Boolean, callback: IPermissionCallback) {
        TODO("Not yet implemented")
    }

    override fun deniedPermissions(activity: FragmentActivity, permissions: MutableList<String>, never: Boolean, callback: IPermissionCallback) {
        TODO("Not yet implemented")
    }
}