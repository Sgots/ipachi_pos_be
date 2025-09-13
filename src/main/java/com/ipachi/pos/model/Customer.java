package com.ipachi.pos.model;

public class Customer {
    private Long id;
    private String firstname;
    private String lastname;
    private String mobileNumber;
    private String identificationNumber;
    private double availableBalance;
    private String fingerprintTemplate; // nullable

    public Customer() {}

    public Customer(Long id, String firstname, String lastname, String mobileNumber, String identificationNumber,
                    double availableBalance, String fingerprintTemplate) {
        this.id = id;
        this.firstname = firstname;
        this.lastname = lastname;
        this.mobileNumber = mobileNumber;
        this.identificationNumber = identificationNumber;
        this.availableBalance = availableBalance;
        this.fingerprintTemplate = fingerprintTemplate;
    }

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFirstname() { return firstname; }
    public void setFirstname(String firstname) { this.firstname = firstname; }
    public String getLastname() { return lastname; }
    public void setLastname(String lastname) { this.lastname = lastname; }
    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }
    public String getIdentificationNumber() { return identificationNumber; }
    public void setIdentificationNumber(String identificationNumber) { this.identificationNumber = identificationNumber; }
    public double getAvailableBalance() { return availableBalance; }
    public void setAvailableBalance(double availableBalance) { this.availableBalance = availableBalance; }
    public String getFingerprintTemplate() { return fingerprintTemplate; }
    public void setFingerprintTemplate(String fingerprintTemplate) { this.fingerprintTemplate = fingerprintTemplate; }
}
