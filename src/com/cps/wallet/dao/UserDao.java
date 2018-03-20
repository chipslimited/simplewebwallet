package com.cps.wallet.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.cps.wallet.entity.Mnemonics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.cps.wallet.db.DBUtil;

import javax.sql.DataSource;

@Repository
public class UserDao{

    @Autowired
    DataSource dataSource;

	public List<String> getUserAdderss(Integer userId){
		Connection conn = DBUtil.getConnection(dataSource);
		List<String> list = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		sb.append("select account from user_wallet where user_id = ?");
		try {
			PreparedStatement ps = conn.prepareStatement(sb.toString());
			ps.setInt(1,userId);
			ResultSet rs = ps.executeQuery();
			int count = 0;
			while(rs.next()){
				count++;
				list.add(rs.getString("account"));
			}
			return list;
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	public boolean checkPassWord(Integer userId,String password){
        Connection conn = DBUtil.getConnection(dataSource);
		StringBuilder sb = new StringBuilder();
		sb.append("select password from user where id = ? and password = ?");
		try {
			PreparedStatement ps = conn.prepareStatement(sb.toString());
			ps.setInt(1, userId);
			ps.setString(2, password);
			ResultSet rs = ps.executeQuery();
			boolean b = false;
			while(rs.next()){
				b = rs.getBoolean("password");
			}
			return b;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}finally {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

    public Integer checkPassWord(String username,String password){
        Connection conn = DBUtil.getConnection(dataSource);
        StringBuilder sb = new StringBuilder();
        sb.append("select id,password from user where username = ? and password = ?");
        try {
            PreparedStatement ps = conn.prepareStatement(sb.toString());
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            Integer userId = -1;
            while(rs.next()){
                userId = rs.getInt("id");
            }
            return userId;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

	public String getPrivKey(String address){
        Connection conn = DBUtil.getConnection(dataSource);
		StringBuilder sb = new StringBuilder();
		sb.append("select private_key from user_wallet where account = ?");
		try{
			PreparedStatement ps = conn.prepareStatement(sb.toString());
			ps.setString(1,address);
			ResultSet rs = ps.executeQuery();
			String privKey = "";
			while(rs.next()){
				 privKey = rs.getString("private_key");
			}
			return privKey;
		}catch(SQLException e){
			e.printStackTrace();
			return null;
		}finally{
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	public Integer createUser(String name,String password,String mobile){
        Connection conn = DBUtil.getConnection(dataSource);
		StringBuilder sb  = new StringBuilder();
		sb.append("insert into user(username,password,mobile) values(?,?,?)");
		try {
			PreparedStatement ps = conn.prepareStatement(sb.toString());
			ps.setString(1, name);
			ps.setString(2,password);
			ps.setString(3,mobile);
			return ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}finally{
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public boolean walletExists(Integer userId, String account){
        Connection conn = DBUtil.getConnection(dataSource);
        try {
            PreparedStatement ps = conn.prepareStatement("select id from user_wallet where id=? and account = ?");
            ps.setInt(1,userId);
            ps.setString(2,account);
            ResultSet resultSet = ps.executeQuery();
            boolean exists = resultSet.next();
            resultSet.close();
            return exists;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }finally{
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public List<String> walletAddresses(Integer userId){
	    ArrayList<String> addresses = new ArrayList<>();
        Connection conn = DBUtil.getConnection(dataSource);
        try {
            PreparedStatement ps = conn.prepareStatement("select id,account from user_wallet where id=?");
            ps.setInt(1,userId);
            ResultSet resultSet = ps.executeQuery();
            while (resultSet.next()){
                addresses.add(resultSet.getString("account"));
            }
            resultSet.close();
            return addresses;
        } catch (SQLException e) {
            e.printStackTrace();
            return addresses;
        }finally{
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

	public Integer importWallet(Integer userId,String account,String privKey,String mnemonics){

	    if(walletExists(userId, account)){
	        return 1;
        }

        Connection conn = DBUtil.getConnection(dataSource);
		StringBuilder sb = new StringBuilder();

		sb.append("insert into user_wallet(user_id,account,private_key,mnemonics) values(?,?,?,?)");
		try {
			PreparedStatement ps = conn.prepareStatement(sb.toString());
			ps.setInt(1,userId);
			ps.setString(2,account);
			ps.setString(3,privKey);
			ps.setString(4,mnemonics);
			return ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}finally{
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

    public Mnemonics exportWallet(Integer userId, String account){
        Connection conn = DBUtil.getConnection(dataSource);
        StringBuilder sb = new StringBuilder();
        sb.append("select * from user_wallet where user_id=? and account=?");//(user_id,account,private_key,mnemonics) values(?,?,?,?)
        try {
            Mnemonics mnemonics = new Mnemonics();
            PreparedStatement ps = conn.prepareStatement(sb.toString());
            ps.setInt(1,userId);
            ps.setString(2,account);
            ResultSet resultSet = ps.executeQuery();
            while (resultSet.next()){
                mnemonics.setUserId(userId);
                mnemonics.setMnemonics(resultSet.getString("mnemonics"));
            }
            return mnemonics;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }finally{
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
