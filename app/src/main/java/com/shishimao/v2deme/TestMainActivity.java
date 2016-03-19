package com.shishimao.v2deme;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.shishimao.sdk.Configs;
import com.shishimao.sdk.Errors;
import com.shishimao.sdk.RTCat;
import com.shishimao.sdk.Receiver;
import com.shishimao.sdk.Receiver.ReceiverObserver;
import com.shishimao.sdk.Sender;
import com.shishimao.sdk.Sender.SenderObserver;
import com.shishimao.sdk.Session;
import com.shishimao.sdk.Session.SessionObserver;
import com.shishimao.sdk.Stream;
import com.shishimao.sdk.Stream.StreamObserver;
import com.shishimao.sdk.apprtc.AppRTCAudioManager;
import com.shishimao.sdk.http.RTCatRequests;
import com.shishimao.sdk.tools.L;
import com.shishimao.sdk.view.VideoPlayer;
import com.shishimao.sdk.view.VideoPlayerLayout;

import org.json.JSONObject;


import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by chencong on 3/2/16.
 */
public class TestMainActivity extends Activity implements AdapterView.OnItemSelectedListener{
    private final static String TAG  = "TestMainActivity";

    ArrayAdapter<CharSequence> adapter;

    VideoPlayerLayout videoRenderLayout;
    VideoPlayer localVideoPlayer;
    String[] audio_device_list;
    Spinner spinner;
    Resources res;

    //webrtc
    RTCat cat;
    Stream localStream;
    Session session;

    HashMap<String,Sender> senders = new HashMap<>();
    HashMap<String,Receiver> receivers = new HashMap<>();

    ArrayList<VideoPlayer> render_list = new ArrayList<>();
    HashMap<String,VideoPlayerLayout> render2_list = new HashMap<>();

    int layout_width = 50;
    int layout_height = 50;

    int x = 0;
    int y = 0;

    public String token;

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
        videoRenderLayout = (VideoPlayerLayout) findViewById(R.id.local_video_layout);
        videoRenderLayout.setPosition(50, 50, 50, 50);


        try {
            cat = new RTCat(TestMainActivity.this,true,true,true,false, AppRTCAudioManager.AudioDevice.EARPIECE ,RTCat.CodecSupported.VP8, L.VERBOSE);
        }catch (AssertionError e){
            Log.d(TAG,"no such device");
        }

        cat.initVideoPlayer(localVideoPlayer);


        localStream = cat.createStream();
        //增加监听事件,监听是摄像头切换事件
        localStream.addObserver(new StreamObserver() {

            @Override
            public void afterSwitch(boolean isFrontCamera) {

            }
        });

        localStream.play(localVideoPlayer);

        createSession(null);
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

                            if (session.getWits().size() < 3)
                            {
                                JSONObject attr = new JSONObject();
                                try {
                                    attr.put("type", "main");
                                    attr.put("name", "old wang");
                                } catch (Exception e) {

                                }

                                session.sendTo(localStream,true,attr, token);
                            }
                        }

                        @Override
                        public void close() {
                            finish();
                        }

                        @Override
                        public void out(String token) {
                            final VideoPlayerLayout layout =  render2_list.get(token);

                            if( x == 0 && y == 50)
                            {
                                x = 50 ; y =0;
                            }else if(x == 50 && y == 0)
                            {
                                x = 0;
                            }


                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if(layout != null)
                                    {
                                        RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.video_layout);
                                        relativeLayout.removeView(layout);
                                    }
                                }
                            });
                        }

                        @Override
                        public void connected(final ArrayList wits) {
                            l("connected main");

                            String wit = "";
                            for (int i = 0; i < wits.size(); i++) {
                                if( i == 3)
                                {
                                    break;
                                }
                                try {
                                    wit = wit + wits.get(i);

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }


                            JSONObject attr = new JSONObject();
                            try {
                                attr.put("type", "main");
                                attr.put("name", "old wang");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            session.send(localStream,true,attr);
                        }

                        @Override
                        public void remote(final Receiver receiver) {
                            try {
                                    receivers.put(receiver.getId(), receiver);

                                    receiver.addObserver(new ReceiverObserver() {
                                        @Override
                                        public void audioLog(JSONObject jsonObject) {

                                        }

                                        @Override
                                        public void videoLog(JSONObject jsonObject) {

                                        }

                                        @Override
                                        public void error(Errors errors) {

                                        }

                                        @Override
                                        public void stream(final Stream stream) {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    t(receiver.getFrom() + " stream");
                                                    VideoPlayer videoViewRemote = new VideoPlayer(TestMainActivity.this);
                                                    render_list.add(videoViewRemote);

                                                    cat.initVideoPlayer(videoViewRemote);

                                                    RelativeLayout layout = (RelativeLayout) findViewById(R.id.video_layout);
                                                    VideoPlayerLayout remote_video_layout = new VideoPlayerLayout(TestMainActivity.this);

                                                    render2_list.put(receiver.getFrom(),remote_video_layout);

                                                    remote_video_layout.addView(videoViewRemote);

                                                    remote_video_layout.setPosition(x,y,layout_width,layout_height);

                                                    if( x == 0 && y == 0)
                                                    {
                                                        x = 50;
                                                    }else if(x == 50 && y == 0)
                                                    {
                                                        x = 0; y= 50;
                                                    }

                                                    layout.addView(remote_video_layout);

                                                    stream.play(videoViewRemote);
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
                                public void videoLog(JSONObject jsonObject) {

                                }

                                @Override
                                public void audioLog(JSONObject jsonObject) {

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

        for (VideoPlayer renderer:render_list)
        {
            renderer.release();
        }

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