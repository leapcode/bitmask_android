package se.leap.bitmaskclient.base.utils;

import android.os.Handler;
import android.os.Looper;

public class HandlerProvider {


    public interface HandlerInterface {
        void postDelayed(Runnable r, long delay);
    }


    private static HandlerInterface instance;

    public HandlerProvider(HandlerInterface handlerInterface) {
        instance = handlerInterface;
    }
    public static HandlerInterface get() {
        if (instance == null) {
            instance = new DefaultHandler();
        }
        return instance;
    }

    public static class DefaultHandler implements HandlerInterface {
        Handler handler;

        public DefaultHandler() {
            this.handler = new Handler(Looper.getMainLooper());
        }
        @Override
        public void postDelayed(Runnable r, long delay) {
            this.handler.postDelayed(r, delay);
        }
    }
}

