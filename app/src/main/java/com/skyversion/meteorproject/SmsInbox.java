package com.skyversion.meteorproject;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by ss on 2016-06-04.
 */
public class SmsInbox {
    private String[] paymentList= null;
    private String[] internetPayment = null;

    private String[] bank = {"국민"};
    private String[] receiveNumber = {"01033268141"}; // KB, NongHyup, Busan, woori

    private String money = null, time = null, place = null;
    private String month = null, day = null, year = null;

    private StringBuffer sb;
    private JSONArray jsonArray;
    private Activity mActivity;
    private WebView webView;

    public SmsInbox() throws Exception{}

    public SmsInbox(Activity activity, WebView webView){
        mActivity = activity;
        this.webView = webView;
    }

    public void inbox(Context context, String receiveNumber, String bankName){
        Cursor c;
        sb = new StringBuffer();
        Uri inboxURI = Uri.parse("content://sms/inbox");

        // List required columns
        String[] reqCols = new String[] { "_id", "address", "body", "date" };

        // Get Content Resolver object, which will deal with Content
        // Provider
        ContentResolver cr = context.getContentResolver();

        // Fetch Inbox SMS Message from Built-in Content Provider
        //c = cr.query(inboxURI, reqCols, "address LIKE "+receiveNumber, null, null);
        c = cr.query(inboxURI, reqCols, "address like '" + receiveNumber + "'", null, null);

        DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss.SSS");
        Calendar calendar = Calendar.getInstance();
        // 문자 수신 시간(milliSeconde)를 날짜로 변환

        String year= null;
        long milliSecond;

        if(c != null) {
            while (c.moveToNext()) {
                //Log.d("TAG : ", c.getString(2));
                // query를 하여 받아온 리스트들이 끝날 때 까지 돌아감.
                paymentList = c.getString(2).split("\n");
                // SMS Parsing의 내용은 \n으로 구분한다.

                milliSecond = c.getLong(3);

                calendar.setTimeInMillis(milliSecond);
                year = formatter.format(calendar.getTime());
                sb.insert(0, year);
                year = sb.substring(0, 4);
                sb.delete(0, year.length());

                for (int i = 0; i < paymentList.length; i++) {
                    internetPayment = paymentList[i].split(" ");

                    if (i == paymentList.length - 1) {
                        if (receiveNumber.equals("15881788")) {
                            if(paymentList[i].indexOf("사용")!=-1){
                                sb.insert(0, paymentList[i]);
                                place = sb.substring(0, paymentList[i].indexOf("사용"));
                                sb.delete(0, sb.length());
                            }
                        }//kb bank 사용 delete
                        else if (receiveNumber.equals("15881600"))
                            place = paymentList[i];
                    } // Enterprise Name

                    else if (paymentList[i].indexOf("원") != -1) {
                        if (paymentList[i].indexOf(",") != -1 && internetPayment.length <= 1){
                            sb.insert(0, paymentList[i]);
                            money = sb.substring(0, paymentList[i].length()-1);
                            sb.delete(0, sb.length());
                        }// 일반 매장 결제 시

                        else if(internetPayment.length > 1){
                            for (int j = 0; j < internetPayment.length; j++) {
                                if (internetPayment[j].indexOf(",") != -1){
                                    sb.insert(0, internetPayment[j]);
                                    money = sb.substring(0, internetPayment[j].length()-1);
                                    sb.delete(0, sb.length());
                                }// price
                                else if(internetPayment[j].indexOf("/") != -1){
                                    sb.insert(0, internetPayment[j] + " ");
                                    sb.delete(0, internetPayment[j].length()+1);
                                }

                                else if(internetPayment[j].indexOf(":") != -1){
                                    sb.insert(0, internetPayment[j]);

                                    if(bankName.equals("농협"))
                                        time = sb.toString();

                                    else if(bankName.equals("국민"))
                                        time = sb.substring(6, sb.length());
//                                    time = c.getString(c.getColumnIndex("date"));
                                    //Log.d("TAG", time);
                                    sb.delete(0, sb.length());
                                } // time

                                else if (j == internetPayment.length - 1) {
                                    if (internetPayment[j].indexOf(".") != -1) {
                                        sb.insert(0, internetPayment[j]);
                                        place = sb.substring(internetPayment[j].indexOf(".") + 1, internetPayment[j].length());
                                        sb.delete(0, sb.length());
                                    }
                                } // enterprise
                            }
                        }// 11번가 등 인터넷 결제 시
                    }// paymentList[i] - internetPayment[j] 비교

                    else if (paymentList[i].indexOf("/") != -1 && paymentList[i].indexOf(":") != -1){
                        sb.insert(0, paymentList[i]);
                        month = sb.substring(0, 2);
                        day = sb.substring(3, 5);
                        time = sb.substring(6, 11);
                        sb.delete(0, sb.length());
//                        Log.d("TAG", time);
                    } // payment Time
                }

                if (money != null && day != null && time != null && place != null && month != null && year != null){
                    try{
                        JSONObject obj = new JSONObject();
                        obj.put("bank", bankName);
                        obj.put("time", time);
                        obj.put("day", day);
                        obj.put("month", month);
                        obj.put("year", year);
                        obj.put("place", place);
                        obj.put("money", money);
                        jsonArray.put(obj);
                    }catch(JSONException e){
                        e.printStackTrace();
                    }
                } // time, enterpriseName, won이 null이 아닐 시 값을 넣어줌!

                time = month = day = place = money = null;
                paymentList = internetPayment = null;
            }
        }
        Log.d("TAG", jsonArray.toString());
    }

    public void recvSmsInbox(String msg, String bankName, Long milliSecond){
        jsonArray = new JSONArray();
        DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss.SSS");
        Calendar calendar = Calendar.getInstance();

        calendar.setTimeInMillis(milliSecond);
        String year = formatter.format(calendar.getTime());

        sb = new StringBuffer();

        sb.insert(0, year);
        year = sb.substring(0, 4);
        sb.delete(0, year.length());

        paymentList = msg.split("\n");
        // SMS Parsing의 내용은 \n으로 구분한다.

        calendar.setTimeInMillis(milliSecond);
        year = formatter.format(calendar.getTime());
        sb.insert(0, year);
        year = sb.substring(0, 4);
        sb.delete(0, year.length());

        for (int i = 0; i < paymentList.length; i++) {
            internetPayment = paymentList[i].split(" ");

            if (i == paymentList.length - 1) {
                if (receiveNumber.equals("15881788")) {
                    if(paymentList[i].indexOf("사용")!=-1){
                        sb.insert(0, paymentList[i]);
                        place = sb.substring(0, paymentList[i].indexOf("사용"));
                        sb.delete(0, sb.length());
                    }
                }//kb bank 사용 delete
                else if (receiveNumber.equals("15881600"))
                    place = paymentList[i];
                else
                    place = "지나웍스";
            } // Enterprise Name

            else if (paymentList[i].indexOf("원") != -1) {
                if (paymentList[i].indexOf(",") != -1 && internetPayment.length <= 1){
                    sb.insert(0, paymentList[i]);
                    money = sb.substring(0, paymentList[i].length()-1);
                    sb.delete(0, sb.length());
                }// 일반 매장 결제 시

                else if(internetPayment.length > 1){
                    for (int j = 0; j < internetPayment.length; j++) {
                        if (internetPayment[j].indexOf(",") != -1){
                            sb.insert(0, internetPayment[j]);
                            money = sb.substring(0, internetPayment[j].length()-1);
                            sb.delete(0, sb.length());
                        }// price
                        else if(internetPayment[j].indexOf("/") != -1){
                            sb.insert(0, internetPayment[j] + " ");
                            sb.delete(0, internetPayment[j].length()+1);
                        }

                        else if(internetPayment[j].indexOf(":") != -1){
                            sb.insert(0, internetPayment[j]);

                            if(bankName.equals("농협"))
                                time = sb.toString();

                            else if(bankName.equals("국민"))
                                time = sb.substring(6, sb.length());
//                                    time = c.getString(c.getColumnIndex("date"));
                            //Log.d("TAG", time);
                            sb.delete(0, sb.length());
                        } // time

                        else if (j == internetPayment.length - 1) {
                            if (internetPayment[j].indexOf(".") != -1) {
                                sb.insert(0, internetPayment[j]);
                                place = sb.substring(internetPayment[j].indexOf(".") + 1, internetPayment[j].length());
                                sb.delete(0, sb.length());
                            }
                        } // enterprise
                    }
                }// 11번가 등 인터넷 결제 시
            }// paymentList[i] - internetPayment[j] 비교

            else if (paymentList[i].indexOf("/") != -1 && paymentList[i].indexOf(":") != -1){
                sb.insert(0, paymentList[i]);
                month = sb.substring(0, 2);
                day = sb.substring(3, 5);
                time = sb.substring(6, 11);
                sb.delete(0, sb.length());
//                        Log.d("TAG", time);
            } // payment Time
        }

        if (money != null && day != null && time != null && place != null && month != null && year != null){
            try{
                JSONObject obj = new JSONObject();
                obj.put("bank", bankName);
                obj.put("time", time);
                obj.put("day", day);
                obj.put("month", month);
                obj.put("year", year);
                obj.put("place", place);
                obj.put("money", money);
                jsonArray.put(obj);
            }catch(JSONException e){
                e.printStackTrace();
            }
        } // time, enterpriseName, won이 null이 아닐 시 값을 넣어줌!

        time = month = day = place = money = null;
        paymentList = internetPayment = null;

        sendJson();
    }

    public void smsJson(){
        jsonArray = new JSONArray();

        for(int i=0;i<bank.length;i++)
            inbox(mActivity, receiveNumber[i], bank[i]);

        sendJson();
    } // 문자 파싱을 한 후 json으로 변경하여 웹에 전송

    public void sendJson(){
        Log.d("json", jsonArray.toString());
        webView.loadUrl("javascript:parser(" + jsonArray.toString() + ")");
        jsonArray = null;
    } // json 전송
}
