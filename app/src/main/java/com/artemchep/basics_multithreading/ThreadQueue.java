package com.artemchep.basics_multithreading;

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
    private Boolean isRunning = true;
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
            if (queue.isEmpty()) {
                synchronized (this) {
                    try {
                        this.wait();
                        Log.d("ThreadQueue:", currentThread().getName() + " is waiting");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                synchronized (queue) {
                    Log.d("ThreadQueue:", "Thread name: " + currentThread().getName());

                    WithMillis<Message> messageWithMillis = queue.poll();
                    Message message = messageWithMillis.value;

                    long startEncryptingTime = System.currentTimeMillis();
                    final String encrypt = CipherUtil.encrypt(message.plainText);
                    long endEncryptingTime = System.currentTimeMillis() - startEncryptingTime;
                    long duration = messageWithMillis.elapsedMillis + System.currentTimeMillis() + endEncryptingTime;

                    Message newMessage = message.copy(encrypt);
                    final WithMillis<Message> newMessageWithMillis = new WithMillis<>(newMessage, duration);
                    Log.d("ThreadQueue:", "Message: " + newMessage.cipherText);
                    Log.d("ThreadQueue:", "WithMillis<Message>: " + newMessageWithMillis.elapsedMillis);

                    callBack.onListUpdated(newMessageWithMillis);
                }
            }
        }
    }

    public void addItemToQueue(WithMillis<Message> message) throws InterruptedException {
        //add item to queue
        synchronized (this) {
            queue.put(message);
            Log.d("ThreadQueue:", "Item added");
            Log.d("ThreadQueue:", "Queue: " + queue);

            //notify all threads about item added to queue
            this.notifyAll();
            Log.d("ThreadQueue:", "Thread: " + currentThread().getName() + " notifiying");
            Log.d("ThreadQueue:", "Thread: " + currentThread().getState());
        }
    }

    public void dispose() {
        queue = null;
        isRunning = false;
    }
}
