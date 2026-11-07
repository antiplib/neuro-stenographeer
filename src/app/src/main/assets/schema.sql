CREATE TABLE recordings(id INTEGER PRIMARY KEY AUTOINCREMENT, created_ms INTEGER NOT NULL, title TEXT NOT NULL, duration_ms INTEGER NOT NULL, processing_ms INTEGER NOT NULL, summary TEXT NOT NULL);
CREATE TABLE utterances(recording_id INTEGER NOT NULL REFERENCES recordings(id) ON DELETE CASCADE, ordinal INTEGER NOT NULL, speaker_id TEXT NOT NULL, start_ms INTEGER NOT NULL CHECK(start_ms>=0), end_ms INTEGER NOT NULL CHECK(end_ms>=start_ms), text TEXT NOT NULL, PRIMARY KEY(recording_id,ordinal));
CREATE INDEX utterance_time ON utterances(recording_id,start_ms);
