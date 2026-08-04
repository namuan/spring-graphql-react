package com.showroom.vehicle.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "vehicle_model")
public class VehicleModel {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String brand;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "model_year", nullable = false)
    private int modelYear;

    @Column(name = "base_price_cents", nullable = false)
    private long basePriceCents;

    @Column(length = 4000)
    private String description;

    @Column(nullable = false, length = 80)
    private String engine;

    @Column(name = "power_ps", nullable = false)
    private int powerPs;

    @Column(name = "acceleration_s", nullable = false, precision = 4, scale = 1)
    private BigDecimal accelerationS;

    @Column(name = "top_speed_kph", nullable = false)
    private int topSpeedKph;

    @Column(nullable = false, length = 20)
    private String drivetrain;

    @Column(name = "range_km")
    private Integer rangeKm;

    @Column(nullable = false)
    private int seats;

    @OneToMany(mappedBy = "model", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Trim> trims = new ArrayList<>();

    @OneToMany(mappedBy = "model", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VehicleOption> options = new ArrayList<>();

    public VehicleModel() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getModelYear() {
        return modelYear;
    }

    public void setModelYear(int modelYear) {
        this.modelYear = modelYear;
    }

    public long getBasePriceCents() {
        return basePriceCents;
    }

    public void setBasePriceCents(long basePriceCents) {
        this.basePriceCents = basePriceCents;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public int getPowerPs() {
        return powerPs;
    }

    public void setPowerPs(int powerPs) {
        this.powerPs = powerPs;
    }

    public BigDecimal getAccelerationS() {
        return accelerationS;
    }

    public void setAccelerationS(BigDecimal accelerationS) {
        this.accelerationS = accelerationS;
    }

    public int getTopSpeedKph() {
        return topSpeedKph;
    }

    public void setTopSpeedKph(int topSpeedKph) {
        this.topSpeedKph = topSpeedKph;
    }

    public String getDrivetrain() {
        return drivetrain;
    }

    public void setDrivetrain(String drivetrain) {
        this.drivetrain = drivetrain;
    }

    public Integer getRangeKm() {
        return rangeKm;
    }

    public void setRangeKm(Integer rangeKm) {
        this.rangeKm = rangeKm;
    }

    public int getSeats() {
        return seats;
    }

    public void setSeats(int seats) {
        this.seats = seats;
    }

    public List<Trim> getTrims() {
        return trims;
    }

    public void setTrims(List<Trim> trims) {
        this.trims = trims;
    }

    public List<VehicleOption> getOptions() {
        return options;
    }

    public void setOptions(List<VehicleOption> options) {
        this.options = options;
    }
}
