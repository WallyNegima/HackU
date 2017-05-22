package com.example.wally_nagama.paripigrass;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference("message");

    // 音声認識で使うよーんwwwww
    private TextView txvAction;
    private TextView txvRec;
    private static final int REQUEST_CODE = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myRef.setValue("Hello, World!");

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                String value = dataSnapshot.getValue(String.class);
                Log.d("a", "Value is: " + value);
            }

            @Override public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w("a", "Failed to read value.", error.toException());}
        });




                // へーへーボタン（同意を表す貴重なボタン）
                txvAction=(TextView) findViewById(R.id.amin_txvAction);

                txvRec=(TextView) findViewById(R.id.txv_recog);

                //へーへーボタンリスナー
        findViewById(R.id.amin_heybutton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v){
                txvAction.setText(R.string.amin_heybutton);
            }
        });


        // 音声認識リスナー
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

        };

    // startActivityForResultで起動したアクティビティが終了した時に呼び出される関数
    // checkchara.resultRecで参照できる
    CheckCharacter checkchara = new CheckCharacter();

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // 音声認識結果の時
        if(requestCode == REQUEST_CODE && resultCode == RESULT_OK) {

            // 結果文字列リストを取得
            ArrayList<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);

            /* ---1
            // 取得した文字列を結合
            String resultsString = "";
            for(int i = 0; i < results.size(); i++) {
                resultsString += results.get(i)+";";
                */


            if(results.size() > 0) {
                // 認識結果候補で一番有力なものを表示
                txvRec.setText( results.get(0));

            }

            //トーストで結果表示
            //Toast.makeText(this, resultsString, Toast.LENGTH_LONG).show();

            // TextView
            //txvRec.setText(resultsString);

        }

        //super.onActivityResult(requestCode, resultCode, data);    ---1
    }
}
