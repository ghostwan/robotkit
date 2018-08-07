package com.ghostwan.robotkit.sampleapp.test;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.QiSDK;
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks;
import com.aldebaran.qi.sdk.builder.ChatBuilder;
import com.aldebaran.qi.sdk.builder.DiscussBuilder;
import com.aldebaran.qi.sdk.builder.QiChatbotBuilder;
import com.aldebaran.qi.sdk.builder.TopicBuilder;
import com.aldebaran.qi.sdk.object.conversation.*;
import com.ghostwan.robotkit.naoqi.object.RKQiChatExecutor;
import com.ghostwan.robotkit.naoqi.object.RKQiChatExecutorAsync;
import com.ghostwan.robotkit.sampleapp.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestJavaActivity extends AppCompatActivity implements RobotLifecycleCallbacks {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_java);
        QiSDK.register(this, this);
    }

    @Override
    public void onRobotFocusGained(QiContext qiContext) {
        Topic topic = TopicBuilder.with(qiContext).withResource(R.raw.test_topic).build();
        QiChatbot qiChatbot = QiChatbotBuilder.with(qiContext).withTopic(topic).build();
        Chat chat = ChatBuilder.with(qiContext).withChatbot(qiChatbot).build();
        Map<String, QiChatExecutor> executors = new HashMap<>();
        executors.put("animate", new RKQiChatExecutor(qiContext.getSerializer(), new RKQiChatExecutorAsync() {
            @Override
            public Future<Void> runWith(List<String> params) {
                return null;
            }

            @Override
            public Future<Void> stop() {
                return null;
            }
        }));
        qiChatbot.setExecutors(executors);

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
