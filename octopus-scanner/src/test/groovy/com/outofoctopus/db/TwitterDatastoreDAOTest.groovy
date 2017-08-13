package com.outofoctopus.db

import com.google.cloud.Timestamp
import com.google.cloud.datastore.TimestampValue
import com.google.cloud.datastore.testing.LocalDatastoreHelper
import com.google.protobuf.util.Timestamps
import com.outofoctopus.proto.TwitterProtos.TwitterAccount
import org.joda.time.Seconds
import org.junit.After
import org.junit.Before
import org.junit.Test;
import org.threeten.bp.Duration
import org.threeten.bp.temporal.ChronoUnit

import static com.google.common.truth.Truth.assertThat;


class TwitterDatastoreDAOTest extends GroovyTestCase {

    private LocalDatastoreHelper helper = LocalDatastoreHelper.create()
    private TwitterDatastoreDAO dao = new TwitterDatastoreDAO(
            helper.getOptions().getService(), helper.getOptions().projectId)

    private static final ACTIVE_ACCOUNT =
            TwitterAccount.newBuilder()
                    .setActive(true)
                    .setHandle("test_active")
                    .setActiveFrom(Timestamp.now().toProto())
                    .setActiveUntil(Timestamp.now().toProto())
                    .setAuthToken("abc")
                    .build()

    private static final ACCOUNT_TO_ACTIVATE =
            TwitterAccount.newBuilder()
                    .setActive(false)
                    .setHandle("test_to_activate")
                    .setActiveFrom(Timestamp.MIN_VALUE.toProto())
                    .setActiveUntil(com.google.protobuf.Timestamp.newBuilder().setSeconds(Timestamp.MAX_VALUE.getSeconds()).build())
                    .setAuthToken("def")
                    .build()

    @Before
    void setUp() {
        super.setUp()
        helper.start()
    }

    @After
    void tearDown() {
        super.tearDown()
        helper.reset()
    }

    @Test
    void testInsertGet() {
        assertThat(dao.getActiveAccounts()).hasSize(0)
        assertThat(dao.getAccountsToActivate()).hasSize(0)
        dao.insert(ACTIVE_ACCOUNT)
        assertThat(dao.getActiveAccounts()).hasSize(1)
        assertThat(dao.getAccountsToActivate()).hasSize(0)
        dao.insert(ACCOUNT_TO_ACTIVATE)
        assertThat(dao.getActiveAccounts()).hasSize(1)
        assertThat(dao.getActiveAccounts().get(0)).isEqualTo(ACTIVE_ACCOUNT)
        assertThat(dao.getAccountsToActivate()).hasSize(1)
        assertThat(dao.getAccountsToActivate().get(0)).isEqualTo((ACCOUNT_TO_ACTIVATE))
    }
}
