package com.aotuman.unit.leak.asynctask;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.aotuman.unit.R;

public class LeakAsyncTaskActivity extends AppCompatActivity {

    private AsyncTask task;
    public static final String TAG = "LeakAsyncTaskActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* 用匿名内部类的方式创建*/
        task = new AsyncTask() {
            @SuppressLint("WrongThread")
            @Override
            protected Object doInBackground(Object[] params) {
                for (int i = 0; i < 10; i++) {
                    Log.i(TAG, "i=" + i);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (isCancelled()) {
                        break;
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);
                Log.i(TAG, "执行结束了");
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
                Log.i(TAG, "执行了取消");
            }
        };
        task.execute();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        task.cancel(true);
    }

}
