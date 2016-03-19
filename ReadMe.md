## 实时猫 Android SDK V0.2 Demo
根据 [实时猫 Android SDK](https://shishimao.com) 开发的 Demo

## 使用

1. `git clone https://github.com/RTCat/rtcat_android_v0.2_demo.git`
2. 通过Android Studio导入, File > Import Project ,选择项目中的build.gradle文件导入
3. 在项目中增加权限和jar，so文件（详情参考实时猫Android SDK 文档）
4. 把`Config.java.backup`改为`Config.java`,并填入从 `实时猫控制台` 获得的 `APIKEY`,`SECRET`,`SESSSION`

## 代码说明

### res

`VideoPlayerLayout` 相当于一个 `ViewGroup`, 用于显示 `VideoPlayer`,`VideoPlayer`能被包含在任意的`ViewGroup`里
 
`res/layout/test_activity_main.xml`

```
        <com.shishimao.sdk.view.VideoPlayerLayout
            android:id="@+id/local_video_layout"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content">
            <com.shishimao.sdk.view.VideoPlayer
                android:id="@+id/local_video_render"
                android:layout_height="wrap_content"
                android:layout_width="wrap_content"
                ></com.shishimao.sdk.view.VideoPlayer>
        </com.shishimao.sdk.view.VideoPlayerLayout>
```


### activity

`TestMainActivity`  

在使用`实时猫 Android SDK`时，首先要初始化 `RTCat` 对象，如果没有音频输出设备，则会引发`AssertionError`，`RTCat`对象完整介绍，请参考文档.

```
        try {
            cat = new RTCat(TestMainActivity.this,true,true,true,false, AppRTCAudioManager.AudioDevice.EARPIECE ,RTCat.CodecSupported.VP8, L.VERBOSE);
        }catch (AssertionError e){
            Log.d(TAG,"no such device");
        }

```

`VideoPlayer` 用于显示视频流,使用前要通过`RTCat`对象初始化

```
cat.initVideoPlayer(localVideoPlayer);
```

`Stream`对象 通过`RTCat`对象创建，相关参数设置，请参考文档。

```

        localStream = cat.createStream();
        //增加监听事件,监听是摄像头切换事件
        localStream.addObserver(new StreamObserver() {

            @Override
            public void afterSwitch(boolean isFrontCamera) {

            }
        });

        localStream.play(localVideoPlayer);

```

`Session`对象 通过`RTCat`对象创建，目前只支持`P2P`模式，相关参数设置，请参考文档

```
session = cat.createSession(token, Session.SessionType.P2P);
session.addObserver(new SessionObserver() {
			...                        
});
session.connect();
```

`Session`对象创建成功后，增加`Observer`，通过`Observer`监听触发事件.
以下是 `Session`触发的事件及相应说明.

> * `in`：监听用户进入`Session`事件，回调返回连入者的`Token`，开发者可以在这里建立视频或者音频连接。
> * `connected`：监听`Session`连接成功事件，回调返回所有已在`Session`里面的`token`。
> * `remote`：监听`Session`有`Receiver`连入，回调返回连入的`Receiver`，开发者需要在这里处理`Receiver`相关的事件，具体事件在下文说明。
> * `local`：监听`Session`有`Sender`连入，回调返回连入的`Sender`，开发者需要这里处理`Sender`相关的事件，具体事件在下文说明。
> * `out`：监听用户离开`Session`事件，回调返回离开的`Token`，开发者可以在这里回收资源。
> * `close`：监听断开`Session`事件，开发者可以在这里回收资源。
> * `message`：监听`Session`消息事件，回调返回发消息的`Token`和消息内容。
> * `error`：监听`Session`错误事件，回调返回错误信息。

**注：** `session.addObserver(...)` 需要在`session.connect()`调用。

 
接收到`Receiver`对象后，需要增加`Observer`，通过`Observer`监听触发事件。
以下是 `Receiver`触发的事件及相应说明。

> * `stream`：监听流连入`Receiver`事件，回调返回流，开发者可以在这里播放流。
> * `message`：监听`Receiver`消息事件，回调返回消息内容。
> * `audioLog`：监听`Receiver`收到的音频相关日志事件，回调返回日志内容。
> * `videoLog`：监听`Receiver`收到的视频相关日志事件，回调返回日志内容。
> * `error`：监听`Receiver`错误事件，回调返回错误信息。
> * `close`：监听`Receiver`关闭事件。

接收到`Sender`对象后，需要增加`Observer`，通过`Observer`监听触发事件。
以下是`Sender`触发的事件及相应的说明。
> * `audioLog`：监听`Sender `收到的音频相关日志事件，回调返回日志内容。
> * `videoLog`：监听`Sender `收到的视频相关日志事件，回调返回日志内容。
> * `error`：监听`Sender `错误事件，回调返回错误信息。
> * `close`：监听`Sender `关闭事件。







