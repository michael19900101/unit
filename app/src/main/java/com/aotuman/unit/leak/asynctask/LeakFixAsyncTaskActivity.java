package com.aotuman.unit.leak.asynctask;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.aotuman.unit.R;

import java.lang.ref.WeakReference;

public class LeakFixAsyncTaskActivity extends AppCompatActivity {

    private MyTask myTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myTask = new MyTask(this);
        myTask.execute();
    }

    static class MyTask extends AsyncTask<String, Integer, String> {
        private WeakReference<Activity> weakAty;

        public MyTask(Activity activity) {
            weakAty = new WeakReference<>(activity);
        }

        @Override
        protected String doInBackground(String... params) {
            for (int i = 0; i < 50; i++) {
                Log.i("Mytask", "i=" + i);
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
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.i("Mytask", "执行结束了");
            LeakFixAsyncTaskActivity mActivity;
            if ((mActivity = (LeakFixAsyncTaskActivity) weakAty.get()) != null) {
                mActivity.doSomething();
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            Log.i("Mytask", "执行了取消");
        }
    }

    private void doSomething() {
        //为保险，还是需要判断下当前activity是否已经销毁，因为weakReference修饰的对象并不是马上就能被回收
        Log.i("LeakFAsyncTaskActivity", "异步任务完成，更新UI");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("LeakFAsyncTaskActivity", "onDestroy");
        myTask.cancel(true);
    }
}
