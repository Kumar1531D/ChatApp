package com.chatapp.dao;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.Gson;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@WebServlet("/chats")
public class ChatServlet extends HttpServlet {
	
	private static final String JDBC_URL = "jdbc:mysql://localhost:3306/chatApp";
	private static final String JDBC_USER = "root";
	private static final String JDBC_PASS = "sabari";
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		
		PrintWriter out = response.getWriter();
		String action = request.getParameter("action");
		
		try {
			if(action.equalsIgnoreCase("listFriends")) {
				Connection con = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
				
				JSONArray files = new JSONArray();
				
				String userName = request.getParameter("userName");
				
				String qry  = "select l.user_name as name from loginInfo l join friends f on f.user_id=(select id from loginInfo where user_name=?) where f.friend_id=l.id ";
				
				PreparedStatement ps = con.prepareStatement(qry);
				
				ps.setString(1, userName);
				
				ResultSet r = ps.executeQuery();
				
				while(r.next()) {
					files.put(r.getString("name"));
				}
				
				out.write(files.toString());
				out.flush();
				
				con.close();
				ps.close();
				r.close();
			}
			else if (action.equalsIgnoreCase("loadMessages")) {
				
				String userName = request.getParameter("userName");
	            String friendName = request.getParameter("friendName");

	            List<Message> messages = new ArrayList<Message>(); 
	            
	            Connection con = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
	            
	            String qry = "SELECT m.sender_id, m.receiver_id, m.message as msg , m.timestamp, l.user_name AS sender_name "
	            		+ "FROM messages m "
	            		+ "JOIN loginInfo l ON m.sender_id = l.id "
	            		+ "WHERE (m.sender_id = (SELECT id FROM loginInfo WHERE user_name = ?) "
	            		+ "       AND m.receiver_id = (SELECT id FROM loginInfo WHERE user_name =?)) "
	            		+ "   OR (m.sender_id = (SELECT id FROM loginInfo WHERE user_name = ?) "
	            		+ "       AND m.receiver_id = (SELECT id FROM loginInfo WHERE user_name = ?)) "
	            		+ "ORDER BY m.timestamp ASC;";
	            
	            PreparedStatement p = con.prepareStatement(qry);
	            
	            p.setString(1, userName);
	            p.setString(2, friendName);
	            p.setString(3, friendName);
	            p.setString(4, userName);
	            
	            ResultSet r = p.executeQuery();
	            
	            while(r.next()) {
	            	Message m = new Message(r.getString("sender_name"), r.getNString("msg"));
	            	messages.add(m);
	            }
	            
	            response.setContentType("application/json");
	            response.getWriter().write(new Gson().toJson(messages)); 
	            
	            con.close();
	            p.close();
	            r.close();
				
			}else if (action.equals("listGroups")) {
			    String userName = request.getParameter("userName");
			    List<Group> groups = getUserGroups(userName);

			    // Convert the group list to JSON
			    String json = new Gson().toJson(groups);

			    // Send response to frontend
			    response.setContentType("application/json");
			    response.getWriter().write(json);
			}else if (action.equals("getGroupMessages")) {
			    int groupId = Integer.parseInt(request.getParameter("groupId"));
			    List<Message> messages = getGroupMessages(groupId);

			    String json = new Gson().toJson(messages);
			    response.setContentType("application/json");
			    response.getWriter().write(json);
			}


		}catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
	}

	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		StringBuilder sb = new StringBuilder();
		String line;
		BufferedReader reader = request.getReader();

		while ((line = reader.readLine()) != null) {
			sb.append(line);
		}

		JSONObject requestedData = new JSONObject(sb.toString());
		
		String action = request.getParameter("action");
		
		try {
			if(action.equals("insertMsg")) {
				Connection con = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
				
				String msg = requestedData.getString("msg");
				String sender = requestedData.getString("sender");
				String receiver = requestedData.getString("receiver");
				
				String qry = "insert into messages(sender_id,receiver_id,message,timestamp) select l1.id,l2.id,?,now() from loginInfo l1,loginInfo l2 where l1.user_name=? and l2.user_name=?;";
				
				PreparedStatement p = con.prepareStatement(qry);
				p.setString(1, msg);
				p.setString(2, sender);
				p.setString(3, receiver);
				
				p.executeUpdate();
				
				con.close();
				p.close();
			}
			else if (action.equals("insertFriend")) {
				Connection con = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
				
				String uName = requestedData.getString("name");
				String friend = requestedData.getString("friend");
				
				String qry = "insert into friends(user_id,friend_id) select l1.id,l2.id from loginInfo l1,loginInfo l2 \r\n"
						+ "where l1.user_name=? and l2.user_name=?;";
				
				PreparedStatement p = con.prepareStatement(qry);
				
				p.setString(1, uName);
				p.setString(2, friend);
				
				p.executeUpdate();
				
				p.setString(1, friend);
				p.setString(2, uName);
				
				p.executeUpdate();
				
				con.close();
				p.close();
				
				
			}
			else if (action.equals("insertGroupMsg")) {
				Connection con = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
				
				String msg = requestedData.getString("msg");
				String sender = requestedData.getString("sender");
				int groupId = requestedData.getInt("groupId");
				
				String qry = "insert into group_messages(group_id,sender_id,message,timestamp) values(?,(select id from loginInfo where user_name=?),?,now());";
				
				PreparedStatement p = con.prepareStatement(qry);
				p.setInt(1, groupId);
				p.setString(2, sender);
				p.setString(3, msg);
				
				p.executeUpdate();
				
				con.close();
				p.close();
			}
			else if (action.equals("addMember")) {
				Connection con = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
				
				String memberName = requestedData.getString("memberName");
				int groupId = requestedData.getInt("groupId");
				
				String qry = "insert into group_members(group_id,user_id) values(?,(select id from loginInfo where user_name=?));";
				
				PreparedStatement p = con.prepareStatement(qry);
				p.setInt(1, groupId);
				p.setString(2, memberName);
				
				p.executeUpdate();
				
				JSONObject responseData = new JSONObject();
				
				responseData.put("message", "Member added successfully");
				
				response.setContentType("application/json");
			    response.getWriter().write(responseData.toString());
				
				con.close();
				p.close();
			}
			else if (action.equals("removeMember")) {
				Connection con = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
				
				String memberName = requestedData.getString("memberName");
				int groupId = requestedData.getInt("groupId");
				
				String qry = "delete from group_members where group_id=? and user_id=(select id from loginInfo where user_name=?);";
				
				PreparedStatement p = con.prepareStatement(qry);
				p.setInt(1, groupId);
				p.setString(2, memberName);
				
				p.executeUpdate();
				
				JSONObject responseData = new JSONObject();
				
				responseData.put("message", "Member removed successfully");
				
				response.setContentType("application/json");
			    response.getWriter().write(responseData.toString());
				
				con.close();
				p.close();
			}
			else if (action.equals("insertGroup")) {
				Connection con = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
				
				String gName = requestedData.getString("gName");
				String uName = requestedData.getString("creator");
				
				String qry = "insert into group_table(group_name,created_by,created_at) values(?,(select id from loginInfo where user_name=?),now());";
				PreparedStatement p = con.prepareStatement(qry);
				p.setString(1, gName);
				p.setString(2, uName);
				
				p.executeUpdate();
				
				qry = "insert into group_members(group_id,user_id) values((select id from group_table where group_name=?),(select id from loginInfo where user_name=?));\r\n";
				p = con.prepareStatement(qry);
				p.setString(1, gName);
				p.setString(2, uName);
				
				p.executeUpdate();
				
				con.close();
				p.close();
			}
		}catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public List<Group> getUserGroups(String userName) {
	    List<Group> groups = new ArrayList<>();
	    String query = "SELECT g.id, g.group_name,l1.user_name as created_by FROM group_table g "
	    		+ "	                   JOIN group_members gm ON g.id = gm.group_id "
	    		+ "	                   JOIN loginInfo l ON gm.user_id = l.id "
	    		+ "                    JOIN loginInfo l1 on l1.id=g.created_by "
	    		+ "	                   WHERE l.user_name = ?;";

	    try (Connection con = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
	         PreparedStatement ps = con.prepareStatement(query)) {
	        
	        ps.setString(1, userName);
	        ResultSet rs = ps.executeQuery();
	        
	        while (rs.next()) {
	            groups.add(new Group(rs.getInt("id"), rs.getString("group_name"),rs.getString("created_by")));
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    
	    return groups;
	}
	
	public List<Message> getGroupMessages(int groupId) {
	    List<Message> messages = new ArrayList<>();
	    String query = "SELECT m.sender_id, m.message, m.timestamp, l.user_name AS sender_name " +
	                   "FROM group_messages m " +
	                   "JOIN loginInfo l ON m.sender_id = l.id " +
	                   "WHERE m.group_id = ? ORDER BY m.timestamp ASC";

	    try (Connection con = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
	         PreparedStatement ps = con.prepareStatement(query)) {
	        
	        ps.setInt(1, groupId);
	        ResultSet rs = ps.executeQuery();
	        
	        while (rs.next()) {
	            messages.add(new Message( rs.getString("sender_name"),
	                                     rs.getString("message")));
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    
	    return messages;
	}



}
