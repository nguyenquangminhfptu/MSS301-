-- Only for the isolated local Docker instance.
ALTER LOGIN [sa] WITH CHECK_POLICY = OFF;
ALTER LOGIN [sa] WITH PASSWORD = N'sa';
GO
