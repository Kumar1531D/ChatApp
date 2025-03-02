package com.chatapp.login;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.JSONObject;

import com.chatapp.token.JWTUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
	
	private static final String JDBC_URL = "jdbc:mysql://localhost:3306/chatApp";
    private static final String JDBC_USER = "root"; 
    private static final String JDBC_PASS = "sabari";
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		StringBuilder sb = new StringBuilder();
		String line;
		BufferedReader reader = request.getReader();

		while ((line = reader.readLine()) != null) {
			sb.append(line);
		}

		PrintWriter out = response.getWriter();
		JSONObject requestedData = new JSONObject(sb.toString());
		String userName = requestedData.getString("userName");
		String password = requestedData.getString("password");
		
		JSONObject jsonResponse = new JSONObject();
		
		if(checkInfo(userName, password)) {
			jsonResponse.put("access", "ok");
			JWTUtil jwt = new JWTUtil();
			jsonResponse.put("jwt", jwt.generateToken(userName));
		}
		else {
			jsonResponse.put("access", "notok");
		}
		
		out.write(jsonResponse.toString());
		out.flush();
		reader.close();
		out.close();
	}
	
	public boolean checkInfo(String uname,String pass) { 
		
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			
			Connection con = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
			
			PreparedStatement ps = con.prepareStatement("select * from loginInfo where user_name=? and password=?");
			
			ps.setString(1, uname);
			ps.setString(2, pass);
			
			ResultSet rs = ps.executeQuery();
			
			if(rs.next()) {
				return true;
			}
			
			con.close();
			ps.close();
			rs.close();
			
		}
		catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
		
	}


}
