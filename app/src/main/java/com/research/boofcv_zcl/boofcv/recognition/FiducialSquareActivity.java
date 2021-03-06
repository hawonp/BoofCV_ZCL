package com.research.boofcv_zcl.boofcv.recognition;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.ToggleButton;

//import org.boofcv.android.DemoBitmapCamera2Activity;
//import org.boofcv.android.DemoProcessingAbstract;
//import org.boofcv.android.R;
//import org.boofcv.android.misc.RenderCube3D;
//import

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.research.boofcv_zcl.R;
import com.research.boofcv_zcl.boofcv.DemoBitmapCamera2Activity;
import com.research.boofcv_zcl.boofcv.DemoProcessingAbstract;
import com.research.boofcv_zcl.boofcv.misc.RenderCube3D;

import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_F64;
import org.ddogleg.struct.DogArray_I64;

import java.util.ArrayList;
import java.util.ListIterator;

import boofcv.abst.fiducial.CalibrationFiducialDetector;
import boofcv.abst.fiducial.FiducialDetector;
import boofcv.abst.fiducial.FiducialStability;
import boofcv.abst.fiducial.SquareBase_to_FiducialDetector;
import boofcv.abst.fiducial.calib.CalibrationDetectorChessboardBinary;
import boofcv.abst.fiducial.calib.CalibrationDetectorChessboardX;
import boofcv.abst.fiducial.calib.CalibrationDetectorCircleHexagonalGrid;
import boofcv.abst.fiducial.calib.CalibrationDetectorCircleRegularGrid;
import boofcv.abst.fiducial.calib.CalibrationDetectorSquareGrid;
import boofcv.abst.geo.calibration.DetectSingleFiducialCalibration;
import boofcv.android.ConvertBitmap;
import boofcv.android.VisualizeImageData;
import boofcv.factory.distort.LensDistortionFactory;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import georegression.struct.se.Se3_F64;

/**
 * Base class for square fiducials
 *
 * @author Peter Abeles
 */
public abstract class FiducialSquareActivity extends DemoBitmapCamera2Activity
        implements View.OnTouchListener {
    public static final String TAG = "FiducialSquareActivity";

    final Object lock = new Object();
    volatile boolean robust = true;
    volatile int binaryThreshold = 100;

    Se3_F64 targetToCamera = new Se3_F64();

    Class help;

    // this text is displayed
    protected String textToDraw = "";

    // If true then the background will be the thresholded image
    protected boolean showThreshold = false;

    // if false it won't process images
    protected volatile boolean detectFiducial = true;

    // Which layout it should use
    int layout = R.layout.fiducial_controls;

    // touch event
    private ArrayList<String> temp;

    private Context context;

    FiducialSquareActivity(Class help) {
        super(Resolution.MEDIUM);
        super.changeResolutionOnSlow = true;
        this.help = help;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LayoutInflater inflater = getLayoutInflater();
        LinearLayout controls = (LinearLayout) inflater.inflate(layout, null);

        final ToggleButton toggle = controls.findViewById(R.id.toggle_robust);
        final SeekBar seek = controls.findViewById(R.id.slider_threshold);

        configureControls(toggle, seek);

        setControls(controls);
        displayView.setOnTouchListener(this);
        this.context = this.getApplicationContext();

        temp = new ArrayList<String>();
        // bottom sheet
//        mBottomSheetLayout = findViewById(R.id.bottom_sheet_layout);
//        sheetBehavior = BottomSheetBehavior.from(mBottomSheetLayout);
//        header_Arrow_Image = findViewById(R.id.bottom_sheet_arrow);
//
//        header_Arrow_Image.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                if(sheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED){
//                    sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
//                } else {
//                    sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
//                }
//
//            }
//        });
//
//        sheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
//            @Override
//            public void onStateChanged(@NonNull View bottomSheet, int newState) {
//            }
//            @Override
//            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
//
//                header_Arrow_Image.setRotation(slideOffset * 180);
//            }
//        });
    }

    protected void configureControls(ToggleButton toggle, SeekBar seek) {
        robust = toggle.isChecked();
        binaryThreshold = seek.getProgress();

        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                synchronized (lock) {
                    binaryThreshold = progress;
                    createNewProcessor();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            synchronized (lock) {
                robust = isChecked;
                if (robust) {
                    seek.setEnabled(false);
                } else {
                    seek.setEnabled(true);
                }
                createNewProcessor();
            }
        });
    }


    public void pressedHelp(View view) {
        Intent intent = new Intent(this, help);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    //TODO onTouch method
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (MotionEvent.ACTION_DOWN == event.getActionMasked()) {
//            showThreshold = !showThreshold;

            // Dialog for showing data
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
//            LayoutInflater inflater = this.getLayoutInflater();
//
//            // 2. Chain together various setter methods to set the dialog characteristics
//            builder.setView(inflater.inflate(R.layout.info_layout, null))
//                    .setNegativeButton("Close", new DialogInterface.OnClickListener() {
//                public void onClick(DialogInterface dialog, int id) {
//                    // User cancelled the dialog
//                }
//            });
            builder.setTitle("Dialog");
            builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // user cancelled the dialog
                }
            });

            AlertDialog dialog = builder.create();

//            dialog.show();

            //TODO cannot access detected cubes here for some reason
//            for(RenderCube3D cube : cubes){
//
//                Toast.makeText(this, cube.point + " " + cube.width, Toast.LENGTH_SHORT).show();
//            }
            return true;
        }
        return false;
    }

    @Override
    public void createNewProcessor() {
        if (detectFiducial)
            setProcessing(new FiducialProcessor(createDetector()));
    }

    protected abstract FiducialDetector<GrayU8> createDetector();

    protected class FiducialProcessor<T extends ImageBase<T>> extends DemoProcessingAbstract<T> {
        FiducialDetector<T> detector;

        Paint paintStability = new Paint();
        Paint paintStabilityBad = new Paint();

        RenderCube3D renderCube = new RenderCube3D();
        Paint paintSelected = new Paint();
        private Paint paintTextView = new Paint(); // text drawn directory to screen

        Rect bounds = new Rect();

        double currentStability;
        double maxStability = 0.3;
        FiducialStability stabilityResults = new FiducialStability();

        final DogArray<Se3_F64> listPose = new DogArray<>(Se3_F64::new);
        final DogArray_F64 listWidths = new DogArray_F64();
        final DogArray_I64 listIDs = new DogArray_I64();
        CameraPinholeBrown intrinsic;

        protected FiducialProcessor(FiducialDetector<T> detector) {
            super(detector.getInputType());

            this.detector = detector;

            paintStability.setColor(Color.argb(0xFF / 2, 0, 0xFF, 0));
            paintStability.setStyle(Paint.Style.FILL);
            paintStabilityBad.setColor(Color.argb(0xFF / 2, 0xFF, 0, 0));
            paintStabilityBad.setStyle(Paint.Style.FILL);

            paintSelected.setColor(Color.argb(0xFF / 2, 0xFF, 0, 0));


            // Create out paint to use for drawing
            paintTextView.setARGB(255, 255, 100, 100);

        }

        @Override
        public void initialize(int imageWidth, int imageHeight, int sensorOrientation) {
            // sanity check requirements
            if (imageWidth == 0 || imageHeight == 0)
                throw new RuntimeException("BUG! Called with zero width and height");

            if (!isCameraCalibrated()) {
                Toast.makeText(FiducialSquareActivity.this, "Calibrate camera for better results!", Toast.LENGTH_LONG).show();
            }

            // the adjustment requires knowing what the camera's resolution is. The camera
            // must be initialized at this point
            paintTextView.setTextSize(24 * cameraToDisplayDensity);

            renderCube.initialize(cameraToDisplayDensity);

            intrinsic = lookupIntrinsics();
            detector.setLensDistortion(LensDistortionFactory.narrow(intrinsic), imageWidth, imageHeight);

//			Log.i(TAG,"intrinsic fx = "+intrinsic.fx+" fy = "+intrinsic.fy);
//			Log.i(TAG,"intrinsic cx = "+intrinsic.cx+" cy = "+intrinsic.cy);
//			Log.i(TAG,"intrinsic width = "+intrinsic.width+"  imgW = "+imageWidth);

        }

        // TODO Figure out a way to add touch detection here OR you can save the widths and ids and do touch detection in the OnTouch method above
        @Override
        public void onDraw(Canvas canvas, Matrix imageToView) {
            canvas.drawBitmap(bitmap, imageToView, null);

            double stability;

            canvas.save();
            canvas.concat(imageToView);
            synchronized (listPose) {
                for (int i = 0; i < listPose.size; i++) {
                    double width = listWidths.get(i);
                    long id = listIDs.get(i);
                    renderCube.drawCube("" + id, listPose.get(i), intrinsic, width, canvas);
                    Log.d("POS", "" + width + " " + id);
//                    Toast.makeText(context, "" + (int) width, Toast.LENGTH_SHORT).show();
                }

                stability = currentStability / maxStability;
            }

            canvas.restore();

            // draw stability bar
            float w = canvas.getWidth() / 10;
            float bottom = canvas.getHeight() / 4;
            if (stability > 0.5)
                canvas.drawRect(0, (float) ((1.0 - stability) * bottom), w, bottom, paintStabilityBad);
            else
                canvas.drawRect(0, (float) ((1.0 - stability) * bottom), w, bottom, paintStability);

            if (textToDraw != null) {
                renderDrawText(canvas);
            }
        }

        @Override
        public void process(T input) {
            if (!detectFiducial) {
                ConvertBitmap.boofToBitmap(input, bitmap, bitmapTmp);
                return;
            }

            detector.detect(input);


            if (!showThreshold) {
                ConvertBitmap.boofToBitmap(input, bitmap, bitmapTmp);
            } else {
                GrayU8 binary;
                if (detector instanceof CalibrationFiducialDetector) {
                    DetectSingleFiducialCalibration a = ((CalibrationFiducialDetector) detector).getCalibDetector();
                    if (a instanceof CalibrationDetectorChessboardX) {
                        binary = null;
                    } else if (a instanceof CalibrationDetectorChessboardBinary) {
                        binary = ((CalibrationDetectorChessboardBinary) a).getAlgorithm().getBinary();
                    } else if (a instanceof CalibrationDetectorSquareGrid) {
                        binary = ((CalibrationDetectorSquareGrid) a).getAlgorithm().getBinary();
                    } else if (a instanceof CalibrationDetectorCircleHexagonalGrid) {
                        binary = ((CalibrationDetectorCircleHexagonalGrid) a).getDetector().getBinary();
                    } else if (a instanceof CalibrationDetectorCircleRegularGrid) {
                        binary = ((CalibrationDetectorCircleRegularGrid) a).getDetector().getBinary();
                    } else {
                        throw new RuntimeException("BUG: Unknown calibration detector " + a.getClass().getSimpleName());
                    }
                } else {
                    binary = ((SquareBase_to_FiducialDetector) detector).getAlgorithm().getBinary();
                }
                if (binary != null)
                    VisualizeImageData.binaryToBitmap(binary, false, bitmap, bitmapTmp);
                else
                    ConvertBitmap.boofToBitmap(input, bitmap, bitmapTmp);
            }

            // save the results for displaying in the UI thread
            synchronized (listPose) {
                listPose.reset();
                listWidths.reset();
                listIDs.reset();

                currentStability = 0;
                for (int i = 0; i < detector.totalFound(); i++) {
                    detector.computeStability(i, 1.5, stabilityResults);
                    currentStability = Math.max(stabilityResults.orientation, currentStability);

                    detector.getFiducialToCamera(i, targetToCamera);
                    listPose.grow().setTo(targetToCamera);
                    listWidths.add(detector.getWidth(i));
                    listIDs.add(detector.getId(i));
                    temp.add("lmao: " + detector.getId(i) + " no u: " + detector.getWidth(i));
                }
            }
        }

        private void renderDrawText(Canvas canvas) {
            paintTextView.getTextBounds(textToDraw, 0, textToDraw.length(), bounds);

            int textLength = bounds.width();
            int textHeight = bounds.height();

            int x0 = canvas.getWidth() / 2 - textLength / 2;
            int y0 = canvas.getHeight() / 2 + textHeight / 2;

            canvas.drawText(textToDraw, x0, y0, paintTextView);

        }
    }
}
