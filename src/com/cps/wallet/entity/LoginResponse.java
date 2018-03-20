package com.cps.wallet.entity;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by fengss on 2018/3/16.
 */
public class LoginResponse {
    private Integer userId;

    private String sessionId;

    private Map<String,BigInteger> map = new HashMap<String,BigInteger>();

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Map<String, BigInteger> getMap() {
        return map;
    }

    public void setMap(Map<String, BigInteger> map) {
        this.map = map;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
