package com.sam_chordas.android.stockhawk.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.text.format.DateUtils;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.data.DetailQuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;

/**
 * Created by hania on 23.03.16.
 */
public class DetailStockTaskService extends GcmTaskService{
    private String LOG_TAG = DetailStockTaskService.class.getSimpleName();

    private Context mContext;
    private boolean isUpdate;
    private OkHttpClient client = new OkHttpClient();

    public DetailStockTaskService(){}

    public DetailStockTaskService(Context context) {mContext = context;}

    String fetchData(String url) throws IOException{
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    @Override
    public int onRunTask(TaskParams taskParams) {
        Cursor initQueryCursor;
        if (mContext == null){
            mContext = this;
        }
        StringBuilder urlStringBuilder = new StringBuilder();
        try {
            urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
            urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.historicaldata where symbol "
                    + "in (", "UTF-8"));
        }catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        isUpdate = true;
        String stockInput = taskParams.getExtras().getString("symbol");
        initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Detail.CONTENT_URI_DETAIL, null, "" + DetailQuoteColumns.SYMBOL + "=?", new String[] {stockInput}, null);
        if (initQueryCursor.getCount() == 0 || initQueryCursor == null) { //TODO jezeli sa nie aktualne, lub za stare
            try {
                urlStringBuilder.append(URLEncoder.encode("\"" + stockInput + "\")", "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            urlStringBuilder.append(URLEncoder.encode("and startDate = \"2015-03-23\" and endDate = \""+ Utils.dateToString(new Date()) +"\"")); //TODO start date
            // finalize the URL for the API query.
            urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
                    + "org%2Falltableswithkeys&callback=");
            Log.d(LOG_TAG, urlStringBuilder.toString());
        }
        else if (initQueryCursor.moveToFirst()) {
            String end = initQueryCursor.getString(initQueryCursor.getColumnIndex(DetailQuoteColumns.DATE));
            Date endDate = Utils.longToDate(end);
            initQueryCursor.moveToLast();
            String start = initQueryCursor.getString(initQueryCursor.getColumnIndex(DetailQuoteColumns.DATE));
            Date startDate = Utils.longToDate(start);
            Date today = new Date();
            Date yesterday = Utils.yesterday();
            //Date today = Utils.stringToDate("2016-04-28");
            if (!Utils.dateToString(yesterday).equals(Utils.dateToString(endDate))){
                Log.d(LOG_TAG, "Wczoraj..." + Utils.dateToString(yesterday));
                Log.d(LOG_TAG, "EndDate..." + Utils.dateToString(endDate));
                try {
                    urlStringBuilder.append(URLEncoder.encode("\"" + stockInput + "\")", "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                urlStringBuilder.append(URLEncoder.encode("and startDate = \"" + Utils.dateToString(Utils.nextDay(end)) + "\" and endDate = \"" + Utils.dateToString(yesterday) + "\""));
                // finalize the URL for the API query.
                urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
                        + "org%2Falltableswithkeys&callback=");
                Log.d(LOG_TAG, urlStringBuilder.toString());
                if (startDate.after(Utils.yearBeforeDate(yesterday))){
                    mContext.getContentResolver().delete(QuoteProvider.Detail.CONTENT_URI_DETAIL, DetailQuoteColumns.DATE + " <= ?", new String[] {Utils.dateToLong(Utils.yearBeforeDate(yesterday))});
                }
            }
        }
        else {
            Log.d(LOG_TAG, "Nic nie updatuje...");
        }
        initQueryCursor.close();

        String urlString;
        String getResponse;
        int result = GcmNetworkManager.RESULT_FAILURE;

        if (urlStringBuilder != null){
            urlString = urlStringBuilder.toString();
            try{
                getResponse = fetchData(urlString);
                result = GcmNetworkManager.RESULT_SUCCESS;
                try {
                    //ContentValues contentValues = new ContentValues();
//                        if (isUpdate){
//                            mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
//                                    null, null);
//                        }
                    mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                            Utils.quoteJsonToContentValsDetail(getResponse));
                }catch (RemoteException | OperationApplicationException e) {
                    Log.e(LOG_TAG, "Error applying batch insert", e);
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        return result;
    }
}
