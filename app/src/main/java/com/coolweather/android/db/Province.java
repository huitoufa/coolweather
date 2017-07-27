package com.coolweather.android.db;

import org.litepal.crud.DataSupport;

/**
 * Created by zhaoyh on 7/27 0027.
 */

/**
 * Litepal 中每个实体类都必须继承自  DataSupport
 */
public class Province extends DataSupport{
    private int id;
    private String provinceName;
    private int provinceCode;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    public int getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(int provinceCode) {
        this.provinceCode = provinceCode;
    }
}
