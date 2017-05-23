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

public class MainActivity extends AppCompatActivity {
    User user;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();
    Button button, roomCreateButton;
    EditText editText, userName, roomNumber;
    int userNum;
    Context act = this;
    ChildEventListener childEventListener;
    String key;

    // 音声認識で使うよーんwwwwww
    private TextView txvAction;
    private TextView txvRec;
    private static final int REQUEST_CODE = 0;
    public Context context;
    public CheckResult checkResult;
    private String result_voce;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        context = this;
        button = (Button)findViewById(R.id.button);
        roomCreateButton = (Button)findViewById(R.id.userCreate);
        editText = (EditText)findViewById(R.id.edittext);
        userName = (EditText)findViewById(R.id.userName);
        roomNumber = (EditText)findViewById(R.id.roomNumber);
        userNum = 0;

        user = new User();

        //user追加
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                myRef.child("user").setValue(editText.getText().toString());
            }
        });

        //roomを作成する
        roomCreateButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){

                String roomId = roomNumber.getText().toString();
                Room room = new Room(roomId);
                myRef = database.getReference("room" + roomId);
                userNum = 0;

                //roomのuser数を見て人数を数える
                //ユーザーのリストなどを見張る
                childEventListener = new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                        Log.d("a", "onChildAdded:" + dataSnapshot);

                        // A new comment has been added, add it to the displayed list
                        userNum+=1;
                        // ...
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                        Log.d("a", "onChildChanged:" + dataSnapshot.getKey());

                        // A comment has changed, use the key to determine if we are displaying this
                        // comment and if so displayed the changed comment.
                        //Comment newComment = dataSnapshot.getValue(Comment.class);
                        //String commentKey = dataSnapshot.getKey();
                        // ...
                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {
                        Log.d("a", "onChildRemoved:" + dataSnapshot.getKey());

                        // A comment has changed, use the key to determine if we are displaying this
                        // comment and if so remove it.
                        String commentKey = dataSnapshot.getKey();

                        // ...
                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                        Log.d("a", "onChildMoved:" + dataSnapshot.getKey());

                        // A comment has changed position, use the key to determine if we are
                        // displaying this comment and if so move it.
                        Comment movedComment = dataSnapshot.getValue(Comment.class);
                        String commentKey = dataSnapshot.getKey();

                        // ...
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
                    //user.joined = true;
                    key = myRef.push().getKey();
                    user.userName = userName.getText().toString();
                    user.userKey = key;
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // ここに1秒後に実行したい処理
                            Log.d("Firebase", String.format("usernum:%d", userNum));
                            myRef.child(key).child("userID").setValue(String.valueOf(userNum));
                            myRef.child(key).child("userName").setValue(user.userName);
                            user.userId = String.valueOf(userNum);
                        }
                    }, 500);
                }


            }
        });


        txvAction = (TextView) findViewById(R.id.amin_txvAction);
        txvRec = (TextView) findViewById(R.id.txv_recog);

        /*---    へーへーボタンリスナー ---*/
        findViewById(R.id.amin_heybutton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txvAction.setText(R.string.amin_heybutton);
                //Toast.makeText(context, "乾杯", Toast.LENGTH_SHORT).show();
            }
        });




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
                        Toast.makeText(context, R.string.amin_rouletteMode, Toast.LENGTH_LONG).show();
                        Intent intent3 = new Intent(this, Roulette.class);
                        startActivity(intent3);
                        break;

                    case "ルーレット":
                        Toast.makeText(context, R.string.amin_rouletteMode, Toast.LENGTH_LONG).show();
                        // ルーレットへのインテント
                        Intent intent2 = new Intent(this, Roulette.class);
                        startActivity(intent2);
                        break;


                /*---   司会者   ---*/
                    case "司会者になりました":
                        Toast.makeText(context, R.string.amin_speech, Toast.LENGTH_LONG).show();
                        break;
                    case "司会者":
                        Toast.makeText(context, R.string.amin_speech, Toast.LENGTH_LONG).show();
                        break;

                /*---   一気飲み   ---*/
                    case "一気飲み":
                        Toast.makeText(context, R.string.amin_ikkinomi, Toast.LENGTH_LONG).show();
                        break;
                    case "一気飲みします":
                        Toast.makeText(context, R.string.amin_ikkinomi, Toast.LENGTH_LONG).show();
                        break;



                }

            }

            //super.onActivityResult(requestCode, resultCode, data);    ---1

            // 認識後
            //checkResult.returnCharacter();

        }


    }
}
