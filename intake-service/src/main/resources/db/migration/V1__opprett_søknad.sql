CREATE TABLE soknad (
                        id UUID PRIMARY KEY,
                        soker_navn VARCHAR(200) NOT NULL,
                        soker_epost VARCHAR(200) NOT NULL,
                        type VARCHAR(50) NOT NULL,
                        status VARCHAR(30) NOT NULL,
                        opprettet TIMESTAMP WITH TIME ZONE NOT NULL
);