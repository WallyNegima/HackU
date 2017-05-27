package com.example.wally_nagama.paripigrass;

import com.google.firebase.database.DatabaseReference;

import java.util.Random;

/**
 * Created by a on 2017/05/27.
 */

public class Roulette {
    User user;
    DatabaseReference dr;

    public Roulette(User user, DatabaseReference dr){
        this.user = user;
        this.dr = dr;
    }

    public void start(){
        Random rnd = new Random();
        dr.child("Roulette").child("light_now").setValue(1);
        dr.child("Roulette").child("count").setValue(rnd.nextInt(5) + 10);
        dr.child("Roulette").child("color").setValue(user.now_color);
    }


}
