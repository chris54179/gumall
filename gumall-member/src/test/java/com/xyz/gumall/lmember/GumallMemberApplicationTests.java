package com.xyz.gumall.lmember;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.Md5Crypt;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

//@SpringBootTest
public class GumallMemberApplicationTests {

	@Test
	public void contextLoads() {
		String s = DigestUtils.md5Hex("123456");
//		System.out.println(s);

//		String s1 = Md5Crypt.md5Crypt("123456".getBytes(), "$1$qq");
//		System.out.println(s1);

		BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
		String encode = passwordEncoder.encode("123456");

		boolean matches = passwordEncoder.matches("123456", "$2a$10$Zq0uAwmvnAmLvIUay8Es9uH62mpS6ZcE/X8gbfMWFr3Hq992MU/ye");

		System.out.println(encode+"="+matches);
	}

}
