DROP TABLE IF EXISTS logfileindex;
CREATE TABLE logfileindex (
    id SERIAL PRIMARY KEY,
    mark VARCHAR(8) NOT NULL,
    locoNumber INTEGER NOT NULL,
    device VARCHAR(64) NOT NULL,
    endTime TIMESTAMP NOT NULL,
    logFilePath TEXT NOT NULL
);