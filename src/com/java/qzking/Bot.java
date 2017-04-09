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
	private static final String BOT_NAME = "qzkingBot";
	private SmartFox smartFox;
	private BotType type;
	private String botName;
	private boolean isInvited = false;
	private List<Integer> userIds = new ArrayList<>();
	private int counter = 0;
	private ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();

	public Bot(BotType type, int index) {
		this.setType(type);
		this.botName = BOT_NAME + index;
		System.out.println("start bot with type: " + type);
		init();
	}

	private void init() {
		initSmartFox();
	}

	public void start() {
		connect();
	}

	public void stop() {
		disconnect();
		init();
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
		System.out.println(this.botName + " connecting to server .....");
		this.smartFox.connect(ServerConfig.SERVER_ADDRESS, ServerConfig.PORT);
	}

	private void disconnect() {
		this.smartFox.disconnect();
		this.timer.shutdown();
		
	}

	private void login() {
		ISFSObject obj = new SFSObject();
		obj.putUtfString("did", "qzkingBot");
		obj.putUtfString("displayName", this.botName);
		// obj.putUtfString("fid", "test");
		// obj.putUtfString("em", "test");

		LoginRequest request = new LoginRequest(this.botName, this.botName, ServerConfig.ZONE, obj);
		this.smartFox.send(request);
	}

	private void restart() {
		stop();
		start();
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
		if (!this.isInvited) {
			System.out.println(this.botName + " send invite play battle request to user " + invite_id);
			ISFSObject params = new SFSObject();
			params.putBool("pvp", true);
			List<Integer> invites = new ArrayList<>();
			invites.add(invite_id);
			params.putIntArray("inviteList", invites);
			params.putInt("gemFee", 5);
			params.putInt("coinFee", 5);
			// obj.putBool("cancel", false);

			this.smartFox.send(new ExtensionRequest("9", params));
			this.isInvited = true;
		}

	}

	private void startRumbleGame(List<Integer> userIds) {
		System.out.println(this.botName + " send start rumble game to number user " + userIds.size());
		ISFSObject params = new SFSObject();
		params.putBool("battle", true);
		List<Integer> invites = new ArrayList<>();
		invites.addAll(userIds);
		params.putIntArray("inviteList", invites);
		params.putInt("gemFee", 5);
		params.putInt("coinFee", 5);
		// obj.putBool("cancel", false);

		this.smartFox.send(new ExtensionRequest("9", params));
	}


//	private void sendInvitePlayRumbleGame(int invite_id) {
//		System.out.println(this.botName + " send invite rumble game to user " + invite_id);
//		ISFSObject params = new SFSObject();
//		List<Integer> invites = new ArrayList<>();
//		invites.add(invite_id);
//		params.putIntArray("inviteList", invites);
//
//		this.smartFox.send(new ExtensionRequest("30", params));
//	}

//	private void startGameSolo() {
//		System.out.println(this.botName + " start game solo");
//		ISFSObject params = new SFSObject();
//		params.putBool("solo", true);
//		this.smartFox.send(new ExtensionRequest("9", params));
//	}

	private void onConnection(BaseEvent event) {
		System.out.println(this.botName + " connection to server success");
		login();
	}

	private void onConnectionLost(BaseEvent event) {
		System.out.println(this.botName + " connection fail, try again");
		counter += 1;
		if (counter > 5) {
			disconnect();
		} else {
			start();
		}
	}

	private void onLogin(BaseEvent event) {
		// on login response
		System.out.println(this.botName + " on login response ");
	}

	private void onJoinRoom(BaseEvent event) {
		System.out.println(this.botName + " join room");
		ISFSObject obj = new SFSObject();
		this.smartFox.send(new ExtensionRequest("hello", obj));
//		if (this.getType() == BotType.AUTO_INVITE) {
//			Random rand = new Random();
//			int value = rand.nextInt(3);
//			if(value%2==0){
//				startRumbleGame(userIds);
//			}else {
//				if (this.userIds.size() > 0) {
//					System.out.println("number user onlines is: " + this.userIds.size());
//					for (int userId : this.userIds) {
//						 sendInviteRequest(userId);
//					}
//				} else {
////					 startGameSolo();
//				}
//			}
//		} 
		monitorLag();
	}

	private void onExtensionResponse(BaseEvent event) {
		String command = (String) event.getArguments().get("cmd");
//		System.out.println(this.botName + " receive command " + command);
		ISFSObject params = (SFSObject) event.getArguments().get("params");
		ISFSObject obj = new SFSObject();
		switch (command) {
		// ONLINE LIST
		case "27":
			SFSArray arr = new SFSArray();
			arr = (SFSArray) params.getSFSArray("lFound");
			if (arr != null) {
				for (int i = 0; i < arr.size(); i++) {
					SFSObject userOnline = (SFSObject) arr.getSFSObject(i);
					int userId = userOnline.getInt("id");
					String displayName = userOnline.getUtfString("display");
					System.out.println("user " + userId + " with " + displayName);
					if (!displayName.contains("qzkingBot")) {
						this.userIds.add(userId);
					}
				}
			} else {
				System.out.println("no body in server ");

			}

			break;
		// CLIENT_INVITE
		case "30":
			this.isInvited = true;
			String roomName = params.getUtfString("rName");
			String ownerName = params.getUtfString("ownerName");
			String ownerID = params.getUtfString("ownerID");
			int gameType = 0;
			if(params.containsKey("gameType")){
				gameType = params.getInt("gameType");
			}

			if (roomName == null) {
				break;
			}
			if (gameType == 2) {
				System.out.println("bot receive invite play rumble game ");
				// receive invite play rumble game
				ISFSObject param = new SFSObject();
				param.putBool("accept", true);
				param.putUtfString("rName", roomName);
				// obj.putBool("cancel", false);
				System.out.println("bot will join rumble game in room " + roomName);
				this.smartFox.send(new ExtensionRequest("38", param));
				break;
			}
			System.out.println(roomName + " & " + ownerName + " & " + ownerID);
			obj.putUtfString("rName", roomName);
			System.out.println(this.botName + " send accept room" + roomName);
			obj.putBool("accept", true);
			smartFox.send(new ExtensionRequest(Event.CLIENT_INVITERESPONSE, obj));
			obj = null;
			break;
		// client receive quiz -> send answer
		case "38":
			System.out.println("receive join rumble game response from server ");
			if(params.containsKey("current")){
				int currentUser = params.getInt("current");
				System.out.println("current player in game is "+currentUser);
			}

			break;
		case "29":
			System.out.println("receive message kick from server");
			break;
		case "12":
			System.out.println(this.botName + " send answer");
			int currentQuiz = params.getInt("currQuiz");
			String question = params.getUtfString("question");
			String correctAnswer = params.getUtfString("correctAnswer");
//			int correctId = params.getInt("correctId");
//			System.out.println("correct Id is "+correctId);
			System.out.println(this.botName + " play quiz " + currentQuiz + " question: " + question);
			int answerId = 0;
//			if(currentQuiz<=5){
////				answerId = correctId;
//			}else {
//				Random random = new Random();
//				int i = random.nextInt(3 - 0 + 1) + 0;
//				answerId = i;
//			}
			Random random = new Random();
			int i = random.nextInt(3 - 0 + 1) + 0;
			answerId = i;
			System.out.println(" correct answer: " + correctAnswer);
			obj = new SFSObject();
			obj.putUtfString("answer", correctAnswer);
			obj.putInt("awsId", answerId);
			Random rand = new Random();
			int sleep = rand.nextInt(15);
			try {
				Thread.sleep(sleep * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
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
			restart();
			break;
		// ERROR
		case "0":
			this.isInvited = false;
			if (params.containsKey("msg")) {
				String error = params.getUtfString("msg");
				System.out.println("has msg error: " + error);
			}
			if (params.containsKey("code")) {
				int errorCode = params.getInt("code");
				System.out.println("error code: " + errorCode);
			}
			 restart();
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

	public BotType getType() {
		return type;
	}

	public void setType(BotType type) {
		this.type = type;
	}
}
