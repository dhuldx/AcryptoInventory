
package com.example.android.inventory;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.android.inventory.data.InventoryContract.InventoryEntry;

import static android.content.ContentValues.TAG;
import static com.example.android.inventory.R.drawable.ic_insert_placeholder;

/**
 * {@link InventoryCursorAdapter} is an adapter for a list or grid view
 * that uses a {@link Cursor} of inventory data as its data source. This adapter knows
 * how to create list items for each row of inventory data in the {@link Cursor}.
 */
public class InventoryCursorAdapter extends CursorAdapter {


    private Context mContext;

    /**
     * Constructs a new {@link InventoryCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    public InventoryCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Inflate a list item view using the layout specified in list_item.xml
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * This method binds the inventory data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current inventory can be set on the name TextView
     * in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        // Find individual views that we want to modify in the list item layout
        TextView nameTextView = (TextView) view.findViewById(R.id.name);
        TextView codeTextView = (TextView) view.findViewById(R.id.code);
        TextView inventoryTextView = (TextView) view.findViewById(R.id.inventory);
        TextView priceTextView = (TextView) view.findViewById(R.id.price);
        TextView sellTextView = (TextView) view.findViewById(R.id.sold);
        ImageView picView = (ImageView) view.findViewById(R.id.image);
        ImageView sellCrypto = (ImageView) view.findViewById(R.id.sale_product);

        // Find the columns of inventory attributes that we're interested in
        int nameColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_CRYPTO_NAME);
        int codeColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_CRYPTO_CODE);
        int inventoryColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY);
        int priceColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_PRICE);
        int sellColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_SALES);
        int picColumnIndex = cursor.getColumnIndex(InventoryEntry.COLMUN_PICTURE);

        // Read the inventory attributes from the Cursor for the current inventory
        int id = cursor.getInt(cursor.getColumnIndex(InventoryEntry._ID));
        final String inventoryName = cursor.getString(nameColumnIndex);
        String inventoryCode = cursor.getString(codeColumnIndex);
        final int inventoryBal = cursor.getInt(inventoryColumnIndex);
        String inventoryPrice = "$" + cursor.getString(priceColumnIndex);
        final int inventorySold = cursor.getInt(sellColumnIndex);
        Uri picUri = Uri.parse(cursor.getString(picColumnIndex));
        // If the inventory breed is empty string or null, then use some default text
        // that says "Unknown breed", so the TextView isn't blank.
        if (TextUtils.isEmpty(inventoryCode)) {
            inventoryCode = context.getString(R.string.unknown_code);
        }


        String cryptoQuantity = String.valueOf(inventoryBal) + " SOH";
        String cryptoSold = String.valueOf(inventorySold) + " Sold";
        // Update the TextViews with the attributes for the current inventory
        nameTextView.setText(inventoryName);
        codeTextView.setText(inventoryCode);
        inventoryTextView.setText(cryptoQuantity);
        priceTextView.setText(inventoryPrice);
        sellTextView.setText(cryptoSold);
        final Uri currentProductUri = ContentUris.withAppendedId(InventoryEntry.CONTENT_URI, id);

        Log.d(TAG, "genero Uri: " + currentProductUri + " Product name: " + inventoryName + " id: " + id);

//We use Glide to import photo images
        Glide.with(context).load(picUri)
                .placeholder(R.mipmap.ic_launcher)
                .error(ic_insert_placeholder)
                .crossFade()
                .centerCrop()
                .into(picView);


        sellCrypto.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Log.d(TAG, inventoryName + " quantity= " + inventoryBal);
                ContentResolver resolver = view.getContext().getContentResolver();
                ContentValues values = new ContentValues();
                if (inventoryBal > 0) {
                    int bal = inventoryBal;
                    int sold = inventorySold;
                    Log.d(TAG, "new quabtity= " + bal);
                    values.put(InventoryEntry.COLUMN_INVENTORY, --bal);
                    values.put(InventoryEntry.COLUMN_SALES, ++sold);
                    resolver.update(
                            currentProductUri,
                            values,
                            null,
                            null
                    );
                    context.getContentResolver().notifyChange(currentProductUri, null);
                } else {
                    Toast.makeText(context, "Item out of stock", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

}
