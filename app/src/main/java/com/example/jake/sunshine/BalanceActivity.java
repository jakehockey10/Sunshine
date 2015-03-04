package com.example.jake.sunshine;

import com.example.jake.sunshine.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLES31;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class BalanceActivity extends Activity implements SensorEventListener {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;
    
    private GLSurfaceView mGLView;
    private SensorManager mSensorManager;
    
    // USING http://www.thousand-thoughts.com/2012/03/android-sensor-fusion-tutorial/
    
    // angular speeds from gyro
    private float[] gyro = new float[3];
    // rotation matrix from gyro data
    private float[] gyroMatrix = new float[9];
    // orientation angles from gyro matrix
    private float[] gyroOrientation = new float[3];
    // magnetic field vector
    private float[] magnet = new float[3];
    // accelerometer vector
    private float[] accel = new float[3];
    // orientation angles from accel and magnet
    private float[] accMagOrientation = new float[3];
    // final orientation angles from sensor fusion
    private float[] fusedOrientation = new float[3];
    // accelerometer and magnetometer based rotation matrix
    private float[] rotationMatrix = new float[9];

    public static final int TIME_CONSTANT = 30;
    public static final float FILTER_COEFFICIENT = 0.98f;
    private Timer fuseTimer = new Timer();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_balance);
        setupActionBar();

        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        final View contentView = findViewById(R.id.fullscreen_content);

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                    // Cached values.
                    int mControlsHeight;
                    int mShortAnimTime;

                    @Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                    public void onVisibilityChange(boolean visible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                            // If the ViewPropertyAnimator API is available
                            // (Honeycomb MR2 and later), use it to animate the
                            // in-layout UI controls at the bottom of the
                            // screen.
                            if (mControlsHeight == 0) {
                                mControlsHeight = controlsView.getHeight();
                            }
                            if (mShortAnimTime == 0) {
                                mShortAnimTime = getResources().getInteger(
                                        android.R.integer.config_shortAnimTime);
                            }
                            controlsView.animate()
                                    .translationY(visible ? 0 : mControlsHeight)
                                    .setDuration(mShortAnimTime);
                        } else {
                            // If the ViewPropertyAnimator APIs aren't
                            // available, simply show or hide the in-layout UI
                            // controls.
                            controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                        }

                        if (visible && AUTO_HIDE) {
                            // Schedule a hide().
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });

        // Set up the user interaction to manually show or hide the system UI.
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                } else {
                    mSystemUiHider.show();
                }
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);
        
        // Coming from: http://developer.android.com/training/graphics/opengl/environment.html
        // Create a GLSurfaceView instance and set it
        // as the ContentView for this Activity.
        mGLView = new MyGLSurfaceView(this);
        setContentView(mGLView);

        // Get an instance of the SensorManager
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        
        // initialize matrices
        initializeMatrices();
        
        // initialize sensor listeners
        initListeners();
        
        // wait for one second until gyroscope and magnetometer/accelerometer
        // data is initialized then schedule the complementary filter task
        fuseTimer.scheduleAtFixedRate(new CalculateFusedOrientationTask(), 1000, TIME_CONSTANT);
    }

    private void initListeners() {
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    private void initializeMatrices() {
        gyroOrientation[0] = 0.0f;
        gyroOrientation[1] = 0.0f;
        gyroOrientation[2] = 0.0f;
        
        // initialize gyroMatrix with identity matrix
        gyroMatrix[0] = 1.0f; gyroMatrix[1] = 0.0f; gyroMatrix[2] = 0.0f;
        gyroMatrix[3] = 0.0f; gyroMatrix[4] = 1.0f; gyroMatrix[5] = 0.0f;
        gyroMatrix[6] = 0.0f; gyroMatrix[7] = 0.0f; gyroMatrix[8] = 1.0f;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGLView.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLView.onResume();
        initListeners();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Show the Up button in the action bar.
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            // TODO: If Settings has multiple levels, Up should navigate up
            // that hierarchy.
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

//    private float[] gravity = new float[3];
//    private float[] geomag = new float[3];
//    private float[] rotationMatrix = new float[16];
    
    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                // copy new accelerometer data into accel array
                // then calculate new orientation
                System.arraycopy(event.values, 0, accel, 0, 3);
                calculateAccMagOrientation();
                break;
            case Sensor.TYPE_GYROSCOPE:
                // process gyro data
                gyroFunction(event);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                // copy new magnetometer data into magnet array
                System.arraycopy(event.values, 0, magnet, 0, 3);
                break;
        }
//        int type=event.sensor.getType();

//        //Smoothing the sensor data a bit
//        if (type == Sensor.TYPE_MAGNETIC_FIELD) {
//            geomag[0]=(geomag[0]*1+event.values[0])*0.5f;
//            geomag[1]=(geomag[1]*1+event.values[1])*0.5f;
//            geomag[2]=(geomag[2]*1+event.values[2])*0.5f;
//        } else if (type == Sensor.TYPE_ACCELEROMETER) {
//            gravity[0]=(gravity[0]*2+event.values[0])*0.33334f;
//            gravity[1]=(gravity[1]*2+event.values[1])*0.33334f;
//            gravity[2]=(gravity[2]*2+event.values[2])*0.33334f;
//        }
//
//        if ((type==Sensor.TYPE_MAGNETIC_FIELD) || (type==Sensor.TYPE_ACCELEROMETER)) {
//            rotationMatrix = new float[16];
//            SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomag);
//            SensorManager.remapCoordinateSystem(
//                    rotationMatrix,
//                    SensorManager.AXIS_Y,
//                    SensorManager.AXIS_MINUS_X,
//                    rotationMatrix );
//        }
    }

    private void calculateAccMagOrientation() {
        if (SensorManager.getRotationMatrix(rotationMatrix, null, accel, magnet)) {
            SensorManager.getOrientation(rotationMatrix, accMagOrientation);
        }
    }
    
    public static final float EPSILON = 0.000000001f;
    
    private void getRotationVectorFromGyro(
            float[] gyroValues, float[] deltaRotationVector, float timeFactor) {
        float[] normValues = new float[3];
        
        // Calculate the angular speed of the sample
        float omegaMagnitude = 
                (float)Math.sqrt(gyroValues[0] * gyroValues[0] + 
                gyroValues[1] * gyroValues[1] + 
                gyroValues[2] * gyroValues[2]);
        
        // Normalize the rotation vector if it's big enough to get the axis
        if (omegaMagnitude > EPSILON) {
            normValues[0] = gyroValues[0] / omegaMagnitude;
            normValues[1] = gyroValues[1] / omegaMagnitude;
            normValues[2] = gyroValues[2] / omegaMagnitude;
        }
        
        // Integrate around this axis with the angular speed by the timestep
        // in order to get a delta rotation from this sample over the timestep
        // We will convert this axis-angle representation fo the delta rotation
        // into a quaternion before turning it into the rotation matrix.
        float thetaOverTwo = omegaMagnitude * timeFactor;
        float sinThetaOverTwo = (float)Math.sin(thetaOverTwo);
        float cosThetaOverTwo = (float)Math.cos(thetaOverTwo);
        deltaRotationVector[0] = sinThetaOverTwo * normValues[0];
        deltaRotationVector[1] = sinThetaOverTwo * normValues[1];
        deltaRotationVector[2] = sinThetaOverTwo * normValues[2];
        deltaRotationVector[3] = cosThetaOverTwo;
    }
    
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float timestamp;
    private boolean initState = true;
    
    public void gyroFunction(SensorEvent event) {
        // don't start until first accelerometer/magnetometer orientation has been acquired
        if (accMagOrientation == null) return;
        
        // initialization of the gyroscope based rotation matrix
        if (initState) {
            float[] initMatrix = new float[9];
            initMatrix = getRotationMatrixFromOrientation(accMagOrientation);
            float[] test = new float[3];
            SensorManager.getOrientation(initMatrix, test);
            gyroMatrix = matrixMultiplication(gyroMatrix, initMatrix);
            initState = false;
        }
        
        // copy the new gyro values into the gyro array
        // convert the raw gyro data into a rotation vector
        float[] deltaVector = new float[4];
        if (timestamp != 0) {
            final float dT = (event.timestamp - timestamp) * NS2S;
            System.arraycopy(event.values, 0, gyro, 0, 3);
            getRotationVectorFromGyro(gyro, deltaVector, dT / 2.0f);
        }
        
        // measurement done, save current time for next interval
        timestamp = event.timestamp;
        
        // convert rotation vector into rotation matrix
        float[] deltaMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaVector);
        
        // apply the new rotation interval on the gyroscope based rotation matrix
        gyroMatrix = matrixMultiplication(gyroMatrix, deltaMatrix);
        
        // get the gyroscope based orientation from the rotation matrix
        SensorManager.getOrientation(gyroMatrix, gyroOrientation);
    }
    
    // from: http://www.thousand-thoughts.com/2012/03/android-sensor-fusion-tutorial/2/
    // "I have to admit, this function is not optimal and can be improved 
    // in terms of performance, but for this tutorial it will do the trick. 
    // It basically creates a rotation matrix for every axis and multiplies 
    // the matrices in the correct order (y, x, z in our case)."
    private float[] getRotationMatrixFromOrientation(float[] o) {
        float[] xM = new float[9];
        float[] yM = new float[9];
        float[] zM = new float[9];
        
        float sinX = (float)Math.sin(o[1]);
        float cosX = (float)Math.cos(o[1]);
        float sinY = (float)Math.sin(o[2]);
        float cosY = (float)Math.cos(o[2]);
        float sinZ = (float)Math.sin(o[0]);
        float cosZ = (float)Math.cos(o[0]);
        
        // rotation about x-axis (pitch)
        xM[0] = 1.0f; xM[1] = 0.0f; xM[2] = 0.0f;
        xM[3] = 0.0f; xM[4] = cosX; xM[5] = sinX;
        xM[6] = 0.0f; xM[7] = -sinX; xM[8] = cosX;

        // rotation about y-axis (roll)
        yM[0] = cosY; yM[1] = 0.0f; yM[2] = sinY;
        yM[3] = 0.0f; yM[4] = 1.0f; yM[5] = 0.0f;
        yM[6] = -sinY; yM[7] = 0.0f; yM[8] = cosY;

        // rotation about z-axis (azimuth)
        zM[0] = cosZ; zM[1] = sinZ; zM[2] = 0.0f;
        zM[3] = -sinZ; zM[4] = cosZ; zM[5] = 0.0f;
        zM[6] = 0.0f; zM[7] = 0.0f; zM[8] = 1.0f;
        
        // rotation order is y, x, z (roll, pitch, azimuth)
        float[] resultMatrix = matrixMultiplication(xM, yM);
        resultMatrix = matrixMultiplication(zM, resultMatrix);
        return resultMatrix;
    }

    private float[] matrixMultiplication(float[] A, float[] B) {
        float[] result = new float[9];

        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];

        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];

        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];

        return result;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    
    
    
    class CalculateFusedOrientationTask extends TimerTask {

        @Override
        public void run() {
            float oneMinusCoeff = 1.0f - FILTER_COEFFICIENT;
            fusedOrientation[0] = 
                    FILTER_COEFFICIENT * gyroOrientation[0]
                    + oneMinusCoeff * accMagOrientation[0];
            fusedOrientation[1] =
                    FILTER_COEFFICIENT * gyroOrientation[1]
                    + oneMinusCoeff * accMagOrientation[1];
            fusedOrientation[2] =
                    FILTER_COEFFICIENT * gyroOrientation[2]
                    + oneMinusCoeff * accMagOrientation[2];
            
            // overwrite gyro matrix and orientation with fused orientation
            // to compensate gyro drift
            gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation);
            System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3);
        }
    }
    


    class MyGLSurfaceView extends GLSurfaceView {

        private MyGLRenderer mRenderer;

        private float mDownX = 0.0f;
        private float mDownY = 0.0f;

        public MyGLSurfaceView(Context context) {
            super(context);

            mRenderer = new MyGLRenderer();
            this.setRenderer(mRenderer);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            switch (action) {
//                case MotionEvent.ACTION_DOWN:
//                    mDownX = event.getX();
//                    mDownY = event.getY();
//                    return true;
//                case MotionEvent.ACTION_UP:
//                    return true;
//                case MotionEvent.ACTION_MOVE:
//                    float mX = event.getX();
//                    float mY = event.getY();
//                    mRenderer.mLightX += (mX-mDownX)/10;
//                    mRenderer.mLightY -= (mY-mDownY)/10;
//                    mDownX = mX;
//                    mDownY = mY;
//                    return true;
                default:
                    return super.onTouchEvent(event);
            }
        }
    }




    public class MyGLRenderer implements GLSurfaceView.Renderer {
        // Ambient light
        private final float[] mat_ambient = { 0.2f, 0.3f, 0.4f, 1.0f };
        private FloatBuffer mat_ambient_buf;
        // Parallel incident light
        private final float[] mat_diffuse = { 0.4f, 0.6f, 0.8f, 1.0f };
        private FloatBuffer mat_diffuse_buf;
        // The highlighted area
        private final float[] mat_specular = { 0.2f * 0.4f, 0.2f * 0.6f, 0.2f * 0.8f, 1.0f };
        private FloatBuffer mat_specular_buf;

        private Sphere mSphere = new Sphere();

        public volatile float mLightX = 10f;
        public volatile float mLightY = 10f;
        public volatile float mLightZ = 10f;

        @Override
        public void onDrawFrame(GL10 gl) {
            // To clear the screen and the depth buffer
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
            // Reset the modelview matrix
            
            
            
            
            gl.glMatrixMode(GL10.GL_MODELVIEW);
            
            
            
            
            
            gl.glLoadIdentity();

            gl.glEnable(GL10.GL_LIGHTING);
            gl.glEnable(GL10.GL_LIGHT0);

            // Texture of material
            gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT, mat_ambient_buf);
            gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_DIFFUSE, mat_diffuse_buf);
            gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_SPECULAR, mat_specular_buf);
            // Specular exponent 0~128 less rough
            gl.glMaterialf(GL10.GL_FRONT_AND_BACK, GL10.GL_SHININESS, 96.0f);

            //The position of the light source
            float[] light_position = {mLightX, mLightY, mLightZ, 0.0f};
            ByteBuffer mpbb = ByteBuffer.allocateDirect(light_position.length*4);
            mpbb.order(ByteOrder.nativeOrder());
            FloatBuffer mat_posiBuf = mpbb.asFloatBuffer();
            mat_posiBuf.put(light_position);
            mat_posiBuf.position(0);
            gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, mat_posiBuf);
            
            
            
            
            float[] rotationMatrix = new float[16];
//            SensorManager.getRotationMatrixFromVector(rotationMatrix, gyroOrientation);
            gl.glRotatef((float) (gyroOrientation[2] * 180 / Math.PI), 0, 1, 0);
            gl.glRotatef((float) (gyroOrientation[1] * 180 / Math.PI), 1, 0, 0);
            gl.glRotatef((float) (gyroOrientation[0] * 180 / Math.PI), 0, 0, 1);
//            gl.glMultMatrixf(rotationMatrix, 0);
//            gl.glTranslatef(0, 2, 0);
            
            
            
            
            
            gl.glTranslatef(0.0f, 0.0f, -6.0f);
            mSphere.draw(gl);
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {

            // Set the output screen size
            gl.glViewport(0, 0, width, height);

            // Projection matrix
            gl.glMatrixMode(GL10.GL_PROJECTION);
            // Reset the projection matrix
            gl.glLoadIdentity();
            // Set the viewport size
            // gl.glFrustumf(0, width, 0, height, 0.1f, 100.0f);

            GLU.gluPerspective(gl, 90.0f, (float) width / height, 0.1f, 50.0f);

            // Select the model view matrix
            gl.glMatrixMode(GL10.GL_MODELVIEW);
            // Reset the modelview matrix
            gl.glLoadIdentity();

        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig arg1) {
            // On the perspective correction
            gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);
            // Background: Black
            gl.glClearColor(0, 0.0f, 0.0f, 0.0f);
            // Start the smooth shading
            gl.glShadeModel(GL10.GL_SMOOTH);

            // Reset the depth buffer
            gl.glClearDepthf(1.0f);
            // Start the depth test
            gl.glEnable(GL10.GL_DEPTH_TEST);
            // Type the depth test
            gl.glDepthFunc(GL10.GL_LEQUAL);

            initBuffers();
        }

        private void initBuffers() {
            ByteBuffer bufTemp = ByteBuffer.allocateDirect(mat_ambient.length * 4);
            bufTemp.order(ByteOrder.nativeOrder());
            mat_ambient_buf = bufTemp.asFloatBuffer();
            mat_ambient_buf.put(mat_ambient);
            mat_ambient_buf.position(0);

            bufTemp = ByteBuffer.allocateDirect(mat_diffuse.length * 4);
            bufTemp.order(ByteOrder.nativeOrder());
            mat_diffuse_buf = bufTemp.asFloatBuffer();
            mat_diffuse_buf.put(mat_diffuse);
            mat_diffuse_buf.position(0);

            bufTemp = ByteBuffer.allocateDirect(mat_specular.length * 4);
            bufTemp.order(ByteOrder.nativeOrder());
            mat_specular_buf = bufTemp.asFloatBuffer();
            mat_specular_buf.put(mat_specular);
            mat_specular_buf.position(0);
        }
    }
    
    public class Surface {
        
        public void draw(GL10 gl) {

        }
    }

    // Calculation of spherical vertex
    public class Sphere {

        public void draw(GL10 gl) {

            float	angleA, angleB;
            float	cos, sin;
            float	r1, r2;
            float	h1, h2;
            float	step = 2.0f;
            float[][] v = new float[32][3];
            ByteBuffer vbb;
            FloatBuffer vBuf;

            vbb = ByteBuffer.allocateDirect(v.length * v[0].length * 4);
            vbb.order(ByteOrder.nativeOrder());
            vBuf = vbb.asFloatBuffer();

            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);

            for (angleA = -90.0f; angleA <90.0f; angleA += step) {
                int	n = 0;

                r1 = (float)Math.cos(angleA * Math.PI / 180.0);
                r2 = (float)Math.cos((angleA + step) * Math.PI / 180.0);
                h1 = (float)Math.sin(angleA * Math.PI / 180.0);
                h2 = (float)Math.sin((angleA + step) * Math.PI / 180.0);

                // Fixed latitude, 360 degrees rotation to traverse a weft
                for (angleB = 0.0f; angleB <= 360.0f; angleB += step) {

                    cos = (float)Math.cos(angleB * Math.PI / 180.0);
                    sin = -(float)Math.sin(angleB * Math.PI / 180.0);

                    v[n][0] = (r2 * cos);
                    v[n][1] = (h2);
                    v[n][2] = (r2 * sin);
                    v[n + 1][0] = (r1 * cos);
                    v[n + 1][1] = (h1);
                    v[n + 1][2] = (r1 * sin);

                    vBuf.put(v[n]);
                    vBuf.put(v[n + 1]);

                    n += 2;

                    if(n>31){
                        vBuf.position(0);

                        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vBuf);
                        gl.glNormalPointer(GL10.GL_FLOAT, 0, vBuf);
                        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, n);

                        n = 0;
                        angleB -= step;
                    }

                }
                vBuf.position(0);

                gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vBuf);
                gl.glNormalPointer(GL10.GL_FLOAT, 0, vBuf);
                gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, n);
            }

            gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
        }
    }
}
