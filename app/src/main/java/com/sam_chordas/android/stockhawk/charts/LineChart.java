package com.sam_chordas.android.stockhawk.charts;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.support.v7.widget.CardView;
import android.util.Log;

import com.db.chart.Tools;
import com.db.chart.model.LineSet;
import com.db.chart.view.AxisController;
import com.db.chart.view.LineChartView;
import com.db.chart.view.animation.Animation;
import com.sam_chordas.android.stockhawk.R;

/**
 * Created by hania on 25.03.16.
 */
public class LineChart {

    private final LineChartView mChart;
    Context mContext;
    Animation mAnim;

    private String[] mLabels= {};
    private float[] mValues = {};

    public LineChart(CardView card, Context context, int id){

        mContext = context;
        mChart = (LineChartView) card.findViewById(id);
    }

    public void show(Runnable action) {

        LineSet dataset = new LineSet(mLabels, mValues);
        dataset.setColor(Color.parseColor("#53c1bd"))
                .setFill(Color.parseColor("#3d6c73"))
                .setGradientFill(new int[]{Color.parseColor("#364d5a"), Color.parseColor("#3f7178")}, null);

        mChart.reset();
        mChart.addData(dataset);

        mChart.setBorderSpacing(1)
                .setXLabels(AxisController.LabelPosition.NONE)
                .setYLabels(AxisController.LabelPosition.NONE)
                .setXAxis(false)
                .setYAxis(false)
                .setBorderSpacing(Tools.fromDpToPx(5));

        mAnim = new Animation().setEndAction(action);

        mChart.show(mAnim);
    }

    public void init(float[] values, String[] labels){
        mValues = values;
        mLabels = labels;
        show(unlockAction);
    }

    private final Runnable unlockAction =  new Runnable() {
        @Override
        public void run() {
            new Handler().postDelayed(new Runnable() {
                public void run() {
                }
            }, 500);
        }
    };

}
