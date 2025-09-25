package com.mss301.authservice.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProfileCreationRequest {

    @NotNull
    @Email(message = "Email should be valid")
    String email;

    @NotNull
    @Size(min = 1, max = 50, message = "Username must be between 1 and 50 characters")
    String username;

    @NotNull
    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    String firstName;

    @NotNull
    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    String lastName;

    @Size(max = 15, message = "Phone number must not exceed 15 characters")
    String phone;

    LocalDate dateOfBirth;

    @Size(max = 10, message = "Gender must not exceed 10 characters")
    String gender;

    @Size(max = 255, message = "Address must not exceed 255 characters")
    String address;

    @Size(max = 100, message = "City must not exceed 100 characters")
    String city;

    @Size(max = 100, message = "Country must not exceed 100 characters")
    String country;

    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    String postalCode;

    @Size(max = 500, message = "Bio must not exceed 500 characters")
    String bio;

    String profilePictureUrl;

    @NotNull
    Long tenantId;
}
