package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Report {
    public String type;
    public String complainant;
    public  String subject;
    public  String tekst;

    public Report(String type, String complainant, String subject, String tekst){
        this.type=type;
        this.complainant =complainant;
        this.subject =subject;
        this.tekst =tekst;
    }
    public Report(){
        this.type=null;
        this.complainant =null;
        this.subject =null;
        this.tekst =null;
    }
    public String getType(){
        return type;
    }
    public String getComplainant(){
        return complainant;
    }
    public String getSubject(){
        return subject;
    }
    public String getTekst(){
        return tekst;
    }
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("Type", this.type);
        map.put("Complainant", this.complainant);
        map.put("Subject", this.subject);
        map.put("Tekst", this.tekst);

        return map;
    }


}
