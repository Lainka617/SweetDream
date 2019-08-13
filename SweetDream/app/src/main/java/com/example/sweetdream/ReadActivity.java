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

/**
 * Created by Administrator on 2016/7/15 0015.
 */
public class ReadActivity extends BaseActivity implements SpeechSynthesizerListener {
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
    @Bind(R.id.tv_stop_read)
    TextView tv_stop_read;
    @Bind(R.id.rl_read_bottom)
    RelativeLayout rl_read_bottom;
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
    // popwindow是否显示
    private Boolean isShow = false;
//    private SettingDialog mSettingDialog;
//    private PageModeDialog mPageModeDialog;
    private Boolean mDayOrNight;
    // 语音合成客户端
    private SpeechSynthesizer mSpeechSynthesizer;
    private boolean isSpeaking = false;

    /**
     * new code for timer
     */

    private TimerTask timerTask;
    int alartTime = 61;
    private Timer timer = new Timer();
    Vibrator vibrator;

    // 接收电池信息更新的广播
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
        //获取屏幕宽高
        WindowManager manage = getWindowManager();
        Display display = manage.getDefaultDisplay();
        Point displaysize = new Point();
        display.getSize(displaysize);
        screenWidth = displaysize.x;
        screenHeight = displaysize.y;
        //保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //隐藏
        hideSystemUI();
        //改变屏幕亮度
//        if (!config.isSystemLight()) {
//            BrightnessUtil.setBrightness(this, config.getLight());
//        }
        //获取intent中的携带的信息
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
        if (!isShow){
            hideSystemUI();
        }
        if (mSpeechSynthesizer != null){
            mSpeechSynthesizer.resume();
        }
    }

    @Override
    protected void onStop(){
        super.onStop();
        if (mSpeechSynthesizer != null){
            mSpeechSynthesizer.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pageFactory.clear();
        bookpage = null;
        unregisterReceiver(myReceiver);
        isSpeaking = false;
        if (mSpeechSynthesizer != null){
            mSpeechSynthesizer.release();
        }
    }

//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        // TODO Auto-generated method stub
//        if (keyCode == KeyEvent.KEYCODE_BACK) {
//            if (isShow){
//                hideReadSetting();
//                return true;
//            }
//            if (mSettingDialog.isShowing()){
//                mSettingDialog.hide();
//                return true;
//            }
//            if (mPageModeDialog.isShowing()){
//                mPageModeDialog.hide();
//                return true;
//            }
//            finish();
//        }
//        return super.onKeyDown(keyCode, event);
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.read, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_add_bookmark){
            if (pageFactory.getCurrentPage() != null) {
                List<BookMarks> bookMarksList = DataSupport.where("bookpath = ? and begin = ?", pageFactory.getBookPath(),pageFactory.getCurrentPage().getBegin() + "").find(BookMarks.class);

                if (!bookMarksList.isEmpty()){
                    Toast.makeText(ReadActivity.this, "该书签已存在", Toast.LENGTH_SHORT).show();
                }else {
                    BookMarks bookMarks = new BookMarks();
                    String word = "";
                    for (String line : pageFactory.getCurrentPage().getLines()) {
                        word += line;
                    }
                    try {
                        SimpleDateFormat sf = new SimpleDateFormat(
                                "yyyy-MM-dd HH:mm ss");
                        String time = sf.format(new Date());
                        bookMarks.setTime(time);
                        bookMarks.setBegin(pageFactory.getCurrentPage().getBegin());
                        bookMarks.setText(word);
                        bookMarks.setBookpath(pageFactory.getBookPath());
                        bookMarks.save();

                        Toast.makeText(ReadActivity.this, "书签添加成功", Toast.LENGTH_SHORT).show();
                    } catch (SQLException e) {
                        Toast.makeText(ReadActivity.this, "该书签已存在", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(ReadActivity.this, "添加书签失败", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }else if (id == R.id.action_read_book){
            initialTts();
            if (mSpeechSynthesizer != null){
                mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_VOLUME, "5");
                mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEED, "5");
                mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_PITCH, "5");
                mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEAKER, "0");
//                mSpeechSynthesizer.setParam(SpeechSynthesizer. MIX_MODE_DEFAULT);
//                mSpeechSynthesizer.setParam(SpeechSynthesizer. AUDIO_ENCODE_AMR);
//                mSpeechSynthesizer.setParam(SpeechSynthesizer. AUDIO_BITRA TE_AMR_15K85);
                mSpeechSynthesizer.setParam(SpeechSynthesizer. PARAM_VOCODER_OPTIM_LEVEL, "0");
                int result = mSpeechSynthesizer.speak(pageFactory.getCurrentPage().getLineToString());
                if (result < 0) {
                    Log.e(TAG,"error,please look up error code in doc or URL:http://yuyin.baidu.com/docs/tts/122 ");
                }else{
                    hideReadSetting();
                    isSpeaking = true;
                }
            }
        }

        return super.onOptionsItemSelected(item);
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

//    public BookPageWidget getPageWidget() {
//        return bookpage;
//    }

    /**
     * 隐藏菜单。沉浸式阅读
     */
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

    //显示书本进度
    public void showProgress(float progress){
        if (rl_progress.getVisibility() != View.VISIBLE) {
            rl_progress.setVisibility(View.VISIBLE);
        }
        setProgress(progress);
    }

    //隐藏书本进度
    public void hideProgress(){
        rl_progress.setVisibility(View.GONE);
    }

//    public void initDayOrNight(){
//        mDayOrNight = config.getDayOrNight();
//        if (mDayOrNight){
//            tv_dayornight.setText(getResources().getString(R.string.read_setting_day));
//        }else{
//            tv_dayornight.setText(getResources().getString(R.string.read_setting_night));
//        }
//    }

//    //改变显示模式
//    public void changeDayOrNight(){
//        if (mDayOrNight){
//            mDayOrNight = false;
//            tv_dayornight.setText(getResources().getString(R.string.read_setting_night));
//        }else{
//            mDayOrNight = true;
//            tv_dayornight.setText(getResources().getString(R.string.read_setting_day));
//        }
//        config.setDayOrNight(mDayOrNight);
//        pageFactory.setDayOrNight(mDayOrNight);
//    }

    private void setProgress(float progress){
        DecimalFormat decimalFormat=new DecimalFormat("00.00");//构造方法的字符格式这里如果小数不足2位,会以0补足.
        String p=decimalFormat.format(progress * 100.0);//format 返回的是字符串
        tv_progress.setText(p + "%");
    }

//    public void setSeekBarProgress(float progress){
//        sb_progress.setProgress((int) (progress * 10000));
//    }

    private void showReadSetting(){
        isShow = true;
        rl_progress.setVisibility(View.GONE);

        if (isSpeaking){
            Animation topAnim = AnimationUtils.loadAnimation(this, R.anim.dialog_top_enter);
            rl_read_bottom.startAnimation(topAnim);
            rl_read_bottom.setVisibility(View.VISIBLE);
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
        if (rl_read_bottom.getVisibility() == View.VISIBLE) {
            rl_read_bottom.startAnimation(topAnim);
        }
//        ll_top.startAnimation(topAnim);
        rl_bottom.setVisibility(View.GONE);
        rl_read_bottom.setVisibility(View.GONE);
//        ll_top.setVisibility(View.GONE);
        appbar.setVisibility(View.GONE);
        hideSystemUI();
    }

    private void initialTts() {
        this.mSpeechSynthesizer = SpeechSynthesizer.getInstance();
        this.mSpeechSynthesizer.setContext(this);
        this.mSpeechSynthesizer.setSpeechSynthesizerListener(this);
        // 文本模型文件路径 (离线引擎使用)
        this.mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_TTS_TEXT_MODEL_FILE, ((AppContext)getApplication()).getTTPath() + "/"
                + AppContext.TEXT_MODEL_NAME);
        // 声学模型文件路径 (离线引擎使用)
        this.mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_TTS_SPEECH_MODEL_FILE, ((AppContext)getApplication()).getTTPath() + "/"
                + AppContext.SPEECH_FEMALE_MODEL_NAME);
        // 本地授权文件路径,如未设置将使用默认路径.设置临时授权文件路径，LICENCE_FILE_NAME请替换成临时授权文件的实际路径，仅在使用临时license文件时需要进行设置，如果在[应用管理]中开通了正式离线授权，不需要设置该参数，建议将该行代码删除（离线引擎）
        // 如果合成结果出现临时授权文件将要到期的提示，说明使用了临时授权文件，请删除临时授权即可。
//        this.mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_TTS_LICENCE_FILE, ((AppContext)getApplication()).getTTPath() + "/"
//                + AppContext.LICENSE_FILE_NAME);
        // 请替换为语音开发者平台上注册应用得到的App ID (离线授权)
        this.mSpeechSynthesizer.setAppId("8921835"/*这里只是为了让Demo运行使用的APPID,请替换成自己的id。*/);
        // 请替换为语音开发者平台注册应用得到的apikey和secretkey (在线授权)
        this.mSpeechSynthesizer.setApiKey("sjEFlROl4j090FtDTHlEpvFB",
                "a2d95dc24960e03ef2d41a5fb1a2c025"/*这里只是为了让Demo正常运行使用APIKey,请替换成自己的APIKey*/);
        // 发音人（在线引擎），可用参数为0,1,2,3。。。（服务器端会动态增加，各值含义参考文档，以文档说明为准。0--普通女声，1--普通男声，2--特别男声，3--情感男声。。。）
        this.mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEAKER, "0");
        // 设置Mix模式的合成策略
        this.mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_MIX_MODE, SpeechSynthesizer.MIX_MODE_DEFAULT);
        // 授权检测接口(只是通过AuthInfo进行检验授权是否成功。)
        // AuthInfo接口用于测试开发者是否成功申请了在线或者离线授权，如果测试授权成功了，可以删除AuthInfo部分的代码（该接口首次验证时比较耗时），不会影响正常使用（合成使用时SDK内部会自动验证授权）
        AuthInfo authInfo = this.mSpeechSynthesizer.auth(TtsMode.MIX);

        if (authInfo.isSuccess()) {
            Log.e(TAG,"auth success");
        } else {
            String errorMsg = authInfo.getTtsError().getDetailMessage();
            Log.e(TAG,"auth failed errorMsg=" + errorMsg);
        }

        // 初始化tts
        mSpeechSynthesizer.initTts(TtsMode.MIX);
        // 加载离线英文资源（提供离线英文合成功能）
        int result = mSpeechSynthesizer.loadEnglishModel(((AppContext)getApplication()).getTTPath() + "/" + AppContext.ENGLISH_TEXT_MODEL_NAME, ((AppContext)getApplication()).getTTPath()
                        + "/" + AppContext.ENGLISH_SPEECH_FEMALE_MODEL_NAME);
//        toPrint("loadEnglishModel result=" + result);
//
//        //打印引擎信息和model基本信息
//        printEngineInfo();
    }

//    @OnClick({R.id.tv_progress, R.id.rl_progress, R.id.tv_pre, R.id.sb_progress, R.id.tv_next, R.id.tv_directory, R.id.tv_dayornight,R.id.tv_pagemode, R.id.tv_setting, R.id.bookpop_bottom, R.id.rl_bottom,R.id.tv_stop_read})
//    public void onClick(View view) {
//        switch (view.getId()) {
////            case R.id.btn_return:
////                finish();
////                break;
////            case R.id.ll_top:
////                break;
//            case R.id.tv_progress:
//                break;
//            case R.id.rl_progress:
//                break;
////            case R.id.tv_pre:
////                pageFactory.preChapter();
////                break;
////            case R.id.sb_progress:
////                break;
////            case R.id.tv_next:
////                pageFactory.nextChapter();
////                break;
////            case R.id.tv_directory:
////                Intent intent = new Intent(ReadActivity.this, MarkActivity.class);
////                startActivity(intent);
////                break;
//            case R.id.tv_dayornight:
//                changeDayOrNight();
//                break;
////            case R.id.tv_pagemode:
////                hideReadSetting();
////                mPageModeDialog.show();
////                break;
////            case R.id.tv_setting:
////                hideReadSetting();
////                mSettingDialog.show();
////                break;
//            case R.id.bookpop_bottom:
//                break;
//            case R.id.rl_bottom:
//                break;
//            case R.id.tv_stop_read:
//                if (mSpeechSynthesizer!=null){
//                    mSpeechSynthesizer.stop();
//                    isSpeaking = false;
//                    hideReadSetting();
//                }
//                break;
//        }
//    }

    /*
    * @param arg0
    */
    @Override
    public void onSynthesizeStart(String s) {

    }

    /**
     * 合成数据和进度的回调接口，分多次回调
     *
     * @param utteranceId
     * @param data 合成的音频数据。该音频数据是采样率为16K，2字节精度，单声道的pcm数据。
     * @param progress 文本按字符划分的进度，比如:你好啊 进度是0-3
     */
    @Override
    public void onSynthesizeDataArrived(String utteranceId, byte[] data, int progress) {

    }

    /**
     * 合成正常结束，每句合成正常结束都会回调，如果过程中出错，则回调onError，不再回调此接口
     *
     * @param utteranceId
     */
    @Override
    public void onSynthesizeFinish(String utteranceId) {

    }

    /**
     * 播放开始，每句播放开始都会回调
     *
     * @param utteranceId
     */
    @Override
    public void onSpeechStart(String utteranceId) {

    }

    /**
     * 播放进度回调接口，分多次回调
     *
     * @param utteranceId
     * @param progress 文本按字符划分的进度，比如:你好啊 进度是0-3
     */
    @Override
    public void onSpeechProgressChanged(String utteranceId, int progress) {

    }

    /**
     * 播放正常结束，每句播放正常结束都会回调，如果过程中出错，则回调onError,不再回调此接口
     *
     * @param utteranceId
     */
    @Override
    public void onSpeechFinish(String utteranceId) {
        pageFactory.nextPage();
        if (pageFactory.islastPage()) {
            isSpeaking = false;
            Toast.makeText(ReadActivity.this,"小说已经读完了",Toast.LENGTH_SHORT);
        }else {
            isSpeaking = true;
            mSpeechSynthesizer.speak(pageFactory.getCurrentPage().getLineToString());
        }
    }

    /**
     * 当合成或者播放过程中出错时回调此接口
     *
     * @param utteranceId
     * @param error 包含错误码和错误信息
     */
    @Override
    public void onError(String utteranceId, SpeechError error) {
        mSpeechSynthesizer.stop();
        isSpeaking = false;
        Log.e(TAG,error.description);
    }

}
