package com.aotuman.unit.leak.handler;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.aotuman.unit.R;

/**
 * 参考：https://juejin.im/post/5a692377518825734e3e71ab
 * 造成内存泄露的原因有2个关键条件：
 * 存在“未被处理 / 正处理的消息 -> Handler实例 -> 外部类(Activity)” 的引用关系
 * Handler的生命周期 > 外部类的生命周期
 */
public class LeakInnerClassActivity extends AppCompatActivity {


    public static final String TAG = "LeakInnerClassActivity";
    private Handler showhandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //1. 实例化自定义的Handler类对象->>分析1
        //注：此处并无指定Looper，故自动绑定当前线程(主线程)的Looper、MessageQueue
        showhandler = new FHandler();

        // 2. 启动子线程1
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // a. 定义要发送的消息
                Message msg = Message.obtain();
                msg.what = 1;// 消息标识
                msg.obj = "AA";// 消息存放
                // b. 传入主线程的Handler & 向其MessageQueue发送消息
                showhandler.sendMessage(msg);
            }
        }.start();

        // 3. 启动子线程2
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // a. 定义要发送的消息
                Message msg = Message.obtain();
                msg.what = 2;// 消息标识
                msg.obj = "BB";// 消息存放
                // b. 传入主线程的Handler & 向其MessageQueue发送消息
                showhandler.sendMessage(msg);
            }
        }.start();

    }

    // 分析1：自定义Handler子类
    class FHandler extends Handler {

        // 通过复写handlerMessage() 从而确定更新UI的操作
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    Log.d(TAG, "收到线程1的消息");
                    break;
                case 2:
                    Log.d(TAG, " 收到线程2的消息");
                    break;

            }
        }
    }
}
