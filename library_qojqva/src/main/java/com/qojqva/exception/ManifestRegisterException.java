package com.qojqva.exception;

/**
 * @author #Suyghur.
 * Created on 2021/10/29
 */
public final class ManifestRegisterException extends RuntimeException {

    public ManifestRegisterException() {
        super("No permissions are registered in the manifest file");
    }

    public ManifestRegisterException(String permission) {
        super("The permission " + permission + " you requested is not registered in the manifest file");
    }
}
