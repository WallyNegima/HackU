package com.example.wally_nagama.paripigrass;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.support.annotation.*;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;

import org.w3c.dom.Comment;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    User user;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();
    Button button, roomCreateButton, startRoulette;
    EditText editText, userName, roomNumber;
    Context act = this;
    ChildEventListener childEventListener;
    ChildEventListener childEventListener_now_light;
    String key;
    int color;
    TextView test_tv;


    // 音声認識で使うよーんwwwwww
    private TextView txvAction;
    private TextView txvRec;
    private static final int REQUEST_CODE = 0;
    public Context context;
    public CheckResult checkResult;
    private String result_voce;
    TextView txv_roulette;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        context = this;
        button = (Button)findViewById(R.id.button);
        roomCreateButton = (Button)findViewById(R.id.userCreate);
        startRoulette = (Button)findViewById(R.id.amin_roulette);
        editText = (EditText)findViewById(R.id.edittext);
        userName = (EditText)findViewById(R.id.userName);
        roomNumber = (EditText)findViewById(R.id.roomNumber);
        test_tv = (TextView)findViewById(R.id.test_tv);
        txv_roulette = (TextView)findViewById(R.id.txv_roulette);

        user = new User();

        //user追加
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                user.now_color = Integer.valueOf(editText.getText().toString());
                myRef.child("prost_now").child(key).child("now_color").setValue(user.now_color);
                myRef.child("prost_now").child(key).child("next_color").setValue(user.now_color);
                final ChildEventListener ev = new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
//                        自分が一番乗りのとき
//                        next_colorは一回だけ変える
                        if(dataSnapshot.getKey() == key){
                            return;
                        }
                        Log.d("prost", "onChildAdded:" + dataSnapshot);
                        if (dataSnapshot.child("next_color") == null) {
                            return;
                        }
//                      自身のnext_colorをdataSnap.Child("now_color")に変更
                        color = dataSnapshot.child("next_color").getValue(int.class);
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {}

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {}

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {}

                    @Override
                    public void onCancelled(DatabaseError databaseError) {}
                };
                myRef.child("prost_now").addChildEventListener(ev);
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
//                        Lisnerの解除
//                        next_colorを書き換え
//                        dbの削除
                        myRef.child("prost_now").removeEventListener(ev);
                        user.now_color = color;
                        Log.d("prost","onChildtest"+color);
                        myRef.child("prost_now").child(key).removeValue();
                        test_tv.setText(""+color);
                    }
                }, 500);

            }
        });

        //roomを作成する
        roomCreateButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){

                String roomId = roomNumber.getText().toString();
                Room room = new Room(roomId);
                myRef = database.getReference("room" + roomId);

                //ユーザーのリストなどを見張る
                childEventListener = new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                        Log.d("a", "onChildAdded:" + dataSnapshot);
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                        Log.d("a", "onChildChanged:" + dataSnapshot);

                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {
                        Log.d("a", "onChildRemoved:" + dataSnapshot);

                        if(dataSnapshot.child("userID").getValue() == null){
                            return;
                        }

                        int id = dataSnapshot.child("userID").getValue(int.class);
                        if(id < user.userId){
                            user.userId --;
                            Map<String, Object> childUpdates = new HashMap<>();
                            childUpdates.put("/"+ key + "/userID", user.userId );
                            myRef.updateChildren(childUpdates);
                        }
                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                        Log.d("a", "onChildMoved:" + dataSnapshot.getKey());
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w("a", "postComments:onCancelled", databaseError.toException());
                        Toast.makeText(act, "Failed to load comments.",
                                Toast.LENGTH_SHORT).show();
                    }
                };

                if(user.joined){
                    //すでに部屋に入っているので何もしない
                }else{
                    myRef.addChildEventListener(childEventListener);
                    user.joined = true;
                    key = myRef.push().getKey();
                    registUserID(key,myRef,user);
                    user.userName = userName.getText().toString();
                    user.userKey = key;
                    myRef.child(key).child("userName").setValue(user.userName);
                    myRef.child(key).child("is_kanpai").setValue(false);
                }
            }
        });


        txvAction = (TextView) findViewById(R.id.amin_txvAction);
        txvRec = (TextView) findViewById(R.id.txv_recog);



        /*---     音声認識リスナー   ----*/
        findViewById(R.id.amin_recog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    // 音声認識プロンプトを立ち上げるインテント作成
                    Intent intent = new Intent(
                            RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

                    // 言語モデル： free-form speech recognition
                    // web search terms用のLANGUAGE_MODEL_WEB_SEARCHにすると検索画面
                    intent.putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

                    // プロンプトに表示する文字を設定
                    intent.putExtra(
                            RecognizerIntent.EXTRA_PROMPT,
                            "話せや");

                    // インテント発行
                    startActivityForResult(intent, REQUEST_CODE);
                } catch (ActivityNotFoundException e) {
                    // エラー表示
                    Toast.makeText(MainActivity.this,
                            "ActivityNotFoundException", Toast.LENGTH_LONG).show();
                }
            }
        });


//---------------------------------------------------------　　　　　　ルーレット
         findViewById(R.id.amin_roulette).setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {

                 childEventListener_now_light = new ChildEventListener() {
                     @Override
                     public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                         txv_roulette.setText("追加されました");

                     }

                     @Override
                     public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                         myRef.child("light_now").setValue(user.now_color);


                         new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                             @Override
                             public void run() {
                                 txv_roulette.setText("変更されました");
                             }
                         }, 2000);
                     }

                     @Override
                     public void onChildRemoved(DataSnapshot dataSnapshot) {}

                     @Override
                     public void onChildMoved(DataSnapshot dataSnapshot, String s) {}

                     @Override
                     public void onCancelled(DatabaseError databaseError) {}
                 };
                 myRef.child("light_now").addChildEventListener(childEventListener_now_light);

             }
         });



    }

    ;


    /*---       startActivityForResultで起動したアクティビティが終了した時に呼び出される関数   ---*/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // 音声認識結果の時
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {

            // 結果文字列リストを取得
            ArrayList<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);

            if (results.size() > 0) {
                // 認識結果候補で一番有力なものを表示
                txvRec.setText(results.get(0));
                // checkCharacterに値を渡す
                //checkResult.resultRec = results.get(0);
                result_voce = results.get(0);

                /*
                /*---    この下に結果処理を一応描いてみる   ---*/
                switch (result_voce) {
                /*---   乾杯   ---*/
                    case "乾杯します":
                        Toast.makeText(context, "乾杯！！", Toast.LENGTH_LONG).show();
                        break;
                    case "乾杯":
                        Toast.makeText(context, "乾杯！！", Toast.LENGTH_LONG).show();
                        break;

                /*---   ルーレット   */
                    case "ルーレットモード":
                        Toast.makeText(context, "ルーレットモード", Toast.LENGTH_LONG).show();
                        break;

                    case "ルーレット":
                        Toast.makeText(context, "ルーレットモード", Toast.LENGTH_LONG).show();
                        break;


                /*---   司会者   ---*/
                    case "司会者になりました":
                        Toast.makeText(context, "司会者になりました", Toast.LENGTH_LONG).show();
                        break;
                    case "司会者":
                        Toast.makeText(context, "司会者になりました", Toast.LENGTH_LONG).show();
                        break;

                /*---   一気飲み   ---*/
                    case "一気飲み":
                        Toast.makeText(context, "一気飲み", Toast.LENGTH_LONG).show();
                        break;
                    case "一気飲みします":
                        Toast.makeText(context, "一気飲み", Toast.LENGTH_LONG).show();
                        break;



                }

            }

            //super.onActivityResult(requestCode, resultCode, data);    ---1

            // 認識後
            //checkResult.returnCharacter();

        }


    }

    private void registUserID(final String key, final DatabaseReference databaseReference, final User user){
        final Query query = databaseReference.orderByChild("userID").limitToLast(1);
        query.addChildEventListener(new ChildEventListener(){
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                int id;
                if(dataSnapshot.child("userID").getValue(int.class) == null){
                    //誰もおらんとき
                    id = 0;
                }else{
                    id = dataSnapshot.child("userID").getValue(int.class)+1;
                }
                databaseReference.child(key).child("userID").setValue(id);
                user.userId = id;
                Log.d("registID", "onChildAdded:" + dataSnapshot);
                query.removeEventListener(this);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {}

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });




    }

    private void roulette() {

        childEventListener_now_light = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                txv_roulette.setText("追加されました");

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                myRef.child("light_now").child(key).child("color").setValue(user.now_color);

                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //txv_roulette.setText("ルーレットを開始します");

                        txv_roulette.setText("変更されました");
                    }
                }, 2000);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        myRef.child("light_now").addChildEventListener(childEventListener_now_light);
    }
}
