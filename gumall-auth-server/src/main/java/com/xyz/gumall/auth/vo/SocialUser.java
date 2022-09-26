package com.xyz.gumall.auth.vo;

import lombok.Data;

@Data
public class SocialUser {
    private String access_token;
    private String token_type;
    private String scope;
//    private String remind_in;
//    private long expires_in;
//    private String uid;
//    private String isRealName;
}
