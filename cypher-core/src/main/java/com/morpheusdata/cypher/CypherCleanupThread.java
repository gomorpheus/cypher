package com.morpheusdata.cypher;

import java.util.List;

/**
 * Created by davydotcom on 10/18/17.
 */
public class CypherCleanupThread extends Thread {

	private List<Cypher> cypherClasses;
	public Boolean run = true;
	public CypherCleanupThread(String name) {
		super(name);

	}

	@Override
	public void run() {
		while(run) {
			if(Cypher.cypherClasses != null && Cypher.cypherClasses.size() > 0) {
				for(Cypher cypher : Cypher.cypherClasses) {
					try {
						cypher.purgeExpiredKeys();
					} catch(Exception ex) {
						System.out.println("Error purging expired keys: " + ex.getMessage());
						ex.printStackTrace();
						// ignore silently errors related to purging of expired keys
					}
				}
			}
			try {
				Thread.sleep(60000);
			} catch(InterruptedException ex) {

			}
		}
	}
}
