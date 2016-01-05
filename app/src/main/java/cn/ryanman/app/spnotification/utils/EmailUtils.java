package cn.ryanman.app.spnotification.utils;

import android.util.Log;

import com.sun.mail.imap.IMAPMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

import cn.ryanman.app.spnotification.model.Item;
import cn.ryanman.app.spnotification.model.Request;

public class EmailUtils {

    public static final String PPMID = "PPM Project Number:";
    public static final String PROJECTNAME = "PPM Project Name:";
    public static final String TEAMS = "All Teams Needed:";
    public static final String COMPLETE = "Sizing - Complete:";
    public static final String PLANNINGCYCLE = "Planning Cycle:";
    public static final String COMMENTS = "Comments From Requester:";
    public static final String CONTACT = "Contact Name (SME) - Last, First:";
    public static final String STATUS = "PPM Status:";

    public static final int PPMID_ID = 1;
    public static final int PROJECTNAME_ID = 2;
    public static final int TEAMS_ID = 3;
    public static final int COMPLETE_ID = 4;
    public static final int PLANNINGCYCLE_ID = 5;
    public static final int COMMENTS_ID = 6;
    public static final int CONTACT_ID = 7;
    public static final int STATUS_ID = 8;

    public static final HashMap<String, Integer> map = createHashMap();

    private static HashMap<String, Integer> createHashMap() {
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        map.put(PPMID, PPMID_ID);
        map.put(PROJECTNAME, PROJECTNAME_ID);
        map.put(TEAMS, TEAMS_ID);
        map.put(COMPLETE, COMPLETE_ID);
        map.put(PLANNINGCYCLE, PLANNINGCYCLE_ID);
        map.put(COMMENTS, COMMENTS_ID);
        map.put(CONTACT, CONTACT_ID);
        map.put(STATUS, STATUS_ID);
        return map;
    }

    public static List<Request> parseEmailBody(Message[] messages) throws MessagingException, IOException {
        List<Request> requests = new ArrayList<Request>();
        for (Message message : messages) {
            IMAPMessage msg = (IMAPMessage) message;
            String subject = MimeUtility.decodeText(msg.getSubject());
            System.out.println("Subject: [" + subject + "]");
            if (subject.startsWith("FW: HPIT Request for Sizing - ")) {
                subject = subject.replace("FW: HPIT Request for Sizing -", "").replaceAll(" ", "");
                Object content = message.getContent();
                if (content instanceof MimeMultipart) {
                    MimeMultipart multipart = (MimeMultipart) content;
                    Request request = parseContent(multipart, subject);
                    if (request != null) {
                        requests.add(request);
                        Log.d("SPNotification", request.getPpmid());
                    }
                }
            }
            System.out.println("-------------------------------------------");
        }


        return requests;
    }

    private static Request parseContent(MimeMultipart multipart, String ppmid) throws IOException, MessagingException {
        if (multipart.getCount() < 2) {
            return null;
        }

        BodyPart plainPart = multipart.getBodyPart(0);
        if (!plainPart.isMimeType("text/plain")) {
            return null;
        }

        String content = plainPart.getContent().toString();
        if (!content.contains(Value.HPSBTEAM)) {
            return null;
        }

        Request request = parsePlainEmail(content, ppmid);
        if (request == null) {
            return null;
        }

        if (request.getOperation() == Request.DELETE) {
            return request;
        }

        BodyPart htmlPart = multipart.getBodyPart(1);
        if (!htmlPart.isMimeType("text/html")) {
            return null;
        }
        return parseHTMLEmail(htmlPart.getContent().toString(), request);

    }

    private static Request parsePlainEmail(String content, String ppmid) {

        if (!content.contains(Value.HPSBTEAM))
            return null;

        Request request = new Request();
        // Set Request PPMID
        request.setPpmid(ppmid);
        // Scan Email Content
        Scanner scanner = new Scanner(content);
        String line = null; //记录Scanner读取当前的行
        while (scanner.hasNextLine()) {
            line = scanner.nextLine().trim();
            if ("".equals(line)) {
                continue;
            }
            // Get Request Operation
            else if (line.startsWith(ppmid + " has been ")) {
                if (line.toLowerCase().endsWith("changed")) {
                    request.setOperation(Request.CHANGE);
                } else if (line.toLowerCase().endsWith("added")) {
                    request.setOperation(Request.ADD);
                } else if (line.toLowerCase().endsWith("deleted")) {
                    request.setOperation(Request.DELETE);
                }
                return request;
            }
        }
        return null;
    }

    private static Request parseHTMLEmail(String content, Request request) {

        //从HTML提取内容
        Pattern pattern = Pattern.compile("<body[\\S\\s]*</body>"); //<body></body>之间的代码
        Matcher matcher = pattern.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            sb.append(matcher.group());
            sb.append("\n");
        }
        String formatted = sb.toString().replaceAll("\n", "").replaceAll("&amp;", "&").replaceAll("<br>", Value.BR)
                .replaceAll("<span class=\"editedicon\">Edited</span>",
                        "<span class=\"editedicon\">" + Value.EDITED + "</span>")
                .replaceAll("<span class=\"edited\">", "<span class=\"edited\">" + Value.OLDEDITED)
                .replaceAll("<.*?>", "\n").replaceAll("&nbsp;", "").replace("\r", "").replaceAll("\n{2,}", "\n");
        System.out.println(formatted);

        Scanner scanner = new Scanner(formatted);
        String line = null;
        String title = null;
        String item = null;
        boolean started = false;
        while (scanner.hasNextLine()) {
            line = scanner.nextLine().trim().replaceAll("^\\s+", "");
            if (!started) {
                started = line.startsWith(PLANNINGCYCLE);
            }
            if (started) {
                if (line.equals("")) {
                    continue;
                }
                if (line.endsWith(":")) {
                    if (title != null && item != null) {
                        setRequest(request, title, item);
                    }
                    title = line;
                    item = null;
                } else if (line.equals(Value.EDITED)) {
                    if (item != null) {
                        item = Value.SPLIT + item;
                    }
                } else if (line.startsWith(Value.LASTMODIFY)) {
                    request.setLastmodified(line);
                } else {
                    if (item == null) {
                        item = line;
                    } else {
                        item += Value.SPLIT + line;
                    }
                }
            }
        }
        if (title != null && item != null) {
            setRequest(request, title, item);
        }
        //request.print();
        return request;
    }

    private static void setRequest(Request request, String title, String item) {
        if (!map.containsKey(title)) {  //title不在提取的列表中
            return;
        }
        switch (map.get(title)) {
            case PPMID_ID:  // Get Request Project PPMID
                if (!item.contains(Value.SPLIT)) {
                    request.setPpmid(item);
                } else {
                    Item ppmItem = AppUtils.parseEdited(item);
                    if (ppmItem != null) {
                        if (ppmItem.getNewValue() != null) {
                            request.setPpmid(ppmItem.getNewValue());
                        }
                        if (ppmItem.getOldValue() != null) {
                            request.setOldPpmid(ppmItem.getOldValue());
                        }
                    }
                }
                break;
            case PROJECTNAME_ID:  // Get Request Project Name
                request.setProjectName(item);
                break;
            case TEAMS_ID:  // Get Request All-Teams-Needed
                if (!item.contains(Value.HPSBTEAM)) {  //再次检测 HPSB TEAM
                    request.setHPSB(false);
                } else {
                    request.setHPSB(true);
                }
                request.setTeams(item);
                break;
            case COMPLETE_ID:  // Get Request Sizing-Complete
                request.setComplete(item);
                break;
            case PLANNINGCYCLE_ID: // Get Request Planning Cycle
                request.setPlanningCycle(item);
                break;
            case COMMENTS_ID:  // Get Request Comments
                request.setComments(item);
                break;
            case CONTACT_ID:  // Get Request Status
                request.setContact(item);
                break;
            case STATUS_ID:  // Get Request Status
                request.setPpmStatus(item);
                break;
        }
    }
}