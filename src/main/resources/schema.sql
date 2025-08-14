-- Create database if not exists
CREATE DATABASE IF NOT EXISTS xreal_tech_faq CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE xreal_tech_faq;

-- Create tag table
CREATE TABLE IF NOT EXISTS tag (
    name VARCHAR(100) PRIMARY KEY,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create faq table
CREATE TABLE IF NOT EXISTS faq (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    instruction TEXT,
    url VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    comment TEXT,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_active (active),
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create faq_tag junction table
CREATE TABLE IF NOT EXISTS faq_tag (
    faq_id BIGINT NOT NULL,
    tag VARCHAR(100) NOT NULL,
    PRIMARY KEY (faq_id, tag),
    FOREIGN KEY (faq_id) REFERENCES faq(id) ON DELETE CASCADE,
    FOREIGN KEY (tag) REFERENCES tag(name) ON DELETE RESTRICT,
    INDEX idx_tag (tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default tags (只在不存在时插入)
INSERT IGNORE INTO tag (name, description, active) VALUES
('XREAL One', 'XREAL One smart glasses', TRUE),
('XREAL One Pro', 'XREAL One Pro smart glasses', TRUE),
('XREAL Air', 'XREAL Air smart glasses', TRUE),
('XREAL Air2', 'XREAL Air2 smart glasses', TRUE),
('XREAL Air2 Pro', 'XREAL Air2 Pro smart glasses', TRUE),
('XREAL Air2 Ultra', 'XREAL Air2 Ultra smart glasses', TRUE),
('XREAL Beam', 'XREAL Beam accessory', TRUE),
('XREAL Beam Pro', 'XREAL Beam Pro accessory', TRUE),
('XREAL Light', 'XREAL Light device', TRUE);