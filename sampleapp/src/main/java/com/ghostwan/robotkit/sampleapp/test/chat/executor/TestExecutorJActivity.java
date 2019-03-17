package com.ghostwan.robotkit.sampleapp.test.chat.executor;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import com.aldebaran.qi.Future;
import com.aldebaran.qi.Promise;
import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.QiSDK;
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks;
import com.aldebaran.qi.sdk.builder.*;
import com.aldebaran.qi.sdk.object.actuation.Animate;
import com.aldebaran.qi.sdk.object.actuation.Animation;
import com.aldebaran.qi.sdk.object.conversation.Chat;
import com.aldebaran.qi.sdk.object.conversation.QiChatExecutor;
import com.aldebaran.qi.sdk.object.conversation.QiChatbot;
import com.aldebaran.qi.sdk.object.conversation.Topic;
import com.ghostwan.robotkit.naoqi.object.AnyObjectProviderConverter;
import com.ghostwan.robotkit.naoqi.object.AsyncQiChatExecutor;
import com.ghostwan.robotkit.naoqi.object.SyncQiChatExecutor;
import com.ghostwan.robotkit.sampleapp.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class TestExecutorJActivity extends AppCompatActivity implements RobotLifecycleCallbacks {

    private static final String TAG = "TestExecutorJActivity";
    private Button startButton;
    private Future<Void> chatting = Future.of(null);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_executor);
        startButton = findViewById(R.id.startButton);
        QiSDK.register(this, this);
    }

    @Override
    public void onRobotFocusGained(QiContext qiContext) {

        qiContext.getSerializer().addConverter(new AnyObjectProviderConverter());
        Topic topic = TopicBuilder.with(qiContext).withResource(R.raw.test_execute).build();
        QiChatbot qiChatbot = QiChatbotBuilder.with(qiContext).withTopic(topic).build();

        Map<String, QiChatExecutor> executors = new HashMap<>();
        executors.put("animateSync", new SyncQiChatExecutor(qiContext.getSerializer()) {
            Future<Void> animating;

            @Override
            public void runWith(List<String> params) {
                Animation animation = AnimationBuilder.with(qiContext).withResources(R.raw.taichichuan_a001).build();
                Animate animate = AnimateBuilder.with(qiContext).withAnimation(animation).build();
                animating = animate.async().run();
                try {
                    animating.get();
                } catch (ExecutionException e) {
                    Log.e(TAG, "error :", e);
                }
            }

            @Override
            public void stop() {
                animating.requestCancellation();
            }
        });
        executors.put("animateAsync", new AsyncQiChatExecutor(qiContext.getSerializer(), new QiChatExecutor.Async() {
            Future<Void> animating;

            @Override
            public Future<Void> runWith(List<String> params) {
                Animation animation = AnimationBuilder.with(qiContext).withResources(R.raw.taichichuan_a001).build();
                Animate animate = AnimateBuilder.with(qiContext).withAnimation(animation).build();
                animating =  animate.async().run();
                return animating;
            }

            @Override
            public Future<Void> stop() {
                animating.requestCancellation();
                return Future.of(null);
            }
        }));
        executors.put("playAsync", new AsyncQiChatExecutor(qiContext.getSerializer(), new QiChatExecutor.Async() {
            private MediaPlayer mediaPlayer;

            @Override
            public Future<Void> runWith(List<String> params) {
                Promise<Void> promise = new Promise<>();
                mediaPlayer = MediaPlayer.create(qiContext, R.raw.wait6_sound);
                mediaPlayer.setOnCompletionListener(mp -> {
                    promise.setValue(null);
                    mediaPlayer.release();
                });
                mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    promise.setError("Error : "+what);
                    return true;
                });
                mediaPlayer.start();
                return promise.getFuture();
            }

            @Override
            public Future<Void> stop() {
                mediaPlayer.stop();
                mediaPlayer.release();
                return Future.of(null);
            }

        }));
        qiChatbot.setExecutors(executors);
        Chat chat = ChatBuilder.with(qiContext).withChatbot(qiChatbot).build();

        runOnUiThread(() -> {
            startButton.setEnabled(true);
            startButton.setText("Start");
            startButton.setOnClickListener(v -> {
                if(startButton.getText().equals("Start")) {
                    chatting = chat.async().run();
                    startButton.setText("Stop");
                }
                else {
                    chatting.requestCancellation();
                    startButton.setText("Start");
                }
            });
        });


    }

    @Override
    public void onRobotFocusLost() {

    }

    @Override
    public void onRobotFocusRefused(String reason) {
        Log.e("Test", "error"+reason);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        QiSDK.unregister(this);
    }
}
