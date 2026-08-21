-- ============================================================
-- V1: Core schema for Porter-like delivery platform
-- ============================================================

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone VARCHAR(15) NOT NULL UNIQUE,
    email VARCHAR(150) UNIQUE,
    password_hash VARCHAR(255),
    role VARCHAR(20) NOT NULL,               -- CUSTOMER, RIDER, ADMIN, SUPPORT
    account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL
) ENGINE=InnoDB;

CREATE TABLE refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    device_info VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rt_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE otp_verifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone VARCHAR(15) NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    purpose VARCHAR(20) NOT NULL,
    expires_at DATETIME NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    consumed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_otp_phone (phone)
) ENGINE=InnoDB;

-- ============================================================
CREATE TABLE customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    rating_avg DECIMAL(3,2) DEFAULT 5.00,
    total_trips INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_customer_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE customer_addresses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    label VARCHAR(50),
    address_line VARCHAR(500) NOT NULL,
    lat DECIMAL(10,7) NOT NULL,
    lng DECIMAL(10,7) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_addr_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
) ENGINE=InnoDB;

-- ============================================================
CREATE TABLE riders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    dob DATE,
    gender VARCHAR(20),
    onboarding_status VARCHAR(30) NOT NULL DEFAULT 'REGISTERED',
    online_status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE',
    availability_status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    current_vehicle_id BIGINT NULL,
    rating_avg DECIMAL(3,2) DEFAULT 5.00,
    total_trips INT NOT NULL DEFAULT 0,
    wallet_balance DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    approved_at DATETIME NULL,
    approved_by_admin_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_rider_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_rider_matching (onboarding_status, online_status, availability_status)
) ENGINE=InnoDB;

CREATE TABLE rider_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rider_id BIGINT NOT NULL,
    doc_type VARCHAR(30) NOT NULL,          -- LICENSE, RC, AADHAR, PAN, INSURANCE, PHOTO
    file_url VARCHAR(500) NOT NULL,
    verification_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    rejection_reason VARCHAR(500),
    reviewed_by_admin_id BIGINT NULL,
    reviewed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_doc_rider FOREIGN KEY (rider_id) REFERENCES riders(id),
    INDEX idx_doc_rider (rider_id, verification_status)
) ENGINE=InnoDB;

CREATE TABLE rider_onboarding_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rider_id BIGINT NOT NULL,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    changed_by VARCHAR(100),
    remarks VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_hist_rider FOREIGN KEY (rider_id) REFERENCES riders(id)
) ENGINE=InnoDB;

-- ============================================================
CREATE TABLE vehicle_types (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,        -- BIKE, MINI_TRUCK, TEMPO ...
    capacity_kg DECIMAL(8,2),
    base_fare DECIMAL(10,2) NOT NULL,
    per_km_rate DECIMAL(10,2) NOT NULL,
    per_min_rate DECIMAL(10,2) NOT NULL,
    free_wait_minutes INT NOT NULL DEFAULT 5,
    wait_charge_per_min DECIMAL(10,2) NOT NULL DEFAULT 2.00,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE vehicles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rider_id BIGINT NOT NULL,
    vehicle_type_id BIGINT NOT NULL,
    registration_number VARCHAR(30) NOT NULL UNIQUE,
    model VARCHAR(100),
    year INT,
    verification_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_vehicle_rider FOREIGN KEY (rider_id) REFERENCES riders(id),
    CONSTRAINT fk_vehicle_type FOREIGN KEY (vehicle_type_id) REFERENCES vehicle_types(id)
) ENGINE=InnoDB;

ALTER TABLE riders ADD CONSTRAINT fk_rider_current_vehicle FOREIGN KEY (current_vehicle_id) REFERENCES vehicles(id);

-- ============================================================
CREATE TABLE delivery_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    rider_id BIGINT NULL,
    vehicle_type_id BIGINT NOT NULL,
    pickup_address VARCHAR(500) NOT NULL,
    pickup_lat DECIMAL(10,7) NOT NULL,
    pickup_lng DECIMAL(10,7) NOT NULL,
    drop_address VARCHAR(500) NOT NULL,
    drop_lat DECIMAL(10,7) NOT NULL,
    drop_lng DECIMAL(10,7) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
    estimated_fare DECIMAL(10,2),
    final_fare DECIMAL(10,2),
    estimated_distance_km DECIMAL(8,2),
    actual_distance_km DECIMAL(8,2),
    otp_code VARCHAR(6),
    requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_at DATETIME NULL,
    rider_arrived_at DATETIME NULL,
    trip_started_at DATETIME NULL,
    trip_completed_at DATETIME NULL,
    cancelled_at DATETIME NULL,
    cancelled_by VARCHAR(20) NULL,
    cancellation_reason VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_dr_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_dr_rider FOREIGN KEY (rider_id) REFERENCES riders(id),
    CONSTRAINT fk_dr_vehicle_type FOREIGN KEY (vehicle_type_id) REFERENCES vehicle_types(id),
    INDEX idx_dr_status_created (status, created_at),
    INDEX idx_dr_customer (customer_id),
    INDEX idx_dr_rider (rider_id)
) ENGINE=InnoDB;

CREATE TABLE trip_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    lat DECIMAL(10,7) NULL,
    lng DECIMAL(10,7) NULL,
    recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tsh_trip FOREIGN KEY (trip_id) REFERENCES delivery_requests(id),
    INDEX idx_tsh_trip (trip_id)
) ENGINE=InnoDB;

-- ============================================================
CREATE TABLE fare_details (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id BIGINT NOT NULL UNIQUE,
    base_fare DECIMAL(10,2) NOT NULL DEFAULT 0,
    distance_fare DECIMAL(10,2) NOT NULL DEFAULT 0,
    time_fare DECIMAL(10,2) NOT NULL DEFAULT 0,
    waiting_charge DECIMAL(10,2) NOT NULL DEFAULT 0,
    surge_multiplier DECIMAL(4,2) NOT NULL DEFAULT 1.00,
    surge_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    toll_charge DECIMAL(10,2) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    coupon_code VARCHAR(30),
    tax_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    gross_fare DECIMAL(10,2) NOT NULL DEFAULT 0,
    commission_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    commission_percentage DECIMAL(5,2) NOT NULL DEFAULT 0,
    rider_net_earning DECIMAL(10,2) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fare_trip FOREIGN KEY (trip_id) REFERENCES delivery_requests(id)
) ENGINE=InnoDB;

-- ============================================================
CREATE TABLE commission_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehicle_type_id BIGINT NULL,             -- NULL = global default
    commission_type VARCHAR(20) NOT NULL,    -- PERCENTAGE, FIXED
    value DECIMAL(10,2) NOT NULL,
    effective_from DATETIME NOT NULL,
    effective_to DATETIME NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cc_vehicle_type FOREIGN KEY (vehicle_type_id) REFERENCES vehicle_types(id)
) ENGINE=InnoDB;

CREATE TABLE rider_earnings_ledger (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rider_id BIGINT NOT NULL,
    trip_id BIGINT NULL,
    amount DECIMAL(12,2) NOT NULL,
    type VARCHAR(20) NOT NULL,               -- TRIP_EARNING, INCENTIVE, PENALTY, PAYOUT
    balance_after DECIMAL(12,2) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ledger_rider FOREIGN KEY (rider_id) REFERENCES riders(id),
    CONSTRAINT fk_ledger_trip FOREIGN KEY (trip_id) REFERENCES delivery_requests(id),
    INDEX idx_ledger_rider (rider_id)
) ENGINE=InnoDB;

-- ============================================================
CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    method VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'INITIATED',
    gateway_txn_id VARCHAR(100),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_pay_trip FOREIGN KEY (trip_id) REFERENCES delivery_requests(id),
    CONSTRAINT fk_pay_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
) ENGINE=InnoDB;

-- ============================================================
CREATE TABLE ratings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id BIGINT NOT NULL,
    rated_by VARCHAR(20) NOT NULL,           -- CUSTOMER, RIDER
    rating_value DECIMAL(2,1) NOT NULL,
    comment VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rating_trip FOREIGN KEY (trip_id) REFERENCES delivery_requests(id)
) ENGINE=InnoDB;

CREATE TABLE disputes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id BIGINT NOT NULL,
    raised_by VARCHAR(20) NOT NULL,
    category VARCHAR(50),
    description VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    resolved_by_admin_id BIGINT NULL,
    resolution_notes VARCHAR(1000),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at DATETIME NULL,
    CONSTRAINT fk_dispute_trip FOREIGN KEY (trip_id) REFERENCES delivery_requests(id)
) ENGINE=InnoDB;

-- ============================================================
-- Seed default vehicle types & commission config
INSERT INTO vehicle_types (name, capacity_kg, base_fare, per_km_rate, per_min_rate, free_wait_minutes, wait_charge_per_min) VALUES
 ('BIKE', 20, 25.00, 8.00, 1.00, 5, 1.50),
 ('THREE_WHEELER', 300, 40.00, 12.00, 1.50, 10, 2.00),
 ('MINI_TRUCK', 750, 80.00, 18.00, 2.00, 15, 3.00),
 ('LARGE_TRUCK', 2000, 150.00, 25.00, 2.50, 20, 4.00);

INSERT INTO commission_configs (vehicle_type_id, commission_type, value, effective_from, is_active) VALUES
 (NULL, 'PERCENTAGE', 20.00, NOW(), TRUE);
