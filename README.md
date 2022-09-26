# 商城項目介紹
基於 SpringCloud + SpringCloudAlibaba + MyBatis-Plus + Redis + SpringSession + RabbitMQ + Nginx + ES 實現，Nginx 實現反向代理和動靜分離，採用 Docker、Kubernetes 部署。前台商城系統包括：使用者登錄、註冊、商品搜尋、商品詳情、購物車、下訂單流程、秒殺活動等模組。後台管理系統包括：系統管理、商品系統、優惠行銷、庫存系統、訂單系統、使用者系統、內容管理等七大模組。
# 技術涵蓋
微服務架構+分布式+全端+叢集+部署+自動化維運+可視化 CICD。項目由業務叢集系統+後台管理系統構成，包含前後分離全端開發、Restful介面、資料校驗、閘道器、註冊發現、組態中心、熔斷、限流、降級、鏈路追蹤、性能監控、壓力測試、系統預警、叢集部署、持續整合、持續部署等。
# 
## 架構圖
![](https://github.com/chris54179/gumall/blob/master/IMG/Architecture.jpg)
![](https://github.com/chris54179/gumall/blob/master/IMG/Architecture2.png)
```
gumall
├── gumall-common -- 公共模塊
├── renren-generator -- 程式碼生成器（人人开源）
├── renren-fast -- 後台管理系統（人人开源）
├── gumall-auth-server -- 認證中心
├── gumall-cart -- 購物車服務
├── gumall-coupon -- 優惠券服務
├── gumall-member -- 會員服務
├── gumall-gateway -- 網關服務
├── gumall-order -- 訂單服務
├── gumall-product -- 商品服務
├── gumall-search -- 檢索服務
├── gumall-seckill -- 秒殺服務
├── gumall-third-party -- 第三方服務
└── gumall-ware -- 倉儲服務
```
## 項目演示
### 前台商城系統
#### 首頁
![](https://github.com/chris54179/gumall/blob/master/IMG/p1.png)
#### 商品檢索
使用Elasticsearch實現分類、搜尋欄檢索商品。
![](https://github.com/chris54179/gumall/blob/master/IMG/p2.png)
#### 認證
可使用帳號密碼登入、第三方社交授權（OAuth2.0），第三方支援GitHub。 實現主域名單點登入。

![](https://github.com/chris54179/gumall/blob/master/IMG/p3.png)

#### 商品詳情
秒殺商品緩存到redis，使用分布式鎖扣減庫存，並設置隨機碼防止惡意請求。秒殺成功請求進入隊列創建訂單。使用Sentinel做流控。
![](https://github.com/chris54179/gumall/blob/master/IMG/p4.png)
#### 購物車
購物車分為未登入狀態和登入狀態。未登入時，購物車會在redis中保存一段時間。登入後，將購物車資料和未登入購物車進行合併。
![](https://github.com/chris54179/gumall/blob/master/IMG/p5.png)
#### 結算頁
防重令牌保證提交訂單接口冪等性。
![](https://github.com/chris54179/gumall/blob/master/IMG/p6.png)
#### 支付
支付寶沙箱環境模擬支付
![](https://github.com/chris54179/gumall/blob/master/IMG/p7.png)
![](https://github.com/chris54179/gumall/blob/master/IMG/p8.png)
![](https://github.com/chris54179/gumall/blob/master/IMG/p9.png)
![](https://github.com/chris54179/gumall/blob/master/IMG/p10.png)
#### 訂單
使用RabbitMQ解決下訂單鎖庫存後發生異常，保證庫存正確解鎖。訂單過期自動取消。

![](https://github.com/chris54179/gumall/blob/master/IMG/p11.png)
# KubeSphere 容器平台
#### 服務
![](https://github.com/chris54179/gumall/blob/master/IMG/p12.png)
#### 告警
![](https://github.com/chris54179/gumall/blob/master/IMG/p13.jpg)
#### 動態擴縮容
![](https://github.com/chris54179/gumall/blob/master/IMG/p14.jpg)
#### 流水線
![](https://github.com/chris54179/gumall/blob/master/IMG/p15.jpg)
![](https://github.com/chris54179/gumall/blob/master/IMG/p16.jpg)


## 技術選型

### 後端技術

|        技術        |           說明           |                      官網                       |
| :----------------: | :----------------------: | :---------------------------------------------: |
|     SpringBoot     |       簡化Spring開發       |     https://spring.io/projects/spring-boot      |
|    SpringCloud     |        微服務架構        |     https://spring.io/projects/spring-cloud     |
| SpringCloudAlibaba |        一系列元件        | https://spring.io/projects/spring-cloud-alibaba |
|    MyBatis-Plus    |         ORM框架          |             https://mp.baomidou.com             |
|  renren-generator  | 人人开源的程式碼生成器 |   https://gitee.com/renrenio/renren-generator   |
|   Elasticsearch    |         搜尋引擎         |    https://github.com/elastic/elasticsearch     |
|      RabbitMQ      |         消息隊列         |            https://www.rabbitmq.com             |
|   Springsession    |         會話管理        |    https://projects.spring.io/spring-session    |
|      Redisson      |         分布式鎖         |      https://github.com/redisson/redisson       |
|       Docker       |       應用容器引擎       |             https://www.docker.com              |
|        OSS         |        對象雲端儲存        |  https://github.com/aliyun/aliyun-oss-java-sdk  |
|     Kubernetes     |         容器編排       |             https://kubernetes.io/              |
|     KubeSphere     |         容器平台       |             https://kubesphere.io/              |

### 前端技術

|   技術    |    說明    |           官網            |
| :-------: | :--------: | :-----------------------: |
|    Vue    |  前端框架  |     https://vuejs.org     |
|  Element  | 前端UI框架 | https://element.eleme.io  |
| thymeleaf |  模板引擎  | https://www.thymeleaf.org |
|  node.js  | 伺服器端的js |   https://nodejs.org/en   |

### 開發工具

|     工具      |        說明         |                      官網                       |
| :-----------: | :-----------------: | :---------------------------------------------: |
|     IDEA      |    開發Java程序     |     https://www.jetbrains.com/idea/download     |
| RedisDesktop  | redis客戶端連接工具 |        https://redisdesktop.com/download        |
|  SwitchHosts  |    本地host管理     |       https://oldj.github.io/SwitchHosts        |
|    X-shell    |  Linux遠端連接工具  | http://www.netsarang.com/download/software.html |
|    Navicat    |   資料庫連接工具    |       http://www.formysql.com/xiazai.html       |
| PowerDesigner |   資料庫設計工具    |             http://powerdesigner.de             |
|    Postman    |    API測試工具   |             https://www.postman.com             |
|    Jmeter     |    性能壓測工具     |            https://jmeter.apache.org            |

### 開發環境

|     工具      | 版本號 |                             下載                             |
| :-----------: | :----: | :----------------------------------------------------------: |
|      JDK      |  1.8   | https://www.oracle.com/java/technologies/javase/javase-jdk8-downloads.html |
|     Mysql     |  5.7   |                    https://www.mysql.com                     |
|     Redis     | 5.0.7  |                  https://redis.io/download                   |
| Elasticsearch | 7.4.2  |               https://www.elastic.co/downloads               |
|    Kibana     | 7.4.2  |               https://www.elastic.co/cn/kibana               |
|    Logstash   | 7.4.2  |               https://www.elastic.co/logstash/               |
|   RabbitMQ    | 3.8.2  |            http://www.rabbitmq.com/download.html             |
|     Nginx     | 1.1.0  |              http://nginx.org/en/download.html               |


商城後台系統前端:https://github.com/chris54179/gumall-admin-vue
