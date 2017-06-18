
package com.example.android.inventory.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * API Contract for the Inventory app.
 */
public final class InventoryContract {

    /**
     * The "Content authority" is a name for the entire content provider, similar to the
     * relationship between a domain name and its website.  A convenient string to use for the
     * content authority is the package name for the app, which is guaranteed to be unique on the
     * device.
     */


    public static final String CONTENT_AUTHORITY = "com.example.android.inventory";
    /**
     * Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
     * the content provider.
     */
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_INVENTORY = "inventory";

    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    private InventoryContract() {
    }


    public static final class InventoryEntry implements BaseColumns {

        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_INVENTORY);


        /**
         * The MIME type of the {@link #CONTENT_URI} for a list of inventory.
         */
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_INVENTORY;


        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_INVENTORY;

        /**
         * Name of database table for inventory
         */
        public final static String TABLE_NAME = "inventory";

        public final static String _ID = BaseColumns._ID;
        public final static String COLUMN_CRYPTO_NAME = "crypto";
        public final static String COLUMN_CRYPTO_CODE = "code";
        public final static String COLUMN_SUPPLIER = "supplier";
        public final static String COLUMN_INVENTORY = "inventory";
        public final static String COLUMN_SALES = "sales";
        public final static String COLUMN_PRICE = "price";
        public final static String COLMUN_PICTURE = "picture";


        // Possible values for the crypto
        public static final String CRYPTO1 = "DCRYPTO";
        public static final String CRYPTO2 = "BITCOIN";
        public static final String CRYPTO3 = "ETHERUM";
        public static final String CRYPTO4 = "RIPPLE";
        public static final String CRYPTO5 = "LITECOIN";

        // Possible values for the cryptoCode
        public static final String CRYPTOCODE1 = "DCR";
        public static final String CRYPTOCODE2 = "BTC";
        public static final String CRYPTOCODE3 = "ETH";
        public static final String CRYPTOCODE4 = "XRP";
        public static final String CRYPTOCODE5 = "LTC";



        // Possible values for the supplier of the products.
        public static final int SUPPLIER1 = 0;
        public static final int SUPPLIER2 = 1;
        public static final int SUPPLIER3 = 2;

//        public static Uri buildInventoryURI(long id) {
//            return ContentUris.withAppendedId(CONTENT_URI, id);
//        }
    }

}

