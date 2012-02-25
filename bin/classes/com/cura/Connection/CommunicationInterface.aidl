package com.cura.Connection;

interface CommunicationInterface {
	
	String executeCommand(String command);
	void close();
	boolean connected();
}
