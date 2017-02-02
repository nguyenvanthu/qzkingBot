package com.java.qzking.conf;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public enum BotType {
	AVAILABlE, AUTO_INVITE;
	private static AtomicInteger seed = null;

	private static int genCode() {
		if (seed == null) {
			seed = new AtomicInteger(0);
		}
		return seed.getAndIncrement();
	}

	private int code = genCode();

	public int getCode() {
		return this.code;
	}

	public static BotType fromCode(int code) {
		for (BotType type : values()) {
			if (type.getCode() == code) {
				return type;
			}
		}
		return null;
	}
	
	public static BotType random(){
		Random rand = new Random();
		int code = rand.nextInt(values().length);
		return BotType.fromCode(code);
	}
}
