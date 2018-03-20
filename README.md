# A Web Wallet API for CPS 

## Build Setup

# Prerequisite

Install Git, JDK 1.8, MySQL Server, Maven 3.5 or later and tomcat8


If you are using Ubuntu 16.04 
```bash
sudo apt-get install -y tomcat8 openjdk-9-jdk-headless maven mysql-server
```

# Configuration

Suppose 
* Now you are currently working at a computer which is running Ubuntu 16.04 server
* a Ethereum full node is ready at 192.168.0.65 and its rpc server is serving at 8545.
* CPS contract is at contract address 0x68b16039d79E0f51F109015e55a3311fE999a723
* a MySQL server is running localhost:3306 with root password 123456

Get wallet source code.
```bash
git clone https://github.com/chipslimited/simplewebwallet.git
cd simplewebwallet
```

Initialise database

```bash
mysql -u root -p < database.sql
```

Edit src/application.properties
```bash

web3_url=http://192.168.0.65:8545/
#数据库连接地址
jdbc_url=jdbc\:mysql\://127.0.0.1\:3306/wallet
#用户名
jdbc_user=root
#密码
jdbc_password=123456
contractAddress=0x68b16039d79E0f51F109015e55a3311fE999a723

```

# Package application
``` bash
mvn clean package
```
A webwallet.war will be available in directory dist

# Deployment

Copy webwallet.war to /var/lib/tomcat8/webapps.

Wait a few seconds.

```bash
curl http://localhost:8080/webwallet/swagger-ui.html
```

If webwallet is deployed correctly, you should see
```bash
......
<div id="message-bar" class="swagger-ui-wrap" data-sw-translate>&nbsp;</div>
<div id="swagger-ui-container" class="swagger-ui-wrap"></div>
</body>
</html>
```

Now You can test and use this webwallet by navigating to url http://webwallet-ip-or-domain:8080/webwallet/swagger-ui.html
