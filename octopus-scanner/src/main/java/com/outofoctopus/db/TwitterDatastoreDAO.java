package com.outofoctopus.db;

import com.google.cloud.datastore.*;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.common.collect.ImmutableList;
import com.google.cloud.Timestamp;
import com.outofoctopus.proto.TwitterProtos.TwitterAccount;

public class TwitterDatastoreDAO implements TwitterDAO {
    public static final String KIND_STRING = "twitter";
    public static final String USERNAME_FIELD = "handle";
    public static final String IS_ACTIVE_FIELD = "active";
    public static final String ACTIVE_START_FIELD = "active_from";
    public static final String ACTIVE_END_FIELD = "active_until";
    public static final String AUTHTOKEN_FIELD = "authtoken";

    private final String projectName;

    private final Datastore datastore;
    private final KeyFactory keyFactory;

    public TwitterDatastoreDAO(Datastore datastore, String projectName) {
        this.projectName = projectName;
        this.datastore = datastore;
        this.keyFactory = datastore.newKeyFactory().setKind(KIND_STRING);
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

    public TwitterAccount getAccount(String handle) {
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind(KIND_STRING)
                .setFilter(PropertyFilter.eq(USERNAME_FIELD, handle))
                .build();
        QueryResults<Entity> results = datastore.run(query);

        // return first result
        return parseTwitterAccount(results.next());
    }

    public TwitterAccount insert(TwitterAccount newAccount) {
        FullEntity parsedAccount = parseEntityFromTwitterAccount(newAccount);
        FullEntity insertedAccount = datastore.add(parsedAccount);
        return parseTwitterAccount(insertedAccount);
    }

    public void update(TwitterAccount updatedAccount) {
        Key key = Key.newBuilder(projectName, KIND_STRING, updatedAccount.getHandle()).build();
        FullEntity parsedAccountFull = parseEntityFromTwitterAccount(updatedAccount);
        Entity parsedAccount = Entity.newBuilder(key, parsedAccountFull).build();
        datastore.update(parsedAccount);
    }

    public void delete(String handle) {
        Key key = Key.newBuilder(projectName, KIND_STRING, handle).build();
        datastore.delete(key);
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

        entity.setKey(getKey(account));
        entity.set(USERNAME_FIELD, account.getHandle());
        entity.set(IS_ACTIVE_FIELD, account.getActive());
        entity.set(AUTHTOKEN_FIELD, account.getAuthToken());
        entity.set(ACTIVE_START_FIELD, Timestamp.fromProto(account.getActiveFrom()));
        entity.set(ACTIVE_END_FIELD, Timestamp.fromProto(account.getActiveUntil()));
        return entity.build();
    }

    private Key getKey(TwitterAccount account) {
        return keyFactory.newKey(account.getHandle());
    }
}