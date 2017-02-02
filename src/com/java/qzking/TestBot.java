package com.java.qzking;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.java.qzking.conf.BotType;

public class TestBot {
	public static void main(String[] args) {
		ExecutorService exevutor = Executors.newFixedThreadPool(10);
		exevutor.execute(new Runnable() {
			@Override
			public void run() {
				Bot bot = new Bot(BotType.random());
				bot.start();
			}
		});
	}
}
