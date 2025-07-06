const token = sessionStorage.getItem("jwt");
const ws = new WebSocket(`ws://192.168.1.5:8002/ChatApp/chat/${token}`);
let selectedFriend = null;
const uName = sessionStorage.getItem("username");
let selectedGroupId = null;
let selectedGroupName = null;

let peerConnection = null; // Declare globally

ws.onmessage = async (event) => {
	const messageData = JSON.parse(event.data);
	console.log("on mg" + messageData);

	if (messageData.type === "group") {
		if (messageData.groupId === selectedGroupId) {
			console.log("Group message received for selected group:", messageData.groupId);
			let isSent = messageData.sender === uName;
			displayMessage(messageData.sender, messageData.message, isSent);
		} else {
			console.log("Ignoring group message for unselected group:", messageData.groupId);
		}
	} else if (messageData.type === "private" && (messageData.sender === selectedFriend || messageData.receiver === selectedFriend)) {
		displayMessage(messageData.sender, messageData.message, false);
		console.log("in private");
	}
	 else {
		showNotificationDot(messageData.sender);
	}
};

let localStream;

function addFriend() {
	let name = prompt("Enter the name");

	fetch(`/ChatApp/chats?action=insertFriend`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ name: uName, friend: name })
	})
		.catch(error => console.log("Error Adding friend!" + error));

	alert("Friend added successfully");

	loadFriends();
}

function addGroup() {
	let name = prompt("Enter the Group name");

	fetch(`/ChatApp/chats?action=insertGroup`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ gName: name, creator: uName })
	})
		.catch(error => console.log("Error Creating Group!" + error));

	alert("Group added successfully");

	loadGroups();
}

function sendGroupMessage() {
	const messageInput = document.getElementById("messageInput").value;
	if (!messageInput.trim() || !selectedGroupId) return;

	const messageData = {
		type: "group",
		sender: uName,
		groupId: selectedGroupId,
		message: messageInput
	};

	ws.send(JSON.stringify(messageData));
	document.getElementById("messageInput").value = "";

	fetch("/ChatApp/chats?action=insertGroupMsg", {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ sender: uName, groupId: selectedGroupId, msg: messageInput })
	})
		.catch(error => console.log("Error inserting the Group message!" + error));
}


function sendMessage() {

	if (selectedGroupName != null) {
		sendGroupMessage();
		return;
	}


	const messageInput = document.getElementById("messageInput");
	const message = messageInput.value;
	console.log("from end " + message);
	if (message && selectedFriend) {
		const msgData = JSON.stringify({ type: "private", receiver: selectedFriend, m: message, sender: uName });
		ws.send(msgData);
		displayMessage("You", message, true);
		messageInput.value = "";

		fetch("/ChatApp/chats?action=insertMsg", {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify({ sender: uName, receiver: selectedFriend, msg: message })
		})
			.catch(error => console.log("Error inserting the message!" + error));
	}
}

function loadGroups() {
	fetch(`/ChatApp/ChatServlet?action=listGroups&userName=${uName}`)
		.then(response => response.json())
		.then(groups => {
			const groupList = document.getElementById("group-list");
			groupList.innerHTML = "";

			groups.forEach(group => {
				const groupContainer = document.createElement("div");
				groupContainer.classList.add("group-container");

				const groupName = document.createElement("span");
				groupName.textContent = group.name;
				groupContainer.appendChild(groupName);
				groupContainer.onclick = () => selectGroup(group.id, group.name);
				console.log("creator " + group.creator);
				if (group.creator === uName) {
					const menuButton = document.createElement("button");
					menuButton.innerHTML = "&#8942;";
					menuButton.classList.add("menu-button");
					menuButton.onclick = (event) => {
						event.stopPropagation();
						toggleGroupMenu(event, group.id);
					};
					groupContainer.appendChild(menuButton);
				}

				groupList.appendChild(groupContainer);
			});
		})
		.catch(error => console.error("Error loading groups:", error));
}

function toggleGroupMenu(event, groupId) {

	const existingMenu = document.querySelector(".group-menu");
	if (existingMenu) {
		existingMenu.remove();
		if (existingMenu.getAttribute("data-group-id") === groupId.toString()) {
			return;
		}
	}

	const menu = document.createElement("div");
	menu.classList.add("group-menu");
	menu.setAttribute("data-group-id", groupId);

	const addMember = document.createElement("div");
	addMember.textContent = "➕ Add Member";
	addMember.onclick = () => {
		addMemberToGroup(groupId);
		menu.remove();
	};
	menu.appendChild(addMember);

	const removeMember = document.createElement("div");
	removeMember.textContent = "❌ Remove Member";
	removeMember.onclick = () => {
		removeMemberFromGroup(groupId);
		menu.remove();
	};
	menu.appendChild(removeMember);

	document.body.appendChild(menu);
	const rect = event.target.getBoundingClientRect();
	menu.style.left = `${rect.left}px`;
	menu.style.top = `${rect.bottom + 5}px`;

	document.addEventListener("click", function closeMenu(e) {
		if (!menu.contains(e.target) && e.target !== event.target) {
			menu.remove();
			document.removeEventListener("click", closeMenu);
		}
	});
}



function addMemberToGroup(groupId) {
	const memberName = prompt("Enter the name of the member to add:");

	if (!memberName) return;

	fetch(`/ChatApp/ChatServlet?action=addMember`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ groupId, memberName })
	})
		.then(response => response.json())
		.then(result => alert(result.message))
		.catch(error => console.error("Error adding member:", error));
}

function removeMemberFromGroup(groupId) {
	const memberName = prompt("Enter the name of the member to remove:");

	if (!memberName) return;

	fetch(`/ChatApp/ChatServlet?action=removeMember`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ groupId, memberName })
	})
		.then(response => response.json())
		.then(result => alert(result.message))
		.catch(error => console.error("Error removing member:", error));
}



function selectGroup(groupId, groupName) {
	selectedGroupId = groupId;
	selectedGroupName = groupName;
	selectedFriend = null;
	loadGroupMessages(groupId);
}

function loadGroupMessages(groupId) {
	fetch(`/ChatApp/ChatServlet?action=getGroupMessages&groupId=${groupId}`)
		.then(response => response.json())
		.then(messages => {
			const chatBox = document.getElementById("messages");
			chatBox.innerHTML = "";
			document.getElementById("messages").textContent = `Group: ${selectedGroupName}`;
			messages.forEach(msg => {
				displayMessage(msg.sender_name, msg.message, msg.sender_name === uName);
			});
		})
		.catch(error => console.error("Error loading group messages:", error));
}



function displayMessage(sender, message, isSent) {
	const messageBox = document.getElementById("messages");
	const messageElement = document.createElement("div");
	messageElement.textContent = `${sender}: ${message}`;
	messageElement.classList.add("message", isSent ? "sent" : "received");
	messageBox.appendChild(messageElement);
}

function loadFriends() {
	const friendList = document.getElementById("friend-list");

	fetch(`/ChatApp/chats?action=listFriends&userName=${uName}`)
		.then(response => response.json())
		.then(friends => {

			friendList.innerHTML = "";
			friends.forEach(friend => {
				const userElement = document.createElement("div");
				userElement.textContent = friend;
				userElement.classList.add("user");
				userElement.setAttribute("data-username", friend);
				userElement.onclick = () => selectFriend(friend);
				friendList.appendChild(userElement);
			});

		})

}

function displayChatHistory(messages, currentUser) {
	const messageBox = document.getElementById("messages");
	messageBox.innerHTML = "";
	document.getElementById("messages").innerHTML = `<h3>Chat with ${selectedFriend}</h3>`;

	messages.forEach(msg => {
		const messageElement = document.createElement("div");

		const isSent = msg.sender_name === currentUser;
		let name = isSent ? "You" : msg.sender_name;
		messageElement.textContent = `${name}:${msg.message}`;
		messageElement.classList.add("message", isSent ? "sent" : "received");

		messageBox.appendChild(messageElement);
	});

	messageBox.scrollTop = messageBox.scrollHeight;
}

function showNotificationDot(friendUsername) {
	console.log("notify ");
	const friendElement = document.querySelector(`[data-username='${friendUsername}']`);
	if (friendElement) {
		friendElement.classList.add("new-message");
	}
}

function selectFriend(friend) {
	selectedFriend = friend;
	selectedGroupId = null;
	selectedGroupName = null;
	fetch(`/ChatApp/ChatServlet?action=loadMessages&userName=${uName}&friendName=${selectedFriend}`)
		.then(response => response.json())
		.then(messages => {
			displayChatHistory(messages, uName);
		})
		.catch(error => console.error("Error loading chat history:", error));

	const friendElement = document.querySelector(`[data-username='${selectedFriend}']`);
	if (friendElement) {
		friendElement.classList.remove("new-message");
	}
}

loadFriends();
loadGroups();
