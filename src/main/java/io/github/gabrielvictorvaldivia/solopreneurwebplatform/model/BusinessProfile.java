package io.github.gabrielvictorvaldivia.solopreneurwebplatform.model;

import lombok.Data;

@Data
public class BusinessProfile {
    private Owner owner;
    private Contacts contacts;

    @Data
    public static class Owner {
        private String name;
        private String displayName;
        private String title;
        private String bio;
    }

    @Data
    public static class Contacts {
        private Primary primary;
        private Social social;
        private Business business;
    }

    @Data
    public static class Primary {
        private String email;
        private String phone;
    }

    @Data
    public static class Social {
        private String linkedin;
        private String website;
        private String github;
    }

    @Data
    public static class Business {
        private String companyName;
        private Address address;
    }

    @Data
    public static class Address {
        private String street;
        private String city;
        private String state;
        private String zipCode;
        private String country;
    }
}
