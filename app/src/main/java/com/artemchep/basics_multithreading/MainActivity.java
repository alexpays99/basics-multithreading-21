package com.artemchep.basics_multithreading;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.artemchep.basics_multithreading.domain.Message;
import com.artemchep.basics_multithreading.domain.WithMillis;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ThreadQueueInterface {
    private final String TAG = "MainActivity";
    private final List<WithMillis<Message>> mList = new ArrayList<>();
    private final ThreadQueue threadQueue = new ThreadQueue();
    private final MessageAdapter mAdapter = new MessageAdapter(mList);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        threadQueue.setCallBack(this);

        final RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mAdapter);

        showWelcomeDialog();
    }

    private void showWelcomeDialog() {
        new AlertDialog.Builder(this)
                .setMessage("What are you going to need for this task: Thread, Handler.\n" +
                        "\n" +
                        "1. The main thread should never be blocked.\n" +
                        "2. Messages should be processed sequentially.\n" +
                        "3. The elapsed time SHOULD include the time message spent in the queue.")
                .show();
    }


    public void onPushBtnClick(View view) throws InterruptedException {
        Message message = Message.generate();
        long startTime = System.currentTimeMillis();
        Log.d(TAG, "Current Time: " + startTime);
        insert(new WithMillis<>(message, startTime));
    }

    @UiThread
    public void insert(final WithMillis<Message> message) throws InterruptedException {
        mList.add(message);
        mAdapter.notifyItemInserted(   mList.size() - 1);

        // TODO: Start processing the message (please use CipherUtil#encrypt(...)) here.
        //       After it has been processed, send it to the #update(...) method.

        threadQueue.addItemToQueue(message);
        Log.d(TAG, "Thread: " + Thread.currentThread().getName());

        // How it should look for the end user? Uncomment if you want to see. Please note that
        // you should not use poor decor view to send messages to UI thread.
//        getWindow().getDecorView().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                final long time = System.currentTimeMillis();
//
//                final Message messageNew = message.value.copy(CipherUtil.encrypt(message.value.plainText));
//                final WithMillis<Message> messageNewWithMillis = new WithMillis<>(messageNew, CipherUtil.WORK_MILLIS + (System.currentTimeMillis() - time));
//                update(messageNewWithMillis);
//                Log.d("Thread state is ", String.valueOf(Thread.currentThread().getState()));
//                Log.d("Thread name is ", Thread.currentThread().getName());
//                Log.d("Time spend in queue: ", String.valueOf(CipherUtil.WORK_MILLIS + (System.currentTimeMillis() - time)));
//                Log.d("Thread name is ", Thread.currentThread().getName());
//            }
//        }, CipherUtil.WORK_MILLIS);
    }

    @UiThread
    public void update(final WithMillis<Message> message) {
        for (int i = 0; i < mList.size(); i++) {
            if (mList.get(i).value.key.equals(message.value.key)) {
                mList.set(i, message);
                mAdapter.notifyItemChanged(i);
                return;
            }
        }

        throw new IllegalStateException();
    }

    @Override
    public void onListUpdated(final WithMillis<Message> messageWithMillis) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                update(messageWithMillis);
                Log.d(TAG, "List updated in Thread: " + threadQueue.getName());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        threadQueue.dispose();
    }
}
