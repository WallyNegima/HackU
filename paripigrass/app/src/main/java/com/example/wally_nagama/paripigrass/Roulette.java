package com.example.wally_nagama.paripigrass;

import android.os.Handler;
import android.os.Looper;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import java.util.Random;

/**
 * Created by a on 2017/05/27.
 */

public class Roulette {
    User user;
    DatabaseReference dr;
    int count;
    int now_color;

    public Roulette(User user, DatabaseReference dr){
        this.user = user;
        this.dr = dr;
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
//                                    countが0やったら終了
                    if (count > 0) {
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                dr.child("Roulette").child("light_now").runTransaction(new Transaction.Handler() {
                                    @Override
                                    public Transaction.Result doTransaction(MutableData mutableData) {
                                        mutableData.child("id").setValue(user.nextUserId);
                                        return Transaction.success(mutableData);
                                    }

                                    @Override
                                    public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
        //                                                      TODO   LED消す処理

                                    }
                                });
                                dr.child("Roulette").child("light_now").child("count").setValue(count - 1);
                            }
                        }, 100);
                    } else {
        //                                        TODO::LED色変える
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




}
