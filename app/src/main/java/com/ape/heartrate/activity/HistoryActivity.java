package com.ape.heartrate.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.ape.heartrate.R;
import com.ape.heartrate.adapter.HistoryAdapter;
import com.ape.heartrate.adapter.HistoryViewPagerAdapter;
import com.ape.heartrate.charting.BarChart;
import com.ape.heartrate.charting.ChartData;
import com.ape.heartrate.charting.ColorTemplate;
import com.ape.heartrate.charting.DataSet;
import com.ape.heartrate.charting.Entry;
import com.ape.heartrate.control.HistoryControl;
import com.ape.heartrate.greendao.HistoryEntity;
import com.ape.heartrate.viewpagerindicator.CirclePageIndicator;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private ViewPager m_ViewPager;
    private CirclePageIndicator m_Indicator;
    private LayoutInflater m_Inflater;
    private List<View> m_Views;
    private HistoryControl m_Control;
    private List<HistoryEntity> m_Records;

    private int[] emulateData = new int[]{98};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setViews();
        setValues();
        setListeners();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setViews() {
        m_ViewPager = (ViewPager) findViewById(R.id.history_pager);
        m_Indicator = (CirclePageIndicator) findViewById(R.id.history_indicator);
    }

    private void setValues() {
        m_Control = new HistoryControl(this);
        m_Records = m_Control.getRecords();
        m_Inflater = LayoutInflater.from(this);
        fillViews();
        HistoryViewPagerAdapter m_Adapter = new HistoryViewPagerAdapter(m_Views);
        m_ViewPager.setAdapter(m_Adapter);
        m_Indicator.setViewPager(m_ViewPager);
        m_Indicator.setSnap(true);
        m_Indicator.setCurrentItem(0);
    }

    private void fillViews() {
        m_Views = new ArrayList<View>();
        fillPageOne();
        fillPageTwo();
    }

    private void fillPageOne() {
        View view = m_Inflater.inflate(R.layout.history_pager_one, null);
        BarChart barChart = (BarChart) view.findViewById(R.id.bc_history);
        drawSimpleBarChart(barChart, m_Records);
//        drawSimpleBarChart_Emulate(barChart);
        m_Views.add(view);
    }

    private void fillPageTwo() {
        View view = m_Inflater.inflate(R.layout.history_pager_two, null);
        ListView lv = (ListView) view.findViewById(R.id.lv_history);
        HistoryAdapter adapter = new HistoryAdapter(m_Records, this, R.layout.history_item);
        lv.setAdapter(adapter);
        m_Views.add(view);
    }

    private void setListeners() {

    }

    private void drawSimpleBarChart_Emulate(BarChart mChart) {
        int length = emulateData.length;
        List<HistoryEntity> list = new ArrayList<HistoryEntity>();
        for (int i = 0; i < length; i++) {
            HistoryEntity object = new HistoryEntity();
            object.setRate(emulateData[i]);
            list.add(object);
        }
        drawSimpleBarChart(mChart, list);
    }

//todo    Two big bugs:
//    one is when the values are all the same, the bar will not be drawn.
//    two is when the average is float value, the average text will not be drawn.

    //The configuration maybe need to be put in the xml file or extract them to the constant, but i just put it to the code for convinence
    private void drawSimpleBarChart(BarChart mChart, List<HistoryEntity> m_Records) {
        mChart.setTouchEnabled(false);

        ColorTemplate ct = new ColorTemplate();
        ct.addDataSetColors(new int[]{R.color.bar_fill_color}, this);
        mChart.setColorTemplate(ct);

        mChart.setDrawYValues(false);
        mChart.set3DEnabled(false);

        //mChart.setYRange(50, 140);
        mChart.setYLegendCount(5);
        mChart.setYLegendTextSize(21);
        mChart.setYLegendTextColor(Color.WHITE);
        mChart.setRoundedYLegend(true);

        mChart.setGridColor(Color.WHITE);
        mChart.setGridWidth(2);

        int times = m_Records == null ? 0 : m_Records.size();
        mChart.setConclude(getString(R.string.history_conclude, times));
        mChart.setConcludeTextColor(Color.WHITE);
        mChart.setConcludeTextSize(23);

        mChart.setAverageLineColor(0xFF943868);
        mChart.setAverageTextSize(21);
        mChart.setAverageTextColor(Color.WHITE);

        mChart.setStartAtZero(false);
        mChart.setBarSpace(50f);
        mChart.setGraphOffsets(29, 18, 18, 46);

//        mChart.setEmptyInfoTextColor(Color.WHITE);
//        mChart.setEmptyInfoTextSize(32);
        mChart.setEmptyInfo(null);

        int size = m_Records == null ? 0 : m_Records.size();
        ArrayList<String> xVals = new ArrayList<String>();
        ArrayList<Entry> list = new ArrayList<Entry>(size);
        for (int i = 0; i < size; i++) {
            xVals.add((i) + "");
            list.add(new Entry(m_Records.get(i).getRate(), i));
        }

        DataSet set1 = new DataSet(list, 0);
        ArrayList<DataSet> dataSets = new ArrayList<DataSet>();
        dataSets.add(set1);

        ChartData data = new ChartData(xVals, dataSets);
        mChart.setData(data);
        mChart.invalidate();
    }
}
