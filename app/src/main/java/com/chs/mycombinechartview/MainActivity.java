package com.chs.mycombinechartview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CombineChart combineChart = (CombineChart) findViewById(R.id.cb_chart);
        List<BarChartBean> data = new ArrayList<>();
        data.add(new BarChartBean("7/1",1003,500,600));
        data.add(new BarChartBean("7/2",890,456,123));
        data.add(new BarChartBean("7/3",456,741,654));
        data.add(new BarChartBean("7/4",258,951,12));
        data.add(new BarChartBean("7/5",863,45,99));
        data.add(new BarChartBean("7/6",357,235,456));
        data.add(new BarChartBean("7/7",452,321,55));
        data.add(new BarChartBean("7/8",654,555,666));
        data.add(new BarChartBean("7/9",321,333,222));
        data.add(new BarChartBean("7/10",846,111,444));
        List<Float> winds = new ArrayList<>();
        winds.add(5f);
        winds.add(6f);
        winds.add(8f);
        winds.add(9f);
        winds.add(4f);
        winds.add(7f);
        winds.add(3f);
        winds.add(1f);
        winds.add(5.5f);
        winds.add(4.8f);
        List<Float> hum = new ArrayList<>();
        hum.add(50f);
        hum.add(60f);
        hum.add(80f);
        hum.add(90f);
        hum.add(40f);
        hum.add(70f);
        hum.add(30f);
        hum.add(10f);
        hum.add(55f);
        hum.add(48f);
        List<Float> tem = new ArrayList<>();
        tem.add(38f);
        tem.add(36f);
        tem.add(27f);
        tem.add(22f);
        tem.add(15f);
        tem.add(-20f);
        tem.add(-30f);
        tem.add(-40f);
        tem.add(10f);
        tem.add(18f);

        combineChart.setItems(data,winds,hum,tem);
        combineChart.setOnItemBarClickListener(new CombineChart.OnItemBarClickListener() {
            @Override
            public void onClick(int position) {
                Toast.makeText(MainActivity.this,"点击了："+position,Toast.LENGTH_SHORT).show();
            }
        });
    }
}
