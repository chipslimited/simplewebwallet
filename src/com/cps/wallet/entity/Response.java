package com.cps.wallet.entity;

import java.io.Serializable;

/**
 * Created by fengss on 2018/3/16.
 */
public class Response<T> implements Serializable {
    private Integer errorCode;
    private String errorMsg;
    private T model;


    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public T getModel() {
        return model;
    }

    public void setModel(T model) {
        this.model = model;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }
}
