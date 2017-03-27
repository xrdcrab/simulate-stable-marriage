/*
 * Name: Ruida Xie
 * NSID: rux793
 * Student Number: 11194258
 */
package StableMarriageSimulator;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Inbox;
import akka.actor.Props;
import akka.actor.UntypedActor;

/**
 * @author ruidaxie
 *
 */
public class StableMarriageSimulator {
	
	static int num = 0;
	static HashMap<Object, Object> result = new HashMap<Object, Object>();
	
	// Represents the 2 possible messages woman can send to man
	private enum ManReceive {
		YES, NO, START
	};
	
	private static class Man extends UntypedActor {
		int count = 0;
		// generate the preference rank
		ArrayList<Object> manPreference = new ArrayList<Object>();
		
		public void preStart(){
			
	        Random random = new Random();
	        HashMap<Object, Object> hashMap = new HashMap<Object, Object>();
	        
	        // generate random number store it in HashMap
	        for(int i = 0;i < num;i++){
	            int number = random.nextInt(num);
	            while (hashMap.containsValue("Woman-" + number)) {
	            	number = random.nextInt(num);
				}
	            hashMap.put(i, "Woman-" + number);
	        }
	        
	        // store each value of HashMap in preference list
	        for (Object object : hashMap.values()) {
	        	manPreference.add(object);
			}
	        
	        // print actor name and its preference list
	        System.out.println(getSelf().path().name() + " 's rank: ");
	        for(int i = 0;i < manPreference.size();i++){
	            System.out.print(manPreference.get(i) + ",\t");
	            if (i == manPreference.size()-1) {
					System.out.print("\n");
				}
	        }
		}
		
		public void onReceive(Object message) {
			if (message instanceof ManReceive) {
				ManReceive m = (ManReceive) message;
				if (m == ManReceive.START) {
					getContext().actorSelection("/user/" + manPreference.get(count)).tell("MarryMe", getSelf());
				} else if (m == ManReceive.YES) {
					String sender = getSender().path().name();
					result.put(getSelf().path().name(), sender);
				} else {
					// if woman said no, try next
					System.out.println("===" + getSelf().path().name() + " refused by " + getSender().path().name() + ", try next=== ");
					
					count++;
					if (count < manPreference.size()) {
						getContext().actorSelection("/user/" + manPreference.get(count)).tell("MarryMe", getSelf());
					}
				}
			} else {
				unhandled(message);
			}
		}
	}
	
	private static class Woman extends UntypedActor {
		// generate the preference rank
		ArrayList<Object> womanPreference = new ArrayList<Object>();
		HashMap<Object, Object> match = new HashMap<Object, Object>();
		
		public void preStart() {
			
			match.put(getSelf().path().name(), "No");
			
			Random random = new Random();
			HashMap<Object, Object> hashMap = new HashMap<Object, Object>();
		
			// generate random number store it in HashMap
			for(int i = 0;i < num;i++){
				int number = random.nextInt(num);
				while (hashMap.containsValue("Man-" + number)) {
					number = random.nextInt(num);
				}
				hashMap.put(i, "Man-" + number);
			}
			
			// store each value of HashMap in preference list
			for (Object object : hashMap.values()) {
				womanPreference.add(object);
			}
			
			// print actor name and its preference list
	        System.out.println(getSelf().path().name() + " 's rank: ");
	        for(int i = 0;i < womanPreference.size();i++){
	            System.out.print(womanPreference.get(i) + "\t");
	            if (i == womanPreference.size()-1) {
					System.out.print("\n");
				}
	        }
		}
		
		public void onReceive(Object message) {
			if (message instanceof String) {
				String m = (String) message;
				if (m.equals("MarryMe")) {
					System.out.println(getSender().path().name() + " asked " + getSelf().path().name());
					if (match.get(getSelf().path().name()).equals("No")) {
						match.put(getSelf().path().name(), getSender().path().name());
						getSender().tell(ManReceive.YES, getSelf());
					} else {
						if ( womanPreference.indexOf(match.get(getSelf().path().name())) > womanPreference.indexOf(getSender().path().name()) ) {
							// the woman prefer the new comer
							// say no to previous one
							String previous = (String) match.get(getSelf().path().name());
							match.put(getSelf().path().name(), getSender().path().name());
							getContext().actorSelection("/user/" + previous).tell(ManReceive.NO, getSelf());
							// say yes to the new comer 
							getSender().tell(ManReceive.YES, getSelf());
							
						} else {
							// the woman prefer the previous man
							// say no to the new comer
							getSender().tell(ManReceive.NO, getSelf());
						}
					}
				}
			} else {
				unhandled(message);
			}
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		// Prompt the user for a number of men and women and generate that many man and woman actors
		Scanner in = new Scanner(System.in);
		while (num <= 0) {
			System.out.print("Enter amount (int > 0) of men and women to be generated: ");
			num = in.nextInt();
		}
		in.close();
		
		// create an actor system
		final ActorSystem actorSystem = ActorSystem.create("stable-marriage-actor-system");
		
		// create the witness actor
		
		final ActorRef[] man = new ActorRef[num];
		final ActorRef[] woman = new ActorRef[num];
		
		// create women and men
		for (int i = 0; i < num; i++) {
			woman[i] = actorSystem.actorOf(Props.create(Woman.class), "Woman-" + i);
		}
		for (int i = 0; i < num; i++) {
			man[i] = actorSystem.actorOf(Props.create(Man.class), "Man-" + i);
		}
		// initialize the result
		for (int i = 0; i < num; i++) {
			result.put(man[i].path().name(), null);
		}
		
		// Create an inbox
		final Inbox inbox = Inbox.create(actorSystem);
		

		// tell man start
		System.out.println();
		System.out.println("Start Matching: \nThe result will be shown in 2 seconds.");
		for (int i = 0; i < num; i++) {
			inbox.send(man[i], ManReceive.START);
		}
		
		
		// wait 2 seconds and show the result
		try {
			Thread.sleep(2000);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		System.out.println("\nThe result is: ");
		for(int i = 0; i < num; i++) {
			System.out.println(man[i].path().name() + " married with " + result.get(man[i].path().name()));
		}
		
		// Shut down the system gracefully
		actorSystem.terminate();
	}

}
