-- ============================================================
-- TIME Co-working Management System — Schema
-- PostgreSQL 16
-- ============================================================

-- ────────────────────────────────────────────────────────────
-- USERS (system logins — Admin / Staff)
-- ────────────────────────────────────────────────────────────
CREATE TABLE users (
                       id           BIGSERIAL PRIMARY KEY,
                       username     VARCHAR(60)  NOT NULL UNIQUE,
                       password     VARCHAR(120) NOT NULL,           -- BCrypt hash
                       full_name    VARCHAR(120) NOT NULL,
                       role         VARCHAR(20)  NOT NULL,           -- ADMIN, STAFF
                       enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
                       created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ────────────────────────────────────────────────────────────
-- MEMBERS (co-working members)
-- ────────────────────────────────────────────────────────────
CREATE TABLE members (
                         id                BIGSERIAL PRIMARY KEY,
                         full_name         VARCHAR(120) NOT NULL,
                         email             VARCHAR(120) NOT NULL UNIQUE,
                         phone             VARCHAR(30)  NOT NULL,
                         cnic              VARCHAR(30)  NOT NULL UNIQUE,
                         company           VARCHAR(120),
                         membership_type   VARCHAR(40),
                         join_date         DATE         NOT NULL,
                         expiry_date       DATE         NOT NULL,
                         status            VARCHAR(20)  NOT NULL,      -- ACTIVE, EXPIRED, SUSPENDED, CANCELLED
                         CONSTRAINT chk_member_dates CHECK (expiry_date >= join_date)
);
CREATE INDEX idx_members_status ON members(status);
CREATE INDEX idx_members_company ON members(company);

-- ────────────────────────────────────────────────────────────
-- MEMBERSHIP PLANS
-- ────────────────────────────────────────────────────────────
CREATE TABLE membership_plans (
                                  id                        BIGSERIAL PRIMARY KEY,
                                  plan_name                 VARCHAR(80)  NOT NULL UNIQUE,
                                  duration_days             INT          NOT NULL CHECK (duration_days > 0),
                                  price                     NUMERIC(12,2) NOT NULL CHECK (price >= 0),
                                  allowed_hours             INT          NOT NULL CHECK (allowed_hours >= 0),
                                  included_meeting_hours    INT          NOT NULL DEFAULT 0 CHECK (included_meeting_hours >= 0),
                                  description               TEXT,
                                  active                    BOOLEAN      NOT NULL DEFAULT TRUE
);

-- ────────────────────────────────────────────────────────────
-- MEMBER SUBSCRIPTIONS (history of purchases/renewals)
-- ────────────────────────────────────────────────────────────
CREATE TABLE member_subscriptions (
                                      id            BIGSERIAL PRIMARY KEY,
                                      member_id     BIGINT NOT NULL REFERENCES members(id) ON DELETE CASCADE,
                                      plan_id       BIGINT NOT NULL REFERENCES membership_plans(id),
                                      start_date    DATE   NOT NULL,
                                      end_date      DATE   NOT NULL,
                                      active        BOOLEAN NOT NULL DEFAULT TRUE,
                                      CONSTRAINT chk_sub_dates CHECK (end_date >= start_date)
);
CREATE INDEX idx_sub_member_active ON member_subscriptions(member_id, active);

-- ────────────────────────────────────────────────────────────
-- WORKSPACES (desks, offices, meeting rooms — single table)
-- ────────────────────────────────────────────────────────────
CREATE TABLE workspaces (
                            id                 BIGSERIAL PRIMARY KEY,
                            type               VARCHAR(20) NOT NULL,      -- HOT_DESK, DEDICATED_DESK, OFFICE, MEETING_ROOM
                            code               VARCHAR(40) NOT NULL UNIQUE,
                            floor              VARCHAR(20),
                            zone               VARCHAR(40),
                            capacity           INT,
                            hourly_price       NUMERIC(10,2),
                            monthly_price      NUMERIC(12,2),
                            facilities         TEXT,
                            status             VARCHAR(20) NOT NULL,      -- AVAILABLE, RESERVED, OCCUPIED, MAINTENANCE, UNAVAILABLE
                            assigned_member_id BIGINT REFERENCES members(id),
                            assign_start       DATE,
                            assign_end         DATE
);
CREATE INDEX idx_ws_type_status ON workspaces(type, status);
CREATE INDEX idx_ws_assigned    ON workspaces(assigned_member_id);

-- ────────────────────────────────────────────────────────────
-- BOOKINGS
-- ────────────────────────────────────────────────────────────
CREATE TABLE bookings (
                          id             BIGSERIAL PRIMARY KEY,
                          booking_code   VARCHAR(20) NOT NULL UNIQUE,
                          member_id      BIGINT NOT NULL REFERENCES members(id),
                          workspace_id   BIGINT NOT NULL REFERENCES workspaces(id),
                          booking_date   DATE   NOT NULL,
                          start_time     TIME   NOT NULL,
                          end_time       TIME   NOT NULL,
                          duration_min   INT    NOT NULL CHECK (duration_min > 0),
                          status         VARCHAR(20) NOT NULL,          -- PENDING, CONFIRMED, CHECKED_IN, COMPLETED, CANCELLED, NO_SHOW
                          amount         NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (amount >= 0),
                          created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
                          CONSTRAINT chk_booking_time CHECK (end_time > start_time)
);

-- ⭐ THE INDEX THAT MAKES THE OVERLAP QUERY FAST ⭐
CREATE INDEX idx_booking_ws_date_time
    ON bookings(workspace_id, booking_date, start_time, end_time)
    WHERE status IN ('PENDING','CONFIRMED','CHECKED_IN');

CREATE INDEX idx_booking_member ON bookings(member_id);
CREATE INDEX idx_booking_date   ON bookings(booking_date);

-- ────────────────────────────────────────────────────────────
-- CHECKINS
-- ────────────────────────────────────────────────────────────
CREATE TABLE checkins (
                          id            BIGSERIAL PRIMARY KEY,
                          booking_id    BIGINT NOT NULL UNIQUE REFERENCES bookings(id) ON DELETE CASCADE,
                          checkin_time  TIMESTAMP,
                          checkout_time TIMESTAMP,
                          actual_min    INT,
                          extra_charge  NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (extra_charge >= 0)
);

-- ────────────────────────────────────────────────────────────
-- INVOICES
-- ────────────────────────────────────────────────────────────
CREATE TABLE invoices (
                          id              BIGSERIAL PRIMARY KEY,
                          invoice_number  VARCHAR(30) NOT NULL UNIQUE,  -- INV-2026-0001
                          member_id       BIGINT NOT NULL REFERENCES members(id),
                          issue_date      DATE NOT NULL,
                          subtotal        NUMERIC(12,2) NOT NULL CHECK (subtotal >= 0),
                          discount        NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (discount >= 0),
                          total           NUMERIC(12,2) NOT NULL CHECK (total >= 0),
                          status          VARCHAR(20) NOT NULL          -- DRAFT, ISSUED, PAID, PARTIALLY_PAID, CANCELLED
);
CREATE INDEX idx_invoice_member ON invoices(member_id);
CREATE INDEX idx_invoice_date   ON invoices(issue_date);

-- ────────────────────────────────────────────────────────────
-- INVOICE ITEMS
-- ────────────────────────────────────────────────────────────
CREATE TABLE invoice_items (
                               id           BIGSERIAL PRIMARY KEY,
                               invoice_id   BIGINT NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
                               description  VARCHAR(200) NOT NULL,
                               quantity     INT NOT NULL DEFAULT 1 CHECK (quantity > 0),
                               unit_price   NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0),
                               line_total   NUMERIC(12,2) NOT NULL CHECK (line_total >= 0)
);
CREATE INDEX idx_invitems_invoice ON invoice_items(invoice_id);

-- ────────────────────────────────────────────────────────────
-- PAYMENTS
-- ────────────────────────────────────────────────────────────
CREATE TABLE payments (
                          id             BIGSERIAL PRIMARY KEY,
                          member_id      BIGINT NOT NULL REFERENCES members(id),
                          invoice_id     BIGINT REFERENCES invoices(id),
                          amount         NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
                          payment_date   TIMESTAMP NOT NULL DEFAULT NOW(),
                          method         VARCHAR(20) NOT NULL,          -- CASH, BANK_TRANSFER, CARD, ONLINE, OTHER
                          status         VARCHAR(20) NOT NULL,          -- PAID, PENDING, PARTIALLY_PAID, CANCELLED, REFUNDED
                          reference_no   VARCHAR(60)
);
CREATE INDEX idx_payment_date_status ON payments(payment_date, status);
CREATE INDEX idx_payment_member      ON payments(member_id);

-- ────────────────────────────────────────────────────────────
-- AUDIT LOG
-- ────────────────────────────────────────────────────────────
CREATE TABLE audit_logs (
                            id         BIGSERIAL PRIMARY KEY,
                            username   VARCHAR(60),
                            action     VARCHAR(120) NOT NULL,
                            entity     VARCHAR(60),
                            entity_id  VARCHAR(60),
                            details    TEXT,
                            created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_created ON audit_logs(created_at);
CREATE INDEX idx_audit_entity  ON audit_logs(entity, entity_id);