package com.java.qzking;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.java.qzking.conf.BotType;
import com.java.qzking.conf.Event;
import com.java.qzking.conf.ServerConfig;
import com.java.qzking.event.BaseEventHandler;
import com.smartfoxserver.v2.entities.data.ISFSObject;
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

	private IEventListener onConnection = new BaseEventHandler(this, "onConnection");
	private IEventListener onConnectionLost = new BaseEventHandler(this, "onConnectionLost");
	private IEventListener onLogin = new BaseEventHandler(this, "onLogin");
	private IEventListener onJoinRoom = new BaseEventHandler(this, "onJoinZoom");
	private IEventListener onExtensionResponse = new BaseEventHandler(this, "onExtensionResponse");

	private ScheduledExecutorService timer = Executors.newScheduledThreadPool(2);

	public Bot(BotType type) {
		this.setType(type);
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
		smartFox.addEventListener(SFSEvent.CONNECTION, onConnection);
		smartFox.addEventListener(SFSEvent.CONNECTION_LOST, onConnectionLost);
		smartFox.addEventListener(SFSEvent.LOGIN, onLogin);
		smartFox.addEventListener(SFSEvent.ROOM_JOIN, onJoinRoom);
		smartFox.addEventListener(SFSEvent.EXTENSION_RESPONSE, onExtensionResponse);
	}

	private void connect() {
		System.out.println("connecting to server .....");
		this.smartFox.connect(ServerConfig.SERVER_ADDRESS, ServerConfig.PORT);
	}

	@SuppressWarnings("unused")
	private void disconnect() {
		this.smartFox.disconnect();
		this.timer.shutdown();
	}

	@SuppressWarnings("unused")
	private void onConnection(BaseEvent event) {
		if (event.getArguments().get("success").equals("true")) {
			// connection success
			System.out.println("connection to server success");
			login();
		}
	}

	@SuppressWarnings("unused")
	private void onConnectionLost(BaseEvent event) {
		System.out.println("connection fail, try again");
		start();
	}

	@SuppressWarnings("unused")
	private void onLogin(BaseEvent event) {
		// on login response
		System.out.println("on login response ");
	}

	@SuppressWarnings("unused")
	private void onJoinRoom(BaseEvent event) {
		System.out.println("client test join room");
		monitorLag();
	}

	@SuppressWarnings("unused")
	private void onExtensionResponse(BaseEvent event) {
		String command = (String) event.getArguments().get("cmd");
		ISFSObject params = (SFSObject) event.getArguments().get("params");
		ISFSObject obj = new SFSObject();
		switch (command) {
		// CLIENT_INVITE
		case "30":
			obj.putUtfString("rName", params.getUtfString("rName"));
			System.out.println("----bot send accept room" + params.getUtfString("rName"));
			obj.putBool("accept", true);
			smartFox.send(new ExtensionRequest(Event.CLIENT_INVITERESPONSE, obj));
			obj = null;
			break;
		// CLIENT_ANSWER
		case "12":
			System.out.println("----bot send answer");
			Random random = new Random();
			int i = random.nextInt(3 - 0 + 1) + 0;
			ArrayList<String> s = new ArrayList<String>(params.getUtfStringArray("answer"));
			obj = new SFSObject();
			obj.putUtfString("answer", s.get(i));
			smartFox.send(new ExtensionRequest(Event.ANSWER, obj));
			break;
		case "11":
			System.out.println("----bot's game stopped");
			break;
		default:
			break;
		}
	}

	@Override
	public void dispatch(BaseEvent event) throws SFSException {
		String type = event.getType();
		if (type.equalsIgnoreCase(SFSEvent.CONNECTION)) {
			if (event.getArguments().get("success").equals("true")) {
				// connection success
			}
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
		ISFSObject obj = new SFSObject();
		this.smartFox.send(new ExtensionRequest(Event.PING, obj));
	}

	public BotType getType() {
		return type;
	}

	public void setType(BotType type) {
		this.type = type;
	}
}
