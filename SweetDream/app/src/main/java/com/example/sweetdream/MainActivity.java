package com.example.sweetdream;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.widget.AbsoluteLayout;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.sweetdream.adapter.ShelfAdapter;
import com.example.sweetdream.animation.ContentScaleAnimation;
import com.example.sweetdream.animation.Rotate3DAnimation;
import com.example.sweetdream.base.BaseActivity;
import com.example.sweetdream.db.BookList;
import com.example.sweetdream.filechooser.FileChooserActivity;
import com.example.sweetdream.util.DisplayUtils;
import com.example.sweetdream.view.DragGridView;

import org.litepal.crud.DataSupport;

import java.io.File;
import java.util.List;

import butterknife.Bind;

public class MainActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener, Animation.AnimationListener  {

    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.fab)
    FloatingActionButton fab;
    @Bind(R.id.bookShelf)
    DragGridView bookShelf;


    private WindowManager mWindowManager;
    private AbsoluteLayout wmRootView;
    private View rootView;
    private Typeface typeface;

    private List<BookList> bookLists;
    private ShelfAdapter adapter;
    //the position of clicked book
    private int itemPosition;
    private TextView itemTextView;
    //the x,y of clicked book
    private int[] location = new int[2];

    private static TextView cover;
    private static ImageView content;
    //opening Animation scale times
    private float scaleTimes;
    //opening Animation
    private static ContentScaleAnimation contentAnimation;
    private static Rotate3DAnimation coverAnimation;
    //duration of opening Animation
    public static final int ANIMATION_DURATION = 800;
    //if first animation done
    private boolean mIsOpen = false;
    //count of animations  default:0   1   2
    private int animationCount=0;

    private Config config;
    @Override
    public int getLayoutRes() {
        return R.layout.activity_main;
    }

    @Override
    protected void initData() {
        setSupportActionBar(toolbar);

        config = Config.getInstance();
        getWindow().setBackgroundDrawable(null);
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        wmRootView = new AbsoluteLayout(this);
        rootView = getWindow().getDecorView();

        typeface = config.getTypeface();
        bookLists = DataSupport.findAll(BookList.class);
        adapter = new ShelfAdapter(MainActivity.this,bookLists);
        bookShelf.setAdapter(adapter);
    }

    @Override
    protected void initListener() {
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(MainActivity.this, FileChooserActivity.class);
                MainActivity.this.startActivity(intent);
            }
        });

        bookShelf.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (bookLists.size() > position) {
                    itemPosition = position;
                    String bookname = bookLists.get(itemPosition).getBookname();

                    adapter.setItemToFirst(itemPosition);
                    final BookList bookList = bookLists.get(itemPosition);
                    bookList.setId(bookLists.get(0).getId());
                    final String path = bookList.getBookpath();
                    File file = new File(path);
                    if (!file.exists()){
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle(MainActivity.this.getString(R.string.app_name))
                                .setMessage(path + "file not exists, do you want to delete？")
                                .setPositiveButton("delete", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        DataSupport.deleteAll(BookList.class, "bookpath = ?", path);
                                        bookLists = DataSupport.findAll(BookList.class);
                                        adapter.setBookList(bookLists);
                                    }
                                }).setCancelable(true).show();
                        return;
                    }

                    ReadActivity.openBook(bookList,MainActivity.this);

                }
            }
        });
    }


    @Override
    protected void onRestart(){
        super.onRestart();
        DragGridView.setIsShowDeleteButton(false);
        bookLists = DataSupport.findAll(BookList.class);
        adapter.setBookList(bookLists);
//        closeBookAnimation();
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    @Override
    protected void onStop() {
        DragGridView.setIsShowDeleteButton(false);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        DragGridView.setIsShowDeleteButton(false);
        super.onDestroy();
    }


//    //Animation initialize
//    private void initAnimation() {
//        AccelerateInterpolator interpolator = new AccelerateInterpolator();
//
//        float scale1 = DisplayUtils.getScreenWidthPixels(this) / (float) itemTextView.getMeasuredWidth();
//        float scale2 = DisplayUtils.getScreenHeightPixels(this) / (float) itemTextView.getMeasuredHeight();
//        scaleTimes = scale1 > scale2 ? scale1 : scale2;  //
//
//        contentAnimation = new ContentScaleAnimation( location[0], location[1],scaleTimes, false);
//        contentAnimation.setInterpolator(interpolator);  //设置插值器
//        contentAnimation.setDuration(ANIMATION_DURATION);
//        contentAnimation.setFillAfter(true);  //动画停留在最后一帧
//        contentAnimation.setAnimationListener(this);
//
//        coverAnimation = new Rotate3DAnimation(0, -180, location[0], location[1], scaleTimes, false);
//        coverAnimation.setInterpolator(interpolator);
//        coverAnimation.setDuration(ANIMATION_DURATION);
//        coverAnimation.setFillAfter(true);
//        coverAnimation.setAnimationListener(this);
//    }

//    public void closeBookAnimation() {
//
//        if (mIsOpen && wmRootView!=null) {
//            //因为书本打开后会移动到第一位置，所以要设置新的位置参数
//            contentAnimation.setmPivotXValue(bookShelf.getFirstLocation()[0]);
//            contentAnimation.setmPivotYValue(bookShelf.getFirstLocation()[1]);
//            coverAnimation.setmPivotXValue(bookShelf.getFirstLocation()[0]);
//            coverAnimation.setmPivotYValue(bookShelf.getFirstLocation()[1]);
//
//            AbsoluteLayout.LayoutParams params = new AbsoluteLayout.LayoutParams(
//                    itemTextView.getLayoutParams());
//            params.x = bookShelf.getFirstLocation()[0];
//            params.y = bookShelf.getFirstLocation()[1];//firstLocation[1]在滑动的时候回改变,所以要在dispatchDraw的时候获取该位置值
//            wmRootView.updateViewLayout(cover,params);
//            wmRootView.updateViewLayout(content,params);
//            //动画逆向运行
//            if (!contentAnimation.getMReverse()) {
//                contentAnimation.reverse();
//            }
//            if (!coverAnimation.getMReverse()) {
//                coverAnimation.reverse();
//            }
//            //清除动画再开始动画
//            content.clearAnimation();
//            content.startAnimation(contentAnimation);
//            cover.clearAnimation();
//            cover.startAnimation(coverAnimation);
//        }
//    }

    @Override
    public void onAnimationStart(Animation animation) {

    }

    @Override
    public void onAnimationEnd(Animation animation) {

        if (!mIsOpen) {
            animationCount++;
            if (animationCount >= 2) {
                mIsOpen = true;
                adapter.setItemToFirst(itemPosition);

                BookList bookList = bookLists.get(itemPosition);
                bookList.setId(bookLists.get(0).getId());
                ReadActivity.openBook(bookList,MainActivity.this);
            }

        } else {
            animationCount--;
            if (animationCount <= 0) {
                mIsOpen = false;
                wmRootView.removeView(cover);
                wmRootView.removeView(content);
                mWindowManager.removeView(wmRootView);
            }
        }
    }

    @Override
    public void onAnimationRepeat(Animation animation) {

    }

    //获取dialog属性
    private WindowManager.LayoutParams getDefaultWindowParams() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                0, 0,
                WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,//windown类型,有层级的大的层级会覆盖在小的层级
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                PixelFormat.RGBA_8888);

        return params;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }else if (id == R.id.action_select_file){
//            Intent intent = new Intent(MainActivity.this, FileChooserActivity.class);
//            startActivity(intent);
//        }

//        if (id == R.id.action_select_file){
//            Intent intent = new Intent(MainActivity.this, FileChooserActivity.class);
//            startActivity(intent);
//        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
//        int id = item.getItemId();
//
//        if (id == R.id.nav_feedback) {
////           FeedbackAgent agent = new FeedbackAgent(this);
////           agent.startFeedbackActivity();
//
//        } else if (id == R.id.nav_checkupdate) {
//            //checkUpdate(true);
//        }else if (id == R.id.nav_about) {
//            Intent intent = new Intent(MainActivity.this, AboutActivity.class);
//            startActivity(intent);
//        }

//        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
//        drawer.closeDrawer(GravityCompat.START);
        return true;
    }




}

