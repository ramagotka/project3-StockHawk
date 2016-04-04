package com.sam_chordas.android.stockhawk.widget;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.Build;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;

/**
 * Created by hania on 29.03.16.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class StockWidgetRemoteViewsService extends RemoteViewsService {

    public final String LOG_TAG = StockWidgetRemoteViewsService.class.getName();
    private static final String[] STOCK_COLUMNS = {
            QuoteColumns.SYMBOL,
            QuoteColumns.CHANGE,
            QuoteColumns._ID,
            QuoteColumns.BIDPRICE
    };

    static final int INDEX_SYMBOL = 0;
    static final int INDEX_CHANGE = 1;
    static final int INDEX_ID = 2;
    static final int INDEX_BIDPRICE = 3;

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private Cursor data = null;

            @Override
            public void onCreate() {
            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                }

                final long identityToken = Binder.clearCallingIdentity();
                data = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                        STOCK_COLUMNS,
                        null,
                        null,
                        null);
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    return null;
                }
                RemoteViews views = new RemoteViews(getPackageName(),
                        R.layout.widget_collection_item);
                String symbol = data.getString(INDEX_SYMBOL);
                views.setTextViewText(R.id.stock_symbol, symbol);
                String change = data.getString(INDEX_CHANGE);
                views.setTextViewText(R.id.change, change);
                String bidprice = data.getString(INDEX_BIDPRICE);
                views.setTextViewText(R.id.bid_price, bidprice);
                // TODO?

                final Intent fillInIntent = new Intent();
                fillInIntent.putExtra(getString(R.string.intent_extra), symbol);
                views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);

                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_collection_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (data.moveToPosition(position))
                    return data.getLong(INDEX_ID);
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
