package com.xyz.gumall.gateway.security;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.*;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serializable;
import java.util.Collection;

@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public class MyAccountAuthentication extends AbstractAuthenticationToken implements Serializable {
    private static final long serialVersionUID = 1L;
    private Object credentials;
    private Object principal;



//    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public MyAccountAuthentication(Object credentials, Object principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.credentials = credentials;
        this.principal = principal;
    }

    @Override
    public Object getCredentials() {
        return this.credentials;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }

}
