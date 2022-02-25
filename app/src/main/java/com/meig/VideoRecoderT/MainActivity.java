package com.meig.VideoRecoderT;

import android.Manifest;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.StatFs;
import android.provider.SyncStateContract;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private SurfaceView mSurfaceview;
    private Button mBtnStartStop;
    private Button mBtnPlay;
    private boolean mStartedFlg = false;//是否正在录像
    private boolean mIsPlay = false;//是否正在播放录像
    private MediaRecorder mRecorder;
    private SurfaceHolder mSurfaceHolder;
    //private ImageView mImageView;
    private RadioGroup mRadioGroup;
    private RadioButton mRadioBtn;

    private Camera camera;
    private MediaPlayer mediaPlayer;
    private String path;
    private TextView textView;
    private TextView mPathView;
    private int text = 0;

    private int videoEncoder = MediaRecorder.VideoEncoder.HEVC;
    private int width = 1920;
    private int height = 1080;
    private int frameRate = 24;
    private int bitRate = 2_000_000;
    private int mCheck = -1;

    private int mDeiveceWith = 0;   //保存设备屏幕宽
    private int mDeiveceHeight = 0;
    private static float DEFAUT_RATIO = 1.77f;  //预览图宽高比
    private String prefix = "";

    MediaScannerConnection mMediaScanner = null;

    private android.os.Handler handler = new android.os.Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            text++;
            textView.setText(text+"");
            handler.postDelayed(this,1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDeiveceWith = getWindowManager().getDefaultDisplay().getWidth();
        mDeiveceHeight = getWindowManager().getDefaultDisplay().getHeight();

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        mMediaScanner = new MediaScannerConnection(this, null);
        mMediaScanner.connect();

        mSurfaceview = (SurfaceView) findViewById(R.id.surfaceview);

        android.view.ViewGroup.LayoutParams lp = mSurfaceview.getLayoutParams();

        if (mDeiveceWith > mDeiveceHeight && mDeiveceHeight > 0) {
            lp.width = mDeiveceWith;
            lp.height = mDeiveceWith / 16 * 9;
        } else if (mDeiveceWith > 0) {
            lp.width = mDeiveceWith / 4 * 3;
            lp.height = mDeiveceWith / 9 * 16 / 4 * 3;
        }
        mSurfaceview.setLayoutParams(lp);

        //mImageView = (ImageView) findViewById(R.id.imageview);
        mBtnStartStop = (Button) findViewById(R.id.btnStartStop);
        mBtnPlay = (Button) findViewById(R.id.btnPlayVideo);
        textView = (TextView)findViewById(R.id.text);
        mPathView = (TextView) findViewById(R.id.path);

        mRadioGroup = (RadioGroup) findViewById(R.id.rg);
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.rb1:
                        mCheck = 1;
                        break;
                    case R.id.rb2:
                        mCheck = 2;
                        break;
                    case R.id.rb3:
                        mCheck = 3;
                        break;
                    case R.id.rb4:
                        mCheck = 4;
                        break;
                }
                android.util.Log.i("Yar", " mCheck = " + mCheck);
            }
        });
        mRadioBtn = (RadioButton) findViewById(R.id.rb2);
        mRadioBtn.setChecked(true);
        mCheck = 2;

        mBtnStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mIsPlay) {
                    if (mediaPlayer != null) {
                        mIsPlay = false;
                        mediaPlayer.stop();
                        mediaPlayer.reset();
                        mediaPlayer.release();
                        mediaPlayer = null;
                    }
                }
                if (!mStartedFlg) {
                    //handler.postDelayed(runnable,1000);
                    //.setVisibility(View.GONE);
                    if (mRecorder == null) {
                        mRecorder = new MediaRecorder();
                    }

                    camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
                    if (camera != null) {
                        if (mDeiveceWith > mDeiveceHeight) {
                            camera.setDisplayOrientation(0);
                        } else {
                            camera.setDisplayOrientation(90);
                        }
                        camera.unlock();
                        mRecorder.setCamera(camera);
                    }

                    try {
                        // 这两项需要放在setOutputFormat之前
                        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                        mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

                        // Set output file format
                        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

                        // 这两项需要放在setOutputFormat之后
                        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

                        switch (mCheck) {
                            case 1:
                                videoEncoder = MediaRecorder.VideoEncoder.HEVC;
                                width = 1280;
                                height = 720;
                                //frameRate = 24;
                                bitRate = 600_000;
                                prefix = "H265_720p_";
                                break;
                            case 2:
                                videoEncoder = MediaRecorder.VideoEncoder.HEVC;
                                width = 1920;
                                height = 1080;
                                //frameRate = 24;
                                bitRate = 2_000_000;
                                prefix = "H265_1080p_";
                                break;
                            case 3:
                                videoEncoder = MediaRecorder.VideoEncoder.H264;
                                width = 1280;
                                height = 720;
                                //frameRate = 24;
                                bitRate = 1_000_000;
                                prefix = "H264_720p_";
                                break;
                            case 4:
                                videoEncoder = MediaRecorder.VideoEncoder.H264;
                                width = 1920;
                                height = 1080;
                                //frameRate = 24;
                                bitRate = 2_000_000;
                                prefix = "H264_1080p_";
                                break;
                            default:
                                videoEncoder = MediaRecorder.VideoEncoder.HEVC;
                                width = 1920;
                                height = 1080;
                                //frameRate = 24;
                                bitRate = 2_000_000;
                                prefix = "H265_1080p_";
                                break;
                        }
                        mRecorder.setVideoEncoder(videoEncoder);
                        mRecorder.setVideoSize(width, height);
                        //CameraSource: Requested frame rate (25) is not supported: 15,24,30
                        mRecorder.setVideoFrameRate(frameRate);
                        mRecorder.setVideoEncodingBitRate(bitRate);

                        mRecorder.setOrientationHint(90);
                        //设置记录会话的最大持续时间（毫秒）
                        mRecorder.setMaxDuration(10*60*1000);

                        mRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());

                        path = getSDPath();
                        if (path != null) {
                            File dir = new File(path + "/recordtest");
                            if (!dir.exists()) {
                                dir.mkdir();
                            }
                            path = dir + "/" + prefix + getDate() + ".mp4";
                            mRecorder.setOutputFile(path);
                            mRecorder.prepare();
                            mRecorder.start();
                            handler.postDelayed(runnable,1000);
                            mStartedFlg = true;
                            mBtnStartStop.setText("Stop");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    //stop
                    if (mStartedFlg) {
                        try {
                            handler.removeCallbacks(runnable);
                            mRecorder.stop();
                            mRecorder.reset();
                            mRecorder.release();
                            mRecorder = null;
                            mBtnStartStop.setText("Start");
                            if (camera != null) {
                                camera.release();
                                camera = null;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (mMediaScanner == null) {
                            mMediaScanner = new MediaScannerConnection(MainActivity.this, null);
                        }

                        if (mMediaScanner != null && !mMediaScanner.isConnected()) {
                            mMediaScanner.connect();
                        }

                        if (mMediaScanner !=null && mMediaScanner.isConnected()) {
                            mMediaScanner.scanFile(path, ".mp4");
                        }

                        mPathView.setText(path);
                    }
                    mStartedFlg = false;
                }
            }
        });

        mBtnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mIsPlay = true;
                //mImageView.setVisibility(View.GONE);
                if (mediaPlayer == null) {
                    mediaPlayer = new MediaPlayer();
                }
                mediaPlayer.reset();
                Uri uri = Uri.parse(path);
                mediaPlayer = MediaPlayer.create(MainActivity.this, uri);
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.setDisplay(mSurfaceHolder);
                try{
                    mediaPlayer.prepare();
                }catch (Exception e){
                    e.printStackTrace();
                }
                mediaPlayer.start();
            }
        });

        SurfaceHolder holder = mSurfaceview.getHolder();
        holder.addCallback(mCallback);
        // setType必须设置，要不出错.
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mStartedFlg) {
            //mImageView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 获取系统时间
     *
     * @return
     */
    public static String getDate() {
        Calendar ca = Calendar.getInstance();
        int year = ca.get(Calendar.YEAR);           // 获取年份
        int month = ca.get(Calendar.MONTH);         // 获取月份
        int day = ca.get(Calendar.DATE);            // 获取日
        int minute = ca.get(Calendar.MINUTE);       // 分
        int hour = ca.get(Calendar.HOUR);           // 小时
        int second = ca.get(Calendar.SECOND);       // 秒

        String date = "" + year + (month + 1) + day + hour + minute + second;
        Log.d(TAG, "date:" + date);

        return date;
    }

    /**
     * 获取SD path
     *
     * @return
     */
    public String getSDPath() {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState()
                .equals(android.os.Environment.MEDIA_MOUNTED); // 判断sd卡是否存在
        if (sdCardExist) {
            sdDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);;// 获取跟目录
            return sdDir.toString();
        }

        return null;
    }

    private SurfaceHolder.Callback mCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            mSurfaceHolder = surfaceHolder;
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            // 将holder，这个holder为开始在onCreate里面取得的holder，将它赋给mSurfaceHolder
            mSurfaceHolder = surfaceHolder;
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            mSurfaceview = null;
            mSurfaceHolder = null;
            handler.removeCallbacks(runnable);
            if (mRecorder != null) {
                mRecorder.release();
                mRecorder = null;
                Log.d(TAG, "surfaceDestroyed release mRecorder");
            }
            if (camera != null) {
                camera.release();
                camera = null;
            }
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO}, 100);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 100:
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        Log.i("Yar","onRequestPermissionsResult granted i = " + i);
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            finish();
                        }
                    }
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    //finish();
                } else {
                    finish();
                }
                break;
        }

    }

}