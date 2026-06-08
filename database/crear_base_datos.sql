CREATE DATABASE IF NOT EXISTS convergente_dau_dssm_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'convergente_dau_user'@'localhost'
IDENTIFIED BY 'ConvergenteDau2026*';

GRANT ALL PRIVILEGES ON convergente_dau_dssm_db.*
TO 'convergente_dau_user'@'localhost';

FLUSH PRIVILEGES;
