-- ============================================================
-- Seed data — safe to re-run (uses ON CONFLICT DO NOTHING)
-- ============================================================

-- ── USERS ──────────────────────────────────────────────────
-- Passwords: admin -> "admin123", staff -> "staff123"
INSERT INTO users (username, password, full_name, role, enabled) VALUES
                                                                     ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'System Administrator', 'ADMIN', TRUE),
                                                                     ('staff', '$2a$10$8K1p/a0dL1LXMIgoEDFrwOfgqwAG7VwZ0g1WqHqLdJZHrfPnT8WnO',  'Reception Staff',      'STAFF', TRUE)
    ON CONFLICT (username) DO NOTHING;

-- ── MEMBERSHIP PLANS ───────────────────────────────────────
INSERT INTO membership_plans (plan_name, duration_days, price, allowed_hours, included_meeting_hours, description, active) VALUES
                                                                                                                               ('Day Pass',              1,   1500.00,   8,  0, 'Single day access to hot desk area',               TRUE),
                                                                                                                               ('Weekly Pass',           7,   7000.00,  40,  2, 'One week of hot desk access + 2h meeting room',    TRUE),
                                                                                                                               ('Monthly Hot Desk',     30,  18000.00, 160,  4, 'Unlimited hot desk access for one month',          TRUE),
                                                                                                                               ('Dedicated Desk',       30,  25000.00, 200,  6, 'Reserved desk for one month',                      TRUE),
                                                                                                                               ('Private Office',       30,  65000.00, 240, 10, 'Private office for one month',                     TRUE),
                                                                                                                               ('Startup Package',      30,  45000.00, 200, 12, 'Up to 4 desks + meeting room credits',             TRUE),
                                                                                                                               ('Meeting Room Package', 30,  20000.00,   0, 20, '20 hours of meeting room credits',                 TRUE)
    ON CONFLICT (plan_name) DO NOTHING;

-- ── WORKSPACES ─────────────────────────────────────────────
INSERT INTO workspaces (type, code, floor, zone, capacity, hourly_price, monthly_price, facilities, status) VALUES
                                                                                                                -- Hot desks
                                                                                                                ('HOT_DESK', 'HD-001', '1', 'Zone A', 1, NULL, 18000.00, NULL, 'AVAILABLE'),
                                                                                                                ('HOT_DESK', 'HD-002', '1', 'Zone A', 1, NULL, 18000.00, NULL, 'AVAILABLE'),
                                                                                                                ('HOT_DESK', 'HD-003', '1', 'Zone B', 1, NULL, 18000.00, NULL, 'AVAILABLE'),
                                                                                                                ('HOT_DESK', 'HD-004', '1', 'Zone B', 1, NULL, 18000.00, NULL, 'AVAILABLE'),
                                                                                                                ('HOT_DESK', 'HD-005', '2', 'Zone C', 1, NULL, 18000.00, NULL, 'AVAILABLE'),
                                                                                                                -- Dedicated desks
                                                                                                                ('DEDICATED_DESK', 'DD-001', '2', 'Zone A', 1, NULL, 25000.00, NULL, 'AVAILABLE'),
                                                                                                                ('DEDICATED_DESK', 'DD-002', '2', 'Zone A', 1, NULL, 25000.00, NULL, 'AVAILABLE'),
                                                                                                                ('DEDICATED_DESK', 'DD-003', '2', 'Zone B', 1, NULL, 25000.00, NULL, 'AVAILABLE'),
                                                                                                                -- Private offices
                                                                                                                ('OFFICE', 'OFF-101', '3', 'Executive Wing', 4, NULL, 65000.00, 'AC, Whiteboard, Lockable', 'AVAILABLE'),
                                                                                                                ('OFFICE', 'OFF-102', '3', 'Executive Wing', 6, NULL, 85000.00, 'AC, Whiteboard, Lockable, Balcony', 'AVAILABLE'),
                                                                                                                -- Meeting rooms
                                                                                                                ('MEETING_ROOM', 'MR-01', '1', 'Meeting Hub', 8,  2500.00, NULL, 'Projector, Whiteboard, Video Conf', 'AVAILABLE'),
                                                                                                                ('MEETING_ROOM', 'MR-02', '1', 'Meeting Hub', 12, 4000.00, NULL, 'Projector, Whiteboard, Video Conf, Catering', 'AVAILABLE')
    ON CONFLICT (code) DO NOTHING;

-- ── DEMO MEMBERS ───────────────────────────────────────────
INSERT INTO members (full_name, email, phone, cnic, company, membership_type, join_date, expiry_date, status) VALUES
                                                                                                                  ('Ali Ahmed',        'ali@abctech.pk',    '+923001234567', '35202-1234567-1', 'ABC Technologies', 'Monthly Hot Desk', CURRENT_DATE - 30, CURRENT_DATE + 30, 'ACTIVE'),
                                                                                                                  ('Sara Khan',        'sara@designco.pk',  '+923007654321', '35202-7654321-2', 'DesignCo',         'Dedicated Desk',   CURRENT_DATE - 15, CURRENT_DATE + 15, 'ACTIVE'),
                                                                                                                  ('Bilal Raza',       'bilal@startup.pk',  '+923009876543', '35202-9876543-3', 'Nova Startup',     'Startup Package',  CURRENT_DATE - 10, CURRENT_DATE + 20, 'ACTIVE')
    ON CONFLICT (email) DO NOTHING;