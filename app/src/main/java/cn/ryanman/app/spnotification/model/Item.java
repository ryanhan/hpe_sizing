package cn.ryanman.app.spnotification.model;

/**
 * Created by Ryan on 2015/12/28.
 */
public class Item {

    private String newValue;
    private String oldValue;

    public Item() {
    }

    public Item(String newValue, String oldValue) {
        this.newValue = newValue;
        this.oldValue = oldValue;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }
}
