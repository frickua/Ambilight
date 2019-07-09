package com.frickua.ambilight;


import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.widget.*;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MAIN";

    private static final int PERMISSION_CODE = 1;
    private static final List<Resolution> RESOLUTIONS = new ArrayList<Resolution>() {{
        add(new Resolution(640, 360));
        add(new Resolution(960, 540));
        add(new Resolution(1366, 768));
        add(new Resolution(1600, 900));
    }};
    private int mScreenDensity;
    private MediaProjectionManager mProjectionManager;
    private int mDisplayWidth;
    private int mDisplayHeight;
    private boolean mScreenSharing;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private Surface mSurface;
    private TextureView mSurfaceView;
    private ToggleButton mToggle;
    WebSocketServer webSocketServer;


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new AmbilightService().onStartCommand(MyIntentBuilder.getInstance(this).setCommand(Command.START).build(), 0, AmbilightService.getRandomNumber());
//        setContentView(R.layout.activity_main);
        setContentView(R.layout.media_projection);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        mSurfaceView = (TextureView) findViewById(R.id.surface);
//        mSurfaceView = (SurfaceView) findViewById(R.id.surface);

        final ExecutorService executorService = Executors.newSingleThreadExecutor();

            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    TextView editText = (TextView) findViewById(R.id.ipText);
                    editText.setFocusable(false);
                    WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                    String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
//                    System.out.println(ip);
                    editText.setText(ip);
                    webSocketServer = new MyWebSocket(new InetSocketAddress(8888)) ;
                    webSocketServer.run();
                }

            });


        mSurfaceView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

                mSurface = new Surface(mSurfaceView.getSurfaceTexture());
                final long startTime = System.currentTimeMillis();
                final long[] count = new long[]{0};
//                final ExecutorService executorService = Executors.newSingleThreadExecutor();
                surface.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                    @Override
                    public void onFrameAvailable(final SurfaceTexture surfaceTexture) {
                        count[0]++;
                        ExecutorService executorService = Executors.newSingleThreadExecutor();
                        final Bitmap bitMap = mSurfaceView.getBitmap();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                            Color.convert(bitMap.getPixel(0,0), bitMap.getColorSpace())
                            executorService.submit(new Runnable() {
                                @Override
                                public void run() {
                                    int height = bitMap.getHeight();
                                    int width = bitMap.getWidth();
                                    int hPart = height / 60;
                                    int wPart = width / 3;
                                    StringBuilder sb = new StringBuilder();
                                    long count = 0;
                                    for (int i = 0; i < 60; i++) {
                                        Color avarage = Color.valueOf(Color.rgb(0, 0, 0));
                                        float r = 0;
                                        float g = 0;
                                        float b = 0;
                                        for (int j = 0; j < hPart; j++) {
                                            for (int k = 0; k < 10; k++) {
                                                Color color = Color.valueOf(bitMap.getPixel(k, hPart * i));
//                                                r = r + color.red();
//                                                g = g + color.green();
//                                                b = b + color.blue();
                                                r = r + color.red();
                                                g = g + color.green();
                                                b = b + color.blue();
                                                count++;
                                            }
                                        }
                                        sb.append("L");
                                        sb.append(String.format("#%06X", (0xFFFFFF & Color.valueOf(r/count, g/count, b/count).toArgb())));
                                        sb.append(";");

                                    }

//                                    System.out.println("Color space: " + bitMap.getColorSpace().getName());
//                                    System.out.println("Rate:" + count[0]/(System.currentTimeMillis()/1000 - startTime/1000));
//                                    System.out.println(String.format("#%06X", (0xFFFFFF & bitMap.getPixel(0,100))));
                                    webSocketServer.broadcast(sb.toString());

                                }
                            });

                        }
                    }
                });
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });

        mProjectionManager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        ArrayAdapter<Resolution> arrayAdapter = new ArrayAdapter<Resolution>(
                this, android.R.layout.simple_list_item_1, RESOLUTIONS);
        Spinner s = (Spinner) findViewById(R.id.spinner);
        s.setAdapter(arrayAdapter);
        s.setOnItemSelectedListener(new ResolutionSelector());
        s.setSelection(0);
        mToggle = (ToggleButton) findViewById(R.id.screen_sharing_toggle);
        mToggle.setSaveEnabled(false);
    }

    @Override
    protected void onStop() {
        stopScreenSharing();
        if (webSocketServer != null) {
            try {
                webSocketServer.stop();
            } catch (IOException e) {
                Log.e(TAG, "Unable to stop websocket", e);
            } catch (InterruptedException e) {
                Log.e(TAG, "Unable to stop websocket", e);
            }
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != PERMISSION_CODE) {
            Log.e(TAG, "Unknown request code: " + requestCode);
            return;
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this,
                    "User denied screen sharing permission", Toast.LENGTH_SHORT).show();
            return;
        }

        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        mMediaProjection.registerCallback(new MediaProjectionCallback(), null);
        mVirtualDisplay = createVirtualDisplay();
    }

    public void onToggleScreenShare(View view) {
        if (((ToggleButton) view).isChecked()) {
            shareScreen();
        } else {
            stopScreenSharing();
        }
    }

    private void shareScreen() {
        mScreenSharing = true;
        if (mSurface == null) {
            return;
        }
        if (mMediaProjection == null) {
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(),
                    PERMISSION_CODE);
            return;
        }
        mVirtualDisplay = createVirtualDisplay();
    }

    private void stopScreenSharing() {
        if (mToggle.isChecked()) {
            mToggle.setChecked(false);
        }
        mScreenSharing = false;
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
    }

    private VirtualDisplay createVirtualDisplay() {
        return mMediaProjection.createVirtualDisplay("ScreenSharingDemo",
                mDisplayWidth, mDisplayHeight, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, mSurface,
                null /*Callbacks*/, new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                    }
                } /*Handler*/);
    }

    private void resizeVirtualDisplay() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.resize(mDisplayWidth, mDisplayHeight, mScreenDensity);
    }

    private class ResolutionSelector implements Spinner.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
            Resolution r = (Resolution) parent.getItemAtPosition(pos);
            ViewGroup.LayoutParams lp = mSurfaceView.getLayoutParams();
            if (getResources().getConfiguration().orientation
                    == Configuration.ORIENTATION_LANDSCAPE) {
                mDisplayHeight = r.y;
                mDisplayWidth = r.x;
            } else {
                mDisplayHeight = r.x;
                mDisplayWidth = r.y;
            }
            lp.height = mDisplayHeight;
            lp.width = mDisplayWidth;
            mSurfaceView.setLayoutParams(lp);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) { /* Ignore */ }
    }

    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            mMediaProjection = null;
            stopScreenSharing();
        }
    }

    private class SurfaceCallbacks implements SurfaceHolder.Callback {
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            mDisplayWidth = width;
            mDisplayHeight = height;
            resizeVirtualDisplay();
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            mSurface = holder.getSurface();
            if (mScreenSharing) {
                shareScreen();
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (!mScreenSharing) {
                stopScreenSharing();
            }
        }
    }

    private static class Resolution {
        int x;
        int y;

        public Resolution(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return x + "x" + y;
        }
    }

    private class MyWebSocket extends WebSocketServer {

        public MyWebSocket(InetSocketAddress address) {
            super(address);
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {

        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        }

        @Override
        public void onMessage(WebSocket conn, String message) {

        }

        @Override
        public void onError(WebSocket conn, Exception ex) {
            Log.e(TAG, "Unable to create websocket", ex);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Log.e(TAG, "Repeate", e);
            }
            webSocketServer = new MyWebSocket(new InetSocketAddress(8888));
            webSocketServer.start();
        }

        @Override
        public void onStart() {

        }
    }

}
