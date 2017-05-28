package com.example.wally_nagama.paripigrass;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

import twitter4j.Trend;

import static com.example.wally_nagama.paripigrass.ReConnectBluetooth.VIEW_STATUS;

/**
 * Created by a on 2017/05/27.
 */

public class Roulette {
    User user;
    DatabaseReference dr;
    int count;
    int now_color;
    OutputStream  mmOutputStream;
    Handler  mHandler;


    public Roulette(User user, DatabaseReference dr, OutputStream mmOutputStream, Handler mHandler){
        this.user = user;
        this.dr = dr;
        this.mmOutputStream = mmOutputStream;
        this.mHandler = mHandler;
    }

    public void start(){
        Random rnd = new Random();
        dr.child("Roulette").child("color").setValue(user.now_color);
        dr.child("Roulette").child("light_now").child("count").setValue(rnd.nextInt(5) + 12);
        dr.child("Roulette").child("light_now").child("id").setValue(user.nextUserId);
    }

    public void ichimi(DataSnapshot dataSnapshot){

        if (dataSnapshot.getKey().equals("Roulette")) {
            if (dataSnapshot.hasChild("color")) {
                now_color = dataSnapshot.child("color").getValue(int.class);
            }

            if (dataSnapshot.child("light_now").child("id").getValue() == null) {return;}
            if (dataSnapshot.child("light_now").child("id").getValue(int.class) == user.userId){
                count = dataSnapshot.child("light_now").child("count").getValue(int.class);
//                                    TODO::ピカピカ〜
                Log.d("roulette", "59 led on!!");
                sendBtCommand(color2string(now_color));
//                                    countが0やったら終了
                    if (count > 0) {
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                sendBtCommand("ledoff");
                                dr.child("Roulette").child("light_now").runTransaction(new Transaction.Handler() {
                                    @Override
                                    public Transaction.Result doTransaction(MutableData mutableData) {
                                        mutableData.child("id").setValue(user.nextUserId);
                                        return Transaction.success(mutableData);
                                    }

                                    @Override
                                    public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
        //                                                      TODO   LED消す処理
                                        Log.d("roulette", "75 led off!!!");
                                        //sendBtCommand("ledoff");
                                    }
                                });
                                dr.child("Roulette").child("light_now").child("count").setValue(count - 1);
                            }
                        },500 );
                    } else {
        //                                        TODO::LED色変える
                        Log.d("roulette", "84 led on!!");
                        sendBtCommand(color2string((now_color+1)%8+1));

                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
        //                                        Roulette削除
                                dr.child("Roulette").removeValue();
                            }
                        }, 5000);
                    }
                }
           }
        }
    private void sendBtCommand(String str){
        try{
            mmOutputStream.write(str.getBytes());
        } catch (IOException e) {
            Message valueMsg1 = new Message();
            valueMsg1.what = VIEW_STATUS;
            valueMsg1.obj = "Error3:" + e;
            mHandler.sendMessage(valueMsg1);
        }
    }

    public String color2string(int color) {
        String str;
        switch (color) {
            case 1:
                str = "red";
                break;
            case 2:
                str = "blue";
                break;
            case 3:
                str = "green";
                break;
            case 4:
                str = "purple";
                break;
            case 5:
                str = "yellow";
                break;
            case 6:
                str = "lightblue";
                break;
            case 7:
                str = "pink";
                break;
            case 8:
                str = "orange";
                break;
            default:
                str = "ranbow";
        }
        return str;
    }
}
