package com.a2017taipeinasachallenge.aaa;

import android.os.Handler;
import android.support.annotation.UiThread;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by s352431 on 2017/4/29.
 */

public class ConnectService {
    public interface Callback{
        void callback(boolean success,String json);
    }


    public static void sendGet(final String url, final Callback callback, final Handler handler){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    final String ans = get(url);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.callback(true,ans);
                        }
                    });
                }catch (IOException e){
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.callback(false,"");
                        }
                    });
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static String get(String url) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");


    public static void sendPost(final String url, final String json, final Callback callback, final Handler handler){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    final String ans = post(url,json);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.callback(true,ans);
                        }
                    });
                }catch (IOException e){
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.callback(false,"");
                        }
                    });
                    e.printStackTrace();
                }

            }
        }).start();
    }
    public static String post(String url, String json) throws IOException {
        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }

}
