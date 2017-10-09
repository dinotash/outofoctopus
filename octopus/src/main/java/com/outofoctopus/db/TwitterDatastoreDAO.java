package com.outofoctopus.db;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.outofoctopus.db.TwitterDAOModule.TwitterInject;
import com.outofoctopus.db.TwitterDAOModule.ProjectName;
import com.outofoctopus.proto.TwitterProtos.TwitterAccount;
import java.util.Optional;

public class TwitterDatastoreDAO implements TwitterDAO {
    private static final String KIND_STRING = "twitter";
    private static final String USERNAME_FIELD = "handle";
    private static final String IS_ACTIVE_FIELD = "active";
    private static final String ACTIVE_START_FIELD = "active_from";
    private static final String ACTIVE_END_FIELD = "active_until";
    private static final String ACCESS_TOKEN_FIELD = "access_token";
    private static final String ACCESS_TOKEN_SECRET_FIELD = "access_token_secret";

    private final String projectName;
    private final Datastore datastore;
    private final KeyFactory keyFactory;

    @Inject
    public TwitterDatastoreDAO(Datastore datastore,
       @TwitterInject KeyFactory keyFactory,
       @ProjectName String projectName) {
        this.datastore = datastore;
        this.keyFactory = keyFactory;
        this.projectName = projectName;
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

    public TwitterDAOResult insert(TwitterAccount newAccount) {
        try {
            FullEntity parsedAccount = parseEntityFromTwitterAccount(newAccount);
            datastore.add(parsedAccount);
            return TwitterDAOResult.SUCCESS;
        } catch (DatastoreException e) {
            return parseDatastoreException(e);
        } catch (IllegalArgumentException e) {
            return TwitterDAOResult.INVALID_ARGUMENT;
        }
    }

    public TwitterDAOResult update(TwitterAccount updatedAccount) {
        Key key = Key.newBuilder(projectName, KIND_STRING, updatedAccount.getHandle()).build();
        FullEntity parsedAccountFull = parseEntityFromTwitterAccount(updatedAccount);
        Entity parsedAccount = Entity.newBuilder(key, parsedAccountFull).build();
        try {
            datastore.update(parsedAccount);
            return TwitterDAOResult.SUCCESS;
        } catch (DatastoreException e) {
            return parseDatastoreException(e);
        } catch (IllegalArgumentException e) {
            return TwitterDAOResult.INVALID_ARGUMENT;
        }
    }

    public TwitterDAOResult delete(String handle) {
        try {
            datastore.delete(getKey(handle));
            return TwitterDAOResult.SUCCESS;
        } catch (DatastoreException e) {
            return parseDatastoreException(e);
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
        account.setAccessToken(entity.getString(ACCESS_TOKEN_FIELD));
        account.setAccessTokenSecret(entity.getString(ACCESS_TOKEN_SECRET_FIELD));

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
        entity.set(ACCESS_TOKEN_FIELD, account.getAccessToken());
        entity.set(ACCESS_TOKEN_SECRET_FIELD, account.getAccessTokenSecret());
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

    private TwitterDAOResult parseDatastoreException(DatastoreException e) {
        // Override for updates where I think this makes more sense
        if (e.getReason().equals("INVALID_ARGUMENT")
                && e.getMessage().equals("no entity to update")) {
            return TwitterDAOResult.NOT_FOUND;
        }

        if (e.getReason().equals("INVALID_ARGUMENT")
                && e.getMessage().equals("entity already exists")) {
            return TwitterDAOResult.ALREADY_EXISTS;
        }

        ImmutableMap<String, TwitterDAOResult> mapping =
                new ImmutableMap.Builder<String, TwitterDAOResult>()
                        .put("ABORTED", TwitterDAOResult.ERROR_CAN_RETRY)
                        .put("ALREADY_EXISTS", TwitterDAOResult.ALREADY_EXISTS)
                        .put("DEADLINE_EXCEEDED", TwitterDAOResult.ERROR_CAN_RETRY)
                        .put("FAILED_PRECONDITION", TwitterDAOResult.ERROR_DO_NOT_RETRY)
                        .put("INTERNAL", TwitterDAOResult.ERROR_CAN_RETRY)
                        .put("INVALID_ARGUMENT", TwitterDAOResult.INVALID_ARGUMENT)
                        .put("NOT_FOUND", TwitterDAOResult.NOT_FOUND)
                        .put("PERMISSION_DENIED", TwitterDAOResult.ERROR_DO_NOT_RETRY)
                        .put("RESOURCE_EXHAUSTED", TwitterDAOResult.ERROR_DO_NOT_RETRY)
                        .put("UNAUTHENTICATED", TwitterDAOResult.ERROR_DO_NOT_RETRY)
                        .put("UNAVAILABLE", TwitterDAOResult.ERROR_CAN_RETRY)
                        .build();
        if (mapping.containsKey(e.getReason())) {
            return mapping.get(e.getReason());
        } else {
            return TwitterDAOResult.UNKNOWN;
        }
    }
}