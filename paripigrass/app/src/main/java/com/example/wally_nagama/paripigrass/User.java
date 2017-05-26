package com.example.wally_nagama.paripigrass;

/**
 * Created by wally_nagama on 2017/05/22.
 */

public class User {
    public String userName;
    public String userKey;
    public int userId;
    public int nextUserId;
    public String sensorValue;
    public boolean joined = false;
    public boolean kanpai_gotNewColor = false; //falseなら乾杯でリッスン中，trueなら新しい色をもらった後
    public int now_color = 0;

    //コンストラクタ
    User(){}

}
