package com.java.qzking;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.java.qzking.conf.BotType;
import com.java.qzking.conf.Event;
import com.java.qzking.conf.ServerConfig;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.exceptions.SFSException;

import sfs2x.client.SmartFox;
import sfs2x.client.core.BaseEvent;
import sfs2x.client.core.IEventListener;
import sfs2x.client.core.SFSEvent;
import sfs2x.client.requests.ExtensionRequest;
import sfs2x.client.requests.LoginRequest;

public class Bot implements IEventListener {
	private SmartFox smartFox;
	private BotType type;
	private String botName;
	private List<Integer> userIds = new ArrayList<>();
	private int counter = 0;
	private ScheduledExecutorService timer = Executors.newScheduledThreadPool(2);

	public Bot(BotType type, String name) {
		this.setType(type);
		this.botName = name;
		System.out.println("start bot with type: " + type);
		init();
	}

	private void init() {
		initSmartFox();
	}

	public void start() {
		connect();
	}

	private void initSmartFox() {
		this.smartFox = new SmartFox(ServerConfig.DEBUG_SFS);
		smartFox.addEventListener(SFSEvent.CONNECTION, this);
		smartFox.addEventListener(SFSEvent.CONNECTION_LOST, this);
		smartFox.addEventListener(SFSEvent.LOGIN, this);
		smartFox.addEventListener(SFSEvent.ROOM_JOIN, this);
		smartFox.addEventListener(SFSEvent.EXTENSION_RESPONSE, this);
	}

	private void connect() {
		System.out.println("connecting to server .....");
		this.smartFox.connect(ServerConfig.SERVER_ADDRESS, ServerConfig.PORT);
	}

	private void disconnect() {
		this.smartFox.disconnect();
		this.timer.shutdown();
	}

	private void onConnection(BaseEvent event) {
		System.out.println("connection to server success");
		login();
	}

	private void onConnectionLost(BaseEvent event) {
		System.out.println("connection fail, try again");
		counter += 1;
		if (counter > 5) {
			disconnect();
		} else {
			start();
		}
	}

	private void onLogin(BaseEvent event) {
		// on login response
		System.out.println("on login response ");
	}

	private void onJoinRoom(BaseEvent event) {
		System.out.println(this.botName + " join room");
		if (this.userIds.size() > 0) {
			for (int userId : this.userIds) {
				sendInviteRequest(userId);
			}
		}
		monitorLag();
	}

	private void onExtensionResponse(BaseEvent event) {
		String command = (String) event.getArguments().get("cmd");
		System.out.println("receive " + command);
		ISFSObject params = (SFSObject) event.getArguments().get("params");
		ISFSObject obj = new SFSObject();
		switch (command) {
		// ONLINE LIST
		case "27":
			if (this.getType() == BotType.AUTO_INVITE) {
				SFSArray arr = new SFSArray();
				arr = (SFSArray) params.getSFSArray("lFound");
				if (arr != null) {
					System.out.println("number user online is: " + arr.size());
					for (int i = 0; i < arr.size(); i++) {
						SFSObject userOnline = (SFSObject) arr.getSFSObject(i);
						int userId = userOnline.getInt("id");
						String displayName = userOnline.getUtfString("display");
						System.out.println("user " + userId + " with " + displayName);
						this.userIds.add(userId);
					}
				} else {
					System.out.println("no body in server ");
					startGameSolo();
				}
			}
			break;
		// CLIENT_INVITE
		case "30":
			String roomName = params.getUtfString("rName");
			String ownerName = params.getUtfString("ownerName");
			String ownerID = params.getUtfString("ownerID");
			if (roomName == null) {
				break;
			}
			System.out.println(roomName + " & " + ownerName + " & " + ownerID);
			obj.putUtfString("rName", roomName);
			System.out.println(this.botName + " send accept room" + roomName);
			obj.putBool("accept", true);
			smartFox.send(new ExtensionRequest(Event.CLIENT_INVITERESPONSE, obj));
			obj = null;
			break;
		// CLIENT_ANSWER
		case "12":
			System.out.println(this.botName + " send answer");
			int currentQuiz = params.getInt("currQuiz");
			String question = params.getUtfString("question");
			String correctAnswer = params.getUtfString("correctAnswer");
			System.out.println(this.botName + " play quiz " + currentQuiz + " question: " + question);
			Random random = new Random();
			int i = random.nextInt(3 - 0 + 1) + 0;
			ArrayList<String> s = new ArrayList<String>(params.getUtfStringArray("answer"));
			String randomAns = s.get(i);
			System.out.println("random answer: " + randomAns);
			obj = new SFSObject();
			obj.putUtfString("answer", correctAnswer);
			smartFox.send(new ExtensionRequest(Event.ANSWER, obj));
			break;
		// LIST_QUIZ
		case "12.5":
			ISFSArray quizArr = new SFSArray();
			quizArr = (SFSArray) params.getSFSArray("lQ");
			System.out.println("number quizs: " + quizArr.size());
			break;
		// CLIENT_START
		case "9":
			int currQuiz = params.getInt("currQuiz");
			int time = params.getInt("time");
			int type = params.getInt("solo");
			System.out.println(currQuiz + " & " + time + " & " + type);
			break;
		case "11":
			System.out.println("bot's game stopped");
			break;
		// ERROR
		case "0":
			if (params.containsKey("msg")) {
				String error = params.getUtfString("msg");
				System.out.println("has error: " + error);
			}
			if (params.containsKey("code")) {
				int errorCode = params.getInt("code");
				System.out.println("error code: " + errorCode);
			}
			break;
		default:
			break;
		}
	}

	@Override
	public void dispatch(BaseEvent event) throws SFSException {
		String type = event.getType();
		if (type.equalsIgnoreCase(SFSEvent.CONNECTION)) {
			if (event.getArguments().get("success").equals(true)) {
				onConnection(event);
			} else {
				onConnectionLost(event);
			}
		} else if (type.equalsIgnoreCase(SFSEvent.CONNECTION_LOST)) {
			onConnectionLost(event);
		} else if (type.equalsIgnoreCase(SFSEvent.LOGIN)) {
			onLogin(event);
		} else if (type.equalsIgnoreCase(SFSEvent.ROOM_JOIN)) {
			onJoinRoom(event);
		} else if (type.equalsIgnoreCase(SFSEvent.EXTENSION_RESPONSE)) {
			onExtensionResponse(event);
		}
	}

	private void login() {
		ISFSObject obj = new SFSObject();
		obj.putUtfString("did", "test");
		// obj.putUtfString("fid", "test");
		// obj.putUtfString("em", "test");

		LoginRequest request = new LoginRequest(this.botName, this.botName, ServerConfig.ZONE, obj);
		this.smartFox.send(request);
	}

	private void monitorLag() {
		this.timer.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				ping();
			}
		}, 0, 28800, TimeUnit.MILLISECONDS);
	}

	private void ping() {
		System.out.println(this.botName + " send ping request");
		ISFSObject obj = new SFSObject();
		this.smartFox.send(new ExtensionRequest(Event.PING, obj));
	}

	private void sendInviteRequest(int invite_id) {
		System.out.println(this.botName + " send invite request to user " + invite_id);
		ISFSObject params = new SFSObject();
		params.putBool("pvp", true);
		List<Integer> invites = new ArrayList<>();
		invites.add(invite_id);
		params.putIntArray("inviteList", invites);
		params.putInt("gemFee", 5);
		params.putInt("coinFee", 5);
		// obj.putBool("cancel", false);

		this.smartFox.send(new ExtensionRequest("9", params));
	}

	private void startGameSolo() {
		System.out.println(this.botName + " start game solo");
		ISFSObject params = new SFSObject();
		params.putBool("solo", true);
		this.smartFox.send(new ExtensionRequest("9", params));
	}

	public BotType getType() {
		return type;
	}

	public void setType(BotType type) {
		this.type = type;
	}
}
