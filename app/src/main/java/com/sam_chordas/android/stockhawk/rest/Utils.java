package com.sam_chordas.android.stockhawk.rest;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.net.ConnectivityManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.DetailQuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.widget.StockWidgetProvider;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sam_chordas on 10/8/15.
 */
public class Utils {

    private static String LOG_TAG = Utils.class.getSimpleName();

    public static boolean showPercent = true;

    public static ArrayList quoteJsonToContentVals(String JSON){
        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
        JSONObject jsonObject = null;
        JSONArray resultsArray = null;
        try{
            jsonObject = new JSONObject(JSON);
            if (jsonObject != null && jsonObject.length() != 0){
                jsonObject = jsonObject.getJSONObject("query");
                int count = Integer.parseInt(jsonObject.getString("count"));
                if (count == 1){
                    jsonObject = jsonObject.getJSONObject("results")
                            .getJSONObject("quote");
                    batchOperations.add(buildBatchOperation(jsonObject));
                } else{
                    resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

                    if (resultsArray != null && resultsArray.length() != 0){
                        for (int i = 0; i < resultsArray.length(); i++){
                            jsonObject = resultsArray.getJSONObject(i);
                            batchOperations.add(buildBatchOperation(jsonObject));
                        }
                    }
                }
            }
        } catch (JSONException e){
            Log.e(LOG_TAG, "String to JSON failed: " + e);
        }
        return batchOperations;
    }

    public static ArrayList quoteJsonToContentValsDetail (String JSON){
        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
        JSONObject jsonObject = null;
        JSONArray resultsArray = null;

        try{
            jsonObject = new JSONObject(JSON);
            if (jsonObject != null && jsonObject.length() != 0){
                jsonObject = jsonObject.getJSONObject("query");
                int count = Integer.parseInt(jsonObject.getString("count"));
                if (count == 1){
                    jsonObject = jsonObject.getJSONObject("results")
                            .getJSONObject("quote");
                    batchOperations.add(buildBatchOperationDetail(jsonObject));
                }
                else{
                    resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

                    if (resultsArray != null && resultsArray.length() != 0){
                        for (int i = 0; i < resultsArray.length(); i++){
                            jsonObject = resultsArray.getJSONObject(i);
                            batchOperations.add(buildBatchOperationDetail(jsonObject));
                        }
                    }
                }
            }
        } catch (JSONException e){
            Log.e(LOG_TAG, "String to JSON failed: " + e);
        }
        return batchOperations;
    }

    public static ContentProviderOperation buildBatchOperationDetail(JSONObject jsonObject){
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                QuoteProvider.Detail.CONTENT_URI_DETAIL);
        try {
            builder.withValue(DetailQuoteColumns.SYMBOL, jsonObject.getString("Symbol"));
            builder.withValue(DetailQuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString("Close")).replace(",", "."));
            builder.withValue(DetailQuoteColumns.DATE,stringToLong(jsonObject.getString("Date")));

        } catch (JSONException e){
            e.printStackTrace();
        }
        return builder.build();
    }

    public static String truncateBidPrice(String bidPrice){
        bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
        return bidPrice;
    }

    public static String truncateChange(String change, boolean isPercentChange){
        String weight = change.substring(0,1);
        String ampersand = "";
        if (isPercentChange){
            ampersand = change.substring(change.length() - 1, change.length());
            change = change.substring(0, change.length() - 1);
        }
        change = change.substring(1, change.length());
        if (change.equals("ul") || change.equals("ull")){
            return "0,00";
        }
        double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
        change = String.format("%.2f", round);
        StringBuffer changeBuffer = new StringBuffer(change);
        changeBuffer.insert(0, weight);
        changeBuffer.append(ampersand);
        change = changeBuffer.toString();
        return change;
    }

    public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject){
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                QuoteProvider.Quotes.CONTENT_URI);
        try {
            String change = jsonObject.getString("Change");
            builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString("symbol"));
            builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString("Bid")));
            builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
                    jsonObject.getString("ChangeinPercent"), true));
            builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
            builder.withValue(QuoteColumns.ISCURRENT, 1);
            if (change.charAt(0) == '-'){
                builder.withValue(QuoteColumns.ISUP, 0);
            }else{
                builder.withValue(QuoteColumns.ISUP, 1);
            }

        } catch (JSONException e){
            e.printStackTrace();
        }
        return builder.build();
    }

    public static Date stringToDate(String s){
        ParsePosition pos = new ParsePosition(0);
        SimpleDateFormat simpledateformat = new SimpleDateFormat("yyyy-MM-dd");
        Date stringDate = simpledateformat.parse(s, pos);
        return stringDate;
    }

    public static String dateToString(Date date){
        SimpleDateFormat simpledateformat = new SimpleDateFormat("yyyy-MM-dd");
        return simpledateformat.format(date);
    }

    public static String stringToLong(String s){
        Date stringDate = stringToDate(s);
        long dateLong = stringDate.getTime();
        return String.valueOf(dateLong);
    }

    public static Date longToDate(String l){
        long millisecond = Long.parseLong(l);
        return new Date(millisecond);
    }

    public static Date yesterday(){
        return new Date(System.currentTimeMillis()-24*60*60*1000);
    }

    public static Date nextDay(String s){
        long millisecond = Long.parseLong(s);
        return new Date(millisecond + 24*60*60*1000);
    }

    public static Date yearBefore(String s){
        long millisecond = Long.parseLong(s);
        return new Date(millisecond - 24*60*60*1000*365L);
    }

    public static Date yearBeforeDate(Date date){
        long millisecond = date.getTime();
        return new Date(millisecond - 24*60*60*1000*365L);
    }

    public static String dateToLong(Date date){
        long millisecond = date.getTime();
        return String.valueOf(millisecond);
    }

    public static String longYearBefore(){
        Date date = new Date(System.currentTimeMillis()-24*60*60*1000*365L);
        return String.valueOf(date.getTime());
    }

    public static String longMonthBefore(){
        Date date = new Date(System.currentTimeMillis()-24*60*60*1000*30L);
        return String.valueOf(date.getTime());
    }

    public static String long3MonthsBefore(){
        Date date = new Date(System.currentTimeMillis()-24*60*60*1000*90L);
        return String.valueOf(date.getTime());
    }

    public static boolean notNull(String JSON, Context context){
        try{
            JSONObject jsonObject = new JSONObject(JSON);
            if (jsonObject != null && jsonObject.length() != 0) {
                jsonObject = jsonObject.getJSONObject("query");
                int count = Integer.parseInt(jsonObject.getString("count"));
                if (count == 1) {
                    jsonObject = jsonObject.getJSONObject("results")
                            .getJSONObject("quote");
                    if (jsonObject.getString("Bid").equals("null")){
                        return false;
                    }
                }
            }
        }
        catch (JSONException e){
            Log.e(LOG_TAG, "String to JSON failed: " + e);
        }
        return true;
    }

    public static boolean isOnline(Context context){
        try
        {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            return cm.getActiveNetworkInfo().isConnectedOrConnecting();
        }
        catch (Exception e)
        {
            return false;
        }
    }

    public static String[] symbolList(String JSON){
        JSONObject jsonObject = null;
        JSONArray resultsArray = null;
        String[] symbols = null;

        try{
            jsonObject = new JSONObject(JSON);
            if (jsonObject != null && jsonObject.length() != 0){
                jsonObject = jsonObject.getJSONObject("query");
                int count = Integer.parseInt(jsonObject.getString("count"));
                if (count == 1){
                    jsonObject = jsonObject.getJSONObject("results")
                            .getJSONObject("quote");
                    symbols = new String[] {jsonObject.getString(QuoteColumns.SYMBOL)};
                }
                else{
                    resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");
                    if (resultsArray != null && resultsArray.length() != 0){
                        symbols = new String[resultsArray.length()];
                        for (int i = 0; i < resultsArray.length(); i++){
                            jsonObject = resultsArray.getJSONObject(i);
                            symbols[i] = jsonObject.getString(QuoteColumns.SYMBOL);
                        }
                    }
                }
            }
        } catch (JSONException e){
            Log.e(LOG_TAG, "String to JSON failed: " + e);
        }
        return symbols;
    }

    public static void updateWidgets(Context context){
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisAppWidget = new ComponentName(context.getPackageName(), StockWidgetProvider.class.getName());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list);
    }
}
