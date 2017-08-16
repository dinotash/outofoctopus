package com.outofoctopus.db

import twitter4j.Twitter

import static com.google.common.truth.Truth.assertThat

import com.google.cloud.Timestamp
import com.google.cloud.datastore.testing.LocalDatastoreHelper
import com.outofoctopus.db.TwitterDAO.TwitterDAOResult;
import com.outofoctopus.proto.TwitterProtos.TwitterAccount
import org.junit.Before
import org.junit.Test

class TwitterDatastoreDAOTest extends GroovyTestCase {

    private LocalDatastoreHelper helper = LocalDatastoreHelper.create()
    private TwitterDatastoreDAO dao = new TwitterDatastoreDAO(
            helper.getOptions().getService(), helper.getOptions().projectId)

    private static final long WAIT_MILLIS = 5000 // add a delay to tests to avoid flakiness

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
        helper.start()
        helper.reset()
        super.setUp()
    }

    @Test
    void testInsertGet() {
        // Check empty state
        assertThat(dao.getAllAccounts()).isEmpty()
        assertThat(dao.getActiveAccounts()).isEmpty()
        assertThat(dao.getAccountsToActivate()).isEmpty()
        assertThat(dao.getAccount(ACTIVE_ACCOUNT.getHandle()).isPresent()).isFalse()
        assertThat(dao.getAccount(ACCOUNT_TO_ACTIVATE.getHandle()).isPresent()).isFalse()

        // After adding one account
        TwitterDAOResult insertStatus1 = dao.insert(ACTIVE_ACCOUNT)
        Thread.sleep WAIT_MILLIS
        assertThat(insertStatus1).isEqualTo TwitterDAOResult.SUCCESS
        assertThat(dao.getAllAccounts()).containsExactly ACTIVE_ACCOUNT
        assertThat(dao.getActiveAccounts()).containsExactly ACTIVE_ACCOUNT
        assertThat(dao.getAccountsToActivate()).isEmpty()

        // After adding a second account
        TwitterDAOResult insertStatus2  = dao.insert(ACCOUNT_TO_ACTIVATE)
        Thread.sleep WAIT_MILLIS
        assertThat(insertStatus2).isEqualTo TwitterDAOResult.SUCCESS
        assertThat(dao.getAllAccounts()).containsExactly ACTIVE_ACCOUNT, ACCOUNT_TO_ACTIVATE
        assertThat(dao.getActiveAccounts().get(0)).isEqualTo ACTIVE_ACCOUNT
        assertThat(dao.getAccountsToActivate().get(0)).isEqualTo ACCOUNT_TO_ACTIVATE
        assertThat(dao.getAccount(ACTIVE_ACCOUNT.getHandle()).get()).isEqualTo ACTIVE_ACCOUNT
        assertThat(dao.getAccount(ACCOUNT_TO_ACTIVATE.getHandle()).get()).isEqualTo ACCOUNT_TO_ACTIVATE
    }

    @Test
    void testInsertDuplicate() {
        assertThat(dao.getAllAccounts()).isEmpty()
        assertThat(dao.getActiveAccounts()).isEmpty()
        TwitterDAOResult insertStatus1 = dao.insert(ACTIVE_ACCOUNT)
        TwitterDAOResult insertStatus2 = dao.insert(ACTIVE_ACCOUNT)
        Thread.sleep WAIT_MILLIS
        assertThat(insertStatus1).isEqualTo TwitterDAOResult.SUCCESS
        assertThat(insertStatus2).isEqualTo TwitterDAOResult.UNKNOWN
        assertThat(dao.getAllAccounts()).containsExactly ACTIVE_ACCOUNT
        assertThat(dao.getActiveAccounts()).containsExactly ACTIVE_ACCOUNT
    }

    @Test
    void testInsertUpdate() {
        assertThat(dao.getAllAccounts()).isEmpty()
        assertThat(dao.getActiveAccounts()).isEmpty()
        dao.insert ACTIVE_ACCOUNT
        Thread.sleep WAIT_MILLIS
        TwitterAccount updatedAccount =
                TwitterAccount
                        .newBuilder(ACTIVE_ACCOUNT)
                        .setActive(false)
                        .build()
        TwitterDAOResult updateStatus = dao.update(updatedAccount)
        assertThat(updateStatus).isEqualTo TwitterDAOResult.SUCCESS
        Thread.sleep WAIT_MILLIS
        assertThat(dao.getAllAccounts()).containsExactly updatedAccount
        assertThat(dao.getAccount(ACTIVE_ACCOUNT.getHandle()).get()).isEqualTo updatedAccount
        assertThat(dao.getActiveAccounts()).isEmpty()
    }

    @Test
    void testDelete() {
        assertThat(dao.getAllAccounts()).isEmpty()
        dao.insert ACTIVE_ACCOUNT
        dao.insert ACCOUNT_TO_ACTIVATE
        Thread.sleep WAIT_MILLIS
        assertThat(dao.getAllAccounts()).containsExactly ACTIVE_ACCOUNT, ACCOUNT_TO_ACTIVATE
        TwitterDAOResult deleteStatus = dao.delete(ACTIVE_ACCOUNT.getHandle())
        assertThat(deleteStatus).isEqualTo(TwitterDAOResult.SUCCESS)
        assertThat(dao.getAllAccounts()).containsExactly ACCOUNT_TO_ACTIVATE
    }

    @Test
    void testDeleteNonExistent() {
        assertThat(dao.getAllAccounts()).isEmpty()
        TwitterDAOResult deleteStatus = dao.delete(ACTIVE_ACCOUNT.getHandle())
        Thread.sleep WAIT_MILLIS
        assertThat(deleteStatus).isEqualTo(TwitterDAOResult.NOT_FOUND)
        assertThat(dao.getAllAccounts()).isEmpty()

    }

    @Test
    void testUpdateNonExistent() {
        assertThat(dao.getAllAccounts()).isEmpty()
        TwitterDAOResult updateStatus = dao.update(ACTIVE_ACCOUNT)
        Thread.sleep WAIT_MILLIS
        assertThat(updateStatus).isEqualTo(TwitterDAOResult.NOT_FOUND)
        assertThat(dao.getAllAccounts()).isEmpty()
    }

    @Test
    void testInsertNoHandle() {
        TwitterAccount noHandle = TwitterAccount.newBuilder(ACTIVE_ACCOUNT).clearHandle().build()
        assertThat(dao.getAllAccounts()).isEmpty()
        TwitterDAOResult insertStatus = dao.insert(noHandle)
        Thread.sleep WAIT_MILLIS
        assertThat(insertStatus).isEqualTo(TwitterDAOResult.INVALID_ARGUMENT)
        assertThat(dao.getAllAccounts()).isEmpty()
    }

    @Test
    void testInsertBlankFields() {
        String blankHandle = "test_blank"
        TwitterAccount blank = TwitterAccount.newBuilder().setHandle(blankHandle).build()
        TwitterDAOResult insertStatus = dao.insert(blank)
        assertThat(insertStatus).isEqualTo(TwitterDAOResult.SUCCESS)
        Thread.sleep WAIT_MILLIS
        TwitterAccount blankInserted = dao.getAccount(blankHandle).get()
        assertThat(blankInserted.getActive()).isFalse()
        assertThat(blankInserted.getActiveFrom()).isEqualTo Timestamp.MIN_VALUE.toProto()
        assertThat(blankInserted.getActiveUntil()).isEqualTo Timestamp.MIN_VALUE.toProto()
    }
}