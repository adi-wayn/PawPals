package model;

import java.util.ArrayList;
public class Community {
    private String name;
    private CommunityManager Manager;
    public ArrayList<User> Members;
    public  ArrayList<Report> Reports;

    public Community(String name) {
        this.name = name;
        this.Members = new ArrayList<>();
        this.Reports = new ArrayList<>();
    }

    public Community(String name, CommunityManager manager) {
        this.name = name;
        this.Manager = manager;
        this.Members = new ArrayList<>();
        this.Reports = new ArrayList<>();
    }


    public String getName() { return name; }

    public CommunityManager getManager() {
        return Manager;
    }

    public void setManager(CommunityManager manager) {
        this.Manager = manager;
    }

    public ArrayList<Report> getReports(){
        return Reports;
    }
    //פונקציה שמחזירה את כל מי שמטייפ מסויים
    public ArrayList<Report> getReportByType(String s){
        ArrayList<Report> reportByType=new ArrayList<>();
        for (int i =0;i <Reports.size();i++){
            Report temp = Reports.get(i);
            if (temp.type=="Ad"){
                reportByType.add(temp);
            }
            if (temp.type=="Private message"){
                reportByType.add(temp);
            }
            if (temp.type=="Complaint"){
                reportByType.add(temp);
            }
        }
        return reportByType;
    }

    @Override
    public String toString() {
        return name;
    }
    public void  addReport(Report r){
        this.Reports.add(r);
    }
}
