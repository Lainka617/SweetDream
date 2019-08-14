package com.example.sweetdream;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.example.sweetdream.base.BaseActivity;

public class ThirdActivity extends BaseActivity {


    private Context context;
    private WebView web;

    @Override
    protected void initListener() {

    }

    @Override
    protected void initData() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        web=(WebView) findViewById(R.id.web);
        web.loadUrl("http://textfiles.com/stories/");

        web.getSettings().setJavaScriptEnabled(true);
        web.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });


//        web.setOnLongClickListener(new View.OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View view) {
//                final WebView.HitTestResult hitTestResult = web.getHitTestResult();
//
//                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
//                    builder.setTitle("Attention");
//                    builder.setMessage("save file to storage");
//                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialogInterface, int i) {
//                            String url = hitTestResult.getExtra();
////                            // 下载图片到本地
////                            DownPicUtil.downPic(url, new DownPicUtil.DownFinishListener(){
////
////                                @Override
////                                public void getDownPath(String s) {
////                                    Toast.makeText(context,"下载完成", Toast.LENGTH_LONG).show();
////
////                                }
////                            });
//
//                        }
//                    });
//                    builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
//                        // 自动dismiss
//                        @Override
//                        public void onClick(DialogInterface dialogInterface, int i) {
//                        }
//                    });
//                    AlertDialog dialog = builder.create();
//                    dialog.show();
//
//                return true;
//            }
//        });


    }



    @Override
    public int getLayoutRes() {
        return R.layout.activity_third;
    }
}
