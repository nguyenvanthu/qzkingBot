package com.java.qzking;

import com.java.qzking.conf.BotType;

public class SuperBot {
	public static void main(String[] args) {
		for(int i = 0 ; i<21 ;i++){
			BotType type = BotType.AVAILABlE;
//			if(i %2 ==0){
//				type = BotType.AUTO_INVITE;
//			}
			Bot bot = new Bot(type,i);
			bot.start();
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
