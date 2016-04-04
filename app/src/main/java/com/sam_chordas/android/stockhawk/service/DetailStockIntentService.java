package com.sam_chordas.android.stockhawk.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.TaskParams;

/**
 * Created by hania on 24.03.16.
 */
public class DetailStockIntentService extends IntentService {
    private final String LOG_TAG = DetailStockIntentService.class.getName();
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     */

    public DetailStockIntentService() {
        super(DetailStockIntentService.class.getName());
    }

    public DetailStockIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        DetailStockTaskService detailStockTaskService = new DetailStockTaskService(this);
        Bundle args = new Bundle();
        args.putString("symbol", intent.getStringExtra("symbol"));
        detailStockTaskService.onRunTask(new TaskParams(intent.getStringExtra("tag"), args));
    }
}
