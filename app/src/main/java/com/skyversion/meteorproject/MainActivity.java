package com.skyversion.meteorproject;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsMessage;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends Activity {

    private WebView webView;
    private TextView textView;
    private final Handler mHandler = new Handler();
    private Button sendMsgBtn;
    private TimerTask mTask;
    private Timer mTimer;
    private SmsInbox smsInbox = null;
    private SMSRecvBroadCastReceiver myReceiver;

    private boolean getPreferences(){
        SharedPreferences sp = getSharedPreferences("pref", MODE_PRIVATE);

        if(sp.contains("a"))
            return true;

        return false;
    } // 첫 실행여부 판단을 위해 SharePreference isFirst의 값 존재 확인 여부

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        webView.setWebViewClient(new WebClient());
        webView.getSettings().setJavaScriptEnabled(true); // javascript permission
        webView.getSettings().setBuiltInZoomControls(true); // zoom permission

        webView.addJavascriptInterface(new AndroidBridge(), "smsParser");
        webView.loadUrl("http://192.168.115.172:3000");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();

                result.confirm();
                return true;
//                return super.onJsAlert(view, url, message, result);
            }
        }); // android

        sendMsgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                webView.loadUrl("javascript:parser('"+"ok"+"')");
                smsInbox.smsJson();
            }
        }); // android -> web

        mTask = new TimerTask() {
            @Override
            public void run() {
                if(getPreferences()){}
                else{
                    SharedPreferences sp = getSharedPreferences("pref", MODE_PRIVATE);
                    SharedPreferences.Editor edit = sp.edit();
                    edit.putBoolean("a", true);
                    edit.commit();
                    smsInbox. smsJson();
                }// isFirst 가 없을 시 생성함
            }
        };

        mTimer = new Timer();
        mTimer.schedule(mTask, 1000);

        myReceiver = new SMSRecvBroadCastReceiver();

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(Intent.ACTION_BOOT_COMPLETED);
        intentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");

        registerReceiver(myReceiver, intentFilter);

        smsInbox = new SmsInbox(this, webView);
    }

    private void init(){
        webView = (WebView) findViewById(R.id.webView);
        textView = (TextView) findViewById(R.id.textView);
        sendMsgBtn = (Button) findViewById(R.id.button);
    } // 생성

    private class WebClient extends WebViewClient{
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return super.shouldOverrideUrlLoading(view, url);
        }
    }

    private class AndroidBridge{
        @JavascriptInterface
        public void setMessage(final String arg){
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    textView.setText(arg);
                }
            });
        }
    } // Javascript -> Android

    @Override
    protected void onDestroy() {
        mTimer.cancel();
        unregisterReceiver(myReceiver);
        Log.d("onDestory()", "브로드캐스트리시버 해제");
        // service 해제
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public class SMSRecvBroadCastReceiver extends BroadcastReceiver {
        public static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
        private String phoneNum;
        private String bankName;
        private Long milliSecond;

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(SMS_RECEIVED)){
                Bundle bundle = intent.getExtras();

                if(bundle == null)
                    return;

                Object[] pdusObj = (Object[]) bundle.get("pdus");

                if(pdusObj == null)
                    return;

                String messageBody = "";

                SmsMessage[] smsMessages = new SmsMessage[pdusObj.length];

                for(int i=0;i<pdusObj.length;i++){
                    smsMessages[i] = SmsMessage.createFromPdu((byte[])pdusObj[i]);

                    phoneNum = smsMessages[i].getOriginatingAddress().toString();

                    if(phoneNum.equals("15881600"))
                        bankName = "농협";
                    else if(phoneNum.equals("15881788"))
                        bankName = "국민";

                    milliSecond = smsMessages[i].getTimestampMillis();
                    messageBody+= smsMessages[i].getMessageBody().toString();
                }

                setSmsReceived(messageBody, bankName, milliSecond);
            }
        }

        public void setSmsReceived(String msg, String bankName, Long milliSecond){
            smsInbox.recvSmsInbox(msg, bankName, milliSecond);
        }

//        public void sendJson(){
//            jsonArray = new JSONArray();
//            webView.loadUrl("javascript:parser(" + msgBody + ")");
//        }
    }
    /* 브로드캐스트의 경우 2가지 방법이 있다.
    inner Broadcast receiver must be static ( to be registered through Manifest) : static으로 한 다음 매니페스트에 등록
    Non-static broadcast receiver must be registered and unregistered inside the Parent class : static이 아닌 경우 부모 클래스에 register와 unregister을 써라.
    참고 : http://stackoverflow.com/questions/29947038/java-lang-instantiationexception-class-has-no-zero-argument-constructor
           http://www.masterqna.com/android/44942/%EC%95%A1%ED%8B%B0%EB%B9%84%ED%8B%B0-%EC%84%9C%EB%B9%84%EC%8A%A4-broadcast-receiver-%EC%98%A4%EB%A5%98-%EC%A7%88%EB%AC%B8-%ED%95%B4%EA%B2%B0
    */
}
