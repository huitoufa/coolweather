package com.coolweather.android;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.coolweather.android.db.City;
import com.coolweather.android.db.County;
import com.coolweather.android.db.Province;
import com.coolweather.android.util.Utility;
import com.coolweather.android.util.HttpUtil;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by zhaoyh on 7/27 0027.
 */

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;

    public static final String api = "http://guolin.tech/api/china/";

    public static final String TYPE_PROVINCE = "province";
    public static final String TYPE_CITY = "city";
    public static final String TYPE_COUNTY = "county";


    private ProgressDialog progressDialog;
    private TextView titletext;
    private Button backbutton;
    private ListView listview;

    private ArrayAdapter<String> adapter;

    private List<String> dataList = new ArrayList<String>();

    /**
     * 省列表
     */
    private List<Province> provinceList;

    /**
     * 市列表
     */
    private List<City> cityList;

    /**
     * 县列表
     */
    private List<County> countyList;


    /**
     * 选中的省
     */
    private Province selectedProvince;

    /**
     * 选中的市
     */
    private City selectedCity;

    /**
     * 当前选中的级别
     */
    private int currentLevel;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        this.listview = (ListView) view.findViewById(R.id.list_view);
        this.backbutton = (Button) view.findViewById(R.id.back_button);
        this.titletext = (TextView) view.findViewById(R.id.title_text);

        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);

        listview.setAdapter(adapter);

        return view;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE) {
                    selectedProvince = provinceList.get(position);
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    selectedCity = cityList.get(position);
                    queryCounties();
                }
            }
        });

        backbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLevel == LEVEL_COUNTY) {
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    queryProvinces();
                }
            }
        });
        queryProvinces();
    }


    /**
     * 根据传入的地址和类型，从服务器上查询省市县数据
     *
     * @param address
     * @param type
     */
    private void queryFromServer(String address, final String type) {
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                /**
                 * 通过  runOnUiThread（） 方法回到主线程中处理逻辑
                 */
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                if (TYPE_PROVINCE.equals(type)) {
                    result = Utility.handleProvinceResponse(responseText);
                } else if (TYPE_CITY.equals(type)) {
                    result = Utility.handleCityResponse(responseText, selectedProvince.getId());
                } else if (TYPE_COUNTY.equals(type)) {
                    result = Utility.handleCountyResponse(responseText, selectedCity.getId());
                }
                if (result) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if (TYPE_PROVINCE.equals(type)) {
                                queryProvinces();
                            } else if (TYPE_CITY.equals(type)) {
                                queryCities();
                            } else if (TYPE_COUNTY.equals(type)) {
                                queryCounties();
                            }


                            new AlertDialog.Builder(getActivity())
                                    .setTitle("成功")
                                    .setMessage("从网络获取成功")
                                    .setPositiveButton("确定", null)
                                    .show();
                        }
                    });
                }
            }
    });
}


    /**
     * 查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器查询
     */
    private void queryProvinces() {
        titletext.setText("中国");
        backbutton.setVisibility(View.GONE);
        provinceList = DataSupport.findAll(Province.class);
        if (provinceList.size() > 0) {
            dataList.clear();
            for (Province p : provinceList) {
                dataList.add(p.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listview.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        } else {
            String address = api;
            queryFromServer(address, TYPE_PROVINCE);
        }
    }


    /**
     * 查询选中省内所有的市，优先从数据库查询，如果没有查询到再去服务器查询
     */
    private void queryCities() {
        titletext.setText(selectedProvince.getProvinceName());
        backbutton.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceId = ?", String.valueOf(selectedProvince.getId())).find(City.class);
        if (cityList.size() > 0) {
            dataList.clear();
            for (City city : cityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listview.setSelection(0);
            currentLevel = LEVEL_CITY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = api + provinceCode;
            queryFromServer(address, TYPE_CITY);
        }
    }

    /**
     * 查询市内所有的县，优先从数据库查讯，如果没有查询到再去服务器上查询
     */
    private void queryCounties() {
        titletext.setText(selectedCity.getCityName());
        backbutton.setVisibility(View.VISIBLE);
        countyList = DataSupport.where("cityId = ?", String.valueOf(selectedCity.getId())).find(County.class);
        if (countyList.size() > 0) {
            dataList.clear();
            for (County county : countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetInvalidated();
            listview.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        } else {
            String address = api + selectedProvince.getProvinceCode() + "/" + selectedCity.getCityCode();
            queryFromServer(address, TYPE_COUNTY);
        }
    }


    /**
     * 显示进度对话框
     */
    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载....");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    /**
     * 关闭进度对话框
     */
    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }
}