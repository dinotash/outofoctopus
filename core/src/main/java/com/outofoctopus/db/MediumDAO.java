package com.outofoctopus.db;

import java.util.Optional;

public interface MediumDAO {
    enum MediumName {
        TWITTER
    }

    Optional<String> getConsumerKey(MediumName name);

    Optional<String> getConsumerSecret(MediumName name);
}
