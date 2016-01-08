package cn.ryanman.app.spnotification.model;

public class Request {

    public static final int ADD = 1;
    public static final int CHANGE = 2;
    public static final int DELETE = 3;
    public static final int UNREAD = 4;
    public static final int READ = 5;
    public static final int NOT_STARTED = 6;
    public static final int ASSIGNED = 7;
    public static final int SHARED = 8;

    private String ppmid;
    private String oldPpmid;
    private String planningCycle;
    private String projectName;
    private String comments;
    private String contact;
    private String ppmStatus;
    private String teams;
    private String complete;
    private int operation;
    private boolean HPSB;
    private boolean read;
    private int status;
    private String resource;
    private String lastmodified;
    private boolean important;
    private String sentDate;

    public String getPpmid() {
        return ppmid;
    }

    public void setPpmid(String ppmid) {
        this.ppmid = ppmid;
    }

    public String getOldPpmid() {
        return oldPpmid;
    }

    public void setOldPpmid(String oldPpmid) {
        this.oldPpmid = oldPpmid;
    }

    public String getPlanningCycle() {
        return planningCycle;
    }

    public void setPlanningCycle(String planningCycle) {
        this.planningCycle = planningCycle;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getPpmStatus() {
        return ppmStatus;
    }

    public void setPpmStatus(String ppmStatus) {
        this.ppmStatus = ppmStatus;
    }

    public String getTeams() {
        return teams;
    }

    public void setTeams(String teams) {
        this.teams = teams;
    }

    public String getComplete() {
        return complete;
    }

    public void setComplete(String complete) {
        this.complete = complete;
    }

    public int getOperation() {
        return operation;
    }

    public void setOperation(int operation) {
        this.operation = operation;
    }

    public boolean isHPSB() {
        return HPSB;
    }

    public void setHPSB(boolean HPSB) {
        this.HPSB = HPSB;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getLastmodified() {
        return lastmodified;
    }

    public void setLastmodified(String lastmodified) {
        this.lastmodified = lastmodified;
    }

    public boolean isImportant() {
        return important;
    }

    public void setImportant(boolean important) {
        this.important = important;
    }

    public String getSentDate() {
        return sentDate;
    }

    public void setSentDate(String sentDate) {
        this.sentDate = sentDate;
    }

    @Override
    public String toString() {
        return "Request{" +
                "\nppmid='" + ppmid + '\'' +
                "\noldPpmid='" + oldPpmid + '\'' +
                "\nplanningCycle='" + planningCycle + '\'' +
                "\nprojectName='" + projectName + '\'' +
                "\ncomments='" + comments + '\'' +
                "\ncontact='" + contact + '\'' +
                "\nppmStatus='" + ppmStatus + '\'' +
                "\nteams='" + teams + '\'' +
                "\ncomplete='" + complete + '\'' +
                "\noperation=" + operation +
                "\nHPSB=" + HPSB +
                "\nread=" + read +
                "\nstatus=" + status +
                "\nresource='" + resource + '\'' +
                "\nlastmodified='" + lastmodified + '\'' +
                "\nimportant=" + important +
                "\n}";
    }

    public void print() {

        System.out.println("--------START TO PRINT NEW REQUEST--------");

        System.out.println(this.toString());

        System.out.println("--------STOP TO PRINT NEW REQUEST--------");
    }


}
