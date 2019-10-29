IF db_id('bulk') IS NULL CREATE DATABASE [bulk]; 
GO

USE [bulk]

CREATE LOGIN [appuser] WITH PASSWORD = '~shankt0pus~';
CREATE USER [appuser] FOR LOGIN [appuser];
GO

GRANT SELECT TO [appuser];
GO
GRANT CONTROL ON DATABASE::[bulk] TO [appuser];
GO
