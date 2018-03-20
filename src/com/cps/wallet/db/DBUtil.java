package com.cps.wallet.db;


import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysql.jdbc.log.Log;


/**
 * @author wudong005
 *
 */
public class DBUtil {

//	private static final String URL="jdbc:mysql://192.168.11.78:3306/deploydb";
//	private static final String USER="root";
//	private static final String PASSWORD="123456";
//	
//	private static Connection conn=null;
//	
//	static {
//		try {
//			Class.forName("com.mysql.jdbc.Driver");
//			conn=DriverManager.getConnection(URL, USER, PASSWORD);
//		} catch (ClassNotFoundException e) {
//			e.printStackTrace();
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}
//	}
//	
//	public static Connection getConnection(){
//		return conn;
//	}
	private static final Logger log=LoggerFactory.getLogger(DBUtil.class);

	/**
	 * @return
	 */
	public static final Connection getConnection(DataSource dataSource)
	{
		Connection conn=null;
		try {
			conn=dataSource.getConnection();
		} catch (SQLException e) {
			log.error("获取数据库连接失败："+e);
		}
		return conn;
	}

	/**
	 * 关闭连接
	 * @param conn
	 */
	public static void closeConn(Connection conn)
	{
		try {
			if(conn!=null&&!conn.isClosed())
			{
				conn.setAutoCommit(true);
				conn.close();
			}
		} catch (SQLException e) {
			log.error("关闭数据库连接失败："+e);
		}
	}
	
	
	
	
	
	
}
