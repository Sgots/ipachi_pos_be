package com.ipachi.pos.dto;

public record NewUserSetupResponse(
        Profile profile,
        Business business
) {
    public record Profile(
            String title, String gender, String dob,
            String idType, String idNumber,
            String postalAddress, String physicalAddress, String city, String country, String areaCode, String phone,
            String pictureUrl, String idDocUrl
    ) {}
    public record Business(
            String name, String location, String logoUrl
    ) {}
}
