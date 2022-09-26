package com.xyz.gumall.thirdparty;

import com.aliyun.oss.OSSClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GumallThirdPartyApplicationTests {

	@Test
	public void contextLoads() {
	}

	@Autowired
	private OSSClient ossClient;

	@Test
	public void testUpload2() throws FileNotFoundException {
		// Endpoint以杭州為例，其它Region請按實際情況填寫。
//        String endpoint = "oss-cn-hongkong.aliyuncs.com";
//        // 阿里雲主帳號AccessKey擁有所有API的存取權限，風險很高。強烈建議您建立並使用RAM帳號進行API訪問或日常運維，請登入 https://ram.console.aliyun.com 建立RAM帳號。
////        String accessKeyId = "<yourAccessKeyId>";
////        String accessKeySecret = "<yourAccessKeySecret>";
//        String accessKeyId = "yourAccessKeyId";
//        String accessKeySecret = "yourAccessKeySecret";
		// 建立OSSClient執行個體。
//        OSSClient ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);
		// 上傳檔案。<yourLocalFile>由本地檔案路徑加檔案名包括尾碼組成，例如/users/local/myfile.txt。
		InputStream inputStream = new FileInputStream("Test2.png");
		ossClient.putObject("yourbucket", "Test2.png", inputStream);
		// 關閉OSSClient。
		ossClient.shutdown();
		System.out.println("upload success..");
	}

	@Test
	public void testUpload() {
		// Endpoint以杭州為例，其它Region請按實際情況填寫。
		String endpoint = "oss-cn-hongkong.aliyuncs.com";
		// 阿里雲主帳號AccessKey擁有所有API的存取權限，風險很高。強烈建議您建立並使用RAM帳號進行API訪問或日常運維，請登入 https://ram.console.aliyun.com 建立RAM帳號。
//        String accessKeyId = "<yourAccessKeyId>";
//        String accessKeySecret = "<yourAccessKeySecret>";
		String accessKeyId = "yourAccessKeyId";
		String accessKeySecret = "yourAccessKeySecret";
		// 建立OSSClient執行個體。
		OSSClient ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);
		// 上傳檔案。<yourLocalFile>由本地檔案路徑加檔案名包括尾碼組成，例如/users/local/myfile.txt。
		ossClient.putObject("yourbucket", "Test2.png", new File("Test2.png"));
		// 關閉OSSClient。
		ossClient.shutdown();
		System.out.println("upload success..");
	}
}
