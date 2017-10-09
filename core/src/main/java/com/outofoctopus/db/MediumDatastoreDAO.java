package com.outofoctopus.db;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.inject.Inject;
import com.outofoctopus.db.MediumDAOModule.MediumInject;

import java.util.Optional;

public class MediumDatastoreDAO implements MediumDAO {

    private final Datastore datastore;
    private final KeyFactory keyFactory;

    @Inject
    public MediumDatastoreDAO(
            Datastore datastore,
            @MediumInject KeyFactory keyFactory) {
        this.datastore = datastore;
        this.keyFactory = keyFactory;
    }

    public Optional<String> getConsumerKey(MediumName name) {
        Entity result = datastore.get(getKey(name));
        if (result == null) {
            return Optional.empty();
        }
        return Optional.of(result.getString("consumerKey"));
    }

    public Optional<String> getConsumerSecret(MediumName name) {
        Entity result = datastore.get(getKey(name));
        if (result == null) {
            return Optional.empty();
        }
        return Optional.of(result.getString("consumerSecret"));
    }

    private Key getKey(MediumName name) {
        return keyFactory.newKey(name.toString().toLowerCase());
    }
}
