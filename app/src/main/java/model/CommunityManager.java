package model;

import java.util.Map;

public class CommunityManager extends User {

    public CommunityManager(String name, Community community) {
        super(name, community);
        this.isManager = true;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        //map.put("managedUsers", managedUsersIds);
        return map;
    }
}
