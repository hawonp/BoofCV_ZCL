package com.research.boofcv_zcl;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.research.boofcv_zcl.boofcv.CameraSpecs;
import com.research.boofcv_zcl.boofcv.DemoApplication;
import com.research.boofcv_zcl.calibration.CalibrationActivity;
import com.research.boofcv_zcl.calibration.UndistortDisplayActivity;
import com.research.boofcv_zcl.util.PermissionSupport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import boofcv.io.calibration.CalibrationIO;
import boofcv.struct.calib.CameraPinholeBrown;

public class MainActivity extends AppCompatActivity {

    // UI element imports
    Button btn_calibrate;
    Button btn_camera;
    TextView tv_status;

    // global constants
    private static final String TAG = "ZCL";
    private PermissionSupport permissionSupport;

    // BoofCV code
    boolean waitingCameraPermissions = true;
    DemoApplication app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // base settings
        super.onCreate(savedInstanceState);

        // custom settings
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // keep screen on at all times

        // set layout for main activity
        setContentView(R.layout.activity_main);

        app = (DemoApplication) getApplication();
        if (app == null)
            throw new RuntimeException("App is null!");

        try {
            loadCameraSpecs();
        } catch (NoClassDefFoundError e) {
            // Some people like trying to run this app on really old versions of android and
            // seem to enjoy crashing and reporting the errors.
            e.printStackTrace();
            abortDialog("Camera2 API Required");
            return;
        }


        // UI element initialization
        btn_calibrate = findViewById(R.id.btn_calibrate);
        btn_camera = findViewById(R.id.btn_camera);
        tv_status = findViewById(R.id.tv_status);

        // calibration button event listener
        btn_calibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, CalibrationActivity.class));
            }
        });




        // request for permissions
        permissionSupport = new PermissionSupport(this, this);
        if(permissionSupport.arePermissionsEnabled()){
            Log.d(TAG, "All Permissions Granted");
            Toast.makeText(MainActivity.this, "All Permissions Already Granted", Toast.LENGTH_SHORT).show();
        } else{
            permissionSupport.requestMultiplePermissions();
            Toast.makeText(MainActivity.this, "Requesting All Permissions", Toast.LENGTH_SHORT).show();
        }

        if( app.preference.calibration.isEmpty() ) {
//            Toast.makeText(MainActivity.this, "Please Calibrate Camera First", Toast.LENGTH_SHORT).show();
            btn_calibrate.setEnabled(true);
            btn_camera.setEnabled(false);
            tv_status.setText("Camera Not Calibrated Yet");

        } else{
//            Toast.makeText(MainActivity.this, "Camera Calibrated", Toast.LENGTH_SHORT).show();
            btn_calibrate.setEnabled(false);
            btn_camera.setEnabled(true);
            tv_status.setText("Camera Calibrated!");
            Log.d(TAG, app.preference.calibration.toString());
        }

    } // onCreate method closure


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(permissionSupport.onRequestPermissionsResult(requestCode, permissions, grantResults)){
            Log.d(TAG, "All Permissions Granted Confirmation");
        }
    } //permission method closure

    // get default camera specs
    public static CameraSpecs defaultCameraSpecs(DemoApplication app) {
        for (int i = 0; i < app.specs.size(); i++) {
            CameraSpecs s = app.specs.get(i);
            if (s.deviceId.equals(app.preference.cameraId))
                return s;
        }
        throw new RuntimeException("Can't find default camera");
    }

    public static File getExternalDirectory(Activity activity) {
        // if possible use a public directory. If that fails use a private one
//		if(Objects.equals(Environment.getExternalStorageState(), Environment.MEDIA_MOUNTED)) {
//			File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
//			if( !dir.exists() )
//				dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
//			return new File(dir,"org.boofcv.android");
//		} else {
        return activity.getExternalFilesDir(null);
//		}
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!waitingCameraPermissions && app.changedPreferences) {
            loadIntrinsics(this, app.preference.cameraId, app.preference.calibration, null);
        }
    }

    private void loadCameraSpecs() {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA);

        if (permissionCheck != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    0);
        } else {
            waitingCameraPermissions = false;

            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            if (manager == null)
                throw new RuntimeException("No cameras?!");
            try {
                String[] cameras = manager.getCameraIdList();

                for (String cameraId : cameras) {
                    CameraSpecs c = new CameraSpecs();
                    app.specs.add(c);
                    c.deviceId = cameraId;
                    CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                    Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                    c.facingBack = facing != null && facing == CameraCharacteristics.LENS_FACING_BACK;
                    StreamConfigurationMap map = characteristics.
                            get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    if (map == null) {
                        continue;
                    }
                    Size[] sizes = map.getOutputSizes(ImageFormat.YUV_420_888);
                    if (sizes == null)
                        continue;
                    c.sizes.addAll(Arrays.asList(sizes));
                }
            } catch (CameraAccessException e) {
                throw new RuntimeException("No camera access??? Wasn't it just granted?");
            }

            // Now that it can read the camera set the default settings
            setDefaultPreferences();
        }
    }

    private void setDefaultPreferences() {
        app.preference.showSpeed = false;
        app.preference.autoReduce = true;

        // There are no cameras.  This is possible due to the hardware camera setting being set to false
        // which was a work around a bad design decision where front facing cameras wouldn't be accepted as hardware
        // which is an issue on tablets with only front facing cameras
        if (app.specs.size() == 0) {
            dialogNoCamera();
        }
        // select a front facing camera as the default
        for (int i = 0; i < app.specs.size(); i++) {
            CameraSpecs c = app.specs.get(i);

            app.preference.cameraId = c.deviceId;
            if (c.facingBack) {
                break;
            }
        }

        if (!app.specs.isEmpty()) {
            loadIntrinsics(this, app.preference.cameraId, app.preference.calibration, null);
        }
    }

    private void dialogNoCamera() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your device has no cameras!")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        System.exit(0);
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void dialogNoCameraPermission() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Denied access to the camera! Exiting.")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        System.exit(0);
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }


    public static void loadIntrinsics(Activity activity,
                                      String cameraId,
                                      List<CameraPinholeBrown> intrinsics,
                                      List<File> locations) {
        intrinsics.clear();
        if (locations != null)
            locations.clear();

        File directory = new File(getExternalDirectory(activity), "calibration");
        if (!directory.exists())
            return;
        File files[] = directory.listFiles();
        if (files == null)
            return;
        String prefix = "camera" + cameraId;
        for (File f : files) {
            if (!f.getName().startsWith(prefix))
                continue;
            try {
                FileInputStream fos = new FileInputStream(f);
                Reader reader = new InputStreamReader(fos);
                CameraPinholeBrown intrinsic = CalibrationIO.load(reader);
                intrinsics.add(intrinsic);
                if (locations != null) {
                    locations.add(f);
                }
            } catch (RuntimeException | FileNotFoundException ignore) {
            }
        }
    }

    /**
     * Displays a warning dialog and then exits the activity
     */
    private void abortDialog(String message) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Fatal error");
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                (dialog, which) -> {
                    dialog.dismiss();
                    MainActivity.this.finish();
                });
        alertDialog.show();
    }


} // class closure