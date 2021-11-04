package com.topelec.smarthome;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.topelec.database.DatabaseHelper;

import it.moondroid.coverflowdemo.R;

public class OpenCardActivity extends Activity {

    private final static String TAG = ".OpenCardActivity";
    private TextView idView;
    private EditText rechargeText;
    private ImageButton btnAuthor;
    private ImageButton btnCancelAuthor;
    private Button btnReturn;

    private String currentId = new String();

//    /***接收Group发送来的广播数据，同步更新UI***/
//    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            int what = intent.getExtras().getInt("what");
//            switch (what) {
//                case 1://初始化错误
//                    //TODO:
////                    statusView.setImageDrawable(getResources().getDrawable(R.drawable.buscard_symbol_wrong));
//                    idView.setText(intent.getExtras().getString("Result"));
//                    currentId = null;
//                    oldId = null;
//                    break;
//                case 2://未检测到卡
//                    idView.setText(getResources().getString(R.string.buscard_not_check_card));
////                    statusView.setImageDrawable(getResources().getDrawable(R.drawable.buscard_recharge_standby));
//                    sumView.setText("");
//                    rechargeText.setText("");
//                    currentId = null;
//                    oldId = null;
//                    break;
//                case 3: //成功获取卡号
//                    currentId = intent.getExtras().getString("Result");
//                    idView.setText(currentId);
//                    if (currentId == null) {
//                        idView.setText("");
////                        statusView.setImageDrawable(getResources().getDrawable(R.drawable.standby));
//                    }else {
//                       // if (!currentId.equals(oldId)) { //检测到不同的卡
//                            //TODO:查询数据库，存在：succeed；不存在：未授权
//
//                            updateCardUI(currentId);
//                            oldId = currentId;
//                            Log.v(TAG,"Result = "+ currentId+"");
//                    //    } else {
//                            //TODO:相同的卡，不做处理
//                    //    }
//
//                    }
//
//                    break;
//                default:
//                    break;
//            }
//
//
//        }
//    };

    /**数据库相关**/
    Context mContext;
    DatabaseHelper mDatabaseHelper;
    SQLiteDatabase mDatabase;

    private final static String TABLE_NAME = "HFCard";
    private final static String ID = "_id";
    private final static String CARD_ID = "card_id";
    private final static String SUM = "sum";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smartkiller_recharge);
        /**数据库相关变量初始化**/
        mContext = this;
        mContentView = findViewById(R.id.smart_layout);

        mDatabaseHelper = DatabaseHelper.getInstance(mContext);
        mDatabase = mDatabaseHelper.getReadableDatabase();



        idView = (TextView) findViewById(R.id.idView);
        rechargeText = (EditText) findViewById(R.id.rechargeText);

        // 在这里填入需要返回的内容
        btnReturn = (Button) findViewById(R.id.btn_back);
        btnReturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent returnInte = new Intent();
                returnInte.putExtra("card","卡号");
                setResult(RESULT_OK, returnInte);
                finish();
            }
        });

        btnAuthor = (ImageButton) findViewById(R.id.btn_author);
        btnAuthor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO:判断没有，则创建一条数据：statusView中显示状态；若存在，则直接在statusView中显示状态
                if (currentId == null || currentId.length() == 0) {
                    idView.setText(getResources().getString(R.string.buscard_not_check_card));
                    return;
                }
                String result = searchHFCard(CARD_ID,currentId);
                if ( result == null ) {
                    //TODO:插入新行
                    if (insertHFCard(CARD_ID,currentId) != -1) {
                        idView.setText(getResources().getString(R.string.buscard_author_succeed));
                        updateCardUI(currentId);
                    } else {
                        idView.setText(getResources().getString(R.string.buscard_author_fail));
                    };
                } else if ( result.equals("-1")) {
                    //TODO:查询到多行，错误

                } else {
                    //TODO:本卡已授权
                    idView.setText(R.string.buscard_authored_already);
                }

            }
        });

        btnCancelAuthor = (ImageButton)findViewById(R.id.btn_cancel_author);
        btnCancelAuthor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO:跳出提示框，同意直接删除table中对应的记录条目
                if (currentId == null || currentId.length() == 0) {
                    return;
                }
                Log.v(TAG,"button cancel clicked");
                AlertDialog.Builder builder = new AlertDialog.Builder(OpenCardActivity.this);
                builder.setTitle(getResources().getString(R.string.buscard_if_cancel_item));
                builder.setPositiveButton(getResources().getString(R.string.buscard_OK), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //TODO:删除记录
                        if (deleteHFCard(CARD_ID,currentId) != 0) {
                            idView.setText(getResources().getString(R.string.buscard_cancel_author_succeed));
                            idView.setText("");

                        }else {
                            idView.setText(getResources().getString(R.string.buscard_cancel_author_fail));
                            idView.setText("");
                        }
                    }
                });
                builder.setNegativeButton(getResources().getString(R.string.buscard_CANCEL), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //TODO:这里添加点击确定后的逻辑

                    }
                });
                builder.create().show();
            }
        });



    }

    /**
     *
     * @param CardId 卡号
     */
    private void updateCardUI(String CardId) {
        String searchResult = searchHFCard(CARD_ID,CardId);
        if (searchResult == null || searchResult.length() <= 0) { //如果数据库中没有记录
            idView.setText(getResources().getString(R.string.buscard_please_author_first));
        } else if (searchResult.equals("-1")) {  //返回值为-1，数据库中搜索不止一个记录，错误
            idView.setText(getResources().getString(R.string.buscard_search_more_than_one));
        } else {  //返回金额，更新UI
            idView.setText(CardId);
        }
    }
    /**
     * 查询一条记录
     * @param key
     * @param selectionArgs
     * @return
     */
    private String searchHFCard(String key,String selectionArgs) {
        Cursor cursor = mDatabase.query(TABLE_NAME, new String[]{SUM}, key + "=?", new String[] {selectionArgs}, null, null,null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        if (cursor.getCount() == 1) {
            double sum = cursor.getDouble(0);
            cursor.close();
            return Double.toString(sum);
        }else if (cursor.getCount() == 0) {
            cursor.close();
            return null;
        } else {
            for (int i = 0;i <cursor.getCount();i++)
            {
                Log.v(TAG,"Current cursor = "+Double.toString(cursor.getDouble(0)));
                cursor.moveToNext();
            }
            cursor.close();
            return "-1";
        }
    }

    /**
     * 插入一条记录
     * @param key   需要插入的列名称
     * @param data  对应列赋值
     * @return the row ID of the newly inserted row, or -1 if an error occurred
     */
    private long insertHFCard(String key,String data) {
        ContentValues values = new ContentValues();
        values.put(key,data);
        return mDatabase.insert(TABLE_NAME,null,values);
    }

    /**
     * 删除一条记录
     * @param key
     * @param data
     * @return 返回所删除的行数，否则返回0。
     */
    private int deleteHFCard(String key, String data) {
        return mDatabase.delete(TABLE_NAME,key + "=?", new String[] {data});
    }

    /**
     * 更新一条记录
     * @param key
     * @param data
     * @return 返回充值后的金额金额字符串，错误返回null
     */
    private String updateHFCard(String key, String data,String Column, String value) {
        ContentValues values = new ContentValues();
        String oldSum = searchHFCard(key,data);
        if (oldSum != null && !oldSum.equals("-1")) {
            double sum = Double.valueOf(oldSum) + Double.valueOf(value);
            values.put(Column, sum);
            int result =  mDatabase.update(TABLE_NAME, values, key + "=?",new String[]{data});
            if (result != 0) {
                return Double.toString(sum);
            }
        }

        return null;
    }

    private static final boolean AUTO_HIDE = true;
    private static final int UI_ANIMATION_DELAY = 300;
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    private final Handler mHideHandler = new Handler();

    //UI全屏显示
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };


    @Override
    protected void onStart() {
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        super.onStart();
        /**用于接收group发送过来的广播**/
//        IntentFilter filter = new IntentFilter(CardActivityGroup.recharge_action);
//        registerReceiver(mBroadcastReceiver,filter);
    }
    @Override
    protected void onResume() {
        super.onResume();
        updateCardUI(currentId);
    }

    @Override
    protected void onStop() {
        super.onStop();
//        unregisterReceiver(mBroadcastReceiver);
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
////        getMenuInflater().inflate(R.menu.menu_recharge, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }
}
