-- ==============================================================================
-- Script: delete_selected_module_data.sql
-- Description: Permanently deletes all existing records from the 7 specified
--              modules while preserving users, roles, auth credentials, and profiles.
-- Modules Cleared:
--   1. Graveyard (graveyards)
--   2. Grave (graves)
--   3. Report (reports)
--   4. Saved Grave (favorites)
--   5. Deceased Person (deceased_persons)
--   6. Media Upload (photos)
--   7. Memorial (memorials, relationships)
-- Modules Preserved:
--   - Users (users)
--   - Roles (ADMIN / USER)
--   - Passwords & Auth Tokens (password_reset_tokens, otp_verifications, token_blacklist)
--   - Notification Preferences (user_notification_preferences)
--   - Audit Logs (audit_logs)
-- ==============================================================================

BEGIN;

-- 1. Delete Media Uploads (photos belonging to graves & memorials)
DELETE FROM photos;

-- 2. Delete Memorial & Relationship records
DELETE FROM relationships;
DELETE FROM memorials;

-- 3. Delete Funeral Events referencing deceased persons / graveyards
DELETE FROM funeral_events;

-- 4. Delete Reports referencing graves
DELETE FROM reports;

-- 5. Delete Saved Graves (favorites) referencing graves
DELETE FROM favorites;

-- 6. Delete Deceased Persons referencing graves
DELETE FROM deceased_persons;

-- 7. Delete Graves referencing graveyards
DELETE FROM graves;

-- 8. Delete Graveyards
DELETE FROM graveyards;

COMMIT;
