package com.xyz.gumall.member.exception;

public class UsernameExistException extends RuntimeException {
    public UsernameExistException() {
        super("该用户名已存在");
    }
}
