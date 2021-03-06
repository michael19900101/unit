package com.aotuman.unit.handler;

import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.aotuman.unit.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class HandlerMainActivity extends AppCompatActivity {

    @BindView(R.id.btn_1)
    Button button1;

    @BindView(R.id.btn_2)
    Button button2;

    @BindView(R.id.btn_3)
    Button button3;

    @BindView(R.id.btn_4)
    Button button4;

    @BindView(R.id.btn_5)
    Button button5;

    private Handler mHandler;//


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessToThread1();
            }
        });

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessToThread2();
            }
        });

        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessToThread3();
            }
        });

        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new NonUiThread().start();
            }
        });

        button5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessToThread4();
            }
        });

    }

    //创建子线程
    class MyThread extends Thread{
        private Looper looper;//取出该子线程的Looper

        MyThread(String name){
            super(name);
        }

        public void run() {
            Looper.prepare();//创建该子线程的Looper
            looper = Looper.myLooper();//取出该子线程的Looper
            Looper.loop();//只要调用了该方法才能不断循环取出消息
        }
    }


    /**
     * 主线程向子线程发消息
     */
    private void sendMessToThread1(){
        MyThread myThread = new MyThread("子线程11");
        myThread.start();
        //下面是主线程发送消息
        //Handler初始化的时候，thread.looper还没有初始化，所以会报空指针
        mHandler = new Handler(myThread.looper){
            public void handleMessage(android.os.Message msg) {
                Log.d("当前子线程是----->",Thread.currentThread().getName());
            }
        };
        mHandler.sendEmptyMessage(1);
    }


    /**
     * 主线程向子线程发消息
     */
    private void sendMessToThread2(){
        //实例化一个特殊的线程HandlerThread，必须给其指定一个名字
        HandlerThread thread = new HandlerThread("handler thread");
        thread.start();//千万不要忘记开启这个线程
        //将mHandler与thread相关联
        mHandler = new Handler(thread.getLooper()){
            public void handleMessage(android.os.Message msg) {
                Log.d("当前子线程是----->", Thread.currentThread().getName());
            }
        };
        mHandler.sendEmptyMessage(1);//发送消息

    }

    /**
     * 主线程向子线程发消息
     */
    private void sendMessToThread3(){

        new Thread(new Runnable() {
            @Override
            public void run() {
                // 1.创建子线程的Looper消息轮询器
                //  Looper.prepare() -> sThreadLocal.set(new Looper(quitAllowed)); ->   mQueue = new MessageQueue(quitAllowed);
                Looper.prepare();
                // 2.创建子线程的handler，自动关联该线程的Looper
                //  new Handler() -> mLooper = Looper.myLooper(); -> sThreadLocal.get()
                Handler handler = new Handler(){

                    @Override
                    public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        Log.d("当前子线程是----->", Thread.currentThread().getName());
                        Toast.makeText(getApplicationContext(), "当前子线程是----->"+ Thread.currentThread().getName(), Toast.LENGTH_LONG).show();
                    }
                };
                // 3.发送消息 ,把消息放进消息队列
                // sendMessageAtTime -> enqueueMessage
                handler.sendEmptyMessage(1);
                // 4.Looper消息轮询器不断循环取出消息
                Looper.loop();
                // 5.回调handler的handleMessage方法
                // Message msg = queue.next(); ->  msg.target.dispatchMessage(msg); ->   message.callback.run();
            }
        }).start();
    }

    /**
     * 子线程向子线程发消息
     */
    private void sendMessToThread4(){
        //把looper绑定到子线程中，并且创建一个handler。在另一个线程中通过这个handler发送消息，就可以实现子线程之间的通信了。
        new Thread(new Runnable() {

            @Override
            public void run() {
                Looper.prepare();
                mHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        System.out.println("这个消息是从-->>" + msg.obj+ "过来的，在" + "btn的子线程当中" + "中执行的");
                    }

                };
                Looper.loop();//开始轮循

            }
        }).start();

        new Thread(new Runnable() {

            @Override
            public void run() {
                Message msg = mHandler.obtainMessage();
                msg.obj = "子线程B向子线程A发消息";
                mHandler.sendMessage(msg);
            }
        }).start();
    }



    //非UI线程是可以刷新UI的呀，前提是它要拥有自己的ViewRoot。如果想直接创建ViewRoot实例，你会发现找不到这个类。那怎么做呢？通过WindowManager。
    class NonUiThread extends Thread{
        @Override
        public void run() {
            Looper.prepare();
            TextView tx = new TextView(HandlerMainActivity.this);
            tx.setText("non-UiThread update textview");

            WindowManager windowManager = HandlerMainActivity.this.getWindowManager();
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    200, 200, 200, 200, WindowManager.LayoutParams.FIRST_SUB_WINDOW,
                    WindowManager.LayoutParams.TYPE_TOAST,PixelFormat.OPAQUE);
            //通过windowManager.addView创建了ViewRoot，WindowManagerImpl.java中的addView方法：
            windowManager.addView(tx, params);
            Looper.loop();
        }
    }

    // 子线程通过handler.post(runnable)更新UI
    private void post(){
        mHandler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // 在这里进行UI操作
                    }
                });
            }
        }).start();

    }

    // 子线程通过runOnUiThread()更新UI
    private void runonuithread(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

            }
        });
    }
}
