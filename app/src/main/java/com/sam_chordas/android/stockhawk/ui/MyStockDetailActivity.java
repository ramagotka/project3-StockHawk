package com.sam_chordas.android.stockhawk.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.db.chart.model.LineSet;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.charts.LineChart;
import com.sam_chordas.android.stockhawk.data.DetailQuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.DetailStockIntentService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;

import static android.util.Log.e;

/**
 * Created by hania on 22.03.16.
 */
public class MyStockDetailActivity extends AppCompatActivity {

    private final String LOG_TAG = MyStockDetailActivity.class.getName();
   // private Intent mServiceIntent;
    boolean isConnected;
    private String symbol;
    private LineChart[] mLineChart;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stock_detail);

        Intent intent = getIntent();
        symbol = intent.getStringExtra(getString(R.string.intent_extra));

        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

//        mServiceIntent = new Intent(this, DetailStockIntentService.class);
//        if (savedInstanceState == null){
//            // Run the initialize task service so that some stocks appear upon an empty database
//            mServiceIntent.putExtra("symbol",  symbol);
//            if (isConnected){
//                startService(mServiceIntent);
//            } else{
//                Toast.makeText(this, getString(R.string.network_toast), Toast.LENGTH_SHORT).show();
//            }
//        }
        mLineChart = new LineChart[3];
        Cursor cursor = getContentResolver().query(QuoteProvider.Detail.CONTENT_URI_DETAIL, null, "" + DetailQuoteColumns.SYMBOL + "=?", new String[] {symbol}, null);
        if (cursor.moveToFirst()){
            String end = cursor.getString(cursor.getColumnIndex(DetailQuoteColumns.DATE));
            Date endDate = Utils.longToDate(end);
            Date yesterday = Utils.yesterday();
            if (yesterday.after(endDate)){
                new FetchStockTask().execute(symbol);
            }
            else {
                drowChart(Utils.longYearBefore(), R.id.chart3, 2, R.id.card3);
                drowChart(Utils.long3MonthsBefore(), R.id.chart2, 1, R.id.card2);
                drowChart(Utils.longMonthBefore(), R.id.chart1, 0, R.id.card1);
            }
        }
        else {
            new FetchStockTask().execute(symbol);
        }
        cursor.close();

        TextView textView = (TextView) findViewById(R.id.chart1_title);
        textView.setText(symbol);
    }

    public void drowChart(String from, int id, int which, int id_card){
        Cursor cursor = getContentResolver().query(QuoteProvider.Detail.CONTENT_URI_DETAIL, null, ""
                + DetailQuoteColumns.SYMBOL + "=? AND " + DetailQuoteColumns.DATE + ">=?", new String[] {symbol, from}, null);
        int count = cursor.getCount();
        Log.d(LOG_TAG, "count = " + count);

        if (count > 0){
            String[] arrayLabels = new String[count];
            float[] arrayValues = new float[count];
            int i = 0;
            while (cursor.moveToNext()){
                arrayLabels[count - i - 1] = cursor.getString(cursor.getColumnIndex(DetailQuoteColumns.DATE)); //TODO to sa longi wiec bezsensu trzeba to na cos przeksztalcic?
                arrayValues[count - i - 1] =  Float.parseFloat(cursor.getString(cursor.getColumnIndex(DetailQuoteColumns.BIDPRICE)));
                i++;//TODO: na ostatnim miejscu trzeba dodac dzisiejsza cene...
            }
            if(mLineChart[which] == null)
            {
                mLineChart[which] = new LineChart((CardView) this.findViewById(id_card), this, id);
            }
            mLineChart[which].init(arrayValues, arrayLabels);
        }
        cursor.close();
    }

    private class FetchStockTask extends AsyncTask<String, Void, String> {
        String LOG_TAG = FetchStockTask.class.getName();

        @Override
        protected String doInBackground(String... params) {
            Cursor initQueryCursor;
            String symbol = params[0];
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String json = null;
            String result = null;
            StringBuilder urlStringBuilder = new StringBuilder();

            try {
                Date yesterday = Utils.yesterday();
                initQueryCursor = getContentResolver().query(QuoteProvider.Detail.CONTENT_URI_DETAIL, null, "" + DetailQuoteColumns.SYMBOL + "=?", new String[] {symbol}, null);
                if (initQueryCursor.getCount() == 0 || initQueryCursor == null) { //TODO jezeli sa nie aktualne, lub za stare
                    urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
                    urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.historicaldata where symbol "
                            + "in (", "UTF-8"));
                    try {
                        urlStringBuilder.append(URLEncoder.encode("\"" + symbol + "\")", "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    urlStringBuilder.append(URLEncoder.encode("and startDate = \"" +
                            Utils.dateToString(Utils.yearBeforeDate(yesterday)) +
                            "\" and endDate = \""+ Utils.dateToString(new Date()) +"\""));
                    // finalize the URL for the API query.
                    urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
                            + "org%2Falltableswithkeys&callback=");
                    Log.d(LOG_TAG, "pierwszy if" + urlStringBuilder.toString());
                }
                else if (initQueryCursor.moveToFirst()) {
                    String end = initQueryCursor.getString(initQueryCursor.getColumnIndex(DetailQuoteColumns.DATE));
                    Date endDate = Utils.longToDate(end);
                    initQueryCursor.moveToLast();
                    String start = initQueryCursor.getString(initQueryCursor.getColumnIndex(DetailQuoteColumns.DATE));
                    Date startDate = Utils.longToDate(start);
                    Date today = new Date();
                    //Date today = Utils.stringToDate("2016-04-28");
                    if (yesterday.after(endDate)){
                        Log.d(LOG_TAG, "tutaj?");
                        Log.d(LOG_TAG, "wczoraj..." + Utils.dateToString(yesterday));
                        Log.d(LOG_TAG, "end date..." + Utils.dateToString(endDate));
                        urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
                        urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.historicaldata where symbol "
                                + "in (", "UTF-8"));
                        try {
                            urlStringBuilder.append(URLEncoder.encode("\"" + symbol + "\")", "UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        urlStringBuilder.append(URLEncoder.encode("and startDate = \"" + Utils.dateToString(Utils.nextDay(end)) + "\" and endDate = \"" + Utils.dateToString(yesterday) + "\""));
                        // finalize the URL for the API query.
                        urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
                                + "org%2Falltableswithkeys&callback=");
                        if (startDate.after(Utils.yearBeforeDate(yesterday))){
                            getContentResolver().delete(QuoteProvider.Detail.CONTENT_URI_DETAIL, DetailQuoteColumns.DATE + " <= ?", new String[] {Utils.dateToLong(Utils.yearBeforeDate(yesterday))});
                        }
                    }
                    Log.d(LOG_TAG, "drugi if" + urlStringBuilder.toString());
                }
                else {
                    Log.d(LOG_TAG, "problemmmm...");
                }
                initQueryCursor.close();
                if (urlStringBuilder != null) {
                    URL url = new URL(urlStringBuilder.toString());
                    // Create the request to OpenWeatherMap, and open the connection
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();

                    InputStream inputStream = urlConnection.getInputStream();
                    StringBuffer buffer = new StringBuffer();
                    if (inputStream == null) {
                        // Nothing to do.
                        return null;
                    }
                    reader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        buffer.append(line + "\n");
                    }

                    if (buffer.length() == 0) {
                        return null;
                    }
                    json = buffer.toString();
                    getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                            Utils.quoteJsonToContentValsDetail(json));
                }
                result = symbol;
            }
            catch (Exception e)
            {
                Handler mainThread = new Handler(Looper.getMainLooper());
                mainThread.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.async_tasc_toast), Toast.LENGTH_LONG);
                        toast.show();
                    }
                });
                Log.e(LOG_TAG, e.toString());
            }
            finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s != null){
                drowChart(Utils.longYearBefore(), R.id.chart3, 2, R.id.card3);
                drowChart(Utils.long3MonthsBefore(), R.id.chart2, 1, R.id.card2);
                drowChart(Utils.longMonthBefore(), R.id.chart1, 0, R.id.card1);
            }
            else {
                Toast.makeText(getApplicationContext(), getString(R.string.async_tasc_toast) ,Toast.LENGTH_LONG);
            }
        }
    }

    public void onRadioButtonClicked(View view){
        boolean checked = ((RadioButton) view).isChecked();
        switch(view.getId()) {
            case R.id.radio_month:
            {
                if (checked) {
                    findViewById(R.id.card1).setVisibility(View.VISIBLE);
                    findViewById(R.id.card2).setVisibility(View.INVISIBLE);
                    findViewById(R.id.card3).setVisibility(View.INVISIBLE);
                    break;
                }
            }
            case R.id.radio_3months:
            {
                if (checked) {
                    findViewById(R.id.card1).setVisibility(View.INVISIBLE);
                    findViewById(R.id.card2).setVisibility(View.VISIBLE);
                    findViewById(R.id.card3).setVisibility(View.INVISIBLE);
                    break;
                }
            }
            case R.id.radio_year:
            {
                if (checked) {
                    findViewById(R.id.card1).setVisibility(View.INVISIBLE);
                    findViewById(R.id.card2).setVisibility(View.INVISIBLE);
                    findViewById(R.id.card3).setVisibility(View.VISIBLE);
                    break;
                }
            }
        }
    }

}
