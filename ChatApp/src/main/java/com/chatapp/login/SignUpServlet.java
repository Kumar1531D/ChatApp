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

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/signup")
public class SignUpServlet extends HttpServlet {

	private static final String JDBC_URL = "jdbc:mysql://localhost:3306/chatApp";
    private static final String JDBC_USER = "root"; 
    private static final String JDBC_PASS = "Kumar_1531.,";
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		response.setContentType("application/json");
		response.setContentType("UTF-8");
		
		StringBuilder sb = new StringBuilder();
		String line;
		BufferedReader reader = request.getReader();

		while ((line = reader.readLine()) != null) {
			sb.append(line);
		}
		
		JSONObject requestedData = new JSONObject(sb.toString());
		
		PrintWriter out = response.getWriter();
		String uname = requestedData.getString("userName");
		String pass = requestedData.getString("password");
		String mail = requestedData.getString("email");
		
		JSONObject jsonResponse = new JSONObject();
		
		System.out.println("signup servlet :" + checkInfo(uname));
		
		if(checkInfo(uname)) {
			jsonResponse.put("access", "notok");
		}
		else {
			putDetails(uname,pass,mail);
			
			jsonResponse.put("access", "ok");
		}
		
		out.write(jsonResponse.toString());
		out.flush();
		
	}
	
	private void putDetails(String uname, String pass, String mail) {
		
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			
			Connection con = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
			
			PreparedStatement ps = con.prepareStatement("insert into loginInfo values(null,?,?,?)");
			
			ps.setString(1, uname);
			ps.setString(2, pass);
			ps.setString(3, mail);
			
			ps.executeUpdate();
			
			con.close();
			ps.close();
			
		}
		catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public boolean checkInfo(String uname) { 
		
		System.out.println("User name is "+uname);
		
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			
			Connection con = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
			
			PreparedStatement ps = con.prepareStatement("select * from loginInfo where user_name=?");
			
			ps.setString(1, uname);
			
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
