package com.ghostwan.robotkit.sampleapp.test;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.QiSDK;
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks;
import com.aldebaran.qi.sdk.builder.DiscussBuilder;
import com.aldebaran.qi.sdk.builder.TopicBuilder;
import com.aldebaran.qi.sdk.object.conversation.Bookmark;
import com.aldebaran.qi.sdk.object.conversation.Discuss;
import com.aldebaran.qi.sdk.object.conversation.Topic;
import com.ghostwan.robotkit.sampleapp.R;

public class TestJavaActivity extends AppCompatActivity implements RobotLifecycleCallbacks {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_java);
        QiSDK.register(this, this);
    }

    @Override
    public void onRobotFocusGained(QiContext qiContext) {
        Topic t = TopicBuilder.with(qiContext).withResource(R.raw.presentation_discussion).build();
        Discuss d = DiscussBuilder.with(qiContext).withTopic(t).build();
        d.variable("name").setValue("erwan");
        d.variable("age").setValue("33");
        d.variable("gender").setValue("boy");
        Bookmark b = t.getBookmarks().get("next");
        d.setOnStartedListener(() -> {
            d.goToBookmarkedOutputUtterance(b);
        });
        d.run();

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
