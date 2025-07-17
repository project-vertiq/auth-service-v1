CREATE TABLE user_profile (
    user_id UUID PRIMARY KEY REFERENCES user_auth_details(user_id) ON DELETE CASCADE,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    gender VARCHAR(20),
    date_of_birth DATE,
    role VARCHAR(50) NOT NULL DEFAULT 'user',
    profile_picture_url VARCHAR(255),
    timezone VARCHAR(50)
);