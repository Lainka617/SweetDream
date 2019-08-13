package com.example.sweetdream;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.example.sweetdream.base.BaseActivity;

public class ThirdActivity extends BaseActivity {
    @Override
    protected void initListener() {

    }

    @Override
    protected void initData() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView web=(WebView) findViewById(R.id.web);
        web.loadUrl("http://textfiles.com/stories/");

        web.getSettings().setJavaScriptEnabled(true);
        web.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
    }

    @Override
    public int getLayoutRes() {
        return R.layout.activity_third;
    }
}
