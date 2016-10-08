package com.ape.heartrate.charting;

public interface OnChartValueSelectedListener {

    public void onValuesSelected(Entry[] values, Highlight[] highlights);

    public void onNothingSelected();
}
