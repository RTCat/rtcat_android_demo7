package com.shishimao.StyleDemo;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.shishimao.sdk.Configs;
import com.shishimao.sdk.Errors;
import com.shishimao.sdk.LocalStream;
import com.shishimao.sdk.RTCat;
import com.shishimao.sdk.Receiver;
import com.shishimao.sdk.Receiver.ReceiverObserver;
import com.shishimao.sdk.RemoteStream;
import com.shishimao.sdk.Sender;
import com.shishimao.sdk.Sender.SenderObserver;
import com.shishimao.sdk.Session;
import com.shishimao.sdk.Session.SessionObserver;
import com.shishimao.sdk.apprtc.AppRTCAudioManager;
import com.shishimao.sdk.http.RTCatRequests;
import com.shishimao.sdk.tools.L;
import com.shishimao.sdk.view.VideoPlayer;
import com.shishimao.sdk.view.VideoPlayerLayout;

import org.json.JSONObject;


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class TestMainActivity extends Activity implements AdapterView.OnItemSelectedListener{
    private final static String TAG  = "TestMainActivity";

    ArrayAdapter<CharSequence> adapter;

    VideoPlayerLayout localRenderLayout;
    VideoPlayer localVideoPlayer;
    VideoPlayerLayout remoteRenderLayout;
    VideoPlayer remoteVideoPlayer;
    String[] audio_device_list;
    Spinner spinner;
    Resources res;

    //webrtc
    RTCat cat;
    LocalStream localStream;
    Session session;

    HashMap<String,Sender> senders = new HashMap<>();
    HashMap<String,Receiver> receivers = new HashMap<>();

    public String token;
    boolean isRemotePlay = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_activity_main);

        res = getResources();

        spinner = (Spinner) findViewById(R.id.audio_device_spi);
        spinner.setOnItemSelectedListener(this);


        audio_device_list =  res.getStringArray(R.array.audio_devices);

        adapter = ArrayAdapter.createFromResource(this,
                R.array.audio_devices, android.R.layout.simple_spinner_item);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(adapter);
        //webrtc

        localVideoPlayer = (VideoPlayer) findViewById(R.id.local_video_render);
        localRenderLayout = (VideoPlayerLayout) findViewById(R.id.local_video_layout);
        localRenderLayout.setPosition(0,0,100,100);

        remoteVideoPlayer = (VideoPlayer) findViewById(R.id.remote_video_render);
        remoteRenderLayout = (VideoPlayerLayout) findViewById(R.id.remote_video_layout);
        remoteRenderLayout.setPosition(0,0,0,0);

        try {
            cat = new RTCat(TestMainActivity.this,true,true,true,false, AppRTCAudioManager.AudioDevice.SPEAKER_PHONE ,RTCat.CodecSupported.VP8, L.VERBOSE);
        }catch (AssertionError e){
            Log.d(TAG,"no such device");
        }



        cat.addObserver(new RTCat.RTCatObserver() {
            @Override
            public void init() {
                createLocalStream();
            }
        });

        cat.init();

    }

    public void createLocalStream(){
        cat.initVideoPlayer(localVideoPlayer);
        localStream = cat.createStream(true,true,15,RTCat.VideoFormat.Lv0, LocalStream.CameraFacing.FRONT);
        //增加监听事件,监听是摄像头切换事件
        localStream.addObserver(new LocalStream.StreamObserver() {
            @Override
            public void error(Errors errors) {

            }

            @Override
            public void afterSwitch(boolean isFrontCamera) {}

            @Override
            public void accepted() {
                localStream.play(localVideoPlayer);
                createSession(null);
            }
        });

        localStream.init();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        cat.setAudioDevice(AppRTCAudioManager.AudioDevice.valueOf(audio_device_list[position]));
        Log.d(TAG, audio_device_list[position]);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        Log.d(TAG, "nothing");
    }



    public void switchCamera(View view)
    {
        localStream.switchCamera();
    }

    public void createSession(View view)
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {


                    RTCatRequests requests = new RTCatRequests(Config.APIKEY, Config.SECRET);
                    token = requests.getToken(Config.SESSION, "pub");
                    l("token is " + token);
                    session = cat.createSession(token, Session.SessionType.P2P);

                    session.addObserver(new SessionObserver() {
                        @Override
                        public void in(String token) {
                            l(token + " is in");
                            l(String.valueOf(session.getWits().size()));

                            if (session.getWits().size() == 1)
                            {
                                JSONObject attr = new JSONObject();
                                try {
                                    attr.put("type", "main");
                                    attr.put("name", "old wang");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                session.sendTo(localStream,true,attr, token);
                            }
                        }

                        @Override
                        public void close() {
                            finish();
                        }

                        @Override
                        public void out(final String token) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    l(token + " is out");
                                    remoteRenderLayout.setPosition(0,0,0,0);
                                    localRenderLayout.setPosition(0,0,100,100);
                                    remoteVideoPlayer.requestLayout();
                                    remoteVideoPlayer.release();
                                    isRemotePlay = false;
                                }
                            });
                        }

                        @Override
                        public void connected(final ArrayList wits) {
                            l("connected main");

                            JSONObject attr = new JSONObject();
                            try {
                                attr.put("type", "main");
                                attr.put("name", "old wang");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            if(wits.size() == 1)
                                session.send(localStream,true,attr);
                        }

                        @Override
                        public void remote(final Receiver receiver) {
                            l("get receiver");
                            try {
                                    receivers.put(receiver.getId(), receiver);

                                    receiver.addObserver(new ReceiverObserver() {
                                        @Override
                                        public void error(Errors errors) {

                                        }

                                        @Override
                                        public void log(JSONObject jsonObject) {

                                        }

                                        @Override
                                        public void file(File file) {

                                        }

                                        @Override
                                        public void stream(final RemoteStream stream) {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    if(isRemotePlay)
                                                        return;
                                                    t(receiver.getFrom() + " stream");
                                                    cat.initVideoPlayer(remoteVideoPlayer);
                                                    remoteRenderLayout.setPosition(0,0,100,100);
                                                    localRenderLayout.setPosition(60,0,40,40);
                                                    localVideoPlayer.setZOrderMediaOverlay(true);
                                                    localVideoPlayer.requestLayout();
                                                    stream.play(remoteVideoPlayer);
                                                    isRemotePlay = true;
                                                }
                                            });

                                        }

                                        @Override
                                        public void message(String message) {

                                        }

                                        @Override
                                        public void close() {

                                        }
                                    });

                                    receiver.response();
                                } catch (Exception e) {
                                    l(e.toString());
                                }


                            }

                        @Override
                        public void local(final Sender sender) {
                            senders.put(sender.getId(), sender);
                            sender.addObserver(new SenderObserver() {

                                @Override
                                public void log(JSONObject jsonObject) {

                                }

                                @Override
                                public void fileSending(int i) {

                                }

                                @Override
                                public void fileFinished() {

                                }

                                @Override
                                public void close() {
                                    if(session.getState() == Configs.ConnectState.CONNECTED)
                                    {
                                        session.sendTo(localStream,false,null,sender.getTo());
                                    }
                                }

                                @Override
                                public void error(Errors errors) {

                                }
                            });
                        }

                        @Override
                        public void message(String token, String message) {

                        }

                        @Override
                        public void error(String error) {

                        }
                    });

                    session.connect();

                } catch (Exception e) {
                    l(e.toString());
                }
            }
        }).start();

    }



    public void l(String o)
    {

        Log.d("RTCatLog", o);
    }


    public void t(String o)
    {
        Toast.makeText(TestMainActivity.this, o,
                Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {

        if(localStream != null)
        {
            localStream.dispose();
        }

        if(session != null)
        {
            session.disconnect();
        }


        if(localVideoPlayer != null)
        {
            localVideoPlayer.release();
            localVideoPlayer = null;
        }

        remoteVideoPlayer.release();

        if(cat != null)
        {
            cat.release();
        }

        Log.d("Test","EXIT");

        super.onDestroy();

    }

    @Override
    protected void onStop() {
        if(localStream != null)
        {
            localStream.stop();
        }

        super.onStop();
    }

    @Override
    protected void onResume() {
        if(localStream != null)
        {
            localStream.start();
        }
        super.onResume();
    }
}