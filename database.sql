-- Tao database cho project (SQL Server)
-- Bang users se duoc Hibernate tu tao (ddl-auto=update)

IF DB_ID('jwt_nimbus') IS NULL
    CREATE DATABASE jwt_nimbus;
GO
