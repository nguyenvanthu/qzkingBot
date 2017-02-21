package com.java.qzking;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.java.qzking.conf.BotType;

public class SuperBot {
	public static void main(String[] args) {
		ExecutorService exevutor = Executors.newSingleThreadExecutor();
		exevutor.execute(new Runnable() {
			@Override
			public void run() {
				for(int i = 1 ; i<3 ;i++){
					BotType type = BotType.AVAILABlE;
					if(i %2 ==0){
						type = BotType.AUTO_INVITE;
					}
					Bot bot = new Bot(type,i);
					bot.start();
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}
}
