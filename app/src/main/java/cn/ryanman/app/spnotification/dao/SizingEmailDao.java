package cn.ryanman.app.spnotification.dao;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

import cn.ryanman.app.spnotification.model.Item;
import cn.ryanman.app.spnotification.model.Request;
import cn.ryanman.app.spnotification.utils.AppUtils;
import cn.ryanman.app.spnotification.utils.Value;

public class SizingEmailDao implements EmailDao {

    private final String protocol = "imap";
    private final String username = "gis_hpe@yahoo.com";
    private final String password = "gis123456";

    private final String PPMID = "PPM Project Number:";
    private final String PROJECTNAME = "PPM Project Name:";
    private final String TEAMS = "All Teams Needed:";
    private final String COMPLETE = "Sizing - Complete:";
    private final String PLANNINGCYCLE = "Planning Cycle:";
    private final String COMMENTS = "Comments From Requester:";
    private final String CONTACT = "Contact Name (SME) - Last, First:";
    private final String STATUS = "PPM Status:";

    private final int PPMID_ID = 1;
    private final int PROJECTNAME_ID = 2;
    private final int TEAMS_ID = 3;
    private final int COMPLETE_ID = 4;
    private final int PLANNINGCYCLE_ID = 5;
    private final int COMMENTS_ID = 6;
    private final int CONTACT_ID = 7;
    private final int STATUS_ID = 8;

    private HashMap<String, Integer> titleMap;
    private DatabaseDao databaseDao;
    private Context context;
    private SharedPreferences pref;

    public SizingEmailDao(Context context) {
        databaseDao = new SizingDatabaseDao();
        this.context = context;
        this.pref = context.getSharedPreferences(Value.APPINFO, Context.MODE_PRIVATE);
        titleMap = new HashMap<String, Integer>();
        titleMap.put(PPMID, PPMID_ID);
        titleMap.put(PROJECTNAME, PROJECTNAME_ID);
        titleMap.put(TEAMS, TEAMS_ID);
        titleMap.put(COMPLETE, COMPLETE_ID);
        titleMap.put(PLANNINGCYCLE, PLANNINGCYCLE_ID);
        titleMap.put(COMMENTS, COMMENTS_ID);
        titleMap.put(CONTACT, CONTACT_ID);
        titleMap.put(STATUS, STATUS_ID);
    }

    @Override
    public int getEmail() throws Exception {

        Properties props = System.getProperties();
        props.setProperty("mail.store.protocol", protocol);
        props.setProperty("mail.imap.host", "imap.mail.yahoo.com");
        props.setProperty("mail.imap.port", "993");
        props.put("mail.imap.ssl.enable", true);

        Session session = Session.getInstance(props);

        Store store = session.getStore(protocol);
        store.connect(username, password);
        IMAPFolder inbox = (IMAPFolder)store.getFolder("INBOX");
        inbox.open(Folder.READ_WRITE);
        Message messages[] = inbox.getMessages();
        Log.d("SPNotification", "收件箱中共" + messages.length + "封邮件!");

        long lastEmailUid = pref.getLong(Value.LASTEMAILUID, -1);
        Log.d("SPNotification", "lastEmailUid: " + lastEmailUid);

        if (lastEmailUid != -1 && messages.length > 0) {
            Log.d("SPNotification", "最大UID: " + inbox.getUID(messages[messages.length - 1]));
            for (int i = messages.length - 1; i >= 0; i--) {
                long uid = inbox.getUID(messages[i]);
                if (lastEmailUid >= uid) {
                    //i + 1 to messages.length - 1;
                    messages = Arrays.copyOfRange(messages, i + 1, messages.length);
                    break;
                }
            }
        }
        Log.d("SPNotification", "收件箱中共" + messages.length + "封未读邮件!");
        //Message messages[] = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));

        if (messages.length == 0) {
            return 0;
        }

        List<Request> requests = parseEmailBody(inbox, messages);
        if (requests != null) {
            databaseDao.addRecords(context, requests);
        }
        SharedPreferences.Editor editor = pref.edit();
        editor.putLong(Value.LASTEMAILUID, inbox.getUID(messages[messages.length - 1]));
        editor.commit();
        Log.d("SPNotification", "当前UID: " + inbox.getUID(messages[messages.length - 1]));
        inbox.close(false);
        store.close();
        return requests.size();
    }


    private List<Request> parseEmailBody(IMAPFolder folder, Message[] messages) throws Exception {
        List<Request> requests = new ArrayList<Request>();
        int count = 1;
        for (Message message : messages) {
            IMAPMessage msg = (IMAPMessage) message;

            String subject = MimeUtility.decodeText(msg.getSubject());
            Log.d("SPNotification", count++ + "/" + messages.length + "  Subject: [" + subject + "]");
            if (subject.startsWith("FW: HPIT Request for Sizing - ")) {
                subject = subject.replace("FW: HPIT Request for Sizing -", "").replaceAll(" ", "");
                Object content = message.getContent();
                if (content instanceof MimeMultipart) {
                    MimeMultipart multipart = (MimeMultipart) content;
                    Request request = parseContent(multipart, subject);
                    if (request != null) {
                        //Set Request MessageUID
                        request.setUid(folder.getUID(msg));
                        requests.add(request);
                        Log.d("SPNotification", request.getPpmid());
                    }
                    else{
                        //Archive Email

                    }
                }
            }
            System.out.println("-------------------------------------------");
        }
        return requests;
    }

    private Request parseContent(MimeMultipart multipart, String ppmid) throws IOException, MessagingException {
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

    private Request parsePlainEmail(String content, String ppmid) {

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

    private Request parseHTMLEmail(String content, Request request) {

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
        //System.out.println(formatted);

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

    private void setRequest(Request request, String title, String item) {
        if (!titleMap.containsKey(title)) {  //title不在提取的列表中
            return;
        }
        switch (titleMap.get(title)) {
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
