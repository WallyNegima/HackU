package com.example.wally_nagama.paripigrass;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.util.Date;

import twitter4j.Twitter;
import twitter4j.TwitterException;

/**
 * Created by wally_nagama on 2017/05/27.
 */

public class Tweet {
    private Twitter mTwitter;
    private Context context;
    Date dTime = new Date();
    SharedPreferences preferences;
    String NKANAPI = "numberOfKanapi";


    public Tweet(Context c, Twitter t){
        //コンストラクタ
        this.context = c;
        this.mTwitter = t;
        preferences = c.getSharedPreferences(NKANAPI, Context.MODE_PRIVATE);

    }

    public void tweet() {
        AsyncTask<String, Void, Boolean> task = new AsyncTask<String, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(String... params) {
                try {
                    mTwitter.updateStatus("人生" + preferences.getInt(NKANAPI, 0) + "度目の乾杯！！！！ @ " + dTime.toString());
                    return true;
                } catch (TwitterException e) {
                    e.printStackTrace();
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    showToast("ツイートが完了しました！");
                    //finish();
                } else {
                    showToast("ツイートに失敗しました。。。");
                }
            }
        };
        task.execute("aa");
    }

    private void showToast(String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }
}
