CREATE DATABASE IF NOT EXISTS driving_school DEFAULT CHARACTER SET utf8mb4;
USE driving_school;

CREATE TABLE IF NOT EXISTS lead_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    source VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    owner VARCHAR(64),
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS student (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    id_card VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS course_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    coach_name VARCHAR(64) NOT NULL,
    subject_code VARCHAR(16) NOT NULL,
    plan_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME
);

CREATE TABLE IF NOT EXISTS training_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    coach_name VARCHAR(64) NOT NULL,
    subject_code VARCHAR(16) NOT NULL,
    training_date DATE NOT NULL,
    hours DECIMAL(6,2) NOT NULL,
    remark VARCHAR(255),
    created_at DATETIME
);

CREATE TABLE IF NOT EXISTS exam_site (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    address VARCHAR(255) NOT NULL,
    latitude VARCHAR(32),
    longitude VARCHAR(32),
    route_guide VARCHAR(500),
    session_name VARCHAR(64),
    capacity INT,
    reserved_count INT
);

CREATE TABLE IF NOT EXISTS exam_application (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    subject_code VARCHAR(16) NOT NULL,
    exam_site_id BIGINT NOT NULL,
    exam_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    display_name VARCHAR(64),
    enabled BIT,
    created_at DATETIME
);

CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    module_name VARCHAR(64),
    action_name VARCHAR(64),
    operator_name VARCHAR(64),
    result VARCHAR(32),
    detail VARCHAR(1000),
    created_at DATETIME
);
