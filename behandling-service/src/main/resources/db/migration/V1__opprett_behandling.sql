CREATE TABLE behandling (
                            soknad_id UUID PRIMARY KEY,
                            status VARCHAR(30) NOT NULL,
                            oppdatert TIMESTAMP WITH TIME ZONE NOT NULL
);