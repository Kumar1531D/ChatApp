package com.chatapp.server;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONObject;

import com.chatapp.token.JWTUtil;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint("/chat/{token}")
public class ChatAppServerEndpoint {
	
	private static final String JDBC_URL = "jdbc:mysql://localhost:3306/chatApp";
	private static final String JDBC_USER = "root";
	private static final String JDBC_PASS = "sabari";
	private static Map<String,Session> activeUsers = new ConcurrentHashMap();
	
	@OnOpen
	public void onOpen(Session session,@PathParam("token") String token) {
		String uName = JWTUtil.validateToken(token);
		if(uName == null) {
			try {
				session.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}
		
		activeUsers.put(uName,session);
	}
	
	@OnMessage
	public void onMessage(String message,Session session) {
		
		JSONObject msgData = new JSONObject(message);
		
		String type = msgData.getString("type");
		
		if(type.equals("private")) {
			
			String receiver = msgData.getString("receiver");
			String msgText = msgData.getString("m");
			String sender  = msgData.getString("sender");
			Session receiverSession = activeUsers.get(receiver);
			
			System.out.println("Message is : "+msgText);
			
			if(receiverSession!=null) {
				try {
					JSONObject json = new JSONObject();
					json.put("sender", sender);
					json.put("message", msgText);	   
					json.put("type", "private");
					receiverSession.getBasicRemote().sendText(json.toString());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		else if (type.equals("group")) {
	        // Send message to all group members
			System.out.println("in group");
			
	        int groupId = msgData.getInt("groupId");
	        sendGroupMessage(groupId, msgData);
	    }
		else if (type.equals("offer") || type.equals("answer") || type.equals("ice-candidate")) {
            handleVideoCall(msgData);
        }
		
	}
	
	@OnClose
    public void onClose(Session session) {
        activeUsers.values().remove(session); // Remove user on disconnect
    }
	
	private void sendGroupMessage(int groupId, JSONObject msgData) {
	    List<String> groupMembers = getGroupMembers(groupId);
	    
	    System.out.println("member : "+groupMembers+" id "+groupId);
	    
		String msgText = msgData.getString("message");
		String sender  = msgData.getString("sender");
	    
	    for (String member : groupMembers) {
	        if (activeUsers.containsKey(member)) {
	        	try {
	        		JSONObject json = new JSONObject();
					json.put("sender", sender);
					json.put("message", msgText);
					json.put("type", "group");
					json.put("groupId", groupId);
					activeUsers.get(member).getBasicRemote().sendText(json.toString());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        }
	    }
	}
	
	private List<String> getGroupMembers(int groupId) {
	    List<String> members = new ArrayList<>();
	    String query = "SELECT l.user_name FROM group_members gm " +
	                   "JOIN loginInfo l ON gm.user_id = l.id " +
	                   "WHERE gm.group_id = ?";

	    try (Connection con = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
	         PreparedStatement ps = con.prepareStatement(query)) {
	        
	        ps.setInt(1, groupId);
	        ResultSet rs = ps.executeQuery();
	        
	        while (rs.next()) {
	            members.add(rs.getString("user_name"));
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    
	    return members;
	}
	
	private void handleVideoCall(JSONObject jsonMessage) {
	    String receiver = jsonMessage.getString("receiver");
	    //String sender = jsonMessage.getString("sender");

	    Session receiverSession = activeUsers.get(receiver); // Find recipient's session
	    if (receiverSession != null && receiverSession.isOpen()) {
	        try {
	            receiverSession.getBasicRemote().sendText(jsonMessage.toString());
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    } else {
	        System.out.println("Receiver not online: " + receiver);
	    }
	}



}
