/*
 Copyright© 2010, 2011 Ahmad Balaa, Oday Maleh

 This file is part of Cura.

	Cura is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Cura is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Cura.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.cura.security;

/*
 * Description: This class implements the Service and is used to listen for when the pattern that the user chooses is sent
 * to the phone that has been compromised (this pattern is set in the Settings menu item from the Login Screen) upon which
 * it sends an E-mail to the emergency e-mail address that the user specified in that same Settings menu item, containing the 
 * location of the compromised phone. We used GPS location to determine that.
 */

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class GMailSender extends javax.mail.Authenticator {
 private String mailhost = "smtp.gmail.com";
 private String user;
 private String password;
 private Session session;

 public GMailSender(String user, String password) {
  this.user = user;
  this.password = password;

  Properties props = new Properties();
  props.setProperty("mail.transport.protocol", "smtp");
  props.setProperty("mail.host", mailhost);
  props.put("mail.smtp.auth", "true");
  props.put("mail.smtp.port", "465");
  props.put("mail.smtp.socketFactory.port", "465");
  props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
  props.put("mail.smtp.socketFactory.fallback", "false");
  props.setProperty("mail.smtp.quitwait", "false");

  session = Session.getDefaultInstance(props, this);
 }

 protected PasswordAuthentication getPasswordAuthentication() {
  return new PasswordAuthentication(user, password);
 }

 public synchronized void sendMail(String subject, String body, String sender, String recipients) throws Exception {
  try {
   MimeMessage message = new MimeMessage(session);
   DataHandler handler = new DataHandler(new ByteArrayDataSource(body.getBytes(), "text/plain"));
   message.setSender(new InternetAddress(sender));
   message.setSubject(subject);
   message.setDataHandler(handler);
   if(recipients.indexOf(',') > 0)
	message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients));
   else
	message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipients));
   Transport.send(message);
  }
  catch (Exception e) {

  }
 }

 public class ByteArrayDataSource implements DataSource {
  private byte[] data;
  private String type;

  public ByteArrayDataSource(byte[] data, String type) {
   super();
   this.data = data;
   this.type = type;
  }

  public ByteArrayDataSource(byte[] data) {
   super();
   this.data = data;
  }

  public void setType(String type) {
   this.type = type;
  }

  public String getContentType() {
   if(type == null)
	return "application/octet-stream";
   else
	return type;
  }

  public InputStream getInputStream() throws IOException {
   return new ByteArrayInputStream(data);
  }

  public String getName() {
   return "ByteArrayDataSource";
  }

  public OutputStream getOutputStream() throws IOException {
   throw new IOException("Not Supported");
  }
 }
}