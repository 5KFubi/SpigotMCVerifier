package me.fivekfubi.Discord;

public class ResourceData {

    private String resourceId;
    private String roleId;

    public ResourceData(String resourceId, String roleId){
        this.resourceId = resourceId;
        this.roleId = roleId;
    }


    public void setResourceId(String resourceId){
        this.resourceId = resourceId;
    }

    public void setRoleId(String roleId){
        this.roleId = roleId;
    }


    public String getResourceId(){
        return resourceId;
    }

    public String getRoleId(){
        return roleId;
    }


}
