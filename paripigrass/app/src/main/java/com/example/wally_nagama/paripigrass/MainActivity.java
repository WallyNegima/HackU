package com.example.wally_nagama.paripigrass;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.support.annotation.*;
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

import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

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
                        Toast.makeText(this, "乾杯！！", Toast.LENGTH_LONG).show();
                        break;
                    case "乾杯":
                        Toast.makeText(this, "乾杯！！", Toast.LENGTH_LONG).show();
                        break;

                /*---   ルーレット   */
                    case "ルーレットモード":
                        Toast.makeText(this, R.string.amin_rouletteMode, Toast.LENGTH_LONG).show();
                        break;

                    case "ルーレット":
                        Toast.makeText(this, R.string.amin_rouletteMode, Toast.LENGTH_LONG).show();
                        break;


                /*---   司会者   ---*/
                    case "司会者になりました":
                        Toast.makeText(this, R.string.amin_speech, Toast.LENGTH_LONG).show();
                        break;
                    case "司会者":
                        Toast.makeText(this, R.string.amin_speech, Toast.LENGTH_LONG).show();
                        break;

                /*---   一気飲み   ---*/
                    case "一気飲み":
                        Toast.makeText(this, R.string.amin_ikkinomi, Toast.LENGTH_LONG).show();
                        break;
                    case "一気飲みします":
                        Toast.makeText(this, R.string.amin_ikkinomi, Toast.LENGTH_LONG).show();
                        break;


                }

            }

            //super.onActivityResult(requestCode, resultCode, data);    ---1

            // 認識後
            //checkResult.returnCharacter();

        }


    }
}
