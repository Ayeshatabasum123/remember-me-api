# Database Design Document: School Admission & Student Information System (SIS)

This document provides a detailed layout of the PostgreSQL relational database schema, highlighting entity relationships, fields, data types, constraints, and indexes.

---

## 📊 Entity Relationship Diagram (ERD)

```mermaid
erDiagram
    users {
        int id PK
        varchar username UK
        varchar email UK
        varchar password_hash
        varchar role "SuperAdmin, Admin, Teacher"
        boolean is_approved
        boolean must_change_password
        varchar school_id
        varchar school_name
        varchar principal_name
        varchar phone_number
        timestamp created_at
    }

    schools {
        varchar id PK
        varchar name
        varchar principal_name
        varchar contact_number
        timestamp created_at
    }

    user_schools {
        int user_id FK "users(id)"
        varchar school_id FK "schools(id)"
    }

    pupils {
        int pupil_id PK
        text current_status "Waiting, Current, Graduated, Left, Missing Child"
        text system_calculated_year_group
        text assigned_year_group
        text current_class
        date start_date
        text previous_school
        date leaving_date
        text next_school
        int missing_child_indicator
        date date_la_contacted
        timestamp record_creation_date
        text created_by
        int warning_count
        text forename
        text middle_names
        text surname
        date dob
        text gender
        text fsm
        text rypp
        text eal
        text send
        text ethnicity
        text pg1_name
        text pg1_contact_number
        text pg1_email
        text pg1_ni_number
        text pg2_name
        text pg2_contact_number
        text pg2_email
        text pg2_ni_number
        text street
        text town_city
        text post_code
        text lives_with
        text e1_name
        text e1_contact_number
        text e1_relationship
        text e2_name
        text e2_contact_number
        text e2_relationship
        text e3_name
        text e3_contact_number
        text e3_relationship
        text doctor_name
        text doctor_contact_number
        text medical_conditions
        text allergies
        text birth_marks
        text send_stage
        int is_deleted
        varchar school_id FK "schools(id)"
        text pid UK
    }

    cohorts {
        int cohort_id PK
        text year_group
        int age_at_end_of_year
        int is_active
        varchar school_id FK "schools(id)"
    }

    year_group_classes {
        int id PK
        int cohort_id FK "cohorts(cohort_id)"
        text class_name
        int display_order
        timestamp created_at
    }

    attendance_codes {
        int code_id PK
        varchar code UK
        varchar label
        varchar category "Present, Authorised Absence, Unauthorised Absence"
        int is_active
        timestamp created_at
    }

    attendance_records {
        int attendance_id PK
        int pupil_id FK "pupils(pupil_id)"
        date attendance_date
        varchar session "AM, PM"
        int code_id FK "attendance_codes(code_id)"
        text notes
        varchar recorded_by
        timestamp created_at
        timestamp updated_at
    }

    inclusion_records {
        int record_id PK
        int pupil_id FK "pupils(pupil_id)"
        varchar record_type "concern_form, qft, iep, parents, funding"
        varchar stage
        date record_date
        varchar created_by
        timestamp created_at
        timestamp updated_at
        text data
    }

    qft_records {
        int record_id PK, FK "inclusion_records(record_id)"
        text barriers
        text strategies
        text monitoring
        text additional_notes
        varchar completed_by
    }

    iep_records {
        int record_id PK, FK "inclusion_records(record_id)"
        text target
        text strategies
        text provision
        text success_criteria
        varchar completed_by
    }

    iep_weekly_monitors {
        int id PK
        int record_id FK "iep_records(record_id)"
        date monitor_date
        text notes
    }

    parents_records {
        int record_id PK, FK "inclusion_records(record_id)"
        text setting_lead
        text parents
        text notes
        text our_pledge
        text parents_pledge
        varchar completed_by
    }

    funding_records {
        int record_id PK, FK "inclusion_records(record_id)"
        varchar funding_type
        date start_date
        date end_date
        numeric amount_received
        varchar allocated_to
    }

    audit_log {
        int log_id PK
        int pupil_id FK "pupils(pupil_id)"
        text pupil_name
        text action_type
        text field_changed
        text old_value
        text new_value
        text status_changed_from
        text status_changed_to
        text previous_class
        text new_class
        text system_calculated_year_group
        text assigned_year_group
        text changed_by
        timestamp change_timestamp
        text change_source
        text reason_for_change
        varchar school_id FK "schools(id)"
    }

    pid_sequences {
        varchar school_id PK
        text year_group_code PK
        int last_value
    }

    %% Relationships
    users ||--o{ user_schools : belongs
    schools ||--o{ user_schools : contains
    schools ||--o{ pupils : registers
    schools ||--o{ cohorts : defines
    cohorts ||--o{ year_group_classes : contains
    pupils ||--o{ attendance_records : tracks
    attendance_codes ||--o{ attendance_records : codes
    pupils ||--o{ inclusion_records : holds
    inclusion_records ||--|| qft_records : extends
    inclusion_records ||--|| iep_records : extends
    inclusion_records ||--|| parents_records : extends
    inclusion_records ||--|| funding_records : extends
    iep_records ||--o{ iep_weekly_monitors : monitors
    pupils ||--o{ audit_log : audits
    schools ||--o{ audit_log : logs
```

---

## 🔑 Key Tables & Fields

### 1. `users` Table
Stores user account profiles and application security access levels.
*   `role`: Restricts operations based on three levels: `SuperAdmin`, `Admin`, and `Teacher`.
*   `school_id` and `school_name`: Legacy direct fields, migrated to `user_schools` mapping table for supporting multi-school environments.

### 2. `schools` & `user_schools` Tables
Supports multi-school tenancy:
*   `schools` stores global school entities.
*   `user_schools` defines a many-to-many relationship mapping user profiles to their active schools.

### 3. `pupils` Table
Master student details tracking:
*   `current_status`: Check constraint limits status to `'Waiting'`, `'Current'`, `'Graduated'`, `'Left'`, and `'Missing Child'`.
*   `pid`: Standardized Pupil ID (`PID-<SCHOOL>-<YEAR>-<SEQ>`) formatted for displays.
*   `is_deleted`: Flag implementing soft-deletion routines.

### 4. `inclusion_records` (SEND Polymorphism)
Supports polymorphic storage of Special Educational Needs documentation:
*   Extends metadata: `record_type` constraints (`'concern_form'`, `'qft'`, `'iep'`, `'parents'`, `'funding'`).
*   Sub-type records (`qft_records`, `iep_records`, `parents_records`, `funding_records`) share the same `record_id` acting both as Primary Key and Foreign Key to `inclusion_records` (`ON DELETE CASCADE`).

---

## ⚡ Indexing & Performance Strategy
Optimized indexes cover the hot query paths:
1.  **Audit Logs Filtering**:
    *   `idx_audit_log_school_ts` on `(school_id, change_timestamp DESC)` to optimize log dashboards.
2.  **Pupil Lookups**:
    *   `idx_pupils_school_status_class` on `(school_id, current_status, current_class) WHERE is_deleted = 0`.
    *   `idx_pupils_school_year_group` on `(school_id, assigned_year_group) WHERE is_deleted = 0`.
    *   `idx_pupils_pid` on `(pid)` to enable fast ID lookups.
3.  **Attendance Pivot**:
    *   `idx_attendance_records_pupil_date` on `(pupil_id, attendance_date)`.
