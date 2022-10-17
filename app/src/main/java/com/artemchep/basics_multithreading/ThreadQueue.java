package com.artemchep.basics_multithreading;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.artemchep.basics_multithreading.cipher.CipherUtil;
import com.artemchep.basics_multithreading.domain.Message;
import com.artemchep.basics_multithreading.domain.WithMillis;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

interface ThreadQueueInterface {
    void onListUpdated(WithMillis<Message> messageWithMillis);
}

public class ThreadQueue extends Thread {
    private BlockingQueue<WithMillis<Message>> queue = new LinkedBlockingQueue<WithMillis<Message>>();
    private volatile Boolean isRunning = true;
    private ThreadQueueInterface callBack;

    ThreadQueue() {
        this.start();
    }

    public void setCallBack(ThreadQueueInterface update) {
        callBack = update;
    }

    @Override
    public void run() {
        while (isRunning) {
            Runnable encryptingTask = getRunnable();
            encryptingTask.run();
            Log.d("ThreadQueue:", currentThread().getName());
        }
    }

    private synchronized Runnable getRunnable() {
        while (queue.isEmpty()) {
            try {
                wait();
                Log.d("ThreadQueue:", currentThread().getName() + " is waiting");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Runnable newTask = new Runnable() {
            @Override
            public void run() {
                Log.d("ThreadQueue:", "Thread name: " + currentThread().getName());

                WithMillis<Message> messageWithMillis = queue.poll();
                Message message = messageWithMillis.value;

                final String encrypt = CipherUtil.encrypt(message.plainText);

                Message newMessage = message.copy(encrypt);
                long endEncryptingTime = SystemClock.elapsedRealtime();
                long duration = endEncryptingTime - messageWithMillis.elapsedMillis;

                final WithMillis<Message> newMessageWithMillis = new WithMillis<>(newMessage, duration);
                Log.d("ThreadQueue:", "Message: " + newMessage.cipherText);
                Log.d("ThreadQueue:", "WithMillis<Message>: " + newMessageWithMillis.elapsedMillis);
                Log.d("ThreadQueue:", "Duration: " + duration);

                callBack.onListUpdated(newMessageWithMillis);
            }
        };
        return newTask;
    }

    public void addItemToQueue(WithMillis<Message> message) {
        //add item to queue
        synchronized (this) {
            queue.add(message);
            Log.d("ThreadQueue:", "Item added");
            Log.d("ThreadQueue:", "Queue: " + queue);

            //notify all threads about item added to queue
            this.notifyAll();
            Log.d("ThreadQueue:", "Thread: " + currentThread().getName() + " notifiying");
            Log.d("ThreadQueue:", "Thread: " + currentThread().getState());
        }
    }

    public synchronized void dispoce() {
        isRunning = false;
        notifyAll();
    }
}
