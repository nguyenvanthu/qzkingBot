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
	private List<Integer> userIds = new ArrayList<>();
	private int counter = 0;
	private ScheduledExecutorService timer = Executors.newScheduledThreadPool(2);

	public Bot(BotType type) {
		this.setType(type);
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
		System.out.println("client test join room");
		if(this.userIds.size()>0){
			for(int userId : this.userIds){
				sendInviteRequest(userId);
			}
		}
		monitorLag();
	}

	private void onExtensionResponse(BaseEvent event) {
		String command = (String) event.getArguments().get("cmd");
		ISFSObject params = (SFSObject) event.getArguments().get("params");
		ISFSObject obj = new SFSObject();
		switch (command) {
		// ONLINE LIST
		case "27":
			if(this.getType() == BotType.AUTO_INVITE ){
				SFSArray arr = new SFSArray();
				arr = (SFSArray) params.getSFSArray("lFound");
				if(arr !=null){
					System.out.println("number user online is: "+arr.size());
					for(int i = 0; i<arr.size();i++){
						SFSObject userOnline = (SFSObject) arr.getSFSObject(i);
						int userId = userOnline.getInt("id");
						String displayName = userOnline.getUtfString("display");
						System.out.println("user "+userId+" with "+displayName);
						this.userIds.add(userId);
					}
				}else {
					System.out.println("no body in server ");
				}
			}
			break;
		// CLIENT_INVITE
		case "30":
			obj.putUtfString("rName", params.getUtfString("rName"));
			System.out.println("bot send accept room" + params.getUtfString("rName"));
			obj.putBool("accept", true);
			smartFox.send(new ExtensionRequest(Event.CLIENT_INVITERESPONSE, obj));
			obj = null;
			break;
		// CLIENT_ANSWER
		case "12":
			System.out.println("bot send answer");
			Random random = new Random();
			int i = random.nextInt(3 - 0 + 1) + 0;
			ArrayList<String> s = new ArrayList<String>(params.getUtfStringArray("answer"));
			obj = new SFSObject();
			obj.putUtfString("answer", s.get(i));
			smartFox.send(new ExtensionRequest(Event.ANSWER, obj));
			break;
		// LIST_QUIZ
		case "12.5":
			ISFSArray quizArr = new SFSArray();
			quizArr = (ISFSArray) event.getArguments().get("lQ");
			System.out.println("number quizs: "+quizArr.size());
			break;
		case "11":
			System.out.println("bot's game stopped");
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
		obj.putUtfString("fid", "test");
		obj.putUtfString("em", "test");

		LoginRequest request = new LoginRequest("test", "test", ServerConfig.ZONE, obj);
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
		System.out.println("send ping request");
		ISFSObject obj = new SFSObject();
		this.smartFox.send(new ExtensionRequest(Event.PING, obj));
	}
	
	private void sendInviteRequest(int invite_id){
		System.out.println("send invite request to user "+invite_id);
		ISFSObject obj = new SFSObject();
		obj.putInt("invite_id", invite_id);
//		obj.putBool("cancel", false);
		
		this.smartFox.send(new ExtensionRequest("30", obj));
	}

	public BotType getType() {
		return type;
	}

	public void setType(BotType type) {
		this.type = type;
	}
}
