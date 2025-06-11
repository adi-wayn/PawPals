package model;

import java.util.HashMap;
import java.util.Map;

public class Report {
    public String type;
    public String senderName;
    public  String subject;
    public  String text;

    public Report(String type, String senderName, String subject, String text){
        this.type=type;
        this.senderName = senderName;
        this.subject =subject;
        this.text = text;
    }
    public Report(){
    }
    public String getType(){
        return type;
    }
    public String getSenderName(){
        return senderName;
    }
    public String getSubject(){
        return subject;
    }
    public String getText(){
        return text;
    }
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("Type", this.type);
        map.put("Complainant", this.senderName);
        map.put("Subject", this.subject);
        map.put("Tekst", this.text);

        return map;
    }


}
