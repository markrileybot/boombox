package boombox.android;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public final class PermissionUtils {

	private static final Logger log = LoggerFactory.getLogger(PermissionUtils.class);
	public static final int REQUEST_CODE = PermissionUtils.class.hashCode();

	private static final String[] REQUIRED_PERMS = {
		Manifest.permission.ACCESS_COARSE_LOCATION,
		Manifest.permission.BLUETOOTH,
		Manifest.permission.BLUETOOTH_ADMIN,
	};

	public static boolean checkPermissions(Context context) {
		if(VERSION.SDK_INT >= VERSION_CODES.M) {
			for(String perm : REQUIRED_PERMS) {
				if(context.checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
					log.debug("Need permission: {}", perm);
					return false;
				}
			}
		}
		return true;
	}

	public static boolean requestPermissions(Activity context) {
		if(VERSION.SDK_INT >= VERSION_CODES.M) {
			List<String> permsNeeded = new ArrayList<>();
			for(String perm : REQUIRED_PERMS) {
				if(context.checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
					log.debug("Requesting permission: {}", perm);
					permsNeeded.add(perm);
				}
			}

			// get perms needed
			if(!permsNeeded.isEmpty()) {
				context.requestPermissions(permsNeeded.toArray(new String[permsNeeded.size()]), REQUEST_CODE);
			}
		}
		return true;
	}

	private PermissionUtils() {}
}
