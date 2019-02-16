package com.aotuman.unit.leak;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.aotuman.unit.R;

import java.lang.ref.WeakReference;

/**
 * 解决方法：静态内部类+弱引用
 * 原理 静态内部类 不默认持有外部类的引用，从而使得 “未被处理 / 正处理的消息 -> Handler实例 -> 外部类” 的引用关系 的引用关系 不复存在。
 *
 * 同时，还可加上 使用WeakReference弱引用持有Activity实例
 * 原因：弱引用的对象拥有短暂的生命周期。在垃圾回收器线程扫描时，一旦发现了只具有弱引用的对象，不管当前内存空间足够与否，都会回收它的内存
 */
public class LeakFixActivity extends AppCompatActivity {


    public static final String TAG = "LeakInnerClassActivity";
    private Handler showhandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //1. 实例化自定义的Handler类对象->>分析1
        //注：
        // a. 此处并无指定Looper，故自动绑定当前线程(主线程)的Looper、MessageQueue；
        // b. 定义时需传入持有的Activity实例（弱引用）
        showhandler = new FHandler(this);

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
    // 设置为：静态内部类
    private static class FHandler extends Handler{

        // 定义 弱引用实例
        private WeakReference<Activity> reference;

        // 在构造方法中传入需持有的Activity实例
        public FHandler(Activity activity) {
            // 使用WeakReference弱引用持有Activity实例
            reference = new WeakReference<>(activity); }

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

    //解决方案2：当外部类结束生命周期时，清空Handler内消息队列
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 外部类Activity生命周期结束时，同时清空消息队列 & 结束Handler生命周期
        showhandler.removeCallbacksAndMessages(null);
    }

}
