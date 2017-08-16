package com.outofoctopus.db;

import com.google.cloud.datastore.*;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.common.collect.ImmutableList;
import com.google.cloud.Timestamp;
import com.outofoctopus.proto.TwitterProtos.TwitterAccount;
import java.util.Optional;

// TODO: Error catching; want to have my own way of distinguishing problems


public class TwitterDatastoreDAO implements TwitterDAO {
    private static final String KIND_STRING = "twitter";
    private static final String USERNAME_FIELD = "handle";
    private static final String IS_ACTIVE_FIELD = "active";
    private static final String ACTIVE_START_FIELD = "active_from";
    private static final String ACTIVE_END_FIELD = "active_until";
    private static final String AUTHTOKEN_FIELD = "authtoken";

    private final String projectName;
    private final Datastore datastore;
    private final KeyFactory keyFactory;

    public TwitterDatastoreDAO(Datastore datastore, String projectName) {
        this.projectName = projectName;
        this.datastore = datastore;
        this.keyFactory = datastore.newKeyFactory().setKind(KIND_STRING);
    }

    public ImmutableList<TwitterAccount> getAllAccounts() {
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind(KIND_STRING)
                .build();
        return parseResults(datastore.run(query));
    }

    public ImmutableList<TwitterAccount> getActiveAccounts() {
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind(KIND_STRING)
                .setFilter(PropertyFilter.eq(IS_ACTIVE_FIELD, true))
                .build();
        return parseResults(datastore.run(query));
    }

    public ImmutableList<TwitterAccount> getAccountsToActivate() {
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind(KIND_STRING)
                .setFilter(PropertyFilter.eq(IS_ACTIVE_FIELD, false))
                .setFilter(PropertyFilter.le(ACTIVE_START_FIELD, Timestamp.now()))
                .setFilter(PropertyFilter.gt(ACTIVE_END_FIELD, Timestamp.now()))
                .build();
        return parseResults(datastore.run(query));
    }

    public Optional<TwitterAccount> getAccount(String handle) {
        Entity result = datastore.get(getKey(handle));
        if (result == null) {
            return Optional.empty();
        }
        return Optional.of(parseTwitterAccount(result));
    }

    public boolean insert(TwitterAccount newAccount) {
        try {
            FullEntity parsedAccount = parseEntityFromTwitterAccount(newAccount);
            datastore.add(parsedAccount);
            return true;
        } catch (IllegalArgumentException | DatastoreException e) {
            return false;
        }
    }

    public boolean update(TwitterAccount updatedAccount) {
        Key key = Key.newBuilder(projectName, KIND_STRING, updatedAccount.getHandle()).build();
        FullEntity parsedAccountFull = parseEntityFromTwitterAccount(updatedAccount);
        Entity parsedAccount = Entity.newBuilder(key, parsedAccountFull).build();
        try {
            datastore.update(parsedAccount);
            return true;
        } catch (IllegalArgumentException | DatastoreException e) {
            return false;
        }
    }

    public boolean delete(String handle) {
        try {
            datastore.delete(getKey(handle));
            return true;
        } catch (DatastoreException e) {
            return false;
        }
    }

    private ImmutableList<TwitterAccount> parseResults(QueryResults<Entity> results) {
        ImmutableList.Builder<TwitterAccount> accounts = ImmutableList.builder();
        while (results.hasNext()) {
            accounts.add(parseTwitterAccount(results.next()));
        }
        return accounts.build();
    }

    private TwitterAccount parseTwitterAccount(FullEntity entity) throws IllegalArgumentException {
        TwitterAccount.Builder account = TwitterAccount.newBuilder();

        String handle = entity.getString(USERNAME_FIELD);
        if (handle == null) {
            throw new IllegalArgumentException("Cannot parse twitter account with no handle");
        }
        account.setHandle(handle);
        account.setActive(entity.getBoolean(IS_ACTIVE_FIELD));
        account.setActiveFrom(entity.getTimestamp(ACTIVE_START_FIELD).toProto());
        account.setActiveUntil(entity.getTimestamp(ACTIVE_END_FIELD).toProto());
        account.setAuthToken(entity.getString(AUTHTOKEN_FIELD));

        return account.build();
    }

    private FullEntity parseEntityFromTwitterAccount(TwitterAccount account)
            throws IllegalArgumentException {
        FullEntity.Builder entity = Entity.newBuilder();

        if (!account.hasHandle()) {
            throw new IllegalArgumentException("Unable to create entity from proto with no handle");
        }
        boolean active = account.hasActive() && account.getActive();
        Timestamp activeFrom = account.hasActiveFrom() ? Timestamp.fromProto(account.getActiveFrom()) : Timestamp.MIN_VALUE;
        Timestamp activeTo = account.hasActiveUntil() ? Timestamp.fromProto(account.getActiveUntil()) : Timestamp.MIN_VALUE;

        entity.setKey(getKey(account));
        entity.set(USERNAME_FIELD, account.getHandle());
        entity.set(IS_ACTIVE_FIELD, active);
        entity.set(AUTHTOKEN_FIELD, account.getAuthToken());
        entity.set(ACTIVE_START_FIELD, activeFrom);
        entity.set(ACTIVE_END_FIELD, activeTo);
        return entity.build();
    }

    private Key getKey(TwitterAccount account) {
        return keyFactory.newKey(account.getHandle());
    }

    private Key getKey(String handle) {
        return keyFactory.newKey(handle);
    }
}