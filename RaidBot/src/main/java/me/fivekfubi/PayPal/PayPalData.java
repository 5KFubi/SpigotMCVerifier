package me.fivekfubi.PayPal;

public class PayPalData {
    private String purchaseType;
    private String resourceId;
    private String email;

    public PayPalData(String customField, String email) {
        this.purchaseType = splitCustomField(customField)[0];
        this.resourceId = splitCustomField(customField)[splitCustomField(customField).length - 1];
        this.email = email;
    }

    private String[] splitCustomField(String customField){
        return customField.split("\\|");
    }

    // Setters
    public void setPurchaseType(String purchaseType){
        this.purchaseType = purchaseType;
    }

    public void setResourceId(String resourceId){
        this.resourceId = resourceId;
    }

    public void setEmail(String email){
        this.email = email;
    }

    // Getters

    public String getPurchaseType(){
        return purchaseType;
    }

    public String getResourceId(){
        return resourceId;
    }

    public String getEmail(){
        return email;
    }

}
