package com.xabber.android.data.database;

import android.database.Cursor;
import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.realm.AccountRealm;
import com.xabber.android.data.database.realm.DiscoveryInfoCache;
import com.xabber.android.data.database.realm.EmailRealm;
import com.xabber.android.data.database.realm.SocialBindingRealm;
import com.xabber.android.data.database.realm.SyncStateRealm;
import com.xabber.android.data.database.realm.XMPPAccountSettignsRealm;
import com.xabber.android.data.database.realm.XMPPUserRealm;
import com.xabber.android.data.database.realm.XabberAccountRealm;
import com.xabber.android.data.database.sqlite.AccountTable;
import com.xabber.android.data.log.LogManager;

import io.realm.DynamicRealm;
import io.realm.FieldAttribute;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmMigration;
import io.realm.RealmSchema;
import io.realm.annotations.RealmModule;

public class RealmManager {
    private static final String REALM_DATABASE_NAME = "realm_database.realm";
    private static final int REALM_DATABASE_VERSION = 9;
    private static final String LOG_TAG = RealmManager.class.getSimpleName();
    private final RealmConfiguration realmConfiguration;

    private static RealmManager instance;

    private Realm realmUiThread;

    public static RealmManager getInstance() {
        if (instance == null) {
            instance = new RealmManager();
        }

        return instance;
    }

    private RealmManager() {
        Realm.init(Application.getInstance());
        realmConfiguration = createRealmConfiguration();

        boolean success = Realm.compactRealm(realmConfiguration);
        System.out.println("Realm compact database file result: " + success);

    }

    void deleteRealm() {
        Realm realm = getNewBackgroundRealm();
        Realm.deleteRealm(realm.getConfiguration());
        realm.close();
    }

    @RealmModule(classes = {DiscoveryInfoCache.class, AccountRealm.class, XabberAccountRealm.class,
            XMPPUserRealm.class, EmailRealm.class, SocialBindingRealm.class, XMPPAccountSettignsRealm.class, SyncStateRealm.class})
    static class RealmDatabaseModule {
    }

    private RealmConfiguration createRealmConfiguration() {
        return new RealmConfiguration.Builder()
                .name(REALM_DATABASE_NAME)
                .schemaVersion(REALM_DATABASE_VERSION)
                .migration(new RealmMigration() {
                    @Override
                    public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {
                        RealmSchema schema = realm.getSchema();

                        if (oldVersion == 2) {
                            schema.get(AccountRealm.class.getSimpleName())
                                    .setRequired(AccountRealm.Fields.ID, true);

                            oldVersion++;
                        }

                        if (oldVersion == 3) {
                            schema.get(AccountRealm.class.getSimpleName())
                                    .addField(AccountRealm.Fields.CLEAR_HISTORY_ON_EXIT, boolean.class);
                            schema.get(AccountRealm.class.getSimpleName())
                                    .addField(AccountRealm.Fields.MAM_DEFAULT_BEHAVIOR, String.class);

                            oldVersion++;
                        }

                        if (oldVersion == 4) {
                            schema.get(AccountRealm.class.getSimpleName()).
                                    addField(AccountRealm.Fields.LOAD_HISTORY_SETTINGS, String.class);

                            oldVersion++;
                        }

                        if (oldVersion == 5) {
                            schema.get(AccountRealm.class.getSimpleName())
                                    .addField(AccountRealm.Fields.SUCCESSFUL_CONNECTION_HAPPENED, boolean.class);

                            oldVersion++;
                        }

                        if (oldVersion == 6) {
                            schema.create(XMPPUserRealm.class.getSimpleName())
                                    .addField("id", String.class, FieldAttribute.PRIMARY_KEY, FieldAttribute.REQUIRED)
                                    .addField("username", String.class)
                                    .addField("host", String.class)
                                    .addField("registration_date", String.class);

                            schema.create(EmailRealm.class.getSimpleName())
                                    .addField("id", String.class, FieldAttribute.PRIMARY_KEY, FieldAttribute.REQUIRED)
                                    .addField("email", String.class)
                                    .addField("verified", boolean.class)
                                    .addField("primary", boolean.class);

                            schema.create(SocialBindingRealm.class.getSimpleName())
                                    .addField("id", String.class, FieldAttribute.PRIMARY_KEY, FieldAttribute.REQUIRED)
                                    .addField("provider", String.class)
                                    .addField("uid", String.class)
                                    .addField("firstName", String.class)
                                    .addField("lastName", String.class);

                            schema.create(XabberAccountRealm.class.getSimpleName())
                                    .addField("id", String.class, FieldAttribute.PRIMARY_KEY, FieldAttribute.REQUIRED)
                                    .addField("accountStatus", String.class)
                                    .addField("token", String.class)
                                    .addField("username", String.class)
                                    .addField("firstName", String.class)
                                    .addField("lastName", String.class)
                                    .addField("registerDate", String.class)
                                    .addRealmListField("xmppUsers", schema.get(XMPPUserRealm.class.getSimpleName()))
                                    .addRealmListField("emails", schema.get(EmailRealm.class.getSimpleName()))
                                    .addRealmListField("socialBindings", schema.get(SocialBindingRealm.class.getSimpleName()));

                            schema.create(XMPPAccountSettignsRealm.class.getSimpleName())
                                    .addField("jid", String.class, FieldAttribute.PRIMARY_KEY, FieldAttribute.REQUIRED)
                                    .addField("username", String.class)
                                    .addField("color", String.class)
                                    .addField("token", String.class)
                                    .addField("order", int.class)
                                    .addField("timestamp", int.class)
                                    .addField("synchronization", boolean.class);

                            oldVersion++;
                        }

                        if (oldVersion == 7) {
                            schema.get(AccountRealm.class.getSimpleName())
                                    .addField("token", String.class);

                            oldVersion++;
                        }

                        if (oldVersion == 8) {
                            schema.get(AccountRealm.class.getSimpleName())
                                    .addField("order", Integer.class)
                                    .addField("timestamp", Integer.class)
                                    .addField("syncNotAllowed", boolean.class);

                            schema.create(SyncStateRealm.class.getSimpleName())
                                    .addField("id", String.class, FieldAttribute.PRIMARY_KEY, FieldAttribute.REQUIRED)
                                    .addField("jid", String.class)
                                    .addField("sync", boolean.class);

                            oldVersion++;
                        }
                    }
                })
                .modules(new RealmDatabaseModule())
                .build();
    }

    /**
     * Creates new realm instance for use from background thread.
     * Realm should be closed after use.
     *
     * @return new realm instance
     * @throws IllegalStateException if called from UI (main) thread
     */
    public Realm getNewBackgroundRealm() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("Request background thread message realm from UI thread");
        }

        return getNewRealm();
    }

    /**
     * Creates new realm instance for use from any thread. Better to avoid this method.
     * Realm should be closed after use.
     *
     * @return new realm instance
     */
    public Realm getNewRealm() {
        return Realm.getInstance(realmConfiguration);
    }

    /**
     * Returns realm instance for use from UI (main) thread.
     * Do not close realm after use!
     *
     * @return realm instance for UI thread
     * @throws IllegalStateException if called from background thread
     */
    public Realm getRealmUiThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("Request UI thread message realm from non UI thread");
        }

        if (realmUiThread == null) {
            realmUiThread = Realm.getInstance(realmConfiguration);
        }

        return realmUiThread;
    }


    void copyDataFromSqliteToRealm() {
        Realm realm = getNewBackgroundRealm();

        realm.beginTransaction();

        LogManager.i(LOG_TAG, "copying from SQLite to Realm");
        long counter = 0;
        Cursor cursor = AccountTable.getInstance().list();
        while (cursor.moveToNext()) {
            AccountRealm accountRealm = AccountTable.createAccountRealm(cursor);
            realm.copyToRealm(accountRealm);

            counter++;
        }
        cursor.close();
        LogManager.i(LOG_TAG, counter + " accounts copied to Realm");

        LogManager.i(LOG_TAG, "onSuccess. removing accounts from SQLite:");
        int removedAccounts = AccountTable.getInstance().removeAllAccounts();
        LogManager.i(LOG_TAG, removedAccounts + " accounts removed from SQLite");

        realm.commitTransaction();
        realm.close();
    }
}
