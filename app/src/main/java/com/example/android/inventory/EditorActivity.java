/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.inventory;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.android.inventory.data.InventoryContract.InventoryEntry;

import java.text.NumberFormat;

import static com.example.android.inventory.data.InventoryContract.InventoryEntry.COLUMN_CRYPTO_NAME;
import static com.example.android.inventory.data.InventoryContract.InventoryEntry.CRYPTO2;
import static com.example.android.inventory.data.InventoryContract.InventoryEntry.CRYPTOCODE1;
import static com.example.android.inventory.data.InventoryContract.InventoryEntry.CRYPTOCODE2;
import static com.example.android.inventory.data.InventoryContract.InventoryEntry.CRYPTOCODE3;
import static com.example.android.inventory.data.InventoryContract.InventoryEntry.CRYPTOCODE4;
import static com.example.android.inventory.data.InventoryContract.InventoryEntry.CRYPTOCODE5;

/**
 * Allows user to create a new inventory or edit an existing one.
 */
public class EditorActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Identifier for the inventory data loader
     */
    public static final String TAG = EditorActivity.class.getSimpleName();
    public static final int PICK_PHOTO_REQUEST = 20;
    public static final int EXTERNAL_STORAGE_REQUEST_PERMISSION_CODE = 21;
    private static final int EXISTING_INVENTORY_LOADER = 0;
    /**
     * Content URI for the existing inventory (null if it's a new inventory)
     */
    private Uri mCurrentCryptoUri;

    /**
     * EditText field to enter the inventory's name
     */
    private Spinner mCryptoSpinner;
    private ImageView mCryptoIcon;

    private Spinner mSupplierSpinner;
    private EditText mInventory;
    private EditText mCryptoPrice;
    private EditText mCryptoSold;

    private ImageButton cDelete;
    private ImageButton cOrder;
    private ImageButton cUpdate;

    private String mCurrentPhotoUri = "no images";
    private String mOrderProduct;
    private String mOrderEmail;
    private int mOrderQuantity;


    /**
     * Supplier of the inventory. The possible valid values are in the InventoryContract.java file:
     * {@link InventoryEntry#SUPPLIER1}, {@link InventoryEntry#SUPPLIER2}, or
     * {@link InventoryEntry#SUPPLIER3}.
     */
    private int mSupplier;

    private String mCrypto;
    private String mCryptoCode;
    private Image mCryptoLogo;


    /**
     * Boolean flag that keeps track of whether the inventory has been edited (true) or not (false)
     */
    private boolean mInventoryHasChanged = false;

    /**
     * OnTouchListener that listens for any user touches on a View, implying that they are modifying
     * the view, and we change the mInventoryHasChanged boolean to true.
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mInventoryHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        //Cast UI
        mCryptoSpinner = (Spinner) findViewById(R.id.spinner_crypto);
        mSupplierSpinner = (Spinner) findViewById(R.id.spinner_supplier);
        mInventory = (EditText) findViewById(R.id.edit_inventory);
        mCryptoPrice = (EditText) findViewById(R.id.edit_inventory_price);
        mCryptoSold = (EditText) findViewById(R.id.edit_product_sale);

        //monitor activity so we can protect user
        mCryptoSpinner.setOnTouchListener(mTouchListener);
        mInventory.setOnTouchListener(mTouchListener);
        mCryptoPrice.setOnTouchListener(mTouchListener);
        mCryptoSold.setOnTouchListener(mTouchListener);
        mSupplierSpinner.setOnTouchListener(mTouchListener);

        setSpinner();
        setCryptoSpinner();
        //setSupplierSpinner();

        //Cast ActionButtons
        cDelete = (ImageButton) findViewById(R.id.delete);
        cOrder = (ImageButton) findViewById(R.id.order);
        cUpdate = (ImageButton) findViewById(R.id.save);

        cUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveInventory();
                finish();
            }
        });
        cDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDeleteConfirmationDialog();
            }
        });

        cOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                orderSupplier();
            }
        });

        // Examine the intent that was used to launch this activity,
        // in order to figure out if we're creating a new inventory or editing an existing one.
        Intent intent = getIntent();
        mCurrentCryptoUri = intent.getData();

        // If the intent DOES NOT contain a inventory content URI, then we know that we are
        // creating a new inventory.
        if (mCurrentCryptoUri == null) {
            // This is a new inventory, so change the app bar to say "Add a Inventory"
            setTitle(getString(R.string.editor_activity_title_new_inventory));
            cOrder.setVisibility(View.GONE);
            cDelete.setVisibility(View.GONE);

            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a inventory that hasn't been created yet.)
            invalidateOptionsMenu();
        } else {
            // Otherwise this is an existing inventory, so change app bar to say "Edit Inventory"
            setTitle(getString(R.string.editor_activity_title_edit_inventory));
            cOrder.setVisibility(View.VISIBLE);
            cDelete.setVisibility(View.VISIBLE);
            // Initialize a loader to read the inventory data from the database
            // and display the current values in the editor
            getLoaderManager().initLoader(EXISTING_INVENTORY_LOADER, null, this);
        }
    }


    private void setCryptoSpinner() {
        // Create adapter for spinner. The list options are from the String array it will use
        // the spinner will use the default layout
        ArrayAdapter cryptoSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.array_crypto_options, android.R.layout.simple_spinner_item);

        cryptoSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);


        // Apply the adapter to the spinner
        mCryptoSpinner.setAdapter(cryptoSpinnerAdapter);

        // Set the integer mSelected to the constant values
        mCryptoSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String provide = (String) parent.getItemAtPosition(position);
                if (!TextUtils.isEmpty(provide)) {
                    if (provide.equals(getString(R.string.crypto2))) {
                        mCrypto = CRYPTO2;
                        mCryptoCode = CRYPTOCODE2;

                    } else if (provide.equals(getString(R.string.crypto3))) {
                        mCrypto = InventoryEntry.CRYPTO3;
                        mCryptoCode = CRYPTOCODE3;
                    } else if (provide.equals(getString(R.string.crypto4))) {
                        mCrypto = InventoryEntry.CRYPTO4;
                        mCryptoCode = CRYPTOCODE4;
                    } else if (provide.equals(getString(R.string.crypto5))) {
                        mCrypto = InventoryEntry.CRYPTO5;
                        mCryptoCode = CRYPTOCODE5;
                    } else {
                        mCrypto = InventoryEntry.CRYPTO1;
                        mCryptoCode = CRYPTOCODE1;
                    }
                }
            }

            // Because AdapterView is an abstract class, onNothingSelected must be defined
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mCrypto = InventoryEntry.CRYPTO1;
                mCryptoCode = CRYPTOCODE1;
            }
        });

    }

    /**
     * Setup the dropdown spinner that allows the user to select the gender of the inventory.
     */
    private void setSpinner() {
        // Create adapter for spinner. The list options are from the String array it will use
        // the spinner will use the default layout
        ArrayAdapter supplierSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.array_supplier_options, android.R.layout.simple_spinner_item);


        // Specify dropdown layout style - simple list view with 1 item per line
        supplierSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);

        // Apply the adapter to the spinner
        mSupplierSpinner.setAdapter(supplierSpinnerAdapter);
        // Set the integer mSelected to the constant values
        mSupplierSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = (String) parent.getItemAtPosition(position);
                if (!TextUtils.isEmpty(selection)) {
                    if (selection.equals(getString(R.string.supplier2))) {
                        mSupplier = InventoryEntry.SUPPLIER2;
                    } else if (selection.equals(getString(R.string.supplier3))) {
                        mSupplier = InventoryEntry.SUPPLIER3;
                    } else {
                        mSupplier = InventoryEntry.SUPPLIER1;
                    }
                }
            }

            // Because AdapterView is an abstract class, onNothingSelected must be defined
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mSupplier = InventoryEntry.SUPPLIER1;
            }
        });

    }

    /**
     * Get user input from editor and save inventory into database.
     */
    private void saveInventory() {
        //Read Values from text field
        String nameString = mCrypto;
        String codeString = mCryptoCode;
        Image logoPic = mCryptoLogo;
        String inventoryString = mInventory.getText().toString().trim();
        String salesString = mCryptoSold.getText().toString().trim();
        String priceString = mCryptoPrice.getText().toString().trim();


        //Check if is new or if an update
        if (mCurrentCryptoUri == null && TextUtils.isEmpty(nameString)
                || TextUtils.isEmpty(inventoryString) || TextUtils.isEmpty(salesString) || TextUtils.isEmpty(priceString)) {

            Toast.makeText(this, R.string.err_missing_textfields, Toast.LENGTH_SHORT).show();
            // No change has been made so we can return
            return;
        }

        //We set values for insert update
        ContentValues values = new ContentValues();

        values.put(InventoryEntry.COLUMN_CRYPTO_NAME, mCrypto);
        values.put(InventoryEntry.COLUMN_CRYPTO_CODE, codeString);
        values.put(InventoryEntry.COLUMN_INVENTORY, inventoryString);
        values.put(InventoryEntry.COLUMN_SALES, salesString);
        values.put(InventoryEntry.COLUMN_PRICE, priceString);
        values.put(InventoryEntry.COLUMN_SUPPLIER, mSupplier);
        values.put(InventoryEntry.COLMUN_PICTURE, String.valueOf(mCryptoLogo));

        if (mCurrentCryptoUri == null) {

            Uri insertedRow = getContentResolver().insert(InventoryEntry.CONTENT_URI, values);

            if (insertedRow == null) {
                Toast.makeText(this, R.string.editor_insert_inventory_failed, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, R.string.editor_insert_inventory_successful, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, CatalogActivity.class);
                startActivity(intent);
            }
        } else {
            // We are Updating
            int rowUpdated = getContentResolver().update(mCurrentCryptoUri, values, null, null);

            if (rowUpdated == 0) {
                Toast.makeText(this, R.string.editor_update_inventory_failed, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, R.string.editor_update_inventory_successful, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, CatalogActivity.class);
                startActivity(intent);

            }

        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    /**
     * This method is called after invalidateOptionsMenu(), so that the
     * menu can be updated (some menu items can be hidden or made visible).
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new inventory, hide the "Delete" menu item.
        if (mCurrentCryptoUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save inventory to database
                saveInventory();
                // Exit activity
                finish();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Pop up confirmation dialog for deletion
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the inventory hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!mInventoryHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

   /* public void onPhotoUpdate(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //We are on M or above so we need to ask for runtime permissions
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                invokeGetPhoto();
            } else {
                // we are here if we do not all ready have permissions
                String[] permisionRequest = {Manifest.permission.READ_EXTERNAL_STORAGE};
                requestPermissions(permisionRequest, EXTERNAL_STORAGE_REQUEST_PERMISSION_CODE);
            }
        } else {
            //We are on an older devices so we dont have to ask for runtime permissions
            invokeGetPhoto();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == EXTERNAL_STORAGE_REQUEST_PERMISSION_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //We got a GO from the user
            invokeGetPhoto();
        } else {
            Toast.makeText(this, R.string.err_external_storage_permissions, Toast.LENGTH_LONG).show();
        }
    }

    private void invokeGetPhoto() {
        // invoke the image gallery using an implict intent.
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);

        // where do we want to find the data?
        File pictureDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        String pictureDirectoryPath = pictureDirectory.getPath();
        // finally, get a URI representation
        Uri data = Uri.parse(pictureDirectoryPath);

        // set the data and type.  Get all image types.
        photoPickerIntent.setDataAndType(data, "image*//*");

        // we will invoke this activity, and get something back from it.
        startActivityForResult(photoPickerIntent, PICK_PHOTO_REQUEST);
    }
*/

    /**
     * This method is called when the back button is pressed.
     */
/*
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_PHOTO_REQUEST && resultCode == RESULT_OK) {
            if (data != null) {
                //If we are here, everything processed successfully and we have an Uri data
                Uri mProductPhotoUri = data.getData();
                mCurrentPhotoUri = mProductPhotoUri.toString();
                //  Log.d(TAG, "Selected images " + mProductPhotoUri);

                //We use Glide to import photo images
                Glide.with(this).load(mCurrentPhotoUri)
                        .placeholder(ic_default_home)
                        .crossFade()
                        .fitCenter()
                        .into(mCryptoIcon);
            }
        }
    }
*/
    @Override
    public void onBackPressed() {
        // If the inventory hasn't changed, continue with handling back button press
        if (!mInventoryHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Since the editor shows all inventory attributes, define a projection that contains
        // all columns from the inventory table
        String[] projection = {
                InventoryEntry._ID,
                InventoryEntry.COLUMN_CRYPTO_NAME,
                InventoryEntry.COLUMN_CRYPTO_CODE,
                InventoryEntry.COLUMN_SUPPLIER,
                InventoryEntry.COLUMN_INVENTORY,
                InventoryEntry.COLUMN_PRICE,
                InventoryEntry.COLUMN_SALES,
                InventoryEntry.COLMUN_PICTURE
        };

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                mCurrentCryptoUri,         // Query the content URI for the current inventory
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {

            int nameColumnIndex = cursor.getColumnIndex(COLUMN_CRYPTO_NAME);
            int supplierColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_SUPPLIER);
            int inventoryColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY);
            int priceColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_PRICE);
            int sellColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_SALES);
            //    int picColumnIndex = cursor.getColumnIndex(InventoryEntry.COLMUN_PICTURE);

            // Extract out the value from the Cursor for the given column index
            String crypto = cursor.getString(nameColumnIndex);
            //   String code = cursor.getString(codeColumnIndex);
            int supplier = cursor.getInt(supplierColumnIndex);
            double inventory = cursor.getDouble(inventoryColumnIndex);
            double price = cursor.getDouble(priceColumnIndex);
            double sell = cursor.getDouble(sellColumnIndex);
            //    String photo = cursor.getString(picColumnIndex);
            //   mCurrentPhotoUri = cursor.getString(picColumnIndex);
            //Update photo using Glide

            // Extract out the value from the Cursor for the given column index

       /*     mCurrentPhotoUri = cursor.getString(picColumnIndex);
            //Update photo using Glide
            Glide.with(this).load(mCurrentPhotoUri)
                    .placeholder(R.mipmap.ic_launcher)
                    .error(ic_insert_placeholder)
                    .crossFade()
                    .fitCenter()
                    .into(mCryptoIcon);*/
            // Update the views on the screen with the values from the database
            // mCryptoName.setText(name);
            //   mCryptoCode.setText(code);
            mInventory.setText(String.valueOf(inventory));
            mCryptoPrice.setText(String.valueOf(price));
            NumberFormat nm = NumberFormat.getNumberInstance();
            mCryptoSold.setText(String.valueOf(sell));
            //mCryptoSold.setText(nm.format(sell));
            // mCryptoSold.setText(String.valueOf(sell));

            // Gender is a dropdown spinner, so map the constant value from the database
            // into one of the dropdown options (0 is Unknown, 1 is Male, 2 is Female).
            // Then call setSelection() so that option is displayed on screen as the current selection.
            switch (supplier) {
                case InventoryEntry.SUPPLIER2:
                    mSupplierSpinner.setSelection(1);
                    break;
                case InventoryEntry.SUPPLIER3:
                    mSupplierSpinner.setSelection(2);
                    break;
                default:
                    mSupplierSpinner.setSelection(0);
                    break;
            }
            switch (crypto) {
                case InventoryEntry.CRYPTO2:
                    mCryptoSpinner.setSelection(1);

                    break;
                case InventoryEntry.CRYPTO3:
                    mCryptoSpinner.setSelection(2);
                    break;
                case InventoryEntry.CRYPTO4:
                    mCryptoSpinner.setSelection(3);
                    break;
                case InventoryEntry.CRYPTO5:
                    mCryptoSpinner.setSelection(4);
                    break;
                default:
                    mSupplierSpinner.setSelection(0);
                    break;
            }

            mOrderProduct = crypto;
            mOrderQuantity = 50;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        mCryptoSpinner.setSelection(0);
        //   mCryptoCode.setText("");
        mInventory.setText("");
        mCryptoPrice.setText("");
        mCryptoSold.setText("");
        mSupplierSpinner.setSelection(0);
    }

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when
     *                                   the user confirms they want to discard their changes
     */
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the inventory.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Prompt the user to confirm that they want to delete this inventory.
     */
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the inventory.
                deleteInventory();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the inventory.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the inventory in the database.
     */
    private void deleteInventory() {
        // Only perform the delete if this is an existing inventory.
        if (mCurrentCryptoUri != null) {
            // Call the ContentResolver to delete the inventory at the given content URI.
            // Pass in null for the selection and selection args because the mCurrentInventoryUri
            // content URI already identifies the inventory that we want.
            int rowsDeleted = getContentResolver().delete(mCurrentCryptoUri, null, null);

            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, getString(R.string.editor_delete_inventory_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_delete_inventory_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }

        // Close the activity
        // Close the activity
        finish();
    }

    //Order from supplier
    private void orderSupplier() {


        Intent email = new Intent(Intent.ACTION_SEND);
        email.putExtra(Intent.EXTRA_EMAIL, new String[]{"btc.support@gmail.com"});
        email.putExtra(Intent.EXTRA_SUBJECT, "Order " + mOrderProduct);
        email.putExtra(Intent.EXTRA_TEXT, "Please ship " + mOrderProduct +
                " in quantities " + mOrderQuantity);
        email.setType("message/rfc822");
        try {
            startActivity(Intent.createChooser(email, "Choose an Email client :"));
        } catch (android.content.ActivityNotFoundException ex) {
            ex.printStackTrace();
        }


    }
}
