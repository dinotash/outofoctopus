package com.outofoctopus.db;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.common.collect.ImmutableList;
import com.google.cloud.Timestamp;
import com.outofoctopus.proto.TwitterProtos.TwitterAccount;

public class TwitterDatastoreDAO implements TwitterDAO {
    private static final String PROJECT_NAME = "outofoctopus";
    private static final String KIND_STRING = "twitter";
    private static final String USERNAME_FIELD = "handle";
    private static final String IS_ACTIVE_FIELD = "active";
    private static final String ACTIVE_START_FIELD = "active_from";
    private static final String ACTIVE_END_FIELD = "active_until";
    private static final String AUTHTOKEN_FIELD = "authtoken";

    private final Datastore datastore;

    public TwitterDatastoreDAO(Datastore datastore) {
        this.datastore = datastore;
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
        Key key = Key.newBuilder(PROJECT_NAME, KIND_STRING, updatedAccount.getHandle()).build();
        FullEntity parsedAccountFull = parseEntityFromTwitterAccount(updatedAccount);
        Entity parsedAccount = Entity.newBuilder(key, parsedAccountFull).build();
        datastore.update(parsedAccount);
    }

    public void delete(String handle) {
        Key key = Key.newBuilder(PROJECT_NAME, KIND_STRING, handle).build();
        datastore.delete(key);
    }

    private static ImmutableList<TwitterAccount> parseResults(QueryResults<Entity> results) {
        ImmutableList.Builder<TwitterAccount> accounts = ImmutableList.builder();
        while (results.hasNext()) {
            accounts.add(parseTwitterAccount(results.next()));
        }
        return accounts.build();
    }

    private static TwitterAccount parseTwitterAccount(FullEntity entity) throws IllegalArgumentException {
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

    private static FullEntity parseEntityFromTwitterAccount(TwitterAccount account)
            throws IllegalArgumentException {
        FullEntity.Builder entity = Entity.newBuilder();

        if (!account.hasHandle()) {
            throw new IllegalArgumentException("Unable to create entity from proto with no handle");
        }

        entity.set(USERNAME_FIELD, account.getHandle());
        entity.set(IS_ACTIVE_FIELD, account.getActive());
        entity.set(AUTHTOKEN_FIELD, account.getAuthToken());

        if (account.hasActiveFrom()) {
            entity.set(ACTIVE_START_FIELD, Timestamp.fromProto(account.getActiveFrom()));
        }
        if (account.hasActiveUntil()) {
            entity.set(ACTIVE_END_FIELD, Timestamp.fromProto(account.getActiveUntil()));
        }
        return entity.build();
    }
}