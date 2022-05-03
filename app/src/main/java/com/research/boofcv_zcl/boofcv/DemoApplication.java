package com.research.boofcv_zcl.boofcv;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.util.Log;

import com.research.boofcv_zcl.BuildConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import boofcv.BoofVersion;

public class DemoApplication extends Application
{
    public static final String TAG = "DEMOAPP";

    // contains information on all the cameras.  less error prone and easier to deal with
    public final List<CameraSpecs> specs = new ArrayList<>();
    // specifies which camera to use an image size
    public DemoPreference preference = new DemoPreference();
    // If another activity modifies the demo preferences this needs to be set to true so that it knows to reload
    // camera parameters.
    public boolean changedPreferences = false;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        // Don't initialize error reporting if no destination is specified
        // or if someone is trying to run it on an unsupported android device
        String address = loadAcraAddress(this);
        if( address == null) {
            Log.i(TAG,"Not reporting errors with ACRA");
            return;
        }


    }

    public static String loadAcraAddress( Context context ) {
        if( Build.VERSION.SDK_INT < 22 )
            return null;

        if( !BuildConfig.BUILD_TYPE.equals("release"))
            return null;

        try {
            AssetManager am = context.getAssets();
            InputStream is = am.open("acra.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            return reader.readLine();
        } catch( RuntimeException | IOException e ) {
            return null;
        }
    }
}