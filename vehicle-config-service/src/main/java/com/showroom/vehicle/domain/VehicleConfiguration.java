package com.showroom.vehicle.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "vehicle_configuration")
public class VehicleConfiguration {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "model_id", nullable = false)
    private VehicleModel model;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trim_id", nullable = false)
    private Trim trim;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "configuration_option",
            joinColumns = @JoinColumn(name = "configuration_id"),
            inverseJoinColumns = @JoinColumn(name = "option_id"))
    private Set<VehicleOption> options = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConfigurationStatus status = ConfigurationStatus.DRAFT;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "total_price_cents", nullable = false)
    private long totalPriceCents;

    public VehicleConfiguration() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public VehicleModel getModel() {
        return model;
    }

    public void setModel(VehicleModel model) {
        this.model = model;
    }

    public Trim getTrim() {
        return trim;
    }

    public void setTrim(Trim trim) {
        this.trim = trim;
    }

    public Set<VehicleOption> getOptions() {
        return options;
    }

    public void setOptions(Set<VehicleOption> options) {
        this.options = options;
    }

    public ConfigurationStatus getStatus() {
        return status;
    }

    public void setStatus(ConfigurationStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public long getTotalPriceCents() {
        return totalPriceCents;
    }

    public void setTotalPriceCents(long totalPriceCents) {
        this.totalPriceCents = totalPriceCents;
    }
}
