package com.example.sweetdream;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.SQLException;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import com.google.android.material.appbar.AppBarLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.tts.auth.AuthInfo;
import com.baidu.tts.client.SpeechError;
import com.baidu.tts.client.SpeechSynthesizer;
import com.baidu.tts.client.SpeechSynthesizerListener;
import com.baidu.tts.client.TtsMode;
import com.example.sweetdream.base.BaseActivity;
import com.example.sweetdream.db.BookList;
import com.example.sweetdream.db.BookMarks;
//import com.example.sweetdream.dialog.PageModeDialog;
//import com.example.sweetdream.dialog.SettingDialog;
//import com.example.sweetdream.util.BrightnessUtil;
import com.example.sweetdream.util.PageFactory;
import com.example.sweetdream.view.PageWidget;
import com.water.amraudiorecorder.AMRAudioRecorder;

import org.litepal.crud.DataSupport;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.Bind;


public class ReadActivity extends BaseActivity {
    private static final String TAG = "ReadActivity";
    private final static String EXTRA_BOOK = "bookList";
    private final static int MESSAGE_CHANGEPROGRESS = 1;

    @Bind(R.id.bookpage)
    PageWidget bookpage;
    @Bind(R.id.tv_progress)
    TextView tv_progress;
    @Bind(R.id.rl_progress)
    RelativeLayout rl_progress;

    @Bind(R.id.bookpop_bottom)
    LinearLayout bookpop_bottom;
    @Bind(R.id.rl_bottom)
    RelativeLayout rl_bottom;
    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.appbar)
    AppBarLayout appbar;

    /**
     * new code for record
     */
    ImageButton recordControl;
    ImageButton recordFinish;
    ImageButton displayRecord;
    int recordStatus = 0; //0: notstarted, 1:recording, 2:paused
    int displayStatus = 0;//0: notdisplay, 1:displaying
    boolean recordExist = false;
    String recordPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/sweetdreamrecords/";
    String bookPath = "";

    MediaPlayer recordPlayer;
    AMRAudioRecorder recorder;
    SharedPreferences bookRecordMap;

    //end new code for record

    private Config config;
    private WindowManager.LayoutParams lp;
    private BookList bookList;
    private PageFactory pageFactory;
    private int screenWidth, screenHeight;
    //
    private Boolean isShow = false;
//    private SettingDialog mSettingDialog;
//    private PageModeDialog mPageModeDialog;
    private Boolean mDayOrNight;

    private boolean isSpeaking = false;

    /**
     * new code for timer
     */

    private TimerTask timerTask;
    int alartTime = 61;
    private Timer timer = new Timer();
    Vibrator vibrator;

    //
    private BroadcastReceiver myReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
                Log.e(TAG,Intent.ACTION_BATTERY_CHANGED);
                int level = intent.getIntExtra("level", 0);
                pageFactory.updateBattery(level);
            }else if (intent.getAction().equals(Intent.ACTION_TIME_TICK)){
                Log.e(TAG,Intent.ACTION_TIME_TICK);
                pageFactory.updateTime();
            }
        }
    };

    @Override
    public int getLayoutRes() {
        return R.layout.activity_read;
    }

    @Override
    protected void initData() {
        if(Build.VERSION.SDK_INT >= 14 && Build.VERSION.SDK_INT < 19){
            bookpage.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.mipmap.return_button);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        config = Config.getInstance();
        pageFactory = PageFactory.getInstance();

        IntentFilter mfilter = new IntentFilter();
        mfilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        mfilter.addAction(Intent.ACTION_TIME_TICK);
        registerReceiver(myReceiver, mfilter);

//        mSettingDialog = new SettingDialog(this);
//        mPageModeDialog = new PageModeDialog(this);

        WindowManager manage = getWindowManager();
        Display display = manage.getDefaultDisplay();
        Point displaysize = new Point();
        display.getSize(displaysize);
        screenWidth = displaysize.x;
        screenHeight = displaysize.y;

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        hideSystemUI();

        Intent intent = getIntent();
        bookList = (BookList) intent.getSerializableExtra(EXTRA_BOOK);

        bookpage.setPageMode(config.getPageMode());
        pageFactory.setPageWidget(bookpage);

        try {
            pageFactory.openBook(bookList);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "open failed", Toast.LENGTH_SHORT).show();
        }

        //initDayOrNight();

    }

    @Override
    protected void initListener() {
        pageFactory.setPageEvent(new PageFactory.PageEvent() {
            @Override
            public void changeProgress(float progress) {
                Message message = new Message();
                message.what = MESSAGE_CHANGEPROGRESS;
                message.obj = progress;
                mHandler.sendMessage(message);
            }
        });

        bookpage.setTouchListener(new PageWidget.TouchListener() {


            @Override
            public void center() {
                if (isShow) {
                    hideReadSetting();
                } else {
                    showReadSetting();
                }
            }

            @Override
            public Boolean prePage() {
                if (isShow || isSpeaking){
                    return false;
                }

                pageFactory.prePage();
                if (pageFactory.isfirstPage()) {
                    return false;
                }

                return true;
            }

            @Override
            public Boolean nextPage() {
                Log.e("setTouchListener", "nextPage");
                if (isShow || isSpeaking){
                    return false;
                }

                pageFactory.nextPage();
                if (pageFactory.islastPage()) {
                    return false;
                }
                return true;
            }

            @Override
            public void cancel() {
                pageFactory.cancelPage();
            }

            @Override
             public void setTimer(){
                timerTask.cancel();
                alartTime = 61;
                timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        mHandler.sendEmptyMessage(1);
                    }
                };
                timer.schedule(timerTask, 0, 1000);
            }

        });

    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            alartTime--;
            if(alartTime==0){
                Log.d("vibrator starts working","wuwuwuwu!!!!!");
                vibrator.vibrate(5000);
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.recordControl = (ImageButton)findViewById(R.id.record_btn);
        this.recordFinish = (ImageButton)findViewById(R.id.stop_record_btn);
        this.displayRecord = (ImageButton)findViewById(R.id.display_record);
        this.recordControl.setImageResource(R.drawable.record_record);
        this.recordFinish.setVisibility(View.INVISIBLE);

        this.recordPlayer = new MediaPlayer();

        File dir = new File(this.recordPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        this.recorder = new AMRAudioRecorder(this.recordPath);

        this.bookRecordMap = getSharedPreferences("book_record_map", Context.MODE_PRIVATE);

        String recordPath = this.bookRecordMap.getString(bookList.getBookpath(), "");

        try {
            if(recordPath.isEmpty()){
                this.recordExist = false;
                this.displayRecord.setVisibility(View.INVISIBLE);
            }
            else{
                this.recordExist = true;
                this.recordPlayer.setDataSource(recordPath);
                this.recordPlayer.setVolume(0.5f, 0.5f);
                this.recordPlayer.setLooping(false);
                this.recordPlayer.prepare();
            }
        }catch (Exception e){
            Log.d("recordPlayer", e.toString());
        }
        /**
         * new code for timer
         */

        timerTask = new TimerTask() {
            @Override
            public void run() {
                mHandler.sendEmptyMessage(0);
            }
        };
        timer.schedule(timerTask, 0, 1000);
        vibrator=(Vibrator)getSystemService(Service.VIBRATOR_SERVICE);
        // end

        if (Build.VERSION.SDK_INT >= 23) {
            if (!(getApplicationContext().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    && getApplicationContext().checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
            ) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        1);
            }
        }

        this.displayRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //display
                if(displayStatus == 0){
                    if(recordExist) {
                        recordPlayer.start();
                        displayRecord.setImageResource(R.drawable.display_pause);
                        displayStatus = 1;
                    }
                }
                else{
                    recordPlayer.pause();
                    displayStatus = 0;
                    displayRecord.setImageResource(R.drawable.display_play);
                }
            }
        });

        this.recordFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recordStatus = 0;

                AlertDialog.Builder builder = new AlertDialog.Builder(ReadActivity.this);
                builder.setMessage("Do you want to save your reading record? ");
                builder.setPositiveButton("save",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //save file
                                recordFinish.setVisibility(View.INVISIBLE);
                                recordControl.setImageResource(R.drawable.record_record);
                                recorder.stop();
                                String recordFilePath = recorder.getAudioFilePath();
                                final SharedPreferences.Editor editor = bookRecordMap.edit();
                                editor.putString(bookList.getBookpath(), recordFilePath);
                                editor.apply();
                                try {
                                    recordPlayer = new MediaPlayer();
                                    recordPlayer.setDataSource(recordFilePath);
                                    recordPlayer.setVolume(0.5f, 0.5f);
                                    recordPlayer.setLooping(false);
                                    recordPlayer.prepare();
                                }catch (Exception e){
                                    Log.d("finish record", e.toString());
                                }
                            }
                        });
                builder.setNegativeButton("cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                recordFinish.setVisibility(View.INVISIBLE);
                                recordControl.setImageResource(R.drawable.record_record);
                                recorder.clear();
                            }
                        });
                final Dialog dialog = builder.create();
                dialog.show();
            }
        });

        this.recordControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(recordStatus == 0){
                    recorder.start();
                    recordControl.setImageResource(R.drawable.record_pause);
                    recordStatus = 1;
                    recordFinish.setVisibility(View.VISIBLE);
                }
                else if(recordStatus == 1){
                    recorder.pause();
                    recordControl.setImageResource(R.drawable.record_continue);
                    recordStatus = 2;
                }
                else{
                    recorder.resume();
                    recordControl.setImageResource(R.drawable.record_pause);
                    recordStatus = 1;
                }
            }
        });
    }

    @Override
    protected void onResume(){
        super.onResume();
        timerTask.cancel();
        alartTime = 61;
        timerTask = new TimerTask() {
            @Override
            public void run() {
                mHandler.sendEmptyMessage(1);
            }
        };
        timer.schedule(timerTask, 0, 1000);
        if (!isShow){
            hideSystemUI();
        }
    }

    @Override
    protected void onStop(){
        timerTask.cancel();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerTask.cancel();
        pageFactory.clear();
        bookpage = null;
        unregisterReceiver(myReceiver);
        isSpeaking = false;
    }




    public static boolean openBook(final BookList bookList, Activity context) {
        if (bookList == null){
            throw new NullPointerException("BookList can not be null");
        }
        Log.d("book list name", bookList.getBookname());
        Log.d("book list path", bookList.getBookpath());
        Log.d("book list id", bookList.getId()+"");
        Log.d("book list begin", bookList.getBegin()+"");
        Intent intent = new Intent(context, ReadActivity.class);
        intent.putExtra(EXTRA_BOOK, bookList);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
        context.startActivity(intent);
        return true;
    }


    private void hideSystemUI() {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        //  | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    private void showSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }




    private void showReadSetting(){
        isShow = true;
        rl_progress.setVisibility(View.GONE);

        if (isSpeaking){
//            Animation topAnim = AnimationUtils.loadAnimation(this, R.anim.dialog_top_enter);
//            rl_read_bottom.startAnimation(topAnim);
//            rl_read_bottom.setVisibility(View.VISIBLE);
        }else {
            showSystemUI();

            Animation bottomAnim = AnimationUtils.loadAnimation(this, R.anim.dialog_enter);
            Animation topAnim = AnimationUtils.loadAnimation(this, R.anim.dialog_top_enter);
            rl_bottom.startAnimation(topAnim);
            appbar.startAnimation(topAnim);
//        ll_top.startAnimation(topAnim);
            rl_bottom.setVisibility(View.VISIBLE);
//        ll_top.setVisibility(View.VISIBLE);
            appbar.setVisibility(View.VISIBLE);
        }
    }

    private void hideReadSetting() {
        isShow = false;
        Animation bottomAnim = AnimationUtils.loadAnimation(this, R.anim.dialog_exit);
        Animation topAnim = AnimationUtils.loadAnimation(this, R.anim.dialog_top_exit);
        if (rl_bottom.getVisibility() == View.VISIBLE) {
            rl_bottom.startAnimation(topAnim);
        }
        if (appbar.getVisibility() == View.VISIBLE) {
            appbar.startAnimation(topAnim);
        }

        rl_bottom.setVisibility(View.GONE);
        appbar.setVisibility(View.GONE);
        hideSystemUI();
    }


}
