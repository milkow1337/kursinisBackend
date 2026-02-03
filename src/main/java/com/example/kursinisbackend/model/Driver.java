package com.example.kursinisbackend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Driver extends BasicUser{
    private String licence;
    private LocalDate birthDate; // Renamed from bDate
    @Enumerated(EnumType.STRING)
    private VehicleType vehicleType;

    public Driver(String login, String password, String name, String surname, String phoneNumber, String address, String licence, LocalDate birthDate, VehicleType vehicleType) {
        super(login, password, name, surname, phoneNumber, address);
        this.licence = licence;
        this.birthDate = birthDate;
        this.vehicleType = vehicleType;
    }
}
